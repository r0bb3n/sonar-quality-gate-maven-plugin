# Sonar Quality Gate Maven Plugin

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.r0bb3n/sonar-quality-gate-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.r0bb3n/sonar-quality-gate-maven-plugin)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=io.github.r0bb3n%3Asonar-quality-gate-maven-plugin&metric=alert_status)](https://sonarcloud.io/dashboard?id=io.github.r0bb3n%3Asonar-quality-gate-maven-plugin)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=io.github.r0bb3n%3Asonar-quality-gate-maven-plugin&metric=coverage)](https://sonarcloud.io/dashboard?id=io.github.r0bb3n%3Asonar-quality-gate-maven-plugin)
[![Black Duck Security Risk](https://copilot.blackducksoftware.com/github/repos/r0bb3n/sonar-quality-gate-maven-plugin/branches/master/badge-risk.svg)](https://copilot.blackducksoftware.com/github/repos/r0bb3n/sonar-quality-gate-maven-plugin/branches/master)
![Maven Build](https://github.com/r0bb3n/sonar-quality-gate-maven-plugin/workflows/Maven%20Build/badge.svg)

Check your SonarQube project if it passes its quality gate. If it doesn't, the plugin will fail the maven job.

There a three modes supported:

| Mode                         | Description                                                                                                                                                                                          |
|------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **integrated** (recommended) | Run in conjunction with sonar-maven-plugin (supports branches and pull requests)<br>The required information for fetching the data will be read from a sonar-maven-plugin generated file in `target` |
| **simple**                   | Run stand-alone for a simple SonarQube project (no branches)                                                                                                                                         |
| **advanced**                 | Run stand-alone for a SonarQube project with branch name or pull request                                                                                                                             |

## Prerequisites

* Java 11
* SonarQube (depends on mode)
  * integrated: 5.3+ 
  * simple: 5.4+
  * advanced: 7.7+

## Usage

Include the plugin declaration in your `pom.xml` either in `<build><pluginManagement><plugins>` for
calling it only explicitly on CLI or in `<build><plugins>` to integrate it automatically in your build.  

```xml
<plugin>
  <groupId>io.github.r0bb3n</groupId>
  <artifactId>sonar-quality-gate-maven-plugin</artifactId>
  <version>1.1.0</version>
</plugin>
```

You can also use it without changing your `pom.xml` by calling it fully qualified on CLI:

```
mvn io.github.r0bb3n:sonar-quality-gate-maven-plugin:1.1.0:check
```

**Integrated mode**

```
mvn sonar:sonar sonar-quality-gate:check
```

**Simple mode**

```
mvn sonar-quality-gate:check
```
Ensure that there is no (old) metadata inside `target` from a former sonar-maven-plugin run 
(especially `target/sonar/report-task.txt`) otherwise it will switch automatically to **integrated** mode.

**Advanced mode**

```
mvn sonar-quality-gate:check -Dsonar-quality-gate.branch=develop
```

### Plugin parameters

| Parameter                                 | Description                                                                                                                                                                                     | Used in mode     |
|-------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------|
| `sonar.host.url`                          | sonar host url (aligned to [sonar-maven-plugin analysis parameters][sonar-analysis-param])                                                                                                      | _all_            |
| `sonar.login`                             | sonar login (username or token) for basic auth (aligned to [sonar-maven-plugin analysis parameters][sonar-analysis-param])<br>see also [SonarQube - Web API Authentication][sonar-web-api-auth] | _all_            |
| `sonar.password`                          | sonar password for basic auth (aligned to [sonar-maven-plugin analysis parameters][sonar-analysis-param])<br>see also [SonarQube - Web API Authentication][sonar-web-api-auth]                  | _all_            |
| `sonar.projectKey`                        | project key used in sonar for this project (aligned to [sonar-maven-plugin analysis parameters][sonar-analysis-param])<br>(default: `${project.groupId}:${project.artifactId}`)                 | simple, advanced |
| `sonar-quality-gate.branch`               | name of the branch to check the quality gate in sonar                                                                                                                                           | advanced         |
| `sonar-quality-gate.pullRequest`          | name of the pull request to check the quality gate in sonar                                                                                                                                     | advanced         |
| `sonar-quality-gate.checkTask.attempts`   | How often try to retrieve the analysis id from the task details in sonar until stopping the job<br>(default: `10`)                                                                              | integrated       |
| `sonar-quality-gate.checkTask.interval.s` | How many seconds to wait between two requests when retrieving task details<br>(default: `5`)                                                                                                    | integrated       |

[sonar-analysis-param]: https://docs.sonarqube.org/latest/analysis/analysis-parameters/
[sonar-web-api-auth]: https://docs.sonarqube.org/latest/extend/web-api/

## Internals

### Calling SonarQube Web API

#### Project Status

Get the quality gate status of a project.

**_Endpoint_**

`api/qualitygates/project_status`

**_URL Parameters_**
 
| Parameter     | Supported |
|---------------|-----------|
| `projectKey`  | yes       |
| `branch`      | yes       |
| `pullRequest` | yes       |
| `analysisId`  | yes       |
| `projectId`   | no        |

**_Documentation_** 
  
* on [sonarcloud.io Web-API](https://sonarcloud.io/web_api/api/qualitygates/project_status)
* in your SonarQube instance: `<sonar.host.url>/web_api/api/qualitygates/project_status`

#### Compute Engine Task (ceTask)

This endpoint is used to retrieve the analysis id of a prior sonar-maven-plugin run.

**_Endpoint_**

`api/ce/task`

**_URL Parameters_**
 
| Parameter          | Supported |
|--------------------|-----------|
| `id`               | yes       |
| `additionalFields` | no        |

**_Documentation_** 
  
* on [sonarcloud.io Web-API](https://sonarcloud.io/web_api/api/ce/task)
* in your SonarQube instance: `<sonar.host.url>/web_api/api/ce/task`

### Maven calls

#### Release 

**_prepare_** - manage `pom.xml` and create proper commits and tag and push to remote

```
mvn -B release:prepare -DdevelopmentVersion=1-SNAPSHOT -DreleaseVersion=<release version> -Dtag=v<release version>
```

**_perform_** - checkout version tag, create binaries and deploy via oss.sonatype.org to Maven Central

```
mvn -B release:perform
```

**_clean_** - remove backup/work file (useful if you ran prepare but not perform)

```
mvn -B release:clean
```

### How-To release

1. Update `CHANGELOG.md`: add a section for the upcoming version and move all "unpublished" changes to it
2. Update `README.md`: replace all occurrences of previous version number with upcoming version
3. persist:
   `git add CHANGELOG.md README.md && git commit -m "prepare for release: update CHANGELOG.md/README.md" && git push`
4. create release in git repo:
   `mvn -B release:prepare -DdevelopmentVersion=1-SNAPSHOT -DreleaseVersion=1.1.0 -Dtag=v1.1.0`
5. create and publish binaries:
   `mvn -B release:perform -DreleaseProfiles=build-for-release`
6. Create new release on GitHub ([here](https://github.com/r0bb3n/sonar-quality-gate-maven-plugin/releases/new))
   - choose tag: v1.1.0
   - set title: 1.1.0
   - copy the `CHANGELOG.md` content of the released version
   - upload the files `./target/checkout/target/*.(pom|jar|asc)`
7. publish [staging repository of oss nexus repository](https://oss.sonatype.org/#stagingRepositories)
   (login required) to maven
   central ([overview doc](https://central.sonatype.org/pages/ossrh-guide.html#releasing-to-central)
   / [detailed doc](https://central.sonatype.org/pages/releasing-the-deployment.html))
   1. check content of the staging repo
   2. select staging repo
   3. "Close" repo
   4. evaluation is now running, see repo tab "Activity"
   5. "Refresh" repo view
   6. "Release" repo
   7. copying is ongoing, see repo tab "Activity"
   8. "Refresh" repo view, staging repo will disappear after successful copy process
   9. verify that files are now available on the public release repo:
      [GAV search](https://oss.sonatype.org/#nexus-search;gav~io.github.r0bb3n~sonar-quality-gate-maven-plugin~1.*~~)
