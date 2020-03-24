# Sonar Quality Gate Maven Plugin

![Maven Build](https://github.com/r0bb3n/sonar-quality-gate-maven-plugin/workflows/Maven%20Build/badge.svg)

Check your SonarQube project if it passes its quality gate. Otherwise fail the maven job.

There a three modes supported:

| Mode                         | Description                                                                                                                                                        |
|------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **integrated** (recommended) | Run in conjunction with sonar-maven-plugin<br>The required information for fetching the data will be read from a sonar-maven-plugin generated file in `target` |
| **simple**                   | Run stand-alone for a simple SonarQube project (no branches)                                                                                                       |
| **advanced**                 | Run stand-alone for a SonarQube project with branch name our pull request                                                                                          |

## Prerequisites

* Java 11
* SonarQube (depends on mode)
  * integrated: 5.3+ 
  * simple: 5.4+
  * advanced: 7.7+

## Usage

Depending on the mode

**Integrated**

```
mvn sonar:sonar sonar-quality-gate:check
```

**Simple**

```
mvn sonar-quality-gate:check
```
Ensure that there is no (old) metadata inside `target` from a former sonar-maven-plugin run 
(especially `target/sonar/report-task.txt`) otherwise it will switch automatically to **integrated** mode.

**Advanced**

```
mvn sonar-quality-gate:check -Dsonar.qualitygate.branch=develop
```

### Parameters

| Parameter                                | Description                                                                                                                                                             | Mode             |
|------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------|
| `sonar.host.url`                         | sonar host url                                                                                                                                                          | _all_            |
| `sonar.projectKey`                       | project key used in sonar for this project<br>(default: `${project.groupId}:${project.artifactId}`)                                                                     | simple, advanced |
| `sonar.login`                            | sonar login (username or token) for basic auth, see also [SonarQube - Web API Authentication][sonar-web-api-auth]                                                       | _all_            |
| `sonar.password`                         | sonar password for basic auth, see also [SonarQube - Web API Authentication][sonar-web-api-auth]                                                                        | _all_            |
| `sonar.qualitygate.branch`               | name of the branch to check the quality gate in sonar                                                                                                                   | advanced         |
| `sonar.qualitygate.pullRequest`          | name of the pull request to check the quality gate in sonar                                                                                                             | advanced         |
| `sonar.qualitygate.checkTask.attempts`   | How often try to retrieve the analysis id from the task details in sonar until stopping the job<br>(default: `10`; name plugin configuration node: `checkTaskAttempts`) | integrated       |
| `sonar.qualitygate.checkTask.interval.s` | How many seconds to wait between two requests when retrieving task details<br>(default: `5`; name plugin configuration node: `checkTaskIntervalS`)                      | integrated       |

[sonar-web-api-auth]: https://docs.sonarqube.org/latest/extend/web-api/

## Internals

### Calling SonarQube Web API

#### Project Status

Get the quality gate status of a project.

**Endpoint**

`api/qualitygates/project_status`

**URL Parameters**
 
| Parameter     | Supported |
|---------------|-----------|
| `projectKey`  | yes       |
| `branch`      | yes       |
| `pullRequest` | yes       |
| `analysisId`  | yes       |
| `projectId`   | no        |

**Documentation** 
  
* on [sonarcloud.io Web-API](https://sonarcloud.io/web_api/api/qualitygates/project_status)
* in your SonarQube instance: `<sonar.host.url>/web_api/api/qualitygates/project_status`

#### Compute Engine Task (ceTask)

This endpoint is used to retrieve the analysis id of a prior sonar-maven-plugin run.

**Endpoint**

`api/ce/task`

**URL Parameters**
 
| Parameter          | Supported |
|--------------------|-----------|
| `id`               | yes       |
| `additionalFields` | no        |

**Documentation** 
  
* on [sonarcloud.io Web-API](https://sonarcloud.io/web_api/api/ce/task)
* in your SonarQube instance: `<sonar.host.url>/web_api/api/ce/task`


