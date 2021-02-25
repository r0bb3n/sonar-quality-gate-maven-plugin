# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

--

[unreleased]: https://github.com/r0bb3n/sonar-quality-gate-maven-plugin/compare/v1.1.0...HEAD

## [1.1.0]

### Added

- support multi module projects ([#55](https://github.com/r0bb3n/sonar-quality-gate-maven-plugin/issues/55))
- test coverage increased a lot ([#31](https://github.com/r0bb3n/sonar-quality-gate-maven-plugin/issues/31))

[1.1.0]: https://github.com/r0bb3n/sonar-quality-gate-maven-plugin/compare/v1.0.2...v1.1.0

## [1.0.2]

First version to be published to Maven Central. This requires some modification as
 described in the [Sonatype Guide][sonatype-guide].

### Changed

- `groupId` changed to `io.github.r0bb3n` to enable maven central deployment

[1.0.2]: https://github.com/r0bb3n/sonar-quality-gate-maven-plugin/compare/v1.0.1...v1.0.2

## [1.0.1]

First version to be published to Maven Central. This requires some modification as
 described in the [Sonatype Guide][sonatype-guide].

### Added

- provide `sources.jar` and `javadoc.jar` and signatures for all build artifacts automatically

### Changed

- `groupId` changed to `io.github.r0bb3n.maven` to enable maven central deployment

[1.0.1]: https://github.com/r0bb3n/sonar-quality-gate-maven-plugin/compare/v1.0.0...v1.0.1
[sonatype-guide]: https://central.sonatype.org/pages/producers.html

## [1.0.0]

### Added

- three modes defined: integrated, simple, advanced
- integrated mode: 
  - read meta data from previous run of sonar-maven-plugin to find correct analysis job
  - wait for analysis evaluated in SonarQube (polling for status updates)

### Changed

- parameters renamed to avoid unintended interferences with sonar-maven-plugin parameters

[1.0.0]: https://github.com/r0bb3n/sonar-quality-gate-maven-plugin/compare/v0.1.0...v1.0.0

## [0.1.0]

### Added

- check project status in SonarQube based on supplied parameters (e.g. branch name)
- authorization support for SonarQube

[0.1.0]: https://github.com/r0bb3n/sonar-quality-gate-maven-plugin/releases/tag/v0.1.0
