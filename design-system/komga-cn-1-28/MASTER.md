# Komga-CN 1.28 Design System

This file is the visual source of truth for Komga-CN 1.28. Page-specific files may override layout details but may not weaken accessibility, functional parity, or semantic token rules.

## Product model

- Product: personal comics and ebook library, reader, and server administration application.
- Audience: readers managing small or very large multilingual libraries; administrators operating the server.
- Stack: existing Vue and Vuetify architecture from Komga-CN 1.26.
- Style: Swiss Modernism 2.0 adapted to cover-led media browsing.
- Variance: 5/10; motion: 2/10; density: 8/10.

## Principles

1. Content first: cover artwork and titles lead; chrome stays quiet.
2. Complete functionality: visual modernization never hides or removes a 1.26 action or route.
3. Rational grid: use predictable columns, alignment, and 4/8 px spacing increments.
4. State clarity: selection, progress, unread, disabled, error, and focus states never depend on color alone.
5. Offline by default: no runtime font, icon, analytics, or styling CDN dependencies.

## Semantic colors

### Light

| Token | Value | Use |
|---|---:|---|
| `surface-page` | `#F8FAFC` | Application canvas |
| `surface-card` | `#FFFFFF` | Cards, sheets, dialogs |
| `surface-muted` | `#EEF2F6` | Grouping and inactive surfaces |
| `text-primary` | `#17202A` | Main content |
| `text-secondary` | `#5A6878` | Metadata |
| `border` | `#DDE4EC` | Dividers and outlines |
| `primary` | `#245FCC` | Primary action and active navigation |
| `accent-progress` | `#D9574E` | Reading progress and attention |
| `success` | `#087F78` | Completed and healthy |
| `warning` | `#A65D00` | Warning |
| `danger` | `#C62828` | Destructive and error |
| `focus` | `#0B6FFB` | Keyboard focus ring |

### Dark

| Token | Value | Use |
|---|---:|---|
| `surface-page` | `#11161D` | Application canvas |
| `surface-card` | `#1A212B` | Cards, sheets, dialogs |
| `surface-muted` | `#242E3A` | Grouping and inactive surfaces |
| `text-primary` | `#F2F5F8` | Main content |
| `text-secondary` | `#B5C0CC` | Metadata |
| `border` | `#354150` | Dividers and outlines |
| `primary` | `#79A7FF` | Primary action and active navigation |
| `accent-progress` | `#FF8B80` | Reading progress and attention |
| `success` | `#62CEC5` | Completed and healthy |
| `warning` | `#F2B55F` | Warning |
| `danger` | `#FF8585` | Destructive and error |
| `focus` | `#9EC1FF` | Keyboard focus ring |

All foreground/background pairs must be checked independently for WCAG 2.2 AA. Feature components consume semantic tokens and do not contain raw palette hex values.

## Typography

```css
font-family: "Noto Sans SC", "Noto Sans TC", "Microsoft YaHei UI",
  "PingFang SC", "Hiragino Sans GB", system-ui, sans-serif;
```

No runtime web-font import. Type roles: 12 px metadata only; 14 px compact labels; 16 px default body; 18/20 px section titles; 24/32 px page and dashboard headings. Body line-height is at least 1.5.

## Shape, spacing, elevation

- Spacing: 4, 8, 12, 16, 24, 32, 48 px.
- Radius: 6 px controls, 10 px cards, 14 px sheets/dialogs; pills only for chips and status.
- Elevation: borders first; two restrained shadow levels for floating toolbar and modal only.
- Z-index: content 0, sticky 10, navigation 20, overlay 40, dialog 50, notification 60.

## Navigation

- >=1024 px: persistent collapsible sidebar with icon and label; active route is visible.
- 768-1023 px: compact sidebar or navigation rail.
- <768 px: top app bar plus at most five labeled primary destinations; secondary/admin navigation in a drawer.
- Preserve deep links, browser back behavior, scroll position, filter state, and route guards.
- Provide a skip-to-content link and focus the main heading after route changes when appropriate.

## Media and data components

- Covers reserve their aspect ratio to prevent layout shift and lazy-load below the fold.
- The living-shelf card keeps title, progress, unread/status, and primary action in stable regions.
- Hover never reveals the only path to an action; keyboard focus and touch expose equivalent controls.
- Tables remain tables on desktop; mobile uses labeled rows without dropping data or actions.
- Charts include legends, keyboard-reachable data, locale-aware formatting, and a text/table alternative.

## Forms and feedback

- Visible labels, correct input types, helper text for complex settings, validation on blur.
- Async buttons disable duplicate submission and show progress within 300 ms.
- Errors state cause and recovery action near the relevant control; multi-error forms include a summary.
- Destructive actions are separated and confirmed; notifications use accessible live regions.
- Loading, empty, unauthorized, forbidden, offline, timeout, and server-error states are explicit.

## Motion

- 150-200 ms for hover, focus, press, expand, and dialog state changes.
- Animate opacity or transform only when useful; never animate layout dimensions.
- No decorative continuous motion, parallax, list-wide stagger, or navigation-blocking transitions.
- `prefers-reduced-motion: reduce` removes nonessential transitions.

## Accessibility and responsive gates

- Normal text contrast >=4.5:1; large text and non-text controls >=3:1.
- Visible 2-3 px focus indicator; complete keyboard use without traps.
- Interactive targets >=44x44 CSS px with >=8 px separation where adjacent.
- No horizontal page scroll at 375, 768, 1024, or 1440 px.
- Test 200% zoom, landscape mobile, light/dark themes, and reduced motion.
- Icons use the existing consistent vector/icon system; icon-only controls have accessible names.

## Rejected patterns

- Newsletter/landing-page sections, hero conversion forms, and marketing CTAs.
- Glassmorphism, cyberpunk glow, decorative gradients, and generic bento dashboards.
- Tactile/deformable or jelly controls and layout-shifting press effects.
- Child-oriented `Baloo 2`/`Comic Neue` typography.
- Runtime Google Font imports and third-party design CDNs.
- Replacing the router, API services, stores, reader engines, or backend contracts for visual reasons.
