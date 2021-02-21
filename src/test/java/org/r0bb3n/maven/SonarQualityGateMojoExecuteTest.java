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
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
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
import org.r0bb3n.maven.util.ExceptionMatchers;
import org.r0bb3n.maven.util.LogFacade;

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
    SonarQualityGateMojo sonarQualityGateMojo = new SonarQualityGateMojo();
    setField(sonarQualityGateMojo, "sonarHostUrl", new URL(wireMockClassRule.baseUrl()));
    setField(sonarQualityGateMojo, "checkTaskAttempts", 10);
    setField(sonarQualityGateMojo, "checkTaskIntervalS", 1);
    underTestSpy = Mockito.spy(sonarQualityGateMojo);
    logSpy = Mockito.spy(new LogFacade(log));
    Mockito.when(underTestSpy.getLog()).thenReturn(logSpy);
  }

  @Test
  public void mojoExecuteWithTwoTaskCallsAndOneAnalysisCallOk() throws Exception {
    Mockito.doAnswer(
        invocation -> Optional.of("mojoExecuteWithTwoTaskCallsAndOneAnalysisCallOk_taskId"))
        .when(underTestSpy).findCeTaskId(Mockito.any());

    underTestSpy.execute();
    // assert is difficult - let's check, if a final positive log gets written
    Mockito.verify(logSpy, Mockito.times(1)).info("project status: OK");
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

  private void setField(SonarQualityGateMojo mojo, String fieldName, Object value)
      throws Exception {
    // using @Parameter annotation not possible, since not available during runtime
    Field field = SonarQualityGateMojo.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(mojo, value);
    field.setAccessible(false);
  }
}
