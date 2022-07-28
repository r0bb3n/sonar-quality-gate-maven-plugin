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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.r0bb3n.maven.model.ProjectStatus;
import org.r0bb3n.maven.util.LogFacade;

/**
 * Test cases for sonar server communication
 */
@Log4j2
@RequiredArgsConstructor
@RunWith(Parameterized.class)
public class SonarConnectorProjectStatusTest {

  /**
   * create test data
   */
  @Parameterized.Parameters(name = "branch: {0} pr: {1} conditions-size: {2}")
  public static Iterable<Object[]> generateTestData() {
    List<Object[]> ret = new ArrayList<>(3);
    ret.add(new Object[]{null, null, 9});
    ret.add(new Object[]{"feature/GH-31_increase-test-coverage", null, 5});
    ret.add(new Object[]{null, "58", 5});
    return ret;
  }

  // use one wiremock instance for entire test class
  @ClassRule
  public static WireMockClassRule wireMockClassRule = new WireMockClassRule(
      WireMockConfiguration.options().dynamicPort().usingFilesUnderDirectory(
          "src/test/resources/wiremock/" + SonarConnectorProjectStatusTest.class.getSimpleName()));

  @Rule
  public WireMockClassRule wireMockRule = wireMockClassRule;

  private final String branch;
  private final String pullRequest;
  private final int conditionsSize;

  private SonarConnector underTest;

  /**
   * create instance to test
   */
  @Before
  public void setUp() throws Exception {
    underTest =
        new SonarConnector(new LogFacade(log), new URL("http://localhost:" + wireMockRule.port()),
            "io.github.r0bb3n:sonar-quality-gate-maven-plugin", null, null);
  }

  /**
   * Test requesting and parsing of project status response based on different information
   */
  @Test
  public void retrieveProjectStatus() throws Exception {
    ProjectStatus projectStatus = underTest.retrieveProjectStatus(branch, pullRequest);
    assertNotNull("no projectStatus found", projectStatus);
    assertEquals("Unexpected status value", ProjectStatus.Status.OK, projectStatus.getStatus());
    assertEquals("Unexpected conditions count", conditionsSize,
        projectStatus.getConditions().size());
    log.trace(projectStatus);
  }

}
