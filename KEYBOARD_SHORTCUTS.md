# Keyboard Shortcuts - EtherWave Archive

**Accessibility-First Navigation Guide**

This document lists all keyboard shortcuts available in the EtherWave Archive ham radio logbook application. All functionality is accessible via keyboard for users who cannot use a mouse.

---

## Global Navigation

| Shortcut | Action | Description |
|----------|--------|-------------|
| **Tab** | Move forward | Navigate to the next interactive element |
| **Shift + Tab** | Move backward | Navigate to the previous interactive element |
| **Enter** | Activate | Activate buttons, links, and form submissions |
| **Space** | Toggle | Activate buttons and checkboxes |
| **Escape** | Close/Cancel | Close modals, dropdowns, and cancel operations |

### Skip Navigation

| Shortcut | Action | Context |
|----------|--------|---------|
| **Tab** (first press) | Skip to main content | Appears on first Tab press from page load |
| **Enter** on skip link | Jump to content | Bypasses navigation elements |

---

## QSO Entry Form

| Shortcut | Action | Field |
|----------|--------|-------|
| **Tab** | Next field | Move through: Callsign → Date → Time → Frequency → Band → Mode → RST |
| **Shift + Tab** | Previous field | Move backwards through form fields |
| **Enter** in Callsign | Lookup callsign | Triggers QRZ.com/HamQTH lookup if configured |
| **Ctrl + Enter** | Save QSO | Submit the current QSO entry (when form is valid) |

### Quick Reference Band Buttons

| Shortcut | Action |
|----------|--------|
| **Tab** to button | Navigate to band button |
| **Enter** or **Space** | Set frequency to selected band |

---

## QSO List Navigation

### Tab Controls

| Shortcut | Action | Description |
|----------|--------|-------------|
| **Tab** to tab button | Focus tab | Select a tab button (All, Station 1, Station 2, etc.) |
| **Arrow Right** | Next tab | Move to the next station tab |
| **Arrow Left** | Previous tab | Move to the previous station tab |
| **Enter** or **Space** | Activate tab | Switch to the selected tab |

### Table Navigation

| Shortcut | Action |
|----------|--------|
| **Tab** | Next column header or row action | Navigate through table controls |
| **Shift + Tab** | Previous column header or row action | Navigate backwards |
| **Enter** on delete button | Delete QSO | Remove the selected QSO (with confirmation) |

---

## Map Components

### View Toggle

| Shortcut | Action |
|----------|--------|
| **Tab** to "Table View" button | Focus toggle | Access the accessible view toggle |
| **Enter** or **Space** | Switch views | Toggle between Map and Table view |

### Table View (Accessible Alternative)

| Shortcut | Action | Description |
|----------|--------|-------------|
| **Tab** to column header | Focus sort button | Navigate to sortable column headers |
| **Enter** or **Space** | Sort column | Sort table by selected column (toggles asc/desc) |
| **Tab** through rows | Navigate data | Move through QSO location data |

**Sortable Columns:**
- Callsign
- Grid Square
- Band
- Mode
- Distance (km)
- Station

---

## State/Province Map

### View Toggle

| Shortcut | Action |
|----------|--------|
| **Tab** to toggle button | Focus Grid/Table toggle | Access view switcher |
| **Enter** or **Space** | Switch views | Toggle between visual grid and accessible table |

### Table View

| Shortcut | Action | Columns |
|----------|--------|---------|
| **Tab** to header | Focus sort button | Abbreviation, Full Name, QSO Count, Status |
| **Enter** or **Space** | Sort | Sort by selected column |

---

## Modal Dialogs

### General Modal Navigation

| Shortcut | Action | Description |
|----------|--------|-------------|
| **Tab** | Navigate within modal | Move through modal form fields and buttons |
| **Escape** | Close modal | Cancel and close the modal dialog |
| **Enter** on submit button | Save | Submit the modal form |

### Create Log Modal

**Auto-Focus**: When opened, focus automatically moves to the Log Name input field.

