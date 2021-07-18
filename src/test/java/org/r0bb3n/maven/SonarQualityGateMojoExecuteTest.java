/*
 * Copyright 2021 r0bb3n
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.r0bb3n.maven;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.hamcrest.MockitoHamcrest;
import org.r0bb3n.maven.util.ExceptionMatchers;
import org.r0bb3n.maven.util.LogFacade;
import org.r0bb3n.maven.util.MojoConfigurator;

/**
 * test cases for {@link SonarQualityGateMojo}
 */
@Log4j2
public class SonarQualityGateMojoExecuteTest {

  // use one wiremock instance for entire test class
  @ClassRule
  public static WireMockClassRule wireMockClassRule = new WireMockClassRule(
      WireMockConfiguration.options().dynamicPort().usingFilesUnderDirectory(
          "src/test/resources/wiremock/" + SonarQualityGateMojoExecuteTest.class.getSimpleName()));

  @Rule
  public WireMockClassRule wireMockRule = wireMockClassRule;

  private SonarQualityGateMojo underTestSpy;
  private Log logSpy;

  /**
   * setup testable mojo
   */
  @Before
  public void setUp() throws Exception {
    SonarQualityGateMojo underTest = new SonarQualityGateMojo();
    MojoConfigurator.configure(underTest).applyDefaults()
        .setSonarHostUrl(wireMockClassRule.baseUrl())
        .setSonarProjectKey("io.github.r0bb3n:sonar-quality-gate-maven-plugin")
        .setCheckTaskAttempts(10).setCheckTaskIntervalS(1);
    underTestSpy = Mockito.spy(underTest);
    logSpy = Mockito.spy(new LogFacade(log));
    Mockito.when(underTestSpy.getLog()).thenReturn(logSpy);
  }

  @Test
  public void mojoExecuteWithTwoTaskCallsAndOneAnalysisCallOk() throws Exception {
    Mockito.doAnswer(
        invocation -> Optional.of("mojoExecuteWithTwoTaskCallsAndOneAnalysisCallOk_taskId"))
        .when(underTestSpy).findCeTaskId(Mockito.any());

    underTestSpy.execute();
    // check for done retry
    Mockito.verify(logSpy).info(Mockito.startsWith("Analysis in progress, next retry in"));
    // assert is difficult - let's check, if a final positive log gets written
    Mockito.verify(logSpy).info("project status: OK");
  }

  @Test
  public void mojoExecuteWithAbortAfterOneTaskCall() throws Exception {
    MojoConfigurator.configure(underTestSpy).setCheckTaskAttempts(1);
    Mockito.doAnswer(
        invocation -> Optional.of("mojoExecuteWithTwoTaskCallsAndOneAnalysisCallOk_taskId"))
        .when(underTestSpy).findCeTaskId(Mockito.any());

    MojoExecutionException exc =
        Assert.assertThrows(MojoExecutionException.class, underTestSpy::execute);
    MatcherAssert.assertThat(exc,
        ExceptionMatchers.hasMessageThat(Matchers.startsWith("Could not fetch analysis id")));
  }

  @Test
  public void mojoExecuteWithOneTaskCallAndOneAnalysisCallError() throws Exception {
    Mockito.doAnswer(
        invocation -> Optional.of("mojoExecuteWithOneTaskCallAndOneAnalysisCallError_taskId"))
        .when(underTestSpy).findCeTaskId(Mockito.any());

    MojoFailureException exc =
        Assert.assertThrows(MojoFailureException.class, underTestSpy::execute);
    MatcherAssert.assertThat(exc,
        ExceptionMatchers.hasMessageThat(Matchers.startsWith("Quality Gate not passed")));
  }

  @Test
  public void mojoExecuteWithProjectKeyOk() throws Exception {
    Mockito.doAnswer(invocation -> Optional.empty()).when(underTestSpy).findCeTaskId(Mockito.any());

    underTestSpy.execute();

    Mockito.verify(underTestSpy, Mockito.never()).retrieveAnalysisId(Mockito.any(), Mockito.any());
    // assert is difficult - let's check, if a final positive log gets written
    Mockito.verify(logSpy).info("project status: OK");
  }

  @Test
  public void mojoExecuteWithProjectKeyWarnNoFailOnMiss() throws Exception {
    MojoConfigurator.configure(underTestSpy).setFailOnMiss(false)
        .setSonarProjectKey("io.github.r0bb3n:sonar-quality-gate-maven-plugin_WARN");
    Mockito.doAnswer(invocation -> Optional.empty()).when(underTestSpy).findCeTaskId(Mockito.any());

    underTestSpy.execute();

    Mockito.verify(logSpy).warn(
        MockitoHamcrest.argThat(Matchers.startsWith("Quality Gate not passed (status: WARN)!")));

    Mockito.verify(underTestSpy, Mockito.never()).retrieveAnalysisId(Mockito.any(), Mockito.any());
  }

  @Test
  public void mojoExecuteWithProjectKeyAndBranchNone() throws Exception {
    MojoConfigurator.configure(underTestSpy).setBranch("feature/GH-31_increase-test-coverage");
    Mockito.doAnswer(invocation -> Optional.empty()).when(underTestSpy).findCeTaskId(Mockito.any());

    MojoFailureException exc =
        Assert.assertThrows(MojoFailureException.class, underTestSpy::execute);
    MatcherAssert.assertThat(exc, ExceptionMatchers
        .hasMessageThat(Matchers.startsWith("Quality Gate not passed (status: NONE)!")));

    Mockito.verify(underTestSpy, Mockito.never()).retrieveAnalysisId(Mockito.any(), Mockito.any());
  }

  @Test
  public void mojoExecuteWithProjectKeyAndPullRequestWarn() throws Exception {
    MojoConfigurator.configure(underTestSpy).setPullRequest("59");
    Mockito.doAnswer(invocation -> Optional.empty()).when(underTestSpy).findCeTaskId(Mockito.any());

    MojoFailureException exc =
        Assert.assertThrows(MojoFailureException.class, underTestSpy::execute);
    MatcherAssert.assertThat(exc, ExceptionMatchers
        .hasMessageThat(Matchers.startsWith("Quality Gate not passed (status: WARN)!")));

    Mockito.verify(underTestSpy, Mockito.never()).retrieveAnalysisId(Mockito.any(), Mockito.any());
  }

  @Test
  public void mojoExecuteWithSkipEnabled() throws Exception {
    MojoConfigurator.configure(underTestSpy).setSkip(true);

    underTestSpy.execute();
    // assert is difficult - let's check, if a log gets written for skipping
    Mockito.verify(logSpy).info("skipped");
  }

}
