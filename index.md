<div style="display: flex;justify-content: center;">
<picture align="center">
    <source media="(prefers-color-scheme: light)" srcset="https://summerofcode.withgoogle.com/assets/media/logo.svg">
    <source media="(prefers-color-scheme: dark)" srcset="https://summerofcode.withgoogle.com/assets/media/logo.svg">
    <img alt="Google Summer of Code" src="https://developers.google.com/open-source/gsoc/resources/downloads/GSoC-logo-horizontal.svg" height="40">
</picture>
<picture>
    <source media="(prefers-color-scheme: light)" srcset="https://scala.epfl.ch/resources/img/scala-center-logo-black.png">
    <source media="(prefers-color-scheme: dark)" srcset="https://scala.epfl.ch/resources/img/scala-center-logo.png">
    <img alt="Scala Center" src="https://scala.epfl.ch/resources/img/scala-center-logo.png" height="40">
</picture>
</div>
<p align="center">
    <h1 style="width:100%; text-align: center">Scaladex: Displaying information from POM files </h1>
</p>

### Project Abstract

The Scala ecosystem thrives on information accessibility, and Scaladex stands as a pivotal resource in this domain. However, the current scope of Scaladex's artifact pages does not fully exploit the wealth of metadata available in Maven pom files. This project aims to enhance Scaladex by extracting and presenting additional information from pom files, enriching the user experience and empowering developers with deeper insights into Scala artifacts.

- [GSoC Project Page](https://summerofcode.withgoogle.com/programs/2024/projects/4nuShODP)

- [GSoC Project Proposal](http://LinikToYourGSoCProjectProposal)

- [GitHub Organization Repo](http://github.com/scalacenter/scaladex)

- [GitHub Personal Repo](http://github.com/skingle/scaladex)

- [Commits during GSoC 2024](https://github.com/scalacenter/scaladex/commits/main/?author=skingle&since=2024-05-01&until=2024-08-21)

- [Project](https://github.com/users/skingle/projects/2?pane=info)

### Work Summary

| Pull Requests                                                                            |  Status   |                                  Issue                                  |
| --------------------------------------------------------------------------------------- | :-------: | --------------------------------------------------------------------- |
| [#12](https://github.com/skingle/scaladex/pull/12) Artifact model to include new fields                                                     | Merged ☑️ |  [scalacenter#979](https://github.com/scalacenter/scaladex/issues/979)  |
| [#16](https://github.com/skingle/scaladex/pull/16) Define new fields data structures and update database schema.                            | Merged ☑️ |  [scalacenter#979](https://github.com/scalacenter/scaladex/issues/979)  |
| [#18](https://github.com/skingle/scaladex/pull/18) UI Change: Add metadata fields (version scheme, developers, scala doc) on artifact pages | Merged ☑️ |  [scalacenter#979](https://github.com/scalacenter/scaladex/issues/979)  |
| [#20](https://github.com/skingle/scaladex/pull/20) Added an admin task to sync all artifacts for new metadata fields                        | Merged ☑️ | [scalacenter#1407](https://github.com/scalacenter/scaladex/issues/1407) |

#### Final pull requests
- https://github.com/scalacenter/scaladex/pull/1435
- https://github.com/scalacenter/scaladex/pull/1446

### What's Covered

-  **Enhance the Artifact Model:** Integrated new fields such as scaladocUrl and versionScheme as optional attributes to capture additional metadata from Maven pom files.
- **Improved Artifact Representation:** Updated the Artifact case class to accommodate the new fields, ensuring seamless integration within Scaladex's data structure.
- **Database Integration:** Implemented necessary database schema changes and modifications to support storing and querying the newly added fields, ensuring data consistency and integrity.
- **Serialization:** Developed serializers, leveraging libraries like circe, to facilitate smooth handling of complex data types such as developers' information. 
- **Testing:** Rigorously tested the reading and writing functionalities to ensure accurate extraction and storage of data from Maven pom files, including handling scenarios with missing or optional information.
- **Scheduled Job Implementation:** Implemented a scheduled job mechanism to periodically scan existing artifacts, identifying, and retrieving missing information from Maven pom files, thereby keeping Scaladex up-to-date and comprehensive.

#### Visual Changes
![image](https://github.com/user-attachments/assets/6ec5b110-5d92-492f-bef9-81ef5e97e29b)
![image](https://github.com/user-attachments/assets/b4c0f8e7-bf5c-4758-8f0f-e0ed93dd95b9)

