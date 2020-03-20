package org.r0bb3n.maven;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
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
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.r0bb3n.maven.model.Condition;
import org.r0bb3n.maven.model.ProjectStatus;
import org.r0bb3n.maven.model.ProjectStatusContainer;
import org.r0bb3n.maven.model.Status;

/**
 * Goal which touches a timestamp file.
 */
@Mojo(name = "check")
public class SonarqubeQualityGatesMojo extends AbstractMojo {

  private static final String PROP_SONAR_LOGIN = "sonar.login";
  private static final String PROP_SONAR_PASSWORD = "sonar.password";
  private static final String PROP_SONAR_HOST_URL = "sonar.host.url";

  private static final String SONAR_WEB_API_PATH = "api/qualitygates/project_status";
  private static final String HEADER_NAME_AUTHORIZATION = "Authorization";
  private static final String HEADER_NAME_CONTENT_TYPE = "Content-Type";

  /**
   * sonar host url
   */
  @Parameter(property = PROP_SONAR_HOST_URL, defaultValue = "http://localhost:9000")
  private URL sonarHostUrl;

  /**
   * project key used in sonar for this project
   */
  @Parameter(property = "sonar.projectKey", defaultValue = "${project.groupId}:${project.artifactId}")
  private String sonarProjectKey;

  /**
   * sonar login (username ore token), see also <a href="https://docs.sonarqube.org/latest/extend/web-api/">SonarQube
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
  @Parameter(property = "sonarqube.qualitygates.branch", defaultValue = "master")
  private String sonarqubeQualitygatesBranch;

  public void execute() throws MojoExecutionException, MojoFailureException {
    String in = sonarHostUrl.toExternalForm();
    StringBuilder urlBuilder = new StringBuilder(in);
    if (!in.endsWith("/")) {
      urlBuilder.append("/");
    }
    urlBuilder.append(SONAR_WEB_API_PATH).append(createQuery());
    URI measureUri;
    try {
      measureUri = URI.create(urlBuilder.toString());
    } catch (IllegalArgumentException e) {
      throw new MojoExecutionException(
          String.format("Cannot parse value of '%s': %s", PROP_SONAR_HOST_URL, sonarHostUrl), e);
    }

    getLog().info("Sonar Web API call: " + measureUri);

    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = createRequestBuilder()
        .uri(measureUri)
        .timeout(Duration.ofMinutes(1))
        .header(HEADER_NAME_CONTENT_TYPE, "application/json")
        .GET()
        .build();
    HttpResponse<String> response;
    try {
      response = client.send(request, BodyHandlers.ofString());
    } catch (IOException e) {
      throw new MojoExecutionException(String.format("Error reading from Sonar: %s", measureUri),
          e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new MojoExecutionException("Interrupted", e);
    }

    String json = response.body();
    getLog().debug("Response from Sonar:\n" + json);

    if (response.statusCode() != HttpURLConnection.HTTP_OK) {
      throw new MojoExecutionException(String
          .format("Bad status code '%d' returned from '%s' - Body: %s", response.statusCode(),
              measureUri, json));
    }

    ProjectStatus projectStatus;
    try {
      projectStatus = readProjectStatus(json);
    } catch (JsonProcessingException e) {
      throw new MojoExecutionException(String.format("Error parsing response: %s", json), e);
    }
    if (projectStatus == null) {
      throw new MojoExecutionException(String.format("Error parsing response: %s", json));
    }

    if (projectStatus.getStatus() != Status.OK) {
      String failedConditions = projectStatus.getConditions().stream()
          .filter(c -> c.getStatus() != Status.OK)
          .map(Condition::getMetricKey).collect(Collectors.joining(", "));
      throw new MojoFailureException(
          String.format("Quality Gate not passed! Failed metrics: %s", failedConditions));
    } else {
      getLog().info("project status: " + projectStatus.getStatus());
    }
  }

  /**
   * build query part for sonar url including project related values (projectKey, branch)
   *
   * @return query starting with '?'
   */
  private String createQuery() {
    Map<String, String> params = new LinkedHashMap<>();
    params.put("projectKey", sonarProjectKey);
    params.put("branch", sonarqubeQualitygatesBranch);
    String query = params.entrySet().stream().map(this::toQueryEntry)
        .collect(Collectors.joining("&"));
    return "?" + query;
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
   * Read JSON into {@link ProjectStatus}
   *
   * @param json JSON data
   * @return {@link ProjectStatus} or {@code null} if empty JSON object
   * @throws JsonProcessingException JSON cannot be mapped properly
   */
  protected ProjectStatus readProjectStatus(String json) throws JsonProcessingException {
    ProjectStatusContainer projectStatusContainer = createMapper()
        .readValue(json, ProjectStatusContainer.class);
    return projectStatusContainer.getProjectStatus();
  }
}
