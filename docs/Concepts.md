# Concepts

In this document all relevant decisions are described.

## Site generation and hosting

### Requirements

- keep track of changed/added parameters in new versions
  - the initial idea of keeping all site versions available by using different sub folders was omitted. Rather the 
    `@since` declaration will be used to indicate new functionality. In addition to that, the pages will be hosted 
    as git repo anyway, so that at the end also this history could be used to restore documentation of previous 
    versions.
- included in release process
- hosting on GitHub

### Approach #1: `/docs` folder

Generate and commit into (version-based folders inside) `/docs` of the source repo.

#### Pros

- bound/tagged with related sources

#### Cons

- (generated) HTML code as part of the sources
- no straight-forward approach to include automatically

### Approach #2: `gh-pages` branch

Create a new long-running branch that only contains documentation in the root structure.

#### Pros

- separation of generated code from plugin source code
- support of that approach by [Apache Maven SCM Publish Plugin](https://maven.apache.org/plugins/maven-scm-publish-plugin/)

#### Cons

- no direct link to related sources
- Since it uses a dedicated folder as checkout target, all local git configs have no effect. Thus, it is required, to 
  have all relevant config as part of the `--global` level, when running the release process.
- A manual tag `v#.#.#-site` is required, to identify the exact documentation for a released version. 
