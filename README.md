# CivicSense — Intelligent Civic Action System

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Gemini AI](https://img.shields.io/badge/AI-Gemini%20Flash-8E75FF?logo=google&logoColor=white)](https://ai.google.dev/)
[![Room Database](https://img.shields.io/badge/Database-Room%20SQLite-4285F4?logo=sqlite&logoColor=white)](https://developer.android.com/training/data-storage/room)

**CivicSense** is an enterprise-grade civic action and municipal infrastructure platform built with modern Android architecture (Jetpack Compose, Kotlin Coroutines, Room DB, and Google Gemini AI). It empowers citizens to report civic hazards with photos, voice notes, and geolocation while giving field officers and city administrators real-time triage tools, automated municipal routing, and resolution workflows.

---

## 🏛 Key Features

### 1. 📢 Smart Citizen Reporting
- **Multimodal Submissions**: Report road hazards, broken streetlights, water leakages, waste accumulation, and public safety issues using camera photos, voice notes, or text.
- **AI Automated Triage**: Powered by Gemini 3.5 Flash to automatically detect hazard severity (P1–P4), estimate safety risk index (0–100), identify potential duplicate reports in the vicinity, and route directly to the responsible municipal department (*Public Works, Water Authority, Power & Lighting, Sanitation*).
- **Offline-First Persistence**: High-speed Room database guarantees zero data loss in low-connectivity areas.

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
- **Local Persistence**: Android Room Database (SQLite) + KSP (Kotlin Symbol Processing).
- **AI / LLM Integration**: Google Gemini REST API via Retrofit 2 + Gson Converter.
- **Authentication**: Android Jetpack `CredentialManager` + Google Identity Services (`googleid`).
- **Secrets Management**: Android Secrets Gradle Plugin via `.env` and `BuildConfig`.
- **Testing**: Robolectric & Roborazzi screenshot verification.

---

## 🚀 Getting Started & Installation

### Prerequisites
- **Android Studio** Ladybug (2024.2.1) or newer.
- **JDK**: Java 17 or higher.
- **Android SDK**: Minimum SDK 24 (Android 7.0), Target SDK 34 (Android 14+).

### 1. Clone the Repository
```bash
git clone https://github.com/your-username/civicsense-android.git
cd civicsense-android
```

### 2. Configure API Keys & Secrets

CivicSense uses the **Android Secrets Gradle Plugin** to keep credentials secure.

1. Copy `.env.example` to `.env` in the project root:
   ```bash
   cp .env.example .env
   ```
2. Open `.env` and configure your keys:
   ```env
   # Google Gemini AI API Key for automated triage & department routing
   # Get your key at: https://aistudio.google.com/
   GEMINI_API_KEY=your_gemini_api_key_here

   # (Optional) Google OAuth 2.0 Web Client ID for Google Sign-In
   # Found in Firebase Console -> Authentication -> Sign-in method -> Google -> Web SDK configuration
   GOOGLE_WEB_CLIENT_ID=your_web_client_id.apps.googleusercontent.com
   ```

> **Note**: In AI Studio Build environments, configure your keys directly inside the **Secrets panel** in the sidebar.

### 3. Build & Run the Application

#### Using Android Studio:
1. Open Android Studio and select **Open**, then choose the project folder.
2. Allow Gradle to sync dependencies.
3. Select an Android Virtual Device (AVD) or physical device running Android 7.0+.
4. Click **Run (Shift + F10)**.

#### Using Gradle CLI:
```bash
# Assemble Debug APK
gradle assembleDebug

# Run JVM & Robolectric Unit Tests
gradle :app:testDebugUnitTest
```

The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 📂 Project Structure

```
app/src/main/java/com/example/
├── MainActivity.kt               # Main entry point & window insets configuration
├── data/
│   ├── auth/
│   │   └── AuthManager.kt       # CredentialManager & Google Sign-In engine
│   ├── local/
│   │   ├── CivicDao.kt          # Room SQLite Database access queries
│   │   └── CivicDatabase.kt     # Room Database instance with pre-populated seed data
│   ├── model/
│   │   ├── CivicIncident.kt     # Primary domain models, categories, & priorities
│   │   └── UserRole.kt          # RBAC enum (Citizen, Field Officer, Civic Admin)
│   ├── remote/
│   │   └── GeminiCivicService.kt# Gemini AI triage & smart analysis engine
│   └── repository/
│       └── CivicRepository.kt   # Single source of truth repository layer
└── ui/
    ├── components/              # Reusable UI components (Cards, Badges, Map Canvas)
    ├── navigation/
    │   └── CivicNavigation.kt   # Navigation scaffolding, top app bar, & role switcher
    ├── screens/
    │   ├── HomeScreen.kt        # Primary citizen feed & neighborhood radar
    │   ├── ReportScreen.kt      # AI report wizard with photo & voice capture
    │   ├── IncidentDetailScreen.kt # Timeline, verification votes, & officer updates
    │   ├── MapScreen.kt         # Spatial hazard map with interactive pins
    │   ├── OfficerScreen.kt     # Dispatch queue & resolution submission
    │   ├── AdminInsightsScreen.kt # SLA analytics, heatmaps & AI patterns
    │   ├── TrustCenterScreen.kt # Verification & transparency audit logs
    │   ├── ProfileScreen.kt     # User activity, karma score & account controls
    │   └── AuthDialog.kt        # Authentication modal (Google & Email sign in)
    └── theme/                   # Material 3 Color palette, typography, & shapes
```

---

## 🔒 Security & Best Practices

- **Zero Hardcoded Secrets**: All API tokens are injected via `BuildConfig` and excluded from version control.
- **Graceful Fallbacks**: The Gemini AI pipeline and authentication modules feature automatic heuristic fallbacks, ensuring offline resilience and uninterrupted operation.
- **Edge-to-Edge Design**: Full compliance with Android 15 Edge-to-Edge rendering and window insets standards.

---

## 📄 License
This project is licensed under the Apache License 2.0. See the `LICENSE` file for details.
