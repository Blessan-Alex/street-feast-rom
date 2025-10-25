### ROMS UI/UX Guide (Desktop Admin + Android Chef/Waiter)

Goal: Simple, minimalistic, easy to use. Quality over quantity. Every choice improves speed, clarity, and reduces errors.

## Core Principles
- Clarity first: Only essential info on screen; progressive disclosure for details.
- Single-purpose screens: One primary task per screen to reduce cognitive load.
- Consistent patterns: Same placements for actions, same color meanings, predictable feedback.
- Fast recognition: Use status colors and concise labels; avoid dense text.

## Shared Patterns (All Apps)
- Top bar: App title/logo on left; role indicator and logout on right.
- Lists: Card-based rows with clear hierarchy (title, key meta, actions). Smooth scrolling.
- Actions: One primary action per screen, prominent; secondary actions de-emphasized.
- Feedback: Instant UI change on tap, then realtime sync; toast for success/error.
- Empty/loading/error states: Always show meaningful state with short guidance.

### Color Palette
Status chips
- Created: #9E9E9E (Grey)
- Accepted: #2196F3 (Blue)
- Prepared: #FFC107 (Amber/Yellow)
- Delivered: #4CAF50 (Green)
- Closed: #616161 (Dark Grey)
- Cancelled: #8C1007 (Red)

Core UI
- Primary action: #2196F3 (Blue)
- Success/confirm: #4CAF50 (Green)
- Danger/destructive: #8C1007 (Red)
- Background (canvas): #FFFFFF (White)
- Surface/chrome: #C9CDCF (Grey)
- Text: #212121 (Dark Grey)

Usage
- Use red strictly for destructive actions and Cancelled status; keep primary confirmations in Blue/Green to avoid ambiguity.
- Maintain WCAG contrast ≥ 4.5:1 for all text/button labels (e.g., white text on red/blue/green buttons).

## Desktop (Admin)
Rationale: Admin performs creation, edits, coordination. Needs overview + precise control with minimal navigation.

- Navigation: Left sidebar with three items: Menu, Orders, Create Order.
- Create Order flow: Three clear steps across a single page (Mode → Table/Customer → Items). Always-visible Total and Submit.
- Orders dashboard: Two tabs: Active and Closed/Cancelled. Default to Active. Sort by newest.
- Edit inline: Quantities adjustable in-place; changes highlighted briefly (e.g., yellow flash) to confirm.
- Close/Add more/Cancel: Grouped on order details header; dangerous actions in Red with confirmation.
- Density: Use table/list hybrid for high scan speed; item titles bold, quantities with +/- controls.
- Keyboard support: Enter to submit, Esc to cancel modals, arrow keys for quantity where possible.

Accessibility and speed:
- Minimum 14–16px body text; 44px+ click targets.
- Focus states on interactive elements; visible keyboard focus.
- Maintain 4.5:1 contrast for text.

## Android (Chef)
Rationale: Fast triage and progression. Chef should act with minimal taps.

- Home: Two tabs or filters: Incoming (Created) and In Progress (Accepted).
- Card content: Table/mode prominent, then item summary; total at trailing edge.
- Primary actions:
  - Incoming: Accept (Blue) prominent.
  - In Progress: Mark Prepared (Green) prominent.
- Edit notifications: If admin edits before delivery and kitchen work changes, surface a small "Updated" chip on the card.
- Removal rules: Prepared orders exit Chef’s list automatically; Cancelled removed immediately.

Interaction:
- Single-tap primary action; long-press opens details (optional) to avoid accidental taps.
- Large touch targets (48dp min); thumb-friendly spacing.
- Offline hint: If connection lost, show subtle banner and keep local list until sync.

## Android (Waiter)
Rationale: Quick delivery confirmation from a simple queue.

- Home: Single list of Prepared orders.
- Card content: Table/mode first; item summary concise; delivery mode badge when applicable.
- Primary action: Mark Delivered (Green) large button.
- Reopened orders: When admin adds items post-delivery, show the order reappear with an "Reopened" chip.

Interaction and clarity:
- One-tap confirmation; 1-step undo toast for 5 seconds to correct mistakes.
- Immediate list updates; delivered items depart list.

## Microcopy and Labels
- Buttons: Use verbs that match status transitions: Accept, Prepared, Delivered, Close, Cancel, Add Items.
- Empty states: "No orders yet" / "You’re all caught up" with a simple icon.
- Errors: Short cause + action, e.g., "Couldn’t update order. Retry?".

## Why these choices?
- Reduced navigation and single-primary-action screens speed up operations in busy environments.
- Color-coded statuses minimize reading and improve rapid recognition across roles.
- Inline edits with transient highlights confirm changes without extra dialogs.
- Undo toasts reduce fear of tapping mistakes, improving confidence and speed.


