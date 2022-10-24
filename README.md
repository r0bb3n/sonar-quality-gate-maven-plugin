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
  * see section [Why Java 11?](#why-java-11) for details
* SonarQube (depends on mode)
  * integrated: 5.3+ 
  * simple: 5.4+
  * advanced: 7.7+

## Usage

Include the plugin declaration in your `pom.xml`

```xml
<plugin>
  <groupId>io.github.r0bb3n</groupId>
  <artifactId>sonar-quality-gate-maven-plugin</artifactId>
  <version>1.3.0</version>
</plugin>
```

and execute the following command:

```
mvn sonar:sonar sonar-quality-gate:check
```

For **full reference** of the modes and parameters, please check the [Usage](https://r0bb3n.github.io/sonar-quality-gate-maven-plugin/usage.html)
and [Goals](https://r0bb3n.github.io/sonar-quality-gate-maven-plugin/plugin-info.html) page in the
[sonar-quality-gate-maven-plugin documentation](https://r0bb3n.github.io/sonar-quality-gate-maven-plugin/).

## Internals

### Why Java 11?

**TL;DR** usage of HttpClient (`java.net.http.HttpClient`)

Thinking about also supporting Java 8 led to the conclusion, that this would require relying on at least one additional
dependency (to maintain) for HTTP calls (e.g. Apache HttpClient), which is solved out-of-the-box in Java 11+ by simply
using `java.net.http.HttpClient`. Therefore, I decided to not support Java 8.

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

**_perform_** - checkout version tag, create binaries and deploy via oss.sonatype.org to Maven Central and create
documentation ("site") and push to `gh-pages` branch.

```
mvn -B release:perform -DreleaseProfiles=build-for-release
```

**_clean_** - remove backup/work file (useful if you ran prepare but not perform)

```
mvn -B release:clean
```

### How-To release

0. Ensure the right git configs (username, email, signing stuff, ...) are set on `--global` level (correct `.gitconfig`
   file in user home) - config done on level `--local` is not respected by additional steps that take place in 
   subdirectories of `target` with fresh full-checkouts of the repo (e.g. commits/push during `site-deploy` via
   `maven-scm-publish-plugin`).
1. Update `CHANGELOG.md`: add a section for the upcoming version and move all "unpublished" changes to it
2. Update `README.md`: replace all occurrences of previous version number with upcoming version
3. Persist:
   `git add CHANGELOG.md README.md && git commit -m "prepare for release: update CHANGELOG.md/README.md" && git push`
4. Create release in git repo:
   `mvn -B release:prepare -DdevelopmentVersion=1-SNAPSHOT -DreleaseVersion=1.3.0 -Dtag=v1.3.0`
5. Create and publish binaries and documentation:
   `mvn -B release:perform -DreleaseProfiles=build-for-release`
6. Create and push a tag for the site branch. Change into directory `./target/checkout/target/scmpublish-checkout` and
   fire the according commands: `git tag v1.3.0-site && git push origin v1.3.0-site`
7. Create new release on GitHub ([here](https://github.com/r0bb3n/sonar-quality-gate-maven-plugin/releases/new))
   - choose tag: v1.3.0
   - set title: 1.3.0
   - copy the `CHANGELOG.md` content of the released version
   - upload the files `./target/checkout/target/*.(pom|jar|asc)`
8. publish [staging repository of oss nexus repository](https://oss.sonatype.org/#stagingRepositories)
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
