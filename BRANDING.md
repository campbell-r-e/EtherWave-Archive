# GEKHoosier QSO Suite - Branding Guidelines

## 🎨 Overview

The GEKHoosier QSO Suite is an open-source, Indiana-themed, multi-station ham radio contest logging platform. Our branding reflects the professional, technical nature of amateur radio while celebrating Indiana's identity through color and symbolism.

---

## 📐 Logo

### Primary Logo
![GEKHoosier QSO Suite Logo](frontend/logbook-ui/src/assets/branding/logo.svg)

**Components:**
- **Radio Tower:** Symbolizes amateur radio communication
- **RF Waves:** Represent signal propagation
- **Indiana State Outline:** Pays homage to our Hoosier roots
- **Typography:** Clean, modern, technical

### Logo Variants

| Variant | Usage | Path |
|---------|-------|------|
| **Primary (Light)** | Light backgrounds, default | `/assets/branding/logo.svg` |
| **Dark Mode** | Dark backgrounds, dark theme | `/assets/branding/logo-dark.svg` |
| **Icon Only** | Favicon, mobile apps, small spaces | `/assets/branding/icon.svg` |

### Logo Sizing Guidelines

- **Navbar:** 40-50px height, maintain aspect ratio
- **Large Headers:** 80-100px height
- **Mobile:** 36-40px height
- **Favicon:** 32x32px, 192x192px, 512x512px
- **Minimum Size:** Never smaller than 24px height

### Logo Clear Space
Maintain a clear space around the logo equal to the height of the "G" in "GEK" to ensure visibility and impact.

### Logo Don'ts
❌ Do not stretch or distort the logo
❌ Do not change the colors arbitrarily
❌ Do not add effects (drop shadows, gradients, etc.)
❌ Do not place on busy backgrounds without proper contrast
❌ Do not rotate the logo

---

## 🎨 Color Palette

### Primary Brand Colors

```css
--color-hoosier-blue: #003F87
--color-cardinal-red: #C41E3A
--color-qso-green: #4CAF50
--color-highlight-yellow: #F5C542
```

| Color | Hex | Usage | Accessibility |
|-------|-----|-------|---------------|
| **Hoosier Blue** | `#003F87` | Primary brand, buttons, headers | AAA on white, AA on light gray |
| **Cardinal Red** | `#C41E3A` | Accent, alerts, important actions | AAA on white |
| **QSO Green** | `#4CAF50` | Success states, GOTA (when not station) | AAA on white |
| **Highlight Yellow** | `#F5C542` | Callouts, highlights | AAA on dark backgrounds |

### Station Colors (Must Use Consistently)

Station colors are critical for multi-station contest operations and must be used consistently across all UI components.

```typescript
// Station 1 - Blue
--station-1-primary: #1E88E5
--station-1-light: #64B5F6
--station-1-dark: #1565C0

// Station 2 - Red
--station-2-primary: #E53935
--station-2-light: #EF5350
--station-2-dark: #C62828

// GOTA - Green
--station-gota-primary: #43A047
--station-gota-light: #66BB6A
--station-gota-dark: #2E7D32

// Viewer - Gray
--station-viewer-primary: #9E9E9E
--station-viewer-light: #BDBDBD
--station-viewer-dark: #616161
```

**Implementation:**
```typescript
import { getStationColor } from './config/station-colors';

// Get station color
const color = getStationColor(1); // Returns #1E88E5
const lightColor = getStationColor(1, 'light'); // Returns #64B5F6

// For GOTA stations
const gotaColor = getStationColorByGotaStatus(true, null); // Returns #43A047
```

### Accent Colors

```css
--accent-ai: #7E57C2            /* AI assistance features */
--accent-rig-online: #00E5FF     /* Rig connection status */
--accent-validation-error: #FF7043  /* Form errors, dupes */
--accent-warning: #FFA726        /* Warnings */
--accent-success: #66BB6A        /* Success messages */
--accent-info: #29B6F6           /* Informational */
```

### Background Colors

