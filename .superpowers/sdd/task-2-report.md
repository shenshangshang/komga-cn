# Task 2 report — Komga-CN 1.26 executable baseline

## Status

**DONE_WITH_CONCERNS.** Route/hash evidence and runtime smoke are established. The requested full Gradle gate is red because the baseline contains extensive ktlint violations. The Web command exits 0 but logs a type-check worker OOM. No blocking P0/P1 security defect was established.

## Files and commits

- `docs/superpowers/audits/1.26-baseline.md`
- `scripts/verify-route-parity.sh`
- `scripts/smoke-komga.sh`
- `.superpowers/sdd/task-2-report.md`
- Commit range: recorded below after commit.

## Verification commands and results

1. Route extraction with `sed -nE ... komga-webui/src/router.ts | LC_ALL=C sort -u > /root/komga-1.26-routes.txt`: PASS, 41 routes.
2. Kotlin SHA-256 pipeline (`find komga komga-tray -type f -name '*.kt' ... sha256sum`): PASS, 550 records.
3. Web service/store SHA-256 pipeline: PASS after correcting the source shape to `src/services` plus `src/store.ts` (there is no `src/store/` directory).
4. `scripts/verify-route-parity.sh /root/komga-1.26-routes.txt`: PASS, no diff.
5. `cd komga-webui && npm ci && npm run build`: command exit 0; `npm ci` installed 1,658 packages and webpack compiled with 52 warnings, but ForkTsChecker terminated with V8 heap OOM near 4 GiB. CONCERN: exit status is a false-green for type checking.
6. `./gradlew clean test build`: FAIL at `:komga:ktlintMainSourceSetCheck`; numerous existing style violations across 16 Kotlin files. A trial `./gradlew ktlintFormat` changed 16 files (144 insertions/102 deletions) and still failed on seven non-auto-correctable BookAnalyzer issues; the entire trial diff was restored to avoid unrelated churn.
7. `docker run -d --name komga-task2-smoke -p 127.0.0.1:25610:25600 shenshangshang/komga-cn:1.26.0` followed by smoke script: PASS (`health=UP`, root SPA HTML, unauthenticated users/me=401). Container removed afterward.
8. `npm audit --omit=dev --audit-level=high`: INCONCLUSIVE; configured `registry.npmmirror.com` returns 404 NOT_IMPLEMENTED for the audit endpoint.

## Audit coverage and findings

Commands included `git grep` scans for secret-like assignments, unsafe HTML/URL sinks, concurrency primitives, authorization annotations/security matchers, MySQL/prefetch paths, plus Dockerfile directive review and dependency audit.

- Secrets: no literal credential was identified. Workflow references use `${{ secrets.* }}` and configuration references environment variables. No credential was written.
- Authorization/controllers: global security configuration authenticates remaining endpoints; health is intentionally public; admin/page-stream/download roles are applied to sensitive controllers. Runtime boundary returned 401.
- Unsafe rendering/URL handling: several `v-html` sinks exist (announcements, releases, dialog/card bodies). Some are translations or server-derived HTML. No exploit was established in this bounded audit; provenance/sanitization remains a P2 follow-up risk. New-tab calls use `noopener`.
- Concurrency/prefetch: the prefetch cache/service and synchronized SSE structures were inspected by targeted search. Prefetch count is bounded by configuration, but the cache/lifecycle deserves load and eviction testing; no focused failure was reproduced.
- MySQL: datasource/JOOQ/Flyway paths explicitly use MySQL. Full tests did not reach completion because lint runs first.
- Error handling/reader lifecycle: no blocking finding established; coverage was static and build/runtime smoke only, not an interactive reader stress test.
- Dockerfile: image runs without a `USER` directive (root). Rated security P2 hardening debt; changing runtime ownership requires compatibility testing for `/config` and `/data`, so it was not altered in this baseline task.
- Dependencies: lockfile exists and `npm ci` is reproducible, but vulnerability status is unknown because the mirror lacks npm audit support. GitHub dependency source is branch-like rather than immutable commit pin, a supply-chain follow-up risk.

## Code review self-review

Preflight confirmed a clean feature worktree before Task 2. Implementation is POSIX shell, quotes paths/URLs, uses `mktemp` with cleanup, requires exactly one route baseline argument, and makes smoke retries finite. No UI refactor or Task 3 work was performed. The broad formatter output was explicitly discarded.

## Residual risks

1. Required Gradle full build remains red on baseline lint debt.
2. Web type-checking OOM is masked by a zero command exit.
3. Dependency CVE audit is incomplete due to registry capability.
4. Existing HTML sinks, root container execution, reader lifecycle, and prefetch concurrency need focused follow-up tests.

## Re-review remediation (2026-07-12)

Status changed to **BLOCKED** after executing the previously unreachable test suite.

