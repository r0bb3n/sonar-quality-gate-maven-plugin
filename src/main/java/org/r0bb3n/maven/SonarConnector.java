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
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.maven.plugin.logging.Log;
import org.r0bb3n.maven.model.Container;
import org.r0bb3n.maven.model.ProjectStatus;
import org.r0bb3n.maven.model.ProjectStatusContainer;
import org.r0bb3n.maven.model.Task;
import org.r0bb3n.maven.model.TaskContainer;

@RequiredArgsConstructor
public class SonarConnector {

  private static final String SONAR_WEB_API_PATH_PROJECT_STATUS = "api/qualitygates/project_status";
  private static final String SONAR_WEB_API_PATH_CE_TASK = "api/ce/task";
  private static final String HEADER_NAME_AUTHORIZATION = "Authorization";
  private static final String HEADER_NAME_CONTENT_TYPE = "Content-Type";

  private final Log log;
  private final URL sonarHostUrl;
  private final String sonarLogin;
  private final String sonarPassword;
  private final String sonarProjectKey;


  /**
   * Retrieve task data from sonar server using the ceTaskId
   *
   * @param ceTaskId ce task id to gather details (including analysis id)
   * @return task information
   * @throws IOException error while using URI, fetching response or mapping to Object
   * @throws InterruptedException interrupted while request ongoing
   */
  public Task retrieveTask(String ceTaskId) throws IOException, InterruptedException {
    URI ceTaskUri = createUri(SONAR_WEB_API_PATH_CE_TASK, Collections.singletonMap("id", ceTaskId));
    String ceTaskJson = retrieveResponse(ceTaskUri);
    return parseContainer(TaskContainer.class, ceTaskJson);
  }

  /**
   * Retrieve project status from sonar server by using either analysisId or branch or pull request
   *
   * @param analysisId analysis id
   * @param branch branch name
   * @param pullRequest pull request
   * @return project status data
   * @throws IOException error while using URI, fetching response or mapping to Object
   * @throws InterruptedException interrupted while request ongoing
   */
  public ProjectStatus retrieveProjectStatus(String analysisId, String branch, String pullRequest)
      throws IOException, InterruptedException {
    URI projectStatusUri = createProjectStatusRequestUri(analysisId, branch, pullRequest);
    String projStatJson = retrieveResponse(projectStatusUri);

    return parseContainer(ProjectStatusContainer.class, projStatJson);
  }

  /**
   * Create URI with right base url, web API path and proper query parameters including either
   * analysis id ('integrated' mode), project key ('simple' mode) or project key with either branch
   * or pull request name  ('advanced' mode)
   *
   * @param analysisId analysisId or {@code null}
   * @param branch branch name
   * @param pullRequest pull request name/identifier
   * @throws IOException URI could not be created
   */
  private URI createProjectStatusRequestUri(String analysisId, String branch, String pullRequest)
      throws IOException {
    Map<String, String> params;
    if (analysisId != null) {
      // 'integrated' mode
      params = Collections.singletonMap("analysisId", analysisId);
    } else {
      // 'simple' and 'advanced' mode
      params = new LinkedHashMap<>();
      params.put("projectKey", sonarProjectKey);
      if (!Util.isBlank(branch)) {
        params.put("branch", branch);
      }
      if (!Util.isBlank(pullRequest)) {
        params.put("pullRequest", pullRequest);
      }
    }
    return createUri(SONAR_WEB_API_PATH_PROJECT_STATUS, params);
  }

  /**
   * build our with sonar base url, api path to resource and related query params
   *
   * @param apiPath relative path to resource
   * @param queryParams map with query params, can be empty
   * @return URI to resource
   * @throws IOException malformed URI
   */
  private URI createUri(String apiPath, Map<String, String> queryParams) throws IOException {
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
      throw new IOException("Cannot create valid URI from: " + urlBuilder, e);
    }
  }

  /**
   * Fire a GET request and return response body as String.
   *
   * @param resourceUri resource to get
   * @throws IOException io problems
   * @throws InterruptedException interrupted
   */
  private String retrieveResponse(URI resourceUri) throws IOException, InterruptedException {
    log.info("Sonar Web API call: " + resourceUri);

    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request =
        createRequestBuilder().GET().uri(resourceUri).timeout(Duration.ofMinutes(1))
            .header(HEADER_NAME_CONTENT_TYPE, "application/json").build();
    HttpResponse<String> response;
    try {
      response = client.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException e) {
      throw new IOException(String.format("Error reading from Sonar: %s", resourceUri), e);
    }

    String json = response.body();
    if (log.isDebugEnabled()) {
      log.debug(
          String.format("Response from Sonar (HTTP Status: %d):%n%s", response.statusCode(), json));
    }
    if (response.statusCode() != HttpURLConnection.HTTP_OK) {
      throw new IOException(String
          .format("Bad status code '%d' returned from '%s' - Body: %s", response.statusCode(),
              resourceUri, json));
    } else {
      return json;
    }
  }

  /**
   * Create and configure (add authorization, if provided) request builder
   */
  private HttpRequest.Builder createRequestBuilder() {
    HttpRequest.Builder ret = HttpRequest.newBuilder();
    if (!Util.isBlank(sonarLogin)) {
      if (Util.isBlank(sonarPassword)) {
        ret.header(HEADER_NAME_AUTHORIZATION, basicAuth(sonarLogin, ""));
      } else {
        ret.header(HEADER_NAME_AUTHORIZATION, basicAuth(sonarLogin, sonarPassword));
      }
    }
    return ret;
  }

  /**
   * Parse JSON string into a proper {@link Container} object and return the relevant content
   * object.
   *
   * @param containerClass JSON represents this container class
   * @param json JSON containing data
   * @throws IOException malformed, incomplete or empty JSON input
   */
  protected <T, C extends Container<T>> T parseContainer(Class<C> containerClass, String json)
      throws IOException {
    T content;
    try {
      C container = createMapper().readValue(json, containerClass);
      content = container.getContent();
    } catch (JsonProcessingException e) {
      throw new IOException(
          String.format("Error parsing response into '%s': %s", containerClass.getName(), json), e);
    }
    if (content == null) {
      throw new IOException(String.format("Error parsing response - no content: %s", json));
    }
    return content;
  }

  /**
   * Create and configure {@link ObjectMapper}
   *
   * @return objectMapper
   */
  protected static ObjectMapper createMapper() {
    ObjectMapper mapper = new ObjectMapper();
    // to prevent exception when encountering unknown property:
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    return mapper;
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
   * join key and value with '=' and URL encode both
   *
   * @param entry query parameter
   * @return URL-ready query parameter
   */
  private String toQueryEntry(Map.Entry<String, String> entry) {
    return URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder
        .encode(entry.getValue(), StandardCharsets.UTF_8);
  }

}
