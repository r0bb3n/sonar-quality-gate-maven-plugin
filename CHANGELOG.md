# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0]

### Added

- three modes defined: integrated, simple, advanced
- integrated mode: 
  - read meta data from previous run of sonar-maven-plugin to find correct analysis job
  - wait for analysis evaluated in SonarQube (polling for status updates)

### Changed

- parameters renamed to avoid unintended interferences with sonar-maven-plugin parameters

## [0.1.0]

### Added

- check project status in SonarQube based on supplied parameters (e.g. branch name)
- authorization support for SonarQube


[unreleased]: https://github.com/r0bb3n/sonar-quality-gate-maven-plugin/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/r0bb3n/sonar-quality-gate-maven-plugin/compare/v0.1.0...v1.0.0
[0.1.0]: https://github.com/r0bb3n/sonar-quality-gate-maven-plugin/releases/tag/v0.1.0
