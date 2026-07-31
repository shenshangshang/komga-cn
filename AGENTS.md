# Project Agent Guide

## Project overview

- Name: 神殇漫画 / Komga-CN.
- Purpose: self-hosted Chinese comics and eBook media server derived from Komga and
  `dyphire/komga-cn`.
- Scope: backend APIs, web UI, readers, metadata/scanning, MySQL persistence,
  Docker packaging, and local deployment documentation.
- Supported runtime platforms: Linux containers are the primary release target;
  JVM development is supported wherever JDK 21 and Node.js 20 are available.
- Maturity: actively customized production fork. Preserve upstream compatibility
  where it does not conflict with the documented MySQL and directory-comic design.

## Toolchain and dependency policy

- Kotlin 2.2.0 on JDK 21.
- Spring Boot 3.5.14.
- Gradle Wrapper 8.14.3; use `./gradlew`, never an unpinned system Gradle.
- Vue 2.6.14, Vuetify 2.6.12, TypeScript 4.9.5.
- Node.js 20, as constrained by `.nvmrc`; npm is the frontend package manager.
- `komga-webui/package-lock.json` is authoritative and must remain committed.
- Pin production container bases and preserve lockfiles. Do not update unrelated
  dependencies during feature work.

## Architecture and dependency direction

- `komga/`: Spring Boot backend, REST APIs, domain services, tasks, persistence,
  Flyway migrations, readers' backend support, and packaged web assets.
- `komga-webui/`: Vue/Vuetify single-page application. It consumes backend APIs
  and must not duplicate domain or permission logic.
- `komga/src/main/kotlin/org/gotson/komga/domain/`: domain models, persistence
  interfaces, and services. Domain services depend on persistence interfaces.
- `komga/src/main/kotlin/org/gotson/komga/infrastructure/`: MySQL/JOOQ, search,
  security, filesystem, image, and configuration implementations.
- `komga/src/main/kotlin/org/gotson/komga/interfaces/`: HTTP/API adapters.
- `komga/src/main/kotlin/org/gotson/komga/application/tasks/`: asynchronous task
  scheduling and handlers.
- Infrastructure and interfaces may depend inward on domain contracts. Avoid
  introducing HTTP, Vue, or JOOQ concerns into domain models.
- Public routes, DTO shapes, Vuex state, permissions, readers' navigation,
  prefetch, gestures, shortcuts, and progress persistence are compatibility
  boundaries unless the task explicitly changes them.

## Important design decisions

- MySQL is used for the primary and task data sources. They may share one schema,
  but use separate Hikari pools.
- Flyway owns schema migrations. Migrations must be forward-safe and reviewed for
  rollback compatibility before release.
- Media paths are case-sensitive and unique by library plus URL/path.
- Image directories are first-class books. Deletion must remain path-bounded,
  idempotent, and must never remove sibling or descendant books.
- Nested folders are represented in browsing and download flows without changing
  existing public API contracts.
- `/config` and `/data` are stable container mount points; port `25600` is the
  default HTTP port.
- The AURORA token system is the active UI design baseline. Do not reintroduce
  INKFRAME styling or mix unrelated visual systems.

## Repository layout

- `komga/`: backend source, migrations, tests, and packaged application.
- `komga-webui/`: frontend source, unit tests, locales, and npm lockfile.
- `docs/`: project and upstream documentation.
- `.github/`: CI and repository automation.
- `scripts/`: maintenance/build helpers.
- `Dockerfile`: runtime image assembled from the built backend JAR.
- `README.md`: user-facing installation and operation guide.
- `DOCKERHUB.md`: Docker Hub repository overview.
- `design-system/`: AURORA design references and tokens.

## Canonical commands

Backend and production artifact:

```bash
SKIP_TYPECHECK=true ./gradlew :komga:prepareThymeLeaf :komga:bootJar --no-daemon --max-workers=2
```

Targeted backend tests:

```bash
./gradlew :komga:test --tests 'fully.qualified.TestClass' --no-daemon --max-workers=2
```

Frontend setup, lint, tests, and build:

```bash
cd komga-webui
npm ci
npm run lint
npm run test:unit -- --runInBand
npm run build
```

Docker build:

```bash
docker build -t shenshangshang/komga-cn:<git-sha> .
```

There is no separate repository-wide formatter command documented. Respect
`.editorconfig`, existing Kotlin formatting, ESLint, and nearby conventions.

## Configuration and secrets

- Runtime configuration comes from environment variables or files mounted under
  `/config`.
