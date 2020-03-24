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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.r0bb3n.maven.model.Condition;
import org.r0bb3n.maven.model.Container;
import org.r0bb3n.maven.model.ProjectStatus;
import org.r0bb3n.maven.model.ProjectStatusContainer;
import org.r0bb3n.maven.model.Task;
import org.r0bb3n.maven.model.TaskContainer;

/**
 * Check project status in SonarQube and fail build, if quality gate is not passed
 */
@Mojo(name = "check")
public class SonarQualityGateMojo extends AbstractMojo {

  private static final String PROP_SONAR_LOGIN = "sonar.login";
  private static final String PROP_SONAR_PASSWORD = "sonar.password";
  private static final String PROP_SONAR_HOST_URL = "sonar.host.url";

  private static final String SONAR_WEB_API_PATH_PROJECT_STATUS = "api/qualitygates/project_status";
  private static final String SONAR_WEB_API_PATH_CE_TASK = "api/ce/task";
  private static final String HEADER_NAME_AUTHORIZATION = "Authorization";
  private static final String HEADER_NAME_CONTENT_TYPE = "Content-Type";

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
  @Parameter(property = "sonar.projectKey", defaultValue = "${project.groupId}:${project.artifactId}")
  private String sonarProjectKey;

  /**
   * sonar login (username or token), see also <a href="https://docs.sonarqube.org/latest/extend/web-api/">SonarQube
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
   * request project status from sonar and evaluate quality gate result
   *
   * @throws MojoExecutionException configuration errors, io problems, ...
   * @throws MojoFailureException quality gate evaluates as not passed
   */
  public void execute() throws MojoExecutionException, MojoFailureException {
    String analysisId;
    if (isBlank(branch) && isBlank(pullRequest)) {
      Optional<String> ceTaskIdOpt = findCeTaskId();
      if (ceTaskIdOpt.isPresent()) {
        // previous sonar run found, switching to 'integrated'
        analysisId = retrieveAnalysisId(ceTaskIdOpt.get());
      } else {
        // no previous sonar run found, switching to 'simple'
        analysisId = null;
      }
    } else {
      // branch or PR was supplied, the 'advanced' mode was chosen
      analysisId = null;
    }

    URI projectStatusUri = createProjectStatusRequestUri(analysisId);
    String projStatJson = retrieveResponse(projectStatusUri);

    ProjectStatus projectStatus = parseContainer(ProjectStatusContainer.class, projStatJson);
    if (projectStatus.getStatus() != ProjectStatus.Status.OK) {
      String failedConditions = projectStatus.getConditions().stream()
          .filter(has(ProjectStatus.Status.OK, ProjectStatus.Status.NONE).negate())
          .map(c -> c.getMetricKey() + ":" + c.getStatus()).collect(Collectors.joining(", "));
      throw new MojoFailureException(
          String.format("Quality Gate not passed (status: %s)! Failed metric(s): %s",
              projectStatus.getStatus(), failedConditions));
    } else {
      getLog().info("project status: " + projectStatus.getStatus());
    }
  }

  /**
   * create a predicate to check, if a {@link Condition} has one of the supplied status
   */
  private static Predicate<Condition> has(ProjectStatus.Status... status) {
    return c -> Arrays.asList(status).contains(c.getStatus());
  }

