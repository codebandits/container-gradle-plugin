# Changelog

[![Keep a Changelog](https://img.shields.io/badge/Keep%20a%20Changelog-1.1.0-informational)](https://keepachangelog.com/en/1.1.0/)
[![Semantic Versioning](https://img.shields.io/badge/Semantic%20Versioning-2.0.0-informational)](https://semver.org/spec/v2.0.0.html)
[![clq validated](https://img.shields.io/badge/clq-validated-success)](https://github.com/denisa/clq-action)

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.2] - 2024-11-29

### Fixed

- Resolve project.exec deprecation warnings by replacing with ExecOperations

### Chore

- Upgrade to Gradle 8.11.1

## [0.1.1] - 2024-11-09

### Fixed

- Simplify the path evaluation for docker
- Lazily evaluate dockerRun configuration

## [0.1.0] - 2024-11-07

### Added

- Added privileged property to dockerRun configuration, allowing tasks to run with Docker's privileged mode.

## [0.0.1] - 2024-11-06

### Added

- Initial release of the Gradle plugin.
- ContainerRunTask: Allows container run operations to be executed as Gradle tasks.
- Support for declaring Docker containers as task inputs and outputs.
