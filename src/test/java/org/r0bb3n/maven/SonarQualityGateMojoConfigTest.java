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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.apache.maven.plugin.MojoExecutionException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.r0bb3n.maven.util.ExceptionMatchers;
import org.r0bb3n.maven.util.MojoConfigurator;

/**
 * Test configuration related parts (including file parsing of sonar plugin outcome)
 */
public class SonarQualityGateMojoConfigTest {

  @Rule
  public TemporaryFolder temporaryFolderRule = new TemporaryFolder();

  private SonarQualityGateMojo underTest;
  private MojoConfigurator config;

  @Before
  public void setUp() throws Exception {
    underTest = new SonarQualityGateMojo();
    config = MojoConfigurator.configure(underTest);
  }

  @Test
  public void testMissingLogin() throws Exception {
    config.setSonarPassword("password");

    MojoExecutionException exc =
        Assert.assertThrows(MojoExecutionException.class, underTest::execute);
    MatcherAssert.assertThat(exc, ExceptionMatchers.hasMessageThat(
        Matchers.matchesRegex("you cannot specify '.*' without '.*'")));
  }

  @Test
  public void testFindCeTaskIdOk() throws Exception {
    String ceTaskIdExpected = "some_task_id";
    File buildFolder = createSonarFile(ceTaskIdExpected);
    Optional<String> ceTaskIdActual = underTest.findCeTaskId(buildFolder.getAbsolutePath());
    assertFalse("Optional is empty", ceTaskIdActual.isEmpty());
    assertEquals("wrong id", ceTaskIdExpected, ceTaskIdActual.get());
  }

  @Test
  public void testFindCeTaskIdEmptyValue() throws Exception {
    File buildFolder = createSonarFile("");
    MojoExecutionException exc = Assert.assertThrows(MojoExecutionException.class,
        () -> underTest.findCeTaskId(buildFolder.getAbsolutePath()));
    MatcherAssert.assertThat(exc,
        ExceptionMatchers.hasMessageThat(Matchers.matchesRegex("Property .* not found in .*")));
  }

  @Test
  public void testFindCeTaskIdMissing() throws Exception {
    File buildFolder = createSonarFile(null);
    MojoExecutionException exc = Assert.assertThrows(MojoExecutionException.class,
        () -> underTest.findCeTaskId(buildFolder.getAbsolutePath()));
    MatcherAssert.assertThat(exc,
        ExceptionMatchers.hasMessageThat(Matchers.matchesRegex("Property .* not found in .*")));
  }

  @Test
  public void testFindCeTaskIdNoFile() throws Exception {
    Optional<String> ceTaskIdActual =
        underTest.findCeTaskId(temporaryFolderRule.newFolder().getAbsolutePath());
    assertTrue("Optional is not empty", ceTaskIdActual.isEmpty());
  }

  /**
   * Create new build dir and store a properties file 'report-task.txt' in a sub-folder 'sonar'
   * @param taskId non-null: key=[taskId] ; null: property line omitted
   * @return directory to supply to {@link SonarQualityGateMojo#findCeTaskId}
   */
  private File createSonarFile(String taskId) throws IOException {
    File buildDir = temporaryFolderRule.newFolder();
    File sonarDir = new File(buildDir, "sonar");
    assertTrue("Dir not created: " + sonarDir, sonarDir.mkdir());
    Path taskFile = Paths.get(sonarDir.getAbsolutePath(), "report-task.txt");
    StringBuilder sb = new StringBuilder();
    sb.append("# this is a comment\n");
    if (taskId != null) {
      sb.append("ceTaskId=").append(taskId).append("\n");
    }
    Files.writeString(taskFile, sb, StandardCharsets.UTF_8);
    return buildDir;
  }

}