  /**
   * Check task details and read analysis id. If task is still ongoing ({@link
   * Task.Status#IN_PROGRESS}/{@link Task.Status#PENDING}), the threads sleeps for {@link
   * #checkTaskIntervalS} and tries in total {@link #checkTaskAttempts} times.
   *
   * @param ceTaskId ce task id to gather details (including analysis id)
   * @return analysis id
   * @throws MojoExecutionException task got unsuitable status (({@link Task.Status#FAILED}/{@link
   * Task.Status#CANCELED}) or task is still ongoing but attempt limit is reached.
   */
  private String retrieveAnalysisId(String ceTaskId) throws MojoExecutionException {
    URI ceTaskUri = createUri(SONAR_WEB_API_PATH_CE_TASK, Collections.singletonMap("id", ceTaskId));
    int attemptsLeft = checkTaskAttempts;
    Task.Status status = Task.Status.IN_PROGRESS;
    String analysisId = null;

    while (status.isOngoing() && attemptsLeft-- > 0) {
      String ceTaskJson = retrieveResponse(ceTaskUri);
      Task task = parseContainer(TaskContainer.class, ceTaskJson);
      status = task.getStatus();
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
            throw new MojoExecutionException("Interrupted", e);
          }
          break;
        default:
          throw new MojoExecutionException(
              "Cannot determine analysis id - unsuitable task status: " + status);
      }
    }
    if (analysisId == null) {
      throw new MojoExecutionException(String.format(
          "Could not fetch analysis id within %d requests with an interval of %d seconds (last status: %s). Please "
              + "increase the values 'checkTaskAttempts' and/or 'checkTaskIntervalS' to fit your projects needs.",
          checkTaskAttempts, checkTaskIntervalS, status));
    }

    return analysisId;
  }

  /**
   * Determine compute engine task id ("ceTaskId") of previous run of sonar-maven-plugin
   *
   * @return id to request task details
   * @throws MojoExecutionException io problems when reading sonar-maven-plugin file
   */
  private Optional<String> findCeTaskId() throws MojoExecutionException {
    Path reportTaskPath = Path.of(projectBuildDirectory, "sonar", "report-task.txt");
    if (!Files.exists(reportTaskPath)) {
      getLog().info("no report file from previously sonar-maven-plugin "
          + "run found: " + reportTaskPath);
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
    if (isBlank(ceTaskId)) {
      throw new MojoExecutionException(
          String.format("Property '%s' not found in '%s'", REPORT_TASK_KEY_CE_TASK_ID,
              reportTaskPath));
    } else {
      return Optional.of(ceTaskId);
    }
  }

  /**
   * Create URI with right base url, web API path and proper query parameters including either
   * analysis id ('integrated' mode), project key ('simple' mode) or project key with either branch
   * or pull request name  ('advanced' mode)
   *
   * @param analysisId analysisId or {@code null}
   * @throws MojoExecutionException URI could not be created
   */
  private URI createProjectStatusRequestUri(String analysisId)
      throws MojoExecutionException {
    Map<String, String> params;
    if (analysisId != null) {
      // 'integrated' mode
      params = Collections.singletonMap("analysisId", analysisId);
    } else {
      // 'simple' and 'advanced' mode
      params = new LinkedHashMap<>();
      params.put("projectKey", sonarProjectKey);
      if (!isBlank(branch)) {
        params.put("branch", branch);
      }
      if (!isBlank(pullRequest)) {
        params.put("pullRequest", pullRequest);
      }
    }
    return createUri(SONAR_WEB_API_PATH_PROJECT_STATUS, params);
  }

  /**
   * Fire a GET request and return response body as String.
   *
   * @param resourceUri resource to get
   * @throws MojoExecutionException io problems or interrupted
   */
  private String retrieveResponse(URI resourceUri)
      throws MojoExecutionException {
    getLog().info("Sonar Web API call: " + resourceUri);

    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = createRequestBuilder()
        .GET().uri(resourceUri)
        .timeout(Duration.ofMinutes(1))
        .header(HEADER_NAME_CONTENT_TYPE, "application/json")
        .build();
    HttpResponse<String> response;
    try {
      response = client.send(request, BodyHandlers.ofString());
    } catch (IOException e) {
      throw new MojoExecutionException(String.format("Error reading from Sonar: %s", resourceUri),
          e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new MojoExecutionException("Interrupted", e);
    }

    String json = response.body();
    if (getLog().isDebugEnabled()) {
      getLog().debug(
          String.format("Response from Sonar (HTTP Status: %d):\n%s", response.statusCode(), json));
    }
    if (response.statusCode() != HttpURLConnection.HTTP_OK) {
      throw new MojoExecutionException(String
          .format("Bad status code '%d' returned from '%s' - Body: %s", response.statusCode(),
              resourceUri, json));
    } else {
      return json;
    }

  }

  /**
   * build our with sonar base url, api path to resource and related query params
   *
   * @param apiPath relative path to resource
   * @param queryParams map with query params, can be empty
   * @return URI to resource
   * @throws MojoExecutionException malformed URI
   */
  private URI createUri(String apiPath, Map<String, String> queryParams)
      throws MojoExecutionException {
    String in = sonarHostUrl.toExternalForm();
    StringBuilder urlBuilder = new StringBuilder(in);
    if (!in.endsWith("/")) {
      urlBuilder.append("/");
    }
    urlBuilder.append(apiPath);
    if (!queryParams.isEmpty()) {
      urlBuilder.append("?");
      queryParams.entrySet().stream().map(this::toQueryEntry).map(s -> s + "&")
          .forEachOrdered(urlBuilder::append);
      urlBuilder.deleteCharAt(urlBuilder.length() - 1);
    }
    try {
      return URI.create(urlBuilder.toString());
    } catch (IllegalArgumentException e) {
      throw new MojoExecutionException(
          String
              .format("Cannot parse value of '%s' properly: %s", PROP_SONAR_HOST_URL, sonarHostUrl),
          e);
    }
  }

  /**
   * join key and value with '=' and URL encode both
   *
   * @param entry query parameter
   * @return URL-ready query parameter
   */
  private String toQueryEntry(Map.Entry<String, String> entry) {
    return URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" +
        URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
  }

  /**
   * Create and configure (add authorization, if provided) request builder
   *
   * @throws MojoExecutionException auth information is invalid
   */
  private HttpRequest.Builder createRequestBuilder() throws MojoExecutionException {
    Builder ret = HttpRequest.newBuilder();
    if (!isBlank(sonarLogin)) {
      if (isBlank(sonarPassword)) {
        ret.header(HEADER_NAME_AUTHORIZATION, basicAuth(sonarLogin, ""));
      } else {
        ret.header(HEADER_NAME_AUTHORIZATION, basicAuth(sonarLogin, sonarPassword));
      }
    } else if (!isBlank(sonarPassword)) {
      throw new MojoExecutionException(
          String.format("you cannot specify '%s' without '%s'", PROP_SONAR_PASSWORD,
              PROP_SONAR_LOGIN));
    }
    return ret;
  }

  private boolean isBlank(String s) {
    return s == null || s.isBlank();
  }

  /**
   * create basic auth value for header {@value #HEADER_NAME_AUTHORIZATION}
   *
   * @return Base64 encoded Basic auth header value
   */
  private String basicAuth(String username, String password) {
    return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
  }

  /**
   * Create and configure {@link ObjectMapper}
   *
   * @return objectMapper
   */
  protected ObjectMapper createMapper() {
    ObjectMapper mapper = new ObjectMapper();
    // to prevent exception when encountering unknown property:
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    return mapper;
  }

  /**
   * Parse JSON string into a proper {@link Container} object and return the relevant content
   * object.
   *
   * @param containerClass JSON represents this container class
   * @param json JSON containing data
   * @throws MojoExecutionException malformed, incomplete or empty JSON input
   */
  protected <T, C extends Container<T>> T parseContainer(Class<C> containerClass, String json)
      throws MojoExecutionException {
    T content;
    try {
      C container = createMapper().readValue(json, containerClass);
      content = container.getContent();
    } catch (JsonProcessingException e) {
      throw new MojoExecutionException(
          String.format("Error parsing response into '%s': %s", containerClass.getName(), json), e);
    }
    if (content == null) {
      throw new MojoExecutionException(
          String.format("Error parsing response - no content: %s", json));
    }
    return content;
  }
}
