# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.2.6] - 2023-01-18
### Fixed
- Environment variables are not working in the controller classes [[#111](https://github.com/imi-frankfurt/dataelementhub.model/issues/111)]
  - This bug was the reason why the environment variable exportDirectory was not working
- Prevent multiple definitions of the same language when creating namespaces [[#113](https://github.com/imi-frankfurt/dataelementhub.model/issues/113)]
  - It should not be possible to store more than one definition per language and namespace

## [2.2.5] - 2022-11-29
### Security
- Updated tomcat-embed-core and dehub-dal

## [2.2.4] - 2022-11-23
### Changed
- Do not ignore element path if the parent is outdated
### Fixed
- Do not skip already converted value domains on import

## [2.2.3] - 2022-11-03
### Security
- Update parent.pom to update to spring boot 2.7.5

## [2.2.2] - 2022-11-03
### Security
- Update dependencies, including jackson lib due to [CVE-2022-42003](https://devhub.checkmarx.com/cve-details/CVE-2022-42003/) and [CVE-2022-42004](https://devhub.checkmarx.com/cve-details/CVE-2022-42004/)

## [2.2.1] - 2022-11-03
### Fixed
- There was a bug in the search function that occurred when private namespaces from other users are present [[#105](https://github.com/mig-frankfurt/dataelementhub.model/issues/105)]

## [2.2.0] - 2022-09-29
### Fixed
- Elements can no longer have multiple definitions of the same language [[#95](https://github.com/mig-frankfurt/dataelementhub.model/issues/95)] [[#100](https://github.com/mig-frankfurt/dataelementhub.model/issues/100)]
- Record members of a draft record can be updated [[#98](https://github.com/mig-frankfurt/dataelementhub.model/issues/98)]
- Creating released elements with the placeholder value domain "tbd" is no longer possible  [[#103](https://github.com/mig-frankfurt/dataelementhub.model/issues/103)]
  - This being possible was unintentional
### Security

## [2.1.0] - 2022-07-19
### Fixed
- No longer erroneously mark elements as released when there was an exception [[#93](https://github.com/mig-frankfurt/dataelementhub.model/issues/93)]
  - There was a bug in the order things were done in the release method that could lead to an incorrect status

## [2.0.0] - 2022-07-12
### Added
- Export and Import functionality [[#8](https://github.com/mig-frankfurt/dataelementhub.model/issues/8)] [[#9](https://github.com/mig-frankfurt/dataelementhub.model/issues/9)] [[#33](https://github.com/mig-frankfurt/dataelementhub.model/issues/33)] [[#38](https://github.com/mig-frankfurt/dataelementhub.model/issues/38)]
    - Users can export their namespaces (or parts of them) to either JSON or XML files, and they can import elements to their namespaces as well (given the same data format as the export)
- Method to update the members of dataElementGroup/record [[#17](https://github.com/mig-frankfurt/dataelementhub.model/issues/17)]
  - This allows to check if a dataelementgroup contains members that are outdated, and then update the members to their latest revisions
- Method to return all available paths for a given element [[#23](https://github.com/mig-frankfurt/dataelementhub.model/issues/23)] [[#63](https://github.com/mig-frankfurt/dataelementhub.model/issues/63)]
  - Elements can be contained in multiple groups or records. This returns a list of all paths to the given element.
- Namespace access management [[#13](https://github.com/mig-frankfurt/dataelementhub.model/issues/13)]
  - Namespace administrators can allow other users to access their namespaces (read/write)
### Changed
- Separate namespaceService from elementService [[#28](https://github.com/mig-frankfurt/dataelementhub.model/issues/28)]
  - those are separate endpoints in dehub-rest, so they were split up here as well for consistency.
- replaced CloseableDSLContext with DSLContext [[#89](https://github.com/mig-frankfurt/dataelementhub.model/issues/89)]
### Removed
- element type "STAGED"
### Security
- updated libraries

## [1.2.1] - 2021-12-09
### Fixed
- correctly return namespace members [[#15](https://github.com/mig-frankfurt/dataelementhub.model/issues/15)]
  - there was a bug in an sql statement that caused trouble with dataelements/groups/records with higher revisions than the namespace revision

## [1.2.0] - 2021-11-24
### Added
- retrieving namespace members can optionally exclude elements which are also in a group [[#10](https://github.com/mig-frankfurt/dataelementhub.model/issues/10)]
  - especially when lazily loading and expanding a hierarchical structure in the GUI, it is helpful to get only elements that are direct "descendants" (as seen from a tree-view perspective) of a group
### Changed
- update gson, lombok, jooq and json-schema-validator to latest versions

## [1.1.0] - 2021-10-12
### Changed
- don't expose namespace id [[#3](https://github.com/mig-frankfurt/dataelementhub.model/issues/3)]
  - the internal namespace id was delivered with each element. While not being harmful, this also brought no benefit for users and could potentially leave them confused.
- filtering namespaces or elements no longer condenses array of definitions to a single list [[#5](https://github.com/mig-frankfurt/dataelementhub.model/issues/5)]
  - this was a requirement given by the GUI developers, so it was changed.
### Fixed
- namespace members are correctly returned after namespace update [[#1](https://github.com/mig-frankfurt/dataelementhub.model/issues/1)]
  - there was a problem with the namespace identifiers in the scoped identifier table when namespaces were updated.

## [1.0.0] - 2021-09-24
### Added
- initial version