#### Light Theme (Default)
```css
--bg-primary: #F2F5F7    /* Main background */
--bg-secondary: #FFFFFF  /* Card/panel backgrounds */
--bg-tertiary: #E8EDEF   /* Subtle backgrounds, headers */
--bg-elevated: #FFFFFF   /* Modals, popovers */
```

#### Dark Theme
```css
--bg-primary: #1A1A1A    /* Main background */
--bg-secondary: #242424  /* Card/panel backgrounds */
--bg-tertiary: #2D2D2D   /* Subtle backgrounds, headers */
--bg-elevated: #303030   /* Modals, popovers */
```

### Text Colors

#### Light Theme
```css
--text-primary: #1A1A1A     /* Primary text */
--text-secondary: #4A4A4A   /* Secondary text */
--text-muted: #757575       /* Muted/disabled text */
--text-inverse: #FFFFFF     /* Text on dark backgrounds */
```

#### Dark Theme
```css
--text-primary: #E8EDEF     /* Primary text */
--text-secondary: #B8BCBF   /* Secondary text */
--text-muted: #8A8E91       /* Muted/disabled text */
--text-inverse: #1A1A1A     /* Text on light backgrounds */
```

---

## 🔤 Typography

### Font Stack
```css
font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto,
             'Helvetica Neue', Arial, sans-serif;
```

This native font stack provides:
- ✅ Excellent readability
- ✅ System-native appearance
- ✅ Fast loading (no web fonts)
- ✅ Great accessibility

### Type Scale

| Element | Size | Weight | Line Height | Usage |
|---------|------|--------|-------------|-------|
| **H1** | 2.5rem (40px) | 600 | 1.2 | Page titles |
| **H2** | 2rem (32px) | 600 | 1.3 | Section headers |
| **H3** | 1.75rem (28px) | 600 | 1.4 | Subsection headers |
| **H4** | 1.5rem (24px) | 600 | 1.4 | Card titles |
| **H5** | 1.25rem (20px) | 600 | 1.5 | Component titles |
| **H6** | 1rem (16px) | 600 | 1.5 | Small headers |
| **Body** | 1rem (16px) | 400 | 1.6 | Body text |
| **Small** | 0.875rem (14px) | 400 | 1.5 | Captions, labels |

### Font Weights
- **400** (Regular): Body text
- **500** (Medium): Labels, emphasized text
- **600** (Semibold): Headers, buttons
- **700** (Bold): Extra emphasis (rare)

---

## 🌓 Dark Mode

### Theme Toggle Implementation

The theme system uses a `ThemeService` for state management:

```typescript
import { ThemeService } from './services/theme/theme.service';

constructor(private themeService: ThemeService) {}

toggleTheme() {
  this.themeService.toggleTheme();
}

isDark(): boolean {
  return this.themeService.isDarkTheme();
}
```

### Theme-Aware Styling

Use CSS custom properties that automatically adjust:

```css
/* This works in both light and dark mode */
.my-component {
  background-color: var(--bg-secondary);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
}
```

### Testing Dark Mode
1. Toggle theme using the theme switcher button
2. Verify all text is readable
3. Check color contrast ratios (WCAG AA minimum)
4. Test all interactive states (hover, focus, active)
5. Verify images/logos switch to appropriate variant

---

## 📏 Spacing System

Use consistent spacing throughout the application:

```css
--space-xs: 0.25rem   /* 4px */
--space-sm: 0.5rem    /* 8px */
--space-md: 1rem      /* 16px */
--space-lg: 1.5rem    /* 24px */
--space-xl: 2rem      /* 32px */
--space-2xl: 3rem     /* 48px */
```

---

## 🔘 Components

### Buttons

```html
<!-- Primary action -->
<button class="btn btn-primary">Primary Action</button>

<!-- Secondary action -->
<button class="btn btn-outline-primary">Secondary Action</button>

<!-- Danger/destructive -->
<button class="btn btn-danger">Delete</button>

<!-- Success -->
<button class="btn btn-success">Save</button>
```

### Cards

```html
<div class="card shadow-md">
  <div class="card-header bg-hoosier-blue text-white">
    <h5>Card Title</h5>
  </div>
  <div class="card-body">
    Card content goes here
  </div>
</div>
```