- Generated project-native ktlint baselines with `./gradlew ktlintGenerateBaseline`; this adds only `config/ktlint/baseline.xml` and `komga/config/ktlint/baseline.xml`, with no broad source formatting.
- Fixed a source/test API compatibility defect by defaulting `BookAnalyzer.analyze(..., adPagesDetector)` to `false`. `./gradlew :komga:compileTestKotlin` then exited 0.
- Exact `./gradlew clean test build` advanced through lint and compilation but failed after 898 tests: 50 failures. Representative signatures are duplicate background task execution and `DataIntegrityViolationException` deleting `USER` while `AUTHENTICATION_ACTIVITY` references it. Logs show all test contexts using shared `komga`/`komga_tasks` MySQL schemas. Destructively resetting a shared database is unsafe and is not an acceptable build implementation.
- During root-cause analysis, `komga/src/main/resources/application.yml` was found to contain a literal MySQL endpoint/password. It was replaced with `KOMGA_DATABASE_*` and `KOMGA_TASKS_DB_*` environment placeholders. No replacement credential was written.
- Frontend hard gate: `NODE_OPTIONS=--max-old-space-size=8192 npm run build` completed type checking and emitted `DONE Build complete`, exit 0 in 183.7s. The package build command now invokes Node with the same 8192 MiB heap so `npm run build` is self-contained.
- Official registry audit: `npm audit --registry=https://registry.npmjs.org --omit=dev --audit-level=high` completed and reported 23 vulnerabilities (2 critical, 3 high, 12 moderate, 6 low). The critical `form-data` chain and high `pdfjs-dist` chain are transitive through the reader fork and have no published fix in this graph. The lockfile resolves the GitHub dependency immutably to commit `3ebc8cddc83f1249d12135717f87b0ab1e9a9888`, despite the package manifest's branch label.
- Route script now validates a readable regular baseline, rejects zero extraction, and reports count collapse before diff. Smoke script validates positive attempts, bounds every curl, retains last health diagnostics, and requires Komga app plus asset markers.
- Dockerfile now creates and runs as `komga`, with `/app`, `/config`, and `/data` owned by that account. Runtime rebuild/smoke remains pending because the mandatory source build cannot produce a fully verified artifact.

### Blocking conditions

1. Tests require isolated MySQL schemas/containers with migration lifecycle; current test configuration points at shared schemas and is destructive/non-repeatable. Implementing safe ephemeral schema provisioning is larger than a narrow baseline correction and requires infrastructure authority/configuration.
2. Official dependency audit contains unpatched critical/high reader-chain vulnerabilities. Replacing/forking the reader dependency requires a compatibility/security project, not an unreviewed baseline pin change.
3. Therefore the exact Gradle gate cannot honestly be reported green, and Task 2 must not advance.

## Isolated MySQL re-run

An ephemeral `mysql:8.4` container was started with `/var/lib/mysql` on tmpfs, dedicated `komga` and `komga_tasks` schemas, and loopback-only port 33306. Spring's supported `KOMGA_DATABASE_*` and `KOMGA_TASKS_DB_*` environment bindings pointed the exact Gradle command at it. The container was removed after the run; no volume remained and the shared MySQL server was never contacted or modified.

The exact command reached tests and failed: 475 executed, 54 failed. This disproves the earlier shared-database-only hypothesis and identifies incomplete MySQL porting in the fork:

- `NamingConventionTest`: `PagePrefetchService` violates the project's domain naming architecture rule.
- `DataSourcesConfigurationTest.MemoryMode/WalMode`: tests still require removed `sqliteDataSourceRO` beans.
- DAO/domain assertions expose MySQL timestamp precision and sort/metadata semantic differences.
- Fresh-schema tests leak API keys, collections, and read lists between cases; cleanup ordering/coverage is incomplete.
- Subsequent Spring contexts hit the failure threshold and account for many cascade failures.

The enhanced smoke test was also run against a fresh `shenshangshang/komga-cn:1.26.0` container with tmpfs `/config` for 240 seconds. The process remained running and completed index bootstrap, but `/actuator/health` repeatedly reset the connection and never returned UP. The script correctly failed and retained the last curl diagnostic; container logs were collected before removal. This published image is therefore not a reproducible healthy runtime baseline in the current environment.

## MySQL temporal precision remediation (2026-07-13)

A fresh `mysql:8.4` container used tmpfs for `/var/lib/mysql`, loopback port 33307, and dedicated `komga`/`komga_tasks` schemas. The initial jOOQ cluster ran 178 tests with 12 failures. Eight update persistence tests showed the same production defect: MySQL `DATETIME` truncated audit timestamps to seconds, so rapid updates could retain the same `LAST_MODIFIED_DATE`.

The MySQL bootstrap schema now uses `DATETIME(6)` and `CURRENT_TIMESTAMP(6)`. Migration `V2__datetime_microsecond_precision.sql` upgrades every existing temporal column without dropping nullability or defaults. The focused rerun covered BookDao, BookMetadataAggregationDao, KomgaUserDao, LibraryDao, ReadListDao, ReadProgressDao, SeriesDao, SeriesMetadataDao, and SeriesSearch: 75 tests ran and all eight temporal update assertions passed. Two independent filtering/search failures remain for the next semantics cluster; assertions were not relaxed.
