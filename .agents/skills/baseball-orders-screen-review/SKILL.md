---
name: baseball-orders-screen-review
description: Review a changed backend Thymeleaf screen at representative PC and smartphone viewports and verify every screen element is visible without the page needing to scroll. Use after a template, CSS, or presentation view-model change, before baseball-orders-review.
---

# Baseball Orders Screen Review

Review only the screens whose template (`apps/backend/infrastructure/src/main/resources/templates/*.html`), inline styles, or backing controller/view-model changed during the current session. Compare `git status --short` and `git diff` against the session's starting point to identify them; do not extend the review to screens the current change did not touch.

## Launch the real screen

Use the `run` skill to bring the backend up (`compose.yaml`, or `./gradlew :apps:backend:infrastructure:bootRun` with the `local` profile, per the nearest `AGENTS.md`) so the template renders through its real controller and static assets rather than a static file opened directly. When a screen's layout depends on a submitted result (for example `single-game.html`'s `#transitions` panel), drive the real action once through `claude-in-chrome` to reach that populated state before checking it — checking only the empty initial state does not cover the populated layout.

Use `claude-in-chrome` to navigate, resize the viewport, screenshot, and evaluate layout on the live page.

## Representative viewports

Check each in-scope screen at both:

- PC: 1280 × 800
- Smartphone: 390 × 844 (a current mid-size phone; brackets the `max-width: 760px` and `max-width: 980px` breakpoints already present in the templates)

Use exactly these two sizes unless the user names a different device class; do not build out a long matrix of extra breakpoints.

## What "hidden" and "needs scrolling" mean here

The target is the outer page — the viewport's own scrollbar, measured against `document.scrollingElement`/`window.innerWidth`/`window.innerHeight` — not a template's existing, deliberately scrollable inner region. `single-game.html` already ships `.order-scroll { overflow-x: auto }` and `#transitions { overflow-y: auto; max-height: 640px }` by design; scrolling inside those regions is not itself a finding unless the current change altered their intended content or sizing. A finding is:

- The page requires vertical or horizontal scrolling to reach content that is not inside an intentionally scrollable region.
- An element is clipped by the viewport edge, clipped by an ancestor's `overflow: hidden`, or covered by another element (a fixed/absolute-positioned overlay, a `z-index` conflict) so part of it is unreadable or unclickable.
- A primary control (submit button, required input, navigation link) exists in the DOM but is not reachable without scrolling the page.

## Deterministic check first, screenshot second

For each in-scope screen × viewport, run in the live page (via `claude-in-chrome`'s console/evaluate) something equivalent to:

```js
({
  scrollWidth: document.scrollingElement.scrollWidth,
  scrollHeight: document.scrollingElement.scrollHeight,
  innerWidth: window.innerWidth,
  innerHeight: window.innerHeight,
  overflowing: [...document.querySelectorAll('button, a, input, select, h1, h2, [role="status"]')]
    .filter(el => !el.closest('.order-scroll, #transitions, [data-scrollable]'))
    .map(el => el.getBoundingClientRect())
    .filter(r => r.width > 0 && (r.right > window.innerWidth || r.bottom > window.innerHeight || r.left < 0 || r.top < 0)),
})
```

Treat `scrollWidth > innerWidth` or `scrollHeight > innerHeight` as page-level scroll and report it. Treat any entry in `overflowing` as a hidden or clipped element and report it with its selector or visible text. This measurement is deterministic evidence; do not substitute a visual impression for it. Then take a viewport-sized (not full-page) screenshot as corroborating evidence — a full-page screenshot does not show what the user actually sees without scrolling, so do not treat it as the primary check.

## Report

Per screen, per viewport:

- Screen and route, viewport size.
- Deterministic result: scrollWidth/innerWidth, scrollHeight/innerHeight, and the `overflowing` list (an empty list is a pass on that axis).
- Screenshot evidence (viewport-sized).
- Verdict: `PASS` or `FAIL`, with each `FAIL` naming the specific clipped/hidden element or the page-level scroll cause.

Findings about the existing `.order-scroll`/`#transitions` inner scroll regions, or about screens the current diff did not touch, are out of scope; note them only if the user asks about them explicitly. This skill does not judge visual design, color, or copy — only whether every element is visible without the page needing to scroll.
