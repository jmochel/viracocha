# Read Me

vira (Viracocha): A CLI that maps both AI configuration artifacts and template folders and files into a project. Allows AI configuration artifacts to be synced back to their source.

## GraalVM native image

Prerequisites: [GraalVM for JDK 21](https://www.graalvm.org/) (or another distribution) with `native-image` installed (`gu install native-image` on older GraalVM bundles).

Build a standalone binary named `vira` in `target/`:

```bash
./mvnw -DskipTests -Pgraalvm-native package
./target/vira --help
```

JVM workflow is unchanged: `./mvnw -DskipTests package` then `java -jar target/viracocha-0.1.jar`, or use `scripts/vira` from the repo root.

# Tech Stack


## Micronaut 4.10.10 Documentation

- [User Guide](https://docs.micronaut.io/4.10.10/guide/index.html)
- [API Reference](https://docs.micronaut.io/4.10.10/api/index.html)
- [Configuration Reference](https://docs.micronaut.io/4.10.10/guide/configurationreference.html)
- [Micronaut Guides](https://guides.micronaut.io/index.html)
---

- [Micronaut Maven Plugin documentation](https://micronaut-projects.github.io/micronaut-maven-plugin/latest/)
## Feature maven-enforcer-plugin documentation


- [https://maven.apache.org/enforcer/maven-enforcer-plugin/](https://maven.apache.org/enforcer/maven-enforcer-plugin/)


## Feature github-workflow-ci documentation


- [https://docs.github.com/en/actions](https://docs.github.com/en/actions)


## Feature lombok documentation


- [Micronaut Project Lombok documentation](https://docs.micronaut.io/latest/guide/index.html#lombok)


- [https://projectlombok.org/features/all](https://projectlombok.org/features/all)


## Feature serialization-jackson documentation


- [Micronaut Serialization Jackson Core documentation](https://micronaut-projects.github.io/micronaut-serialization/latest/guide/)


