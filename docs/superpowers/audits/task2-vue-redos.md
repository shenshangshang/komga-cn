# Task 2: Vue 2.7.14 parseHTML ReDoS backport

Date: 2026-07-13

## Root cause and threat boundary

GitHub-reviewed `GHSA-5j4c-8p2g-v4jx` / `CVE-2024-9506` affects Vue 2's `parseHTML` raw-text-element branch. Its dynamic expression `([\\s\\S]*?)(</TAG[^>]*>)` backtracks excessively when a raw-text element has no matching closing tag and hostile text contains many `<` characters. Vue 3's parser uses index-based scanning instead of this expression.

Webpack stats for the production build contains `./node_modules/vue/dist/vue.runtime.esm.js` and no Vue compiler/html-parser module; searches for compiler warning strings also found no production JavaScript bundle hit. Thus the browser-delivered Komga UI does not expose runtime template compilation. The parser is nevertheless reachable during SFC compilation, so both the Vue compiler distributions and `vue-template-compiler` build entry are patched to close the build-time availability risk.

## TDD evidence

RED commit `dfe65a9e` added a bounded Jest regression. It launches compilation in a child process with a hard 1,000 ms timeout, so a regression cannot hang Jest. Unpatched Vue 2.7.14 exceeded that bound; the legal raw-text regression passed.

GREEN commit `6453ba1b` replaces the backtracking expression with case-insensitive `indexOf` scans for the matching close-tag prefix and its terminating `>`, then preserves the existing text normalization, callbacks, offsets, and end-tag handling. No template-size limit or audit suppression was added. The focused test passes 2/2 across runtime-compiler and build-time compiler paths.

## Verification

- `npm ci`: exit 0; `patch-package` replayed the Vue, Vue template compiler, and Vuetify patches.
- Full unit run: 6 suites / 20 tests passed.
- `npm run build -- --report-json`: exit 0 with the configured 8 GiB heap, including type checking; final clean-install build completed in 138.528 seconds.
- Official-registry `npm audit --omit=dev --registry=https://registry.npmjs.org`: exit 1 with 20 findings (6 low, 13 moderate, 1 high). Registry metadata still reports the Vue advisory because it evaluates package versions and cannot recognize local `patch-package` backports.

Evidence: `/root/komga-task2-vue-redos-{red,green-focused,npm-ci,tests,build}.log`, `/root/komga-task2-vue-redos-audit.json`, and `dist/report.json`.

## Residual risk

The project remains on unsupported Vue 2 and registry audit will continue to flag this version. The local fix depends on lifecycle scripts applying patches; CI must keep using `npm ci` without disabling scripts. Future dependency changes must retain replay checks and the bounded regression.
