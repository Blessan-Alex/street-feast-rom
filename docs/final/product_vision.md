# Streat Feast — Product Vision

## 1. Product Summary
**Streat Feast** is a minimalistic, fast, and beginner-friendly restaurant management system designed for small food outlets and kitchens. It enables the **Admin (desktop)** to manage menus and create orders, and the **Chef and Waiter (Android)** to manage food preparation and delivery flow — all in real time via Firebase.

The system replaces complex POS and restaurant tools with a lightweight, offline-capable, intuitive interface that prioritizes speed, clarity, and ease of use.

---

## 2. Problem Statement
Most restaurant POS systems are heavy, expensive, and require training. Small businesses often struggle with:
- Overloaded interfaces with too many options.
- Reliance on paid APIs or SaaS.
- Poor support for offline or low-internet environments.
- Lack of coordination between kitchen and front staff.

**Streat Feast** solves this by providing an ultra-simplified, free, and clear flow for the order lifecycle.

---

## 3. Product Goals
- ⚡ **Fast & Reliable:** Instant updates with Firestore realtime sync.
- 🧠 **Simple UX:** One clear action per screen. Green = good, Red = cancel, Yellow = attention.
- 📱 **Multi-Device:** Admin (Electron desktop), Chef & Waiter (Android apps).
- 🔔 **Live Statuses & Notifications:** Each role gets only relevant updates.
- 🧩 **Scalable Foundation:** Firestore schema allows easy scaling to multiple stores later.
- 🔒 **Secure:** Role-based access using Firebase Auth.

---

## 4. Core Features
| Role | Core Capabilities |
|------|--------------------|
| **Admin** | Manage menu (CSV/manual), create/edit orders, set frequent items, delete data, monitor dashboards |
| **Chef** | Accept orders, mark as prepared, view chef tips |
| **Waiter** | View ready orders, mark delivered/served, receive prepared notifications |

> No pricing, no payments, and no printing — focused purely on the operational flow.

---

## 5. Target Users
- Small and medium-sized restaurants or cloud kitchens.
- Staff with minimal technical experience.
- Teams needing a low-cost, offline-friendly POS alternative.

---

## 6. Success Metrics
| Metric | Target |
|--------|--------|
| Order flow time | < 10 seconds end-to-end |
| UI errors per session | < 1% |
| New user onboarding time | < 5 minutes |
| Offline operation uptime | > 95% |

---

## 7. Guiding Principles
1. **Clarity Over Complexity:** Every element serves a single clear purpose.
2. **Color as Language:** Use color cues for feedback and speed.
3. **Low Overhead:** No third-party paid tools; all Firebase free-tier friendly.
4. **Fast Feedback:** Realtime sync ensures instant team coordination.
5. **Future-Proof:** Modular, Firestore-based backend adaptable for scaling.

---

## 8. One-Line Vision Statement
> "A simple, free, and clear restaurant order management system for real kitchens — built for speed and ease, not complexity."

