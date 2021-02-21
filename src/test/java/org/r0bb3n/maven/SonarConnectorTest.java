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
import lombok.extern.log4j.Log4j2;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.r0bb3n.maven.model.ProjectStatus;
import org.r0bb3n.maven.model.Task;
import org.r0bb3n.maven.util.LogFacade;

/**
 * Test cases for sonar server communication
 */
@Log4j2
public class SonarConnectorTest {

  // use one wiremock instance for entire test class
  @ClassRule
  public static WireMockClassRule wireMockClassRule = new WireMockClassRule(
      WireMockConfiguration.options().dynamicPort().usingFilesUnderDirectory(
          "src/test/resources/wiremock/" + SonarConnectorTest.class.getSimpleName()));

  @Rule
  public WireMockClassRule wireMockRule = wireMockClassRule;

  private SonarConnector underTest;

  /**
   * create instance to test
   */
  @Before
  public void setUp() throws Exception {
    underTest =
        new SonarConnector(new LogFacade(log), new URL("http://localhost:" + wireMockRule.port()),
            null, null, null);
  }

  /**
   * Test requesting and parsing of task response
   */
  @Test
  public void retrieveTask() throws Exception {
    Task task = underTest.retrieveTask("AXe74ZzR1IiFGsn-Op8X");
    assertNotNull("no task found", task);
    assertEquals("Status mismatch", Task.Status.SUCCESS, task.getStatus());
    assertEquals("Analysis id mismatch", "AXe74Z38wlKnb4b_2mMT", task.getAnalysisId());
    log.trace(task);
  }

  /**
   * Test requesting and parsing of project status response based on an analysis
   */
  @Test
  public void retrieveProjectStatusByAnalysisId() throws Exception {
    ProjectStatus projectStatus =
        underTest.retrieveProjectStatusByAnalysisId("AXe74Z38wlKnb4b_2mMT");
    assertNotNull("no projectStatus found", projectStatus);
    assertEquals("Status mismatch", ProjectStatus.Status.OK, projectStatus.getStatus());
    log.trace(projectStatus);
  }
}