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
