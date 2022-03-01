# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - UNRELEASED
### Added
- Method to enable importing elements to a staging area
- Method to retrieve all imports
- Method to get import status by ID
- Method to get all import members
- Method to get an imported element by id
- Method to get all members of a stagedElement by id
- Method to enable converting the stagedElements to drafts
- Method to delete an import by id
- Method to return all available paths for a given element
### Changed
### Deprecated
### Removed
### Fixed
### Security

## [1.2.1] - 2021-12-09
### Fixed
- correctly return namespace members

## [1.2.0] - 2021-11-24
### Added
- retrieving namespace members can optionally exclude elements which are also in a group
### Changed
- update gson, lombok, jooq and json-schema-validator to latest versions

## [1.1.0] - 2021-10-12
### Changed
- don't expose namespace id
- filtering namespaces or elements no longer condenses array of definitions to a single list
### Fixed
- namespace members are correctly returned after namespace update

## [1.0.0] - 2021-09-24
### Added
- initial version
