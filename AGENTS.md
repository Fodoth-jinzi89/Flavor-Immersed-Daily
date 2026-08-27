# NeoForge 1.21.1 Agent Rules

## Project Structure

`src/main/java/` contains the mod implementation; keep compatibility code,
events, mixins, utilities, and other integrations grouped by responsibility.
Resources live in `src/main/resources/`; generated resources live in
`src/generated/resources/`.

## Build (JDK 21)

```text
./gradlew build
./gradlew runClient
./gradlew runServer
./gradlew genIntellijRuns
```

CI workflows are under `.github/workflows/`.

## Code Style

- Java 21 and the existing project formatting.
- PascalCase classes, camelCase methods/fields, UPPER_SNAKE_CASE constants.
- Reuse existing dependencies and keep imports explicit.

### Mixins

- Keep mixins under the existing mixin package and register changes in the
  project's mixin configuration.
- Prefix `@Unique` members with `fid$`.
- Prefer cancellable `@Inject` over `@Overwrite`; document any overwrite.
- Keep helper classes outside mixin classes.

## Agent Constraints

- Target NeoForge 1.21.1 only.
- Make surgical edits; do not reformat unrelated code.
- Put configuration in the configuration/resource system rather than scattered
  constants.
- Preserve existing modules and APIs; avoid speculative abstractions.

## Testing

There is no formal test suite. At minimum run `./gradlew build`; for gameplay
or compatibility changes, also test with and without the target mod when
practical.

## Git

- Use short descriptive commits.
- Update release notes only when explicitly requested.