- Required database variables:
  `KOMGA_DATABASE_URL`, `KOMGA_DATABASE_USERNAME`,
  `KOMGA_DATABASE_PASSWORD`, `KOMGA_TASKS_DB_URL`,
  `KOMGA_TASKS_DB_USERNAME`, and `KOMGA_TASKS_DB_PASSWORD`.
- Pool controls:
  `KOMGA_DATABASE_POOL_SIZE`, `KOMGA_DATABASE_MAX_POOL_SIZE`,
  `KOMGA_TASKS_DB_POOL_SIZE`, and `KOMGA_TASKS_DB_MAX_POOL_SIZE`.
- Other documented controls include `TZ`, `CHS`, `JAVA_TOOL_OPTIONS`,
  `KOMGA_CONFIGDIR`, and `KOMGA_PREFETCH_PAGES`.
- Never commit passwords, tokens, private hostnames/IPs, `.env` files, Docker
  auth JSON, Git credentials, or production Compose overrides.
- Do not place credentials in this file or any Agent/Skill/MCP configuration.

## Data, migrations, and rollback

- JOOQ-generated code follows Flyway schema definitions; regenerate it through
  the existing Gradle tasks when migrations change.
- Do not manually modify production rows as a substitute for a durable migration.
- Preserve MySQL `utf8mb4` support and case-sensitive media-path behavior.
- Before deployment, back up MySQL and `/config`; media storage requires its own
  backup or snapshot policy.
- Keep the previous image tag available. Roll back the Compose image only after
  checking that new Flyway migrations remain backward-compatible.

## Coding, errors, logging, performance, and security

- Prefer small domain services, constructor injection, immutable data, and
  explicit repository interfaces.
- Keep filesystem mutations bounded to normalized library paths. Validate all
  paths before the first destructive operation.
- Deletion and repeated requests must be idempotent or fail without collateral
  mutation.
- Preserve authorization annotations and server-side permission checks.
- Never log credentials, authentication headers, private tokens, or full secrets.
- Use structured, actionable log messages with IDs and safe paths only when
  operationally necessary.
- Avoid unbounded database pools, in-memory archive assembly, and blocking
  downloads. Preserve streaming and backpressure behavior.
- Frontend touch targets, keyboard focus, reduced-motion support, contrast, and
  375/768/1024/1440px behavior are acceptance requirements.

## Test strategy and definition of done

- Write a failing regression test before fixing a defect when practical.
- Backend domain/path logic requires focused JUnit tests; persistence changes
  require migration/JOOQ integration coverage.
- Frontend changes require ESLint and Jest coverage for new behavior.
- Reader changes require regression checks for page navigation, prefetch,
  gestures, direction, fullscreen, shortcuts, and progress persistence.
- Production work is done only when:
  - relevant tests pass;
  - frontend lint passes;
  - production web build and `bootJar` succeed;
  - `git diff --check` is clean;
  - no secrets are present in tracked files;
  - the image is tagged with the Git SHA;
  - the deployed container is healthy with zero restarts;
  - `/` returns HTTP 200, `/actuator/health` is `UP`, and an anonymous protected
    API request returns 401;
  - README/DOCKERHUB/this guide match changed configuration or operations.

## Deployment and release

- Build from a clean commit and tag the image with the 12-character Git SHA.
- Verify the SHA image before moving `latest`.
- Push immutable SHA/version tags first; push `latest` only after health checks.
- Keep Docker and GitHub credentials in their native credential stores, never in
  source, Compose, documentation, shell history, or Agent files.
- Deploy by updating the production Compose image, retain the previous Compose
  and image tag, run health checks, and roll back immediately on core failures.
- CI should run lint, unit/integration tests, production build, secret scanning,
  image build, and health smoke checks before release publication.

## Required agent workflow

- Use the machine-global Skill Depot at
  `C:\Users\shenshang\.skill-depot`; project-local Skill Depot stores are
  disabled.
- Search Skill Depot with focused task context before non-trivial engineering or
  deployment work, then read the selected global package completely.
- CodeGraph is initialized at repository root in `.codegraph/`.
- Run `codegraph explore "<focused symbols or question>"` before grep/find/source
  reads for architecture, call-flow, or impact discovery.
- Run `codegraph sync "<repository-root>"` after material source changes.
- CodeGraph status: initialized on 2026-07-31 with CodeGraph 1.5.0; initial index
  contained 913 files, 14,147 nodes, and 34,122 edges.
- Expected global MCP servers are Headroom, CodeGraph, and Skill Depot. If absent
  in a new Codex session, run the canonical global MCP repair script and restart
  Codex/new task to expose the repaired tools.

