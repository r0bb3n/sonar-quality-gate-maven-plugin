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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.r0bb3n.maven.model.Condition;
import org.r0bb3n.maven.model.ProjectStatus;
import org.r0bb3n.maven.model.Task;

/**
 * Check project status in SonarQube and fail build, if quality gate is not passed
 */
@Mojo(name = "check", aggregator = true)
public class SonarQualityGateMojo extends AbstractMojo {

  private static final String PROP_SONAR_LOGIN = "sonar.login";
  private static final String PROP_SONAR_PASSWORD = "sonar.password";
  private static final String PROP_SONAR_HOST_URL = "sonar.host.url";

  private static final String REPORT_TASK_KEY_CE_TASK_ID = "ceTaskId";

  /**
   * sonar host url<br/> aligned to sonar-maven-plugin analysis parameters, see also
   * <a href="https://docs.sonarqube.org/latest/analysis/analysis-parameters/">SonarQube - Analysis
   * Parameters</a>
   */
  @Parameter(property = PROP_SONAR_HOST_URL, defaultValue = "http://localhost:9000")
  private URL sonarHostUrl;

  /**
   * project key used in sonar for this project<br/> aligned to sonar-maven-plugin analysis
   * parameters, see also
   * <a href="https://docs.sonarqube.org/latest/analysis/analysis-parameters/">SonarQube - Analysis
   * Parameters</a>
   */
  @Parameter(property = "sonar.projectKey",
      defaultValue = "${project.groupId}:${project.artifactId}")
  private String sonarProjectKey;

  /**
   * sonar login (username or token), see also
   * <a href="https://docs.sonarqube.org/latest/extend/web-api/">SonarQube
   * - Web API Authentication</a> <br/> aligned to sonar-maven-plugin analysis parameters, see also
   * <a href="https://docs.sonarqube.org/latest/analysis/analysis-parameters/">SonarQube - Analysis
   * Parameters</a>
   */
  @Parameter(property = PROP_SONAR_LOGIN)
  private String sonarLogin;

  /**
   * sonar password, see also <a href="https://docs.sonarqube.org/latest/extend/web-api/">SonarQube
   * - Web API Authentication</a> <br/> aligned to sonar-maven-plugin analysis parameters, see also
   * <a href="https://docs.sonarqube.org/latest/analysis/analysis-parameters/">SonarQube - Analysis
   * Parameters</a>
   */
  @Parameter(property = PROP_SONAR_PASSWORD)
  private String sonarPassword;

  /**
   * skip the execution of this plugin
   */
  @Parameter(property = "sonar-quality-gate.skip", defaultValue = "false")
  private boolean skip;

  /**
   * fail the execution, if the quality gate was not passed (not `OK`)
   */
  @Parameter(property = "sonar-quality-gate.failOnMiss", defaultValue = "true")
  private boolean failOnMiss;

  /**
   * name of the branch to check the quality gate in sonar
   */
  @Parameter(property = "sonar-quality-gate.branch")
  private String branch;

  /**
   * name of the pull request to check the quality gate in sonar
   */
  @Parameter(property = "sonar-quality-gate.pullRequest")
  private String pullRequest;

  /**
   * How often try to retrieve the analysis id from the task details in sonar until stopping the
   * job
   */
  @Parameter(property = "sonar-quality-gate.checkTask.attempts", defaultValue = "10")
  private int checkTaskAttempts;

  /**
   * How many seconds to wait between two requests when retrieving task details
   */
  @Parameter(property = "sonar-quality-gate.checkTask.interval.s", defaultValue = "5")
  private int checkTaskIntervalS;

  /**
   * INTERNAL - get build directory
   */
  @Parameter(defaultValue = "${project.build.directory}", readonly = true)
  private String projectBuildDirectory;

  /**
   * connector instance to interact with sonar server
   */
  private SonarConnector sonarConnector;

