# Task 2: Vuetify 2.7.1 advisory backports

Date: 2026-07-13

## Scope

Local runtime backports preserve Vuetify 2.7.1 while addressing `GHSA-3jp5-5f8r-q2wg` and `GHSA-9w3x-85mw-4fwm`. `patch-package` reapplies the changes after every clean install.

## TDD evidence

RED commit `241f513f` added two executable Jest regressions. Before the patch, `npm run test:unit -- --runInBand tests/unit/vuetify-security.spec.js` ran both tests and failed because `mergeDeep` copied inherited/forbidden keys and the malicious date string was assigned to `domProps.innerHTML`.

After applying `patches/vuetify+2.7.1.patch`, the same command passed 2/2 tests. A subsequent `npm ci` printed `vuetify@2.7.1 ✔`, proving automatic replay, and the full unit run passed 5/5 suites and 37/37 tests.

| Guarantee | Test | RED | GREEN |
|---|---|---:|---:|
| Deep merge accepts own safe keys but rejects inherited, `__proto__`, `prototype`, and `constructor` keys | `tests/unit/vuetify-security.spec.js` | FAIL | PASS |
| Date formatter output is represented as text and never assigned to `innerHTML` | `tests/unit/vuetify-security.spec.js` | FAIL | PASS |

## Verification

- `npm ci`: exit 0; patch-package replayed `vuetify@2.7.1`.
- `npm run test:unit -- --runInBand`: 5 suites, 37 tests, all passed.
- `npm run build`: exit 0 with the configured 8 GiB heap; build/type checking completed in 139.6 seconds. Existing bundle-size and CSS-order warnings remain warnings.
- `npm audit --omit=dev --registry=https://registry.npmjs.org`: exit 1 with 20 findings (6 low, 13 moderate, 1 high). The report does not list either backported Vuetify advisory as a direct finding. It still lists Vuetify as a dependency path for the distinct unfixed Vue 2 ReDoS advisory. No suppression was introduced.

Logs are stored at `/root/komga-task2-vuetify-{npm-ci,tests,build,audit}.log`.
