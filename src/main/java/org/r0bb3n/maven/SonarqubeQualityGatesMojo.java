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

import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

/**
 * Goal which touches a timestamp file.
 */
@Mojo(name = "check")
public class SonarqubeQualityGatesMojo extends AbstractMojo {

  /**
   * The Constant STATUS_CODE_OK.
   */
  private static final int STATUS_CODE_OK = 200;
  private static final String PROP_SONAR_LOGIN = "sonar.login";
  private static final String PROP_SONAR_PASSWORD = "sonar.password";
  private static final String PROP_SONAR_HOST_URL = "sonar.host.url";

  private static String SONAR_WEB_API_PATH = "api/measures/component";

  /**
   * The sonar host url.
   */
  @Parameter(property = PROP_SONAR_HOST_URL, defaultValue = "http://localhost:9000")
  private URL sonarHostUrl;

  @Parameter(property = "sonar.projectKey", defaultValue = "${project.groupId}:${project.artifactId}")
  private String sonarProjectKey;

  /**
   * property key for sonar login (username ore token), see also
   * <a href="https://docs.sonarqube.org/latest/extend/web-api/">SonarQube - Web API
   * Authentication</a> <br/> aligned to sonar-maven-plugin analysis parameters, see also
   * <a href="https://docs.sonarqube.org/latest/analysis/analysis-parameters/">SonarQube - Analysis
   * Parameters</a>
   */
  @Parameter(property = PROP_SONAR_LOGIN)
  private String sonarLogin;

  /**
   * property key for sonar password, see also
   * <a href="https://docs.sonarqube.org/latest/extend/web-api/">SonarQube - Web API
   * Authentication</a> <br/> aligned to sonar-maven-plugin analysis parameters, see also
   * <a href="https://docs.sonarqube.org/latest/analysis/analysis-parameters/">SonarQube - Analysis
   * Parameters</a>
   */
  @Parameter(property = PROP_SONAR_PASSWORD)
  private String sonarPassword;

  public void execute() throws MojoExecutionException {
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
//    HttpClient client = HttpClient.newBuilder().authenticator(createAuthenticator()).build();

    HttpRequest request = createRequestBuilder()
        .uri(measureUri)
        .timeout(Duration.ofMinutes(1))
        .header("Content-Type", "application/json")
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

    if (response.statusCode() != HttpURLConnection.HTTP_OK) {
      throw new MojoExecutionException(String
          .format("Bad status code '%d' returned from '%s' - Body: %s", response.statusCode(),
              measureUri, json));
    }

    getLog().debug("Response from Sonar:\n" + json);
  }

  private HttpRequest.Builder createRequestBuilder() throws MojoExecutionException {
    Builder ret = HttpRequest.newBuilder();
    if (!isBlank(sonarLogin)) {
      if (isBlank(sonarPassword)) {
        ret.header("Authorization", basicAuth(sonarLogin, ""));
      } else {
        ret.header("Authorization", basicAuth(sonarLogin, sonarPassword));
      }
    } else if (!isBlank(sonarPassword)) {
      throw new MojoExecutionException(
          String.format("you cannot specify '%s' without '%s'", PROP_SONAR_PASSWORD,
              PROP_SONAR_LOGIN));
    }
    return ret;
  }

  private String basicAuth(String username, String password) {
    return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
  }

  private String createQuery() {
    Map<String, String> params = new LinkedHashMap<>();
    params.put("component", sonarProjectKey);
    params.put("metricKeys", "alert_status,quality_gate_details");
//    params.put("branch", "feature/WEFUEL-339_creating_station_models");
    String query = params.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
        .collect(Collectors.joining("&"));
    return "?" + query;
  }

  private boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
