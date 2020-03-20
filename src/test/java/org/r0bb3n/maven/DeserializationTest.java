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