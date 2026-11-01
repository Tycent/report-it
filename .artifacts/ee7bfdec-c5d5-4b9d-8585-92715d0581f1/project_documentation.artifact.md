# Report It — Comprehensive Project Documentation

This document provides a complete architectural and technical reference for the **Report It** project, covering both the Android application and the web landing page / backend integration. It is designed to assist in modifying, maintaining, and scaling the web landing page and app ecosystem.

---

## 1. Project Overview

**Report It** is a civic reporting platform connecting citizens with municipal and utility authorities (**ZINWA**, **City Council**, and **ZESA**). Users can report local service delivery issues (e.g., burst pipes, power outages, water shortages) along with GPS coordinates and photo attachments. Authorities (department admins) review, track, and update the status of these reports in real-time.

---

## 2. Architecture & Technology Stack

### Android App
- **Language**: Kotlin (Jetpack Compose Material 3)
- **Architecture**: Single-Activity (`MainActivity`) managing state and navigation between Jetpack Compose screens.
- **Local Persistence**: Room Database (`AppDatabase`) with offline-first support and automatic schema fallback/migration.
- **Cloud Backend**: Google Firebase (Firestore, Authentication, Storage, Messaging, Analytics).
- **Key Capabilities**: Location Services (`FusedLocationProviderClient`), Camera Capture (`FileProvider` + `ActivityResultContracts.TakePicture`), and File Uploads (Coil image loading).

### Web Landing & Portal (`web-landing/` & `reportit/`)
- **Frontend**: HTML5, CSS3 (Modern responsive design with CSS variables and flexbox/grid), Vanilla JavaScript (`script.js`).
- **Backend / Server**: PHP endpoints (`login.php`, `register.php`, `contact.php`) interacting with Firebase Firestore REST APIs and server-side validation.
- **Cloud Sync & Demo Mode**: Direct JavaScript SDK (`firebase-app-compat.js`, `firebase-firestore-compat.js`) matching the Android app's Firestore data structure, featuring an intelligent **LocalStorage Demo Fallback** for offline or standalone browser testing.

---

## 3. Data Schema & Database Design

Both the Android app and web portal share identical Firestore collections and Room tables:

### A. Users Collection / Table (`users`)
- `phoneNumber` (Primary Key / Document ID): String
- `name`: String
- `address`: String
- `password`: String

### B. Admins Collection / Table (`admins`)
- `forceNumber` (Primary Key / Document ID): String
- `name`: String
- `department`: String (`ZINWA`, `CITY COUNCIL`, `ZESA`, or `Master`)
- `phoneNumber`: String
- `password`: String

### C. Reports Collection / Table (`reports`)
- `id` (Auto-increment integer locally / Firestore auto-ID): Int / String
- `description`: String
- `department`: String (`ZINWA`, `CITY COUNCIL`, `ZESA`)
- `reporterPhone`: String
- `latitude`: Double?
- `longitude`: Double?
- `imageUrl`: String? (Firebase Storage Download URL)
- `status`: String (`Pending`, `In Progress`, `Resolved`)
- `timestamp`: Long (Epoch milliseconds)

---

## 4. Authentication & Master Admin System

### Master Admin & Test Credentials
- **Username / Force Number / Phone**: `Tafadzwa` / `0777777777`
- **Password**: `TYCENT@12`
- **Department**: `Master`

### Privileges & Workflows
1. **Public User Login / Signup**: Citizens log in or sign up using their phone number and password.
2. **Restricted Admin Terminal**: Public self-registration for admins is disabled. Only the **Master Admin** (`Tafadzwa` / `0777777777`) can access the Admin Terminal login.
3. **Manual Admin Creation**: Once logged in, the Master Admin sees a **"Register New Admin Account"** button on their dashboard. They can manually register new departmental admins (`ZINWA`, `City Council`, `ZESA`).
4. **Global Oversight**: The Master Admin has global visibility over all incoming reports across all departments, while departmental admins only see reports assigned to their specific department.

---

## 5. Web Landing Page Structure & Modification Guide

The `web-landing/` and `reportit/` directories house the web frontend and backend PHP handlers.

### Key Files in `web-landing/` & `reportit/`:
- `index.html` / `index.php` / `app.html`: Landing page and dashboard interface containing hero sections, feature grids, reporting modals, and authentication forms.
- `script.js`: Client-side JavaScript handling Firebase initialization, form submissions, Firestore queries, and automated **LocalStorage Demo Mode fallback** for offline testing.
- `login.php` & `register.php`: Server-side PHP scripts handling authentication requests via Firestore REST APIs.
- `style.css`: Comprehensive styling file defining color variables (`--ink`, `--muted`, `--paper`, `--coral`, `--mint`), typography, responsive layouts, and animations.

### How to Run & Test the Website:
1. **Local HTTP Server (Recommended)**:
   ```bash
   cd web-landing
   python -m http.server 8000
   ```
   Open `http://localhost:8000` in your browser.
2. **Standalone Browser Testing (Demo Mode)**: You can also open the HTML files directly. The app will automatically switch to LocalStorage demo mode if offline or unlinked from Firebase.
3. **Test Login Credentials**: Use phone `0777777777` and password `TYCENT@12`.
