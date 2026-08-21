# Design — GoStudio

A locked design system for this app. Every page redesign reads this file before
emitting code. Do not regenerate per page — extend or amend this file when the
system needs to grow.

Produced by a Hallmark `redesign` (multi-page flow) run. The previous palette was
the generic "deep-navy gradient + periwinkle/lavender accent" AI-app default; the
font token `jetbrains_mono` was wired to Roboto (a bug). This system replaces both
with a modern-minimal register tuned for a mobile Go IDE.

## Genre
modern-minimal — the polished dev-tool / IDE register (Linear / Vercel / Zed
school). Confident sans, clean canvas, one restrained signal accent, hairline
borders, generous whitespace. Minimalism with conviction, not the absence of
choice.

## Macrostructure family
- **App pages** (editor, AI chat, terminal): **Workbench** — function carries
  the page; a tool-surface with a focused canvas (code / chat / terminal) and
  chrome (toolbar, tabs, drawer, sheets) around it. No enrichment.
- **List / settings pages** (home hub, settings, about, install): **Long
  Document** — vertical reading rhythm, section-grouped grouped-card lists,
  hero title at the top. Variation lives only in card archetype and section
  rhythm, never in theme.

## Theme
The accent is a single Go-blue, used for active state, focus, links, and the
primary CTA — never as a flood. Greys are tinted cool toward the blue anchor
(hue 255°). No pure black, no pure white.

### Dark
- `--color-paper`     `oklch(16.5% 0.004 255)`   #17181D  near-black canvas
- `--color-surface`   `oklch(20.5% 0.005 255)`   #20232B  card / raised (+3%)
- `--color-surface-2` `oklch(15% 0.004 255)`     #121317  sunken (terminal/output)
- `--color-rule`      `oklch(28% 0.006 255)`     #343841  hairline borders
- `--color-ink`       `oklch(91% 0.004 255)`     #E8EAEE  primary text
- `--color-ink-2`     `oklch(82% 0.005 255)`     #C8CCD4  card titles
- `--color-muted`     `oklch(60% 0.008 255)`     #8A8F9A  subtitle / hint
- `--color-accent`    `oklch(68% 0.14 255)`      #5B8DEF  Go-blue signal
- `--color-accent-ink` `oklch(98% 0.004 255)`    #FAFBFC  text on accent
- `--color-focus`     `oklch(70% 0.16 255)`      #74A0FF  focus ring (3:1+)
- `danger`  `oklch(71% 0.17 22)`   #F87171
- `success` `oklch(76% 0.15 162)`  #34D399
- `warning` `oklch(80% 0.15 75)`   #FBBF24

### Light
- `--color-paper`     `oklch(98.5% 0.003 255)`   #FAFBFC
- `--color-surface`   `oklch(96% 0.004 255)`     #F1F3F6  card
- `--color-surface-2` `oklch(99.5% 0.002 255)`   #FFFFFF  sunken/inputs
- `--color-rule`      `oklch(88% 0.005 255)`     #E0E3E8  hairline
- `--color-ink`       `oklch(18% 0.006 255)`     #1A1D23
- `--color-ink-2`     `oklch(24% 0.006 255)`     #2C303A
- `--color-muted`     `oklch(48% 0.010 255)`     #5F6571
- `--color-accent`    `oklch(52% 0.19 255)`      #1F54E8  Go-blue (stronger on light)
- `--color-accent-ink` `oklch(99% 0.002 255)`    #FFFFFF
- `--color-focus`     `oklch(50% 0.20 255)`      #1A4FD9

The hue never switches between modes; only lightness + chroma move (dark mode
recipe: paper lightens off black, ink darkens off white, accent loses ~0.05
chroma and gains ~16% lightness).

## Typography
- **UI sans (display + body):** Inter — Regular 400 / Medium 500 / SemiBold 600 /
  Bold 700. All non-code text (headlines, titles, labels, body, buttons).
- **Mono (code):** JetBrains Mono — Regular 400. Code, terminal, file paths,
  the editor symbol bar, tool-execution cards in AI.
- Display tracking: `-0.02em` on headline/title roles; body/label `0em`.
- Display weight: SemiBold (600), not Bold — a touch more refined for the genre.
- Type scale (sp): body 16/24 · bodyM 14/20 · bodyS 12/16 · headlineL 32/40 ·
  headlineM 28/36 · headlineS 24/32 · titleL 20/28 · titleM 18/24 · titleS 16/22
  · labelL 14/20 · labelM 12/16 · labelS 11/16.