  /**
   * request project status from sonar and evaluate quality gate result
   *
   * @throws MojoExecutionException configuration errors, io problems, ...
   * @throws MojoFailureException quality gate evaluates as not passed
   */
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skip) {
      getLog().info("skipped");
      return;
    }

    setupSonarConnector();

    String analysisId;
    if (Util.isBlank(branch) && Util.isBlank(pullRequest)) {
      Optional<String> ceTaskIdOpt = findCeTaskId(projectBuildDirectory);
      analysisId = ceTaskIdOpt
          // previous sonar run found, switching to 'integrated'
          .map(this::retrieveAnalysisId)
          // no previous sonar run found, switching to 'simple'
          .orElse(null);
    } else {
      // branch or PR was supplied, the 'advanced' mode was chosen
      analysisId = null;
    }

    ProjectStatus projectStatus = retrieveProjectStatus(analysisId);

    if (projectStatus.getStatus() != ProjectStatus.Status.OK) {
      String failedConditions = projectStatus.getConditions().stream()
          .filter(has(ProjectStatus.Status.OK, ProjectStatus.Status.NONE).negate())
          .map(c -> c.getMetricKey() + ":" + c.getStatus()).collect(Collectors.joining(", "));
      String message = String.format("Quality Gate not passed (status: %s)! Failed metric(s): %s",
          projectStatus.getStatus(), failedConditions);
      if (failOnMiss) {
        throw new MojoFailureException(message);
      } else {
        getLog().warn(message);
      }
    } else {
      getLog().info("project status: " + projectStatus.getStatus());
    }
  }

  /**
   * Read config parameters and create the {@link #sonarConnector}
   * @throws MojoExecutionException in case of invalid config parameters
   */
  protected void setupSonarConnector() throws MojoExecutionException {
    if (Util.isBlank(sonarLogin) && !Util.isBlank(sonarPassword)) {
      throw new MojoExecutionException(String
          .format("you cannot specify '%s' without '%s'", PROP_SONAR_PASSWORD, PROP_SONAR_LOGIN));
    }
    sonarConnector =
        new SonarConnector(getLog(), sonarHostUrl, sonarLogin, sonarPassword, sonarProjectKey);
  }

  /**
   * Call sonar server and retrieve the project status by either a recent analysis or by static
   * values for project, branch or pull request
   *
   * @param analysisId the actual analysis id to check for or {@code null} in case of
   *                   'simple' or 'advanced' mode
   * @return the project status
   * @throws MojoExecutionException in case of IO issues or interruption
   */
  protected ProjectStatus retrieveProjectStatus(String analysisId) throws MojoExecutionException {
    ProjectStatus projectStatus;
    try {
      if (analysisId != null) {
        // 'integrated' mode
        projectStatus = sonarConnector.retrieveProjectStatusByAnalysisId(analysisId);
      } else {
        // 'simple' / 'advanced' mode
        projectStatus = sonarConnector.retrieveProjectStatus(branch, pullRequest);
      }
    } catch (IOException e) {
      throw new MojoExecutionException("error fetching project status", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new MojoExecutionException("Interrupted while fetching project status", e);
    }
    return projectStatus;
  }

  /**
   * Check task details and read analysis id. If task is still ongoing ({@link
   * Task.Status#IN_PROGRESS}/{@link Task.Status#PENDING}), the threads sleeps for {@link
   * #checkTaskIntervalS} and tries in total {@link #checkTaskAttempts} times.
   *
   * <p>Throws MojoExecutionException when task got unsuitable status ({@link Task.Status#FAILED}/
   * {@link Task.Status#CANCELED}) or task is still ongoing but attempt limit is reached or IO
   * errors.
   *
   * @param ceTaskId ce task id to gather details (including analysis id)
   * @return analysis id, not null (unavailable id we cause an exception)
   */
  @SneakyThrows(MojoExecutionException.class)
  protected String retrieveAnalysisId(String ceTaskId) {
    int attemptsLeft = checkTaskAttempts;
    Task.Status status = Task.Status.IN_PROGRESS;
    String analysisId = null;

    while (status.isOngoing() && attemptsLeft-- > 0) {
      Task task;
      try {
        task = sonarConnector.retrieveTask(ceTaskId);
        status = task.getStatus();
      } catch (IOException e) {
        throw new MojoExecutionException("error while retrieving task", e);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new MojoExecutionException("Interrupted while retrieving task", e);
      }
      switch (status) {
        case SUCCESS:
          analysisId = task.getAnalysisId();
          break;
        case IN_PROGRESS:
        case PENDING:
          try {
            getLog().info(String
                .format("Analysis in progress, next retry in %ds (attempts left: %d)",
                    checkTaskIntervalS, attemptsLeft));
            Thread.sleep(TimeUnit.SECONDS.toMillis(checkTaskIntervalS));
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MojoExecutionException("Interrupted while waiting for retry", e);
          }
          break;
        default:
          throw new MojoExecutionException(
              "Cannot determine analysis id - unsuitable task status: " + status);
      }
    }
    if (analysisId == null) {
      throw new MojoExecutionException(String.format(
          "Could not fetch analysis id within %d requests with an interval of %d seconds (last "
              + "status: %s). Please increase the values 'checkTaskAttempts' and/or "
              + "'checkTaskIntervalS' to fit your projects needs.", checkTaskAttempts,
          checkTaskIntervalS, status));
    }

    return analysisId;
  }

  /**
   * Determine compute engine task id ("ceTaskId") of previous run of sonar-maven-plugin
   *
   * @param buildDir build directory, where sonar folder and report task file is located
   * @return id to request task details
   * @throws MojoExecutionException io problems when reading sonar-maven-plugin file
   */
  protected Optional<String> findCeTaskId(String buildDir) throws MojoExecutionException {
    Path reportTaskPath = Path.of(buildDir, "sonar", "report-task.txt");
    if (!Files.exists(reportTaskPath)) {
      getLog()
          .info("no report file from previously sonar-maven-plugin run found: " + reportTaskPath);
      return Optional.empty();
    }
    String ceTaskId;
    try (InputStream is = Files.newInputStream(reportTaskPath)) {
      Properties props = new Properties();
      props.load(is);
      ceTaskId = props.getProperty(REPORT_TASK_KEY_CE_TASK_ID);
    } catch (IOException e) {
      throw new MojoExecutionException(
          String.format("Error parsing properties in: %s", reportTaskPath), e);
    }
    if (Util.isBlank(ceTaskId)) {
      throw new MojoExecutionException(String
          .format("Property '%s' not found in '%s'", REPORT_TASK_KEY_CE_TASK_ID, reportTaskPath));
    } else {
      return Optional.of(ceTaskId);
    }
  }

  /**
   * create a predicate to check, if a {@link Condition} has one of the supplied status
   */
  private static Predicate<Condition> has(ProjectStatus.Status... status) {
    return c -> Arrays.asList(status).contains(c.getStatus());
  }

}
