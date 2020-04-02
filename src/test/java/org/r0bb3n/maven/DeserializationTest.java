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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Before;
import org.junit.Test;
import org.r0bb3n.maven.model.ProjectStatusContainer;
import org.r0bb3n.maven.model.TaskContainer;

public class DeserializationTest {

  private final Log log = new SystemStreamLog();
  private ObjectMapper objectMapper;

  @Before
  public void setUp() {
    objectMapper = new SonarQualityGateMojo().createMapper();
  }

  @Test
  public void readProjectStatusJson() throws Exception {
    ProjectStatusContainer container = objectMapper
        .readValue(DeserializationTest.class.getResource("/project_status-response-valid.json"),
            ProjectStatusContainer.class);
    assertNotNull("no Container", container);
    assertSame("Container not right implemented", container.getProjectStatus(),
        container.getContent());
    assertNotNull("no project status", container.getProjectStatus());
    log.info(container.getProjectStatus().toString());
  }

  @Test
  public void readTaskJson() throws Exception {
    TaskContainer container = objectMapper
        .readValue(DeserializationTest.class.getResource("/task-response-valid.json"),
            TaskContainer.class);

    assertNotNull("no Container", container);
    assertNotNull("no task", container.getTask());
    assertSame("Container not right implemented", container.getTask(), container.getContent());
    assertEquals("unexpected analysisId", "zpBWPVtIZerEQqdqnHdA",
        container.getTask().getAnalysisId());
    log.info(container.getTask().toString());
  }

}