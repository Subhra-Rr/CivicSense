# CivicSense — Intelligent Civic Action System

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Gemini AI](https://img.shields.io/badge/AI-Gemini%20Flash-8E75FF?logo=google&logoColor=white)](https://ai.google.dev/)
[![Firebase Firestore](https://img.shields.io/badge/Cloud%20DB-Firebase%20Firestore-FFCA28?logo=firebase&logoColor=black)](https://firebase.google.com/docs/firestore)
[![Room Database](https://img.shields.io/badge/Local%20Cache-Room%20SQLite-4285F4?logo=sqlite&logoColor=white)](https://developer.android.com/training/data-storage/room)

**CivicSense** is an enterprise-grade civic action and municipal infrastructure platform built with modern Android architecture (Jetpack Compose, Kotlin Coroutines, Cloud Firestore, Room DB, and Google Gemini AI). It empowers citizens to report civic hazards with photos, voice notes, and geolocation while giving field officers and city administrators real-time triage tools, automated municipal routing, and resolution workflows.

---

## 🏛 Key Features

### 1. 📢 Smart Citizen Reporting
- **Multimodal Submissions**: Report road hazards, broken streetlights, water leakages, waste accumulation, and public safety issues using camera photos, voice notes, or text.
- **AI Automated Triage**: Powered by Gemini 3.5 Flash to automatically detect hazard severity (P1–P4), estimate safety risk index (0–100), identify potential duplicate reports in the vicinity, and route directly to the responsible municipal department (*Public Works, Water Authority, Power & Lighting, Sanitation*).
- **Cloud Firestore Real-Time Database**: Multi-device sync with live incident snapshots, real-time status updates, and offline fallback.

### 2. 🗺 Interactive Radar & Spatial Map
- **Neighborhood Heatmap**: Visual pin clustering with priority badges (P1 Critical to P4 Routine).
- **Spatial Duplication Detection**: Automatically matches new incoming citizen reports to active cases within 150 meters.
- **Verification Voting Engine**: Community members can verify, confirm, or upvote neighborhood issues to accelerate dispatch priority.

### 3. 🛡 Role-Based Access Control (RBAC)
- **Citizen View**: Community dashboard, active neighborhood radar, personal activity scorecard, and karma point tracking.
- **Field Officer Operations**: Real-time dispatch queue, assignment management, GPS navigation, and resolution proof submission with timestamped photo uploads.
- **Civic Administrator Insights**: Citywide incident heatmaps, SLA turnaround tracking, department workload metrics, and automated AI hazard hotspot detection.

### 4. 🔐 Secure Authentication System
- **Google Sign-In**: Integrated with modern Android Jetpack `CredentialManager` and `GoogleIdTokenCredential`.
- **Email & Password Authentication**: Complete Sign In, Sign Up, role assignment, and session management.
- **Guest / Demo Mode**: Pre-configured instant access profiles for rapid testing and demonstrations.

---

## 🛠 Tech Stack & Architecture

- **UI Framework**: Modern Jetpack Compose with Material Design 3 (M3).
- **Architecture**: MVVM (Model-View-ViewModel) with Kotlin Coroutines & StateFlow.
- **Cloud Database**: Google Cloud Firestore (`firebase-firestore`) for real-time multiplayer data synchronization.
- **Local Persistence & Cache**: Android Room Database (SQLite) + KSP (Kotlin Symbol Processing).
- **AI / LLM Integration**: Google Gemini REST API via Retrofit 2 + Gson Converter.
- **Authentication**: Android Jetpack `CredentialManager` + Google Identity Services (`googleid`).
- **Secrets Management**: Android Secrets Gradle Plugin via `.env` and `BuildConfig`.
- **Testing**: Robolectric & Roborazzi screenshot verification.

---

## 🔑 How to Get & Configure Environment Variables (Step-by-Step)

To configure your API keys and Firebase credentials, obtain each variable as follows:

### 1. `GEMINI_API_KEY` (For AI Automated Triage & Routing)
1. Go to [Google AI Studio](https://aistudio.google.com/).
2. Sign in with your Google account.
3. Click **Get API Key** in the top navigation or sidebar.
4. Click **Create API key** in a new or existing Google Cloud project.
5. Copy the generated key (starts with `AIzaSy...`).

---

### 2. `FIREBASE_PROJECT_ID`, `FIREBASE_APPLICATION_ID`, `FIREBASE_API_KEY` (For Cloud Firestore)
1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Create a new project (e.g., `civicsense-app`) or open an existing one.
3. In the left sidebar, click **Build &rarr; Firestore Database**, then click **Create database**:
   - Choose your preferred location (e.g., `nam5` or `asia-south1`).
   - Start in **Test mode** (or configure custom security rules).
4. Click the **Project Settings (Gear icon)** in the top-left sidebar &rarr; **General** tab.
5. Scroll down to **Your apps**:
   - If you haven't added an Android app, click the **Android icon**.
   - Enter Package name: `com.example`.
   - Click **Register app**.
   - Download the `google-services.json` file and place it in the `app/` folder (or copy the parameters below into your `.env`):
     - **`FIREBASE_PROJECT_ID`**: Found under *Project ID* (e.g. `civicsense-12345`).
     - **`FIREBASE_APPLICATION_ID`**: Found under *App ID* (e.g. `1:1234567890:android:abcdef1234567890`).
     - **`FIREBASE_API_KEY`**: Found under *Web API Key* (starts with `AIzaSy...`).
     - **`FIREBASE_STORAGE_BUCKET`**: Found under *Storage bucket* (e.g. `civicsense-12345.firebasestorage.app`).

---

### 3. `GOOGLE_WEB_CLIENT_ID` (For Google Sign-In via Credential Manager)
1. In the [Firebase Console](https://console.firebase.google.com/), open your project.
2. Click **Build &rarr; Authentication** in the sidebar.
3. If setting up for the first time, click **Get Started**, then select the **Sign-in method** tab.
4. Select **Google** from the providers list and click **Enable**.
5. Choose your **Project support email**.
6. Expand the **Web SDK configuration** dropdown.
7. Copy the **Web client ID** (it ends with `.apps.googleusercontent.com`).

---

## ⚙️ Applying Secrets to the Project

### Option A: In Google AI Studio Build (Recommended)
1. Open the **Secrets panel** in the AI Studio sidebar.
2. Add your key-value pairs:
   - `GEMINI_API_KEY` = `your_gemini_api_key`
   - `GOOGLE_WEB_CLIENT_ID` = `your_web_client_id.apps.googleusercontent.com`
   - `FIREBASE_PROJECT_ID` = `your-project-id`
   - `FIREBASE_API_KEY` = `your-web-api-key`
   - `FIREBASE_APPLICATION_ID` = `your-app-id`

### Option B: Using `.env` File (Local Android Studio)
1. Create a `.env` file in the project root (copy from `.env.example`):
   ```bash
   cp .env.example .env
   ```
2. Populate the `.env` file:
   ```env
   GEMINI_API_KEY=AIzaSyYourGeminiApiKeyHere
   GOOGLE_WEB_CLIENT_ID=1234567890-abcdef.apps.googleusercontent.com
   FIREBASE_PROJECT_ID=your-project-id
   FIREBASE_APPLICATION_ID=1:1234567890:android:abcdef
   FIREBASE_API_KEY=AIzaSyYourWebApiKey
   ```

---

## 🚀 Building & Running

```bash
# Compile and build the debug APK
gradle assembleDebug

# Run JVM Unit Tests
gradle :app:testDebugUnitTest
```

---

## 📄 License
This project is licensed under the Apache License 2.0. See the `LICENSE` file for details.
