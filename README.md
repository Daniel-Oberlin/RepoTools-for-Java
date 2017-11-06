# RepoTools for Java

RepoTools for Java is a port of a subset of "RepoTools for .NET".

RepoTools is a set of command line tools that can be used to create and maintain repositories of file-based data which are easy to validate against a file/hash manifest using modern cryptographic hash methods.  It serves a similar purpose to the Parchive utility, but uses concepts familiar from git.  A RepoTools repository may be used "live", and the tools can be used to update and maintain the repository, as well to compare and synchronize different copies of the repository.

This port of RepoTools for Java includes the repoTool command, but not the repoSync command.  I the Java version mainly to perform data validations locally on my NAS.  In the future, there may be a port of the repoSync tool for completeness.

## Getting Started

To get started, build the tools using Eclipse.  Once the tools are built, put them into your PATH.  There is a PDF file in "RepoTools for .NET" with an extensive overview, detailed instructions for and examples of use under the Documentation directory.  I'm planning to add a GitHub Wiki in the near future.

### Prerequisites

You'll need Eclipse to build the project files.  You'll need Java to execute the commands.

## Authors

* **Daniel Oberlin** - *Initial work* - [Daniel-Oberlin](https://github.com/Daniel-Oberlin)

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details

## Acknowledgments

* Inspired by git, but serving a similar function to the Parchive utility.
