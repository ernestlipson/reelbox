# DESIGN.md — Reelbox

## Color System (OKLCH)

| Token | OKLCH | Hex | Use |
|---|---|---|---|
| ink | `0.04 0.005 260` | `#0A0A0C` | Page background, deepest surface |
| ink-elevated | `0.08 0.005 260` | `#14141A` | Cards, modals, nav bar |
| ink-card | `0.11 0.006 260` | `#1B1B23` | Poster cards, input backgrounds |
| accent | `0.58 0.22 25` | `#FF4B4B` | Primary actions, selected nav, hero CTA |
| accent-soft | `0.18 0.06 25` | `#2A1417` | Pressed states, subtle accents |
| gold | `0.82 0.15 95` | `#FFC94A` | Ratings, stars, highlights |
| violet | `0.52 0.21 270` | `#8B5CF6` | Secondary actions, trailer buttons |
| text-primary | `0.95 0.005 260` | `#F3F3F6` | Headlines, body text |
| text-secondary | `0.60 0.005 260` | `#9A9AA8` | Metadata, subtitles, disabled |
| hairline | `0.15 0.005 260` | `#26262F` | Dividers, borders, focus rings |

**Strategy: Committed.** Accent carries ~25% of surface area (hero, CTA, selected states). Dark-only — no light theme exists.

## Typography

| Role | Font | Size/Weight | Line Height |
|---|---|---|---|
| Display | Inter Tight / Space Grotesk | 48px / 800 | 1.05 |
| H1 | Inter Tight | 36px / 700 | 1.15 |
| H2 | Inter Tight | 28px / 600 | 1.2 |
| H3 | Inter | 20px / 600 | 1.3 |
| Body | Inter | 16px / 400 | 1.6 |
| Caption | Inter | 13px / 500 | 1.4 |
| Label | Inter | 11px / 600 | 1.3 |

**Scale ratio:** 1.33 (major third). Max line width: 70ch.

## Spacing & Rhythm

- Base unit: 8px
- Section vertical padding: 96px (desktop) / 64px (mobile)
- Component gaps: 24px (cards), 16px (tight groups), 8px (inline)
- Container max-width: 1120px

## Elevation

No box-shadows. Depth via:
- Background layering (ink → ink-elevated → ink-card)
- Border: 1px hairline on cards
- Accent glow on focus: `0 0 0 2px accent`

## Components

### Buttons
- **Primary:** Accent fill, white text, 12px radius, 16px/24px padding
- **Secondary:** Ink-elevated fill, hairline border, text-primary
- **Ghost:** No fill, accent text, underline on hover
- **Icon-only:** 44×44 touch target, ink-elevated bg

### Cards
- Poster card: 2:3 aspect, 12px radius, ink-card bg, hairline border
- Feature card: 16:9 backdrop, gradient overlay, 16px radius

### Navigation
- Bottom nav: 80px height, ink bg, accent selected, text-secondary unselected
- Top bar: 56px + safe area, region pill (rounded-full, ink-elevated)

### Inputs
- Search field: ink-card bg, hairline border, accent focus ring, 50px radius

## Motion

- Easing: `cubic-bezier(0.16, 1, 0.3, 1)` (ease-out-expo)
- Durations: 150ms (micro), 250ms (standard), 400ms (macro)
- No layout animation — opacity + transform only

## Assets Needed

- Hero phone mockup (portrait, 1080×2408)
- 3 feature screenshots (Home, Detail with providers, Search)
- App icon (108×108 adaptive)
- OG image (1200×630)

## Breakpoints

- Mobile: ≤640px
- Tablet: 641–1024px
- Desktop: >1024px

## Accessibility

- WCAG AA contrast on all text (text-primary on ink = 15.8:1)
- Focus visible on all interactive elements
- Reduced motion respected
- Semantic HTML landmarks