| Field Order | Description |
|-------------|-------------|
| 1. Log Name | First input (auto-focused) |
| 2. Description | Textarea |
| 3. Log Type | Select dropdown |
| 4. Public checkbox | Toggle |
| 5. Cancel button | Close without saving |
| 6. Create button | Submit form |

**Focus Return**: When closed (Cancel or X button), focus returns to the button that opened the modal.

### Create Invitation Modal

**Auto-Focus**: Focus moves to the Log select dropdown.

| Field Order | Description |
|-------------|-------------|
| 1. Log | Select dropdown (auto-focused) |
| 2. Username | Text input |
| 3. Role | Select dropdown |
| 4. Station Callsign | Optional text input |
| 5. Message | Optional textarea |
| 6. Cancel | Close without saving |
| 7. Send | Submit invitation |

**Focus Return**: Returns to "Create Invitation" button on close.

---

## Dropdown Menus

### General Dropdown Navigation

| Shortcut | Action |
|----------|--------|
| **Enter** or **Space** on dropdown button | Open menu | Show dropdown options |
| **Arrow Down** | Next option | Move to next menu item |
| **Arrow Up** | Previous option | Move to previous menu item |
| **Enter** or **Space** on option | Select | Choose the focused option |
| **Escape** | Close | Close dropdown without selecting |

### Contest Overlay Dropdown

**Menu Items with Checkboxes:**
- CQ Zones
- ITU Zones
- ARRL Sections
- DXCC Entities

| Shortcut | Action |
|----------|--------|
| **Arrow Down/Up** | Navigate | Move through overlay options |
| **Enter** or **Space** | Toggle | Enable/disable the selected overlay |

**ARIA Announcement**: Screen readers announce "checked" or "unchecked" state.

---

## Invitations Page

### Tab Controls

| Shortcut | Action |
|----------|--------|
| **Tab** to "Received"/"Sent" tabs | Focus tab | Select invitation list view |
| **Arrow Right** | Switch to Sent | Move to Sent Invitations |
| **Arrow Left** | Switch to Received | Move to Received Invitations |
| **Enter** or **Space** | Activate tab | Display selected invitation list |

### Invitation Actions

| Shortcut | Action | Context |
|----------|--------|---------|
| **Tab** to Accept button | Focus action | Received invitations only |
| **Enter** | Accept invitation | Join the log with proposed role |
| **Tab** to Decline button | Focus action | Received invitations only |
| **Enter** | Decline invitation | Reject the invitation |
| **Tab** to Cancel button | Focus action | Sent invitations only |
| **Enter** | Cancel invitation | Withdraw the sent invitation |

---

## Theme Selection

### Changing Themes

**Current Implementation**: Theme is toggled via settings or nav bar button.

| Theme | Description | Best For |
|-------|-------------|----------|
| **Light** | Default theme | General use, bright environments |
| **Dark** | Low-light theme | Night operations, reduced eye strain |
| **High Contrast** | Accessibility theme | Low vision users, maximum contrast |