> Why not Inter Tight: the app ships bundled `.ttf` (no network font fetch),
> and Inter's full family covers all four UI weights in one download. Tight is a
> display-only subfamily; using plain Inter keeps one family for display + body
> (single-family discipline).

## Spacing
4-point named scale. Values live in `color.kt`-adjacent spacing usage (the app
uses raw `dp` literals in composables — keep those; the scale is the canonical
reference, not an enforced token layer):
3 · 4 · 6 · 8 · 12 · 16 · 20 · 24 · 32 · 40 · 56.
## Radius
Two-value system (verified against the implementation — the earlier "8px is the
house shape" line understated what shipped). Named by role, not by number:
- **Cards / grouped-list containers / raised surfaces:** 12dp. Every grouped-card
  list (home hub, settings, tools, AI settings, editor settings, install log,
  About groups) and every standalone `Card` uses 12dp.
- **Inputs, buttons, CTAs, icon chips, swatches:** 8dp. The CTA pair, every
  `OutlinedTextField`/`Button`, the 28dp icon-chip boxes, and color swatches.
Chat message bubbles are a distinct archetype (not cards): their larger radius is
allowed and does not participate in this system. Dialog surfaces hold their own
16dp radius (a deliberate "sheet/modal is rounder than inline" step up from cards).
Never pill (full-round) outside circular icon buttons and avatar-sized chips.

## Motion
- Minimal. Reveals stay off; pages are composed, not animated in.
- Existing nav transitions (tween 300ms fade+slide) and editor chrome tweens
  (220ms) are kept — they are functional, not decorative.
- Easings: keep the existing `tween` defaults; do NOT introduce bounce/overshoot.
- Reduced-motion: spatial collapses to opacity-only ≤ 150ms (deferred — no
  animation is content-bearing today).

## Microinteractions stance
- Silent success — no celebratory toasts for routine actions.
- Optimistic update over confirmation dialogs where data loss isn't at stake.
- Active/focus states use the accent at ≤ 5% viewport coverage.
- Pressed states: surface lightens ~3% (dark) / darkens ~3% (light).

## CTA voice
- **Primary CTA:** filled accent (`--color-accent`) with `--color-accent-ink`
  text, 8px radius, Medium weight, 14sp. One per surface.
- **Secondary CTA:** hairline border (`--color-rule`) on `--color-surface`,
  ink text, same radius.
- Icon buttons: 35dp circular, `--color-surface` fill on `--color-paper`, ink
  icon; pressed → `--color-surface-2`.
- Never gradient fills, never pill (full-round) — 8px radius is the house shape.
- Never gradient fills, never pill (full-round) — see `## Radius` for the full
  two-value system; CTA radius is 8dp, card radius is 12dp.

## Per-page allowances
- App pages (editor / AI / terminal): MUST NOT use enrichment. Function carries
  the page. Accent only on active state + focus + the run/AI toolbar buttons.
- List / settings pages: typography + grouped cards only. No hero imagery.
- Splash: typography-only wordmark; the "Go" glyph may carry the accent.

## What pages MUST share
- The GoStudio wordmark / logotype.
- The Go-blue accent and its placement (≤ 5% per viewport).
- Inter (UI) + JetBrains Mono (code) pairing.
- The CTA voice (8px radius, filled/outline pair).
- Section heading rhythm (grouped-card list idiom on settings/home; toolbar +
  tabs idiom on editor).
- Cool-tinted neutral greys — never flat achromatic grey, never warm.

## What pages MAY differ on
- Card archetype within the grouped-list family (icon-left vs centered vs
  two-line), as long as radius and border voice stay consistent.
- Editor chrome density (compact toolbar vs the floating action cluster).

## Hardcoded-color policy
All colors flow through `app_theme_provider.colors.*` (the `app_colors`
CompositionLocal). Inline `Color(0x…)` / `Color.White|Black|Gray` outside the
theme files is a slop tell and must be replaced with a token. The only
exceptions are the macOS traffic-light dots in any fake-terminal chrome — and
those should not exist (see "Re-drawn chrome forbidden" below); the install
screen's terminal mock was de-chromed in this redesign.

## Re-drawn chrome forbidden
Do not hand-build fake browser/IDE chrome (traffic-light dots, fake title bars
wrapping a log view). Use the real surface + a hairline border, or let the
content stand alone. The previous install screen's macOS-window mock was
removed for this reason.

## Exports
Drop-in formats for re-using this design system in other projects. The Kotlin
source of truth is `app/src/main/kotlin/com/jmwl/gostudio/ui/theme/color.kt`;
these are the portable mirrors.

### tokens.css
```css
:root {
  --color-paper:      oklch(16.5% 0.004 255);
  --color-surface:    oklch(20.5% 0.005 255);
  --color-surface-2:  oklch(15% 0.004 255);
  --color-rule:       oklch(28% 0.006 255);
  --color-ink:        oklch(91% 0.004 255);
  --color-ink-2:      oklch(82% 0.005 255);
  --color-muted:      oklch(60% 0.008 255);
  --color-accent:     oklch(68% 0.14 255);
  --color-accent-ink: oklch(98% 0.004 255);
  --color-focus:      oklch(70% 0.16 255);
  --color-danger:     oklch(71% 0.17 22);
  --color-success:    oklch(76% 0.15 162);
  --color-warning:    oklch(80% 0.15 75);

  --font-display: "Inter", system-ui, sans-serif;
  --font-body:    "Inter", system-ui, sans-serif;
  --font-mono:    "JetBrains Mono", ui-monospace, monospace;

  --space-3xs: 0.1875rem; --space-2xs: 0.25rem; --space-xs: 0.375rem;
  --space-sm:  0.5rem;    --space-md:  0.75rem;  --space-lg: 1rem;
  --space-xl:  1.5rem;    --space-2xl: 2.5rem;   --space-3xl: 3.5rem;

  --text-xs: 0.75rem; --text-sm: 0.875rem; --text-md: 1rem;
  --text-lg: 1.25rem; --text-xl: 1.75rem;  --text-2xl: 2rem;

  --ease-out: cubic-bezier(0.16, 1, 0.3, 1);
  --dur-short: 220ms;
  --radius-card: 8px; --radius-input: 8px; --radius-pill: 9999px;
}
```

### Tailwind v4 `@theme`
```css
@theme {
  --color-paper:   oklch(16.5% 0.004 255);
  --color-surface: oklch(20.5% 0.005 255);
  --color-ink:     oklch(91% 0.004 255);
  --color-accent:  oklch(68% 0.14 255);
  --color-focus:   oklch(70% 0.16 255);
  --font-display:  "Inter", sans-serif;
  --font-body:     "Inter", sans-serif;
  --font-mono:     "JetBrains Mono", monospace;
  --spacing-md:    0.75rem;
  --text-md:       1rem;
  --ease-out:      cubic-bezier(0.16, 1, 0.3, 1);
}
```

### DTCG `tokens.json`
```json
{
  "color": {
    "paper":  { "$value": "oklch(16.5% 0.004 255)", "$type": "color" },
    "surface":{ "$value": "oklch(20.5% 0.005 255)", "$type": "color" },
    "ink":    { "$value": "oklch(91% 0.004 255)",   "$type": "color" },
    "accent": { "$value": "oklch(68% 0.14 255)",    "$type": "color" },
    "focus":  { "$value": "oklch(70% 0.16 255)",    "$type": "color" }
  },
  "font": {
    "display": { "$value": "Inter",        "$type": "fontFamily" },
    "body":    { "$value": "Inter",        "$type": "fontFamily" },
    "mono":    { "$value": "JetBrains Mono","$type": "fontFamily" }
  },
  "space": { "md": { "$value": "0.75rem", "$type": "dimension" } }
}
```

### shadcn/ui CSS variables
```css
:root {
  --background:         16.5% 0.004 255;   /* paper */
  --foreground:         91% 0.004 255;     /* ink */
  --primary:            68% 0.14 255;      /* accent */
  --primary-foreground: 98% 0.004 255;     /* accent-ink */
  --muted:              20.5% 0.005 255;   /* surface */
  --muted-foreground:   60% 0.008 255;     /* muted */
  --border:             28% 0.006 255;     /* rule */
  --input:              28% 0.006 255;     /* rule */
  --ring:               70% 0.16 255;      /* focus */
  --radius:             8px;
}
```