### Station Badges

```html
<!-- Station 1 -->
<span class="badge station-1-bg text-white">Station 1</span>

<!-- Station 2 -->
<span class="badge station-2-bg text-white">Station 2</span>

<!-- GOTA -->
<span class="badge station-gota-bg text-white">GOTA</span>
```

### Table Row Coloring

```html
<tr [style.border-left]="'4px solid ' + getStationColor(qso.stationNumber)">
  <!-- Table cells -->
</tr>
```

---

## ♿ Accessibility

### Color Contrast
- **AAA (7:1):** All body text
- **AA (4.5:1):** Minimum for UI elements
- **AA Large (3:1):** Text 18px+ or 14px+ bold

### Testing Tools
- Chrome DevTools Lighthouse
- WAVE browser extension
- axe DevTools

### Requirements
✅ Keyboard navigation for all interactive elements
✅ Focus indicators visible in both themes
✅ ARIA labels on icon-only buttons
✅ Alt text on images
✅ Semantic HTML structure

---

## 📱 Responsive Design

### Breakpoints

```css
/* Mobile */
@media (max-width: 576px) { }

/* Tablet */
@media (min-width: 577px) and (max-width: 768px) { }

/* Desktop */
@media (min-width: 769px) and (max-width: 1024px) { }

/* Large Desktop */
@media (min-width: 1025px) { }
```

### Logo Responsive Behavior
- **Desktop:** Full logo with text
- **Tablet:** Full logo, slightly smaller
- **Mobile:** Consider icon-only version if space is tight

---

## 🎯 Usage Examples

### QSO List Station Coloring

```typescript
import { getStationColor } from './config/station-colors';

getStationBorder(qso: QSO): string {
  const color = getStationColor(qso.station.stationNumber);
  return `4px solid ${color}`;
}
```

### Dashboard Card Headers

```html
<div class="card">
  <div class="card-header bg-hoosier-blue text-white">
    <h5>📊 Contest Statistics</h5>
  </div>
  <div class="card-body">
    <!-- Content -->
  </div>
</div>
```

### Theme Toggle Button

```html
<button (click)="toggleTheme()"
        class="btn btn-outline-secondary"
        [attr.aria-label]="isDark() ? 'Switch to light mode' : 'Switch to dark mode'">
  @if (isDark()) {
    ☀️ Light
  } @else {
    🌙 Dark
  }
</button>
```

---

## 📦 Asset Locations

```
frontend/logbook-ui/src/assets/branding/
├── logo.svg           # Primary logo (light mode)
├── logo-dark.svg      # Dark mode logo variant
├── icon.svg           # Icon-only version
└── favicons/          # Generated favicon sizes
    ├── favicon-16x16.png
    ├── favicon-32x32.png
    ├── favicon-192x192.png
    └── favicon-512x512.png
```

---

## ✅ Checklist for New Components

When creating new UI components:

- [ ] Use CSS custom properties for colors
- [ ] Test in both light and dark mode
- [ ] Apply station colors consistently
- [ ] Verify keyboard navigation works
- [ ] Check color contrast (WCAG AA minimum)
- [ ] Use semantic HTML
- [ ] Add ARIA labels where needed
- [ ] Test responsive behavior on mobile
- [ ] Follow spacing system
- [ ] Use brand typography scale

---

## 🚀 Quick Start

### Import Station Colors
```typescript
import { getStationColor, BRAND_COLORS } from './config/station-colors';
```

### Import Theme Service
```typescript
import { ThemeService } from './services/theme/theme.service';
```

### Use CSS Variables
```css
.my-element {
  color: var(--text-primary);
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
}
```

---

## 📞 Questions?

For questions about branding, color usage, or accessibility:
- Check this document first
- Review `/src/styles.css` for available CSS variables
- Inspect `/src/app/config/station-colors.ts` for TypeScript utilities

---

**Last Updated:** December 4, 2025
**Version:** 1.0.0
**Maintained by:** GEKHoosier QSO Suite Team
