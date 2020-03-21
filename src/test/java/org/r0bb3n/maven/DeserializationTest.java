/*
 * Copyright 2020 r0bb3n
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

import static org.junit.Assert.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Before;
import org.junit.Test;
import org.r0bb3n.maven.model.ProjectStatusContainer;

public class DeserializationTest {

  private final Log log = new SystemStreamLog();
  private ObjectMapper objectMapper;

  @Before
  public void setUp() {
    objectMapper = new SonarqubeQualityGatesMojo().createMapper();
  }

  @Test
  public void readJson() throws Exception {
    ProjectStatusContainer projectStatusContainer = objectMapper
        .readValue(DeserializationTest.class.getResource("/sonar-response-valid.json"),
            ProjectStatusContainer.class);
    assertNotNull("no Container", projectStatusContainer);
    assertNotNull("no project status", projectStatusContainer.getProjectStatus());
    log.info(projectStatusContainer.getProjectStatus().toString());
  }
}