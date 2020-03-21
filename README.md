# Sonar Quality Gate Maven Plugin

Check your SonarQube project if it passes its quality gate. Otherwise fail the maven job.

## Prerequisites

* Java 11
* SonarQube 7.7+

## Usage

```
mvn sonar-quality-gate:check -Dsonar.qualitygate.branch=develop
```

### Parameters

| Parameter                  | Description                                                                                                       |
|----------------------------|-------------------------------------------------------------------------------------------------------------------|
| `sonar.host.url`           | sonar host url                                                                                                    |
| `sonar.projectKey`         | project key used in sonar for this project                                                                        |
| `sonar.login`              | sonar login (username or token) for basic auth, see also [SonarQube - Web API Authentication][sonar-web-api-auth] |
| `sonar.password`           | sonar password for basic auth, see also [SonarQube - Web API Authentication][sonar-web-api-auth]                  |
| `sonar.qualitygate.branch` | name of the branch to check the quality gate in sonar                                                             |

## Internals

The following endpoint of sonar is used: `api/qualitygates/project_status`. 
The full documentation can be found  
* ... on [sonarcloud.io Web-API](https://sonarcloud.io/web_api/api/qualitygates/project_status)
* ... in your SonarQube instance: `<sonar.host.url>/web_api/api/qualitygates/project_status`

**URL Parameters**
 
| Parameter     | Supported    |
|---------------|--------------|
| `projectKey`  | yes          |
| `branch`      | yes          |
| `pullRequest` | no, not yet. |
| `analysisId`  | no, not yet. |
| `projectId`   | no, not yet. |


[sonar-web-api-auth]: https://docs.sonarqube.org/latest/extend/web-api/