**High Contrast Features:**
- Black background (#000000)
- White text (#FFFFFF)
- Yellow links (#FFFF00)
- Cyan hover states (#00FFFF)
- Bold text and thick borders (2-4px)
- Enhanced focus indicators (4px outlines)

---

## Form Validation

### Error Navigation

| Shortcut | Action |
|----------|--------|
| **Tab** to invalid field | Focus first error | Navigate to fields with validation errors |
| **Read field label** | Screen reader | Announces field name, required status, and error message |

**ARIA Announcements:**
- "Required" fields announced with `aria-required="true"`
- Invalid fields announced with `aria-invalid="true"`
- Error messages linked via `aria-describedby`

---

## Screen Reader Shortcuts

### NVDA (Windows)

| Shortcut | Action |
|----------|--------|
| **NVDA + Arrow Keys** | Navigate elements | Browse page content |
| **H** | Next heading | Jump to next heading |
| **Shift + H** | Previous heading | Jump to previous heading |
| **K** | Next link | Jump to next link |
| **B** | Next button | Jump to next button |
| **F** | Next form field | Jump to next input |
| **T** | Next table | Jump to next data table |
| **NVDA + Space** | Forms mode toggle | Switch between browse and forms mode |

### JAWS (Windows)

| Shortcut | Action |
|----------|--------|
| **Insert + Arrow Keys** | Navigate | Browse content |
| **Insert + F6** | Headings list | View all headings |
| **Insert + F7** | Links list | View all links |
| **Insert + F5** | Form fields list | View all form fields |

### VoiceOver (macOS/iOS)

| Shortcut | Action |
|----------|--------|
| **VO + Arrow Keys** | Navigate | Browse page elements |
| **VO + Command + H** | Next heading | Jump to next heading |
| **VO + Command + L** | Next link | Jump to next link |
| **VO + U** | Rotor | Open navigation rotor (headings, links, form controls) |

**Note**: VO = Control + Option keys

---

## Tips for Keyboard Users

### Best Practices

1. **Use Tab Key**: Primary navigation method for interactive elements
2. **Watch for Focus**: Focus indicators show your current location (cyan/green outline)
3. **Read Labels**: All buttons and form fields have descriptive labels
4. **Use Arrow Keys**: Navigate tabs, dropdowns, and lists
5. **Press Escape**: Close modals and cancel operations
6. **Enable Skip Link**: Press Tab once on page load to skip navigation

### Contest Operations

For rapid QSO logging during contests:

1. **Tab** to Callsign field (or use auto-focus after previous save)
2. **Type callsign** → Press **Enter** to lookup
3. **Tab** through Date/Time/Frequency (usually pre-filled)
4. **Tab** to RST fields
5. **Ctrl + Enter** to save and reset form
6. **Focus returns to Callsign** for next QSO

**Speed**: Experienced keyboard users can log QSOs in 3-5 seconds.

### Accessibility Settings

**Recommended Browser Settings:**
- Enable "Always show text cursor" (Chrome/Edge)
- Enable "Navigate pages with a text cursor" (Firefox)
- Increase text size if needed (Ctrl/Cmd + Plus)
- Enable high contrast mode in OS if available

---

## Frequently Asked Questions

### Q: Can I navigate the entire app without a mouse?
**A:** Yes! All functionality is accessible via keyboard. Tab through elements, use Enter/Space to activate, and Escape to cancel.

### Q: How do I know where keyboard focus is?
**A:** Focus indicators are visible as cyan (light theme) or green (dark theme) outlines around the active element.

### Q: Can I access the map data without seeing it?
**A:** Yes! Use the "Table View" button on map components to switch to an accessible, sortable table with all geographic data.

### Q: Do screen readers work with this app?
**A:** Yes! The app follows WCAG 2.1 AA guidelines with proper ARIA labels, live regions, and semantic HTML.

### Q: How do I change the theme?
**A:** Use the theme selector button in the navigation bar, or access Settings → Theme. Three options: Light, Dark, High Contrast.

### Q: What if focus gets "trapped" in a modal?
**A:** Press **Escape** to close any modal and return focus to the trigger button.

---

## Reporting Accessibility Issues

If you encounter keyboard navigation problems or screen reader issues:

1. Note the specific page/component
2. Describe the expected vs. actual behavior
3. Include your browser and assistive technology versions
4. Report via GitHub Issues or contact the development team

**Accessibility is a priority.** All reports are addressed promptly.

---

## Additional Resources

- [WCAG 2.1 Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)
- [WebAIM Keyboard Accessibility](https://webaim.org/articles/keyboard/)
- [NVDA Screen Reader Download](https://www.nvaccess.org/)
- [JAWS Screen Reader](https://www.freedomscientific.com/products/software/jaws/)
- [VoiceOver User Guide](https://support.apple.com/guide/voiceover/welcome/mac)

---

**Document Version:** 1.0.0
**Last Updated:** December 2025
**Maintained By:** EtherWave Archive Development Team
