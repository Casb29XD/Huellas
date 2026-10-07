# Huella

**Huella** is an Android app (Jetpack Compose) for a pet adoption network in Colombia. People can find dogs, cats and other animals to adopt, report lost and found pets, offer temporary homes (*hogar temporal*), share veterinary events, and earn points and levels as they help. Publications are reviewed by moderators before they go live, and selling animals is not allowed.

The UI is in Spanish. All data is fake and lives in memory (see [Fake data](#fake-in-memory-data)).

<p>
  <img src="docs/screenshots/01_splash.png" width="200" alt="Splash">
  <img src="docs/screenshots/11_home_feed.png" width="200" alt="Home feed">
  <img src="docs/screenshots/16_detail_adoption.png" width="200" alt="Publication detail">
  <img src="docs/screenshots/34_profile_publications.png" width="200" alt="Profile">
</p>

---

## Table of contents

- [Features](#features)
- [Tech stack](#tech-stack)
- [Architecture and folder structure](#architecture-and-folder-structure)
- [Navigation flow](#navigation-flow)
- [Setup](#setup)
- [App walkthrough](#app-walkthrough)
- [Form validation and Snackbar feedback](#form-validation-and-snackbar-feedback)
- [Fake in-memory data](#fake-in-memory-data)
- [Current limitations](#current-limitations)
- [Course requirements mapping](#course-requirements-mapping)

---

## Features

- **Access:** splash, 3-page onboarding (with "Omitir" to skip), login, registration, and password recovery with a confirmation screen.
- **Home feed:** publication cards, category chips (Todas, Adopción, Perdidos, Encontrados, Temporal, Veterinaria), text search, a filters bottom sheet (animal type, size, vaccinated, sterilized, city, distance), and an empty state.
- **Buscar (map):** lost and found pets shown on a drawn map with pins, or as a list. You can filter by Perdidos/Encontrados.
- **Publication detail:** photo pager, info cards, description, location, and an author card. Each category has its own layout: adoption, lost (date and place of loss, "Lo he visto" and "Llamar" buttons), and veterinary event (date, schedule, place, available spots, services, "Cómo llegar").
- **Adoption request:** a "Me interesa adoptar" bottom sheet with an optional message to the author.
- **Report:** a bottom sheet to report a publication (animal sales, abuse, false information, spam, other).
- **Create a publication:** a 3-step wizard (category and photos, then pet data, then description and location), checked at each step, followed by a "Publicación en revisión" screen.
- **Requests:** a *Sent* tab with status tags (Pendiente, Aceptada, Rechazada) and a *Received* tab where you can accept or reject applicants. Accepting asks whether to close the publication.
- **Chat:** a conversation with the other person, linked to a publication.
- **Profile:** level badge, points progress, stats, and two tabs (Mis publicaciones and Historial de puntos). Also a levels screen, a public profile for other users, notifications, edit profile, and settings with logout.
- **Moderation (moderator role):** pending and reported queues, a review detail screen, approve, and reject with a required reason.
- **Gamification:** 4 levels (Amigo Animal, Protector, Guardián, Héroe de las Mascotas) with points for each action.

## Tech stack

Versions are taken from [`gradle/libs.versions.toml`](gradle/libs.versions.toml) and [`app/build.gradle.kts`](app/build.gradle.kts).

| Component | Version |
|---|---|
| Android Gradle Plugin | 9.4.1 |
| Gradle wrapper | 9.6.0 |
| Kotlin (Compose compiler plugin) | 2.2.10 |
| Compose BOM | 2026.02.01 |
| Material 3 + Material Icons Extended | BOM / 1.7.8 |
| Navigation Compose | 2.9.3 |
| Lifecycle (ViewModel Compose, Runtime Compose) | 2.9.4 |
| Activity Compose | 1.10.1 |
| Core KTX | 1.16.0 |
| Coil 3 (`coil-compose` + `coil-network-okhttp`) | 3.3.0 |
| compileSdk / targetSdk | 37 |
| minSdk | 29 (Android 10) |
| Java source/target compatibility | 11 |

The app also uses Kotlin Coroutines and `StateFlow` for UI state, a `Channel` for one-off Snackbar messages, and the Manrope font bundled in `res/font`.

## Architecture and folder structure

The app has one Activity and uses **MVVM**. Each screen is a `@Composable` with its own `ViewModel`. The ViewModel exposes a `StateFlow<UiState>` and a `Flow<String>` of messages that the screen shows in a Snackbar. ViewModels talk to the `HuellaRepository` interface, which today is implemented by an in-memory `FakeRepository`.

```
app/src/main/java/com/desarrolloMovielexample/huella/
├── MainActivity.kt                 # Entry point: edge-to-edge + HuellaTheme + AppNavigation
├── core/
│   ├── components/                 # Reusable UI kit
│   │   ├── Buttons.kt              # PrimaryButton, SecondaryButton, HuellaTextButton
│   │   ├── Controls.kt             # SegmentedSelector, HuellaTabs, HuellaSwitch, HuellaTopBar, HuellaSnackbarHost
│   │   ├── HuellaBottomBar.kt      # 5-item bottom bar with the central "Publicar" button
│   │   ├── HuellaTextField.kt      # Labeled text field with error text, dropdown field
│   │   ├── Images.kt               # PetImage (Coil), Avatar, StripedPlaceholder
│   │   ├── PublicationCards.kt     # PublicationCard, PublicationRowCard
│   │   ├── States.kt               # EmptyState, ErrorState, OfflineBanner, skeletons
│   │   └── Tags.kt                 # Category chips, level badges, status tags, filter chips
│   ├── theme/                      # Color.kt, Type.kt (Manrope), Theme.kt
│   └── utils/Validators.kt         # isValidEmail, isValidPassword (min 8 chars)
├── domain/
│   ├── model/Models.kt             # Category, Level, Publication, User, requests, chat, notifications, moderation…
│   └── repository/
│       ├── HuellaRepository.kt     # Data contract used by all ViewModels
│       └── FakeRepository.kt       # In-memory implementation with seed data and simulated latency
├── features/
│   ├── auth/                       # Splash, Onboarding, Login(+VM), Register(+VM), ForgotPassword(+VM)
│   ├── feed/                       # Home(+VM), Map, PublicationDetail(+VM)
│   ├── createpost/                 # CreatePost(+VM) 3-step wizard, PostSent
│   ├── requests/                   # Requests(+VM) sent/received, Chat(+VM)
│   ├── profile/                    # Profile, Levels, PublicProfile, Notifications, EditProfile, Settings (+VMs)
│   └── moderation/                 # Moderation panel, ModerationDetail (+VM)
└── navigation/
    ├── Routes.kt                   # Sealed class with all routes and createRoute(...) helpers
    └── AppNavigation.kt            # NavHost, Scaffold and bottom bar, tab navigation
```

Resources: launcher icons in `res/mipmap-*` (adaptive icon in `mipmap-anydpi`), illustrations and the paw logo in `res/drawable`, and the Manrope font in `res/font`.

## Navigation flow

Navigation uses **Navigation Compose** with string routes defined in `Routes.kt`. The bottom bar only shows on the four top-level tabs. Each tab keeps its own back stack (`saveState`/`restoreState`).

```
Splash ──► Onboarding (3 pages) ──► Login ──┬─► Register ─────────────┐
                                             ├─► Forgot password      │
                                             └─► Home ◄───────────────┘   (back stack cleared)

Bottom bar:  Inicio (Home) · Buscar (Map) · [Publicar] · Solicitudes (Requests) · Perfil (Profile)

Home ──► Detail/{id} ──► Public profile/{id} ──► Chat/{id}
     ├─► Notifications ──► Requests / Chat / Detail / Levels
     └─► Filters sheet, search
Map (Mapa | Lista) ──► Detail/{id}
Publicar ──► Create post (1/3 → 2/3 → 3/3) ──► Post sent ──► Profile / Home
Requests?tab={0|1} ──► Chat/{id}, Detail/{id}
Profile ──► Edit profile, Settings (──► Change password, Logout → Login), Levels, Detail
        └─► Moderation panel (moderators only) ──► Moderation detail/{id}
```

## Setup

### Prerequisites

- **Android Studio**: a recent stable version that supports **Android Gradle Plugin 9.4** (see the [AGP / Android Studio compatibility table](https://developer.android.com/build/releases/gradle-plugin#android_gradle_plugin_and_android_studio_compatibility)).
- **JDK 17 or newer.** AGP 9.x needs at least JDK 17. The JetBrains Runtime bundled with Android Studio works, and the project was also built with JDK 24.
- **Android SDK Platform 37** (compileSdk/targetSdk 37), installed through the SDK Manager.
- An emulator or a physical device running **Android 10 (API 29) or newer**.
- An internet connection on the device, so the photos (picsum.photos) can load.

### Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/Casb29XD/Huellas.git
   cd Huellas
   ```
2. **Open the project** in Android Studio with *File → Open…* and select the `Huellas` folder (the one that contains `settings.gradle.kts`).
3. **Gradle sync.** Android Studio syncs on its own; if it doesn't, click *Sync Project with Gradle Files*. Install any SDK components it asks for.
4. **Run the app.** Create or start an emulator in *Device Manager* (API 29+), or connect a device with USB debugging on. Then choose the `app` run configuration and press **Run ▶**.
5. **Log in** with the demo account: **`mariana@correo.com` / `12345678`**, or create a new account on the register screen.

### Build from the command line

```bash
# macOS / Linux
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. To install it on a running emulator or device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Set `JAVA_HOME` to a JDK 17+ and point `ANDROID_HOME` (or `sdk.dir` in `local.properties`) to your Android SDK.

## App walkthrough

All of these screenshots were taken from the running app on an emulator (1080×2340).

### 1. Splash and onboarding

The splash shows the logo and the description "Red de adopción de mascotas". It moves on to a 3-page onboarding after about 1.8 s. "Omitir" skips the onboarding and "Comenzar" opens the login.

| Splash | Onboarding 1 | Onboarding 2 | Onboarding 3 |
|:---:|:---:|:---:|:---:|
| <img src="docs/screenshots/01_splash.png" width="180"> | <img src="docs/screenshots/02_onboarding_1.png" width="180"> | <img src="docs/screenshots/03_onboarding_2.png" width="180"> | <img src="docs/screenshots/04_onboarding_3.png" width="180"> |

### 2. Login, register and password recovery

| Login | Login: validation | Register | Register: validation |
|:---:|:---:|:---:|:---:|
| <img src="docs/screenshots/05_login.png" width="180"> | <img src="docs/screenshots/06_login_validation.png" width="180"> | <img src="docs/screenshots/07_register.png" width="180"> | <img src="docs/screenshots/08_register_validation.png" width="180"> |

| Forgot password | Link sent |
|:---:|:---:|
| <img src="docs/screenshots/09_forgot_password.png" width="180"> | <img src="docs/screenshots/10_forgot_password_sent.png" width="180"> |

### 3. Home feed, filters and search

| Home feed | Filters sheet | Search: empty state |
|:---:|:---:|:---:|
| <img src="docs/screenshots/11_home_feed.png" width="200"> | <img src="docs/screenshots/12_filters_sheet.png" width="200"> | <img src="docs/screenshots/13_search_empty.png" width="200"> |

### 4. Buscar: map and list of lost and found pets

| Map mode | List mode |
|:---:|:---:|
| <img src="docs/screenshots/14_map.png" width="200"> | <img src="docs/screenshots/15_map_list.png" width="200"> |

### 5. Publication detail

| Adoption | Adoption (scrolled: location + author) | Lost pet | Veterinary event |
|:---:|:---:|:---:|:---:|
| <img src="docs/screenshots/16_detail_adoption.png" width="180"> | <img src="docs/screenshots/17_detail_adoption_more.png" width="180"> | <img src="docs/screenshots/18_detail_lost.png" width="180"> | <img src="docs/screenshots/19_detail_vet.png" width="180"> |

### 6. Adoption request, report and public profile

| "Me interesa adoptar" | Request sent | Report sheet | Report sent | Author's public profile |
|:---:|:---:|:---:|:---:|:---:|
| <img src="docs/screenshots/20_adopt_sheet.png" width="150"> | <img src="docs/screenshots/21_adopt_request_sent.png" width="150"> | <img src="docs/screenshots/22_report_sheet.png" width="150"> | <img src="docs/screenshots/23_report_sent.png" width="150"> | <img src="docs/screenshots/24_public_profile.png" width="150"> |

### 7. Create a publication

| Step 1: category and photos | Step 2: pet data | Step 2: validation | Step 3: description and location | Sent for review |
|:---:|:---:|:---:|:---:|:---:|
| <img src="docs/screenshots/25_create_post_step1.png" width="150"> | <img src="docs/screenshots/26_create_post_step2.png" width="150"> | <img src="docs/screenshots/27_create_post_validation.png" width="150"> | <img src="docs/screenshots/28_create_post_step3.png" width="150"> | <img src="docs/screenshots/29_post_sent.png" width="150"> |

### 8. Requests and chat

| Sent | Received | Accept dialog | Chat |
|:---:|:---:|:---:|:---:|
| <img src="docs/screenshots/30_requests_sent.png" width="180"> | <img src="docs/screenshots/31_requests_received.png" width="180"> | <img src="docs/screenshots/32_accept_dialog.png" width="180"> | <img src="docs/screenshots/33_chat.png" width="180"> |

### 9. Profile, levels, notifications and settings

| Profile: my publications | Profile: points history | Levels |
|:---:|:---:|:---:|
| <img src="docs/screenshots/34_profile_publications.png" width="200"> | <img src="docs/screenshots/35_profile_points_history.png" width="200"> | <img src="docs/screenshots/36_levels.png" width="200"> |

| Notifications | Edit profile | Settings |
|:---:|:---:|:---:|
| <img src="docs/screenshots/37_notifications.png" width="200"> | <img src="docs/screenshots/38_edit_profile.png" width="200"> | <img src="docs/screenshots/39_settings.png" width="200"> |

### 10. Moderation (moderator role)

The demo user Mariana is a moderator, so a "Panel de moderación" entry appears at the bottom of her profile.

| Pending | Reports | Review detail | Reject dialog | Reject: validation |
|:---:|:---:|:---:|:---:|:---:|
| <img src="docs/screenshots/40_moderation_panel.png" width="150"> | <img src="docs/screenshots/41_moderation_reports.png" width="150"> | <img src="docs/screenshots/42_moderation_detail.png" width="150"> | <img src="docs/screenshots/43_reject_dialog.png" width="150"> | <img src="docs/screenshots/44_reject_validation.png" width="150"> |

## Form validation and Snackbar feedback

Validation lives in the ViewModels, using the helpers in `core/utils/Validators.kt`. When a check fails, the field shows its error inline (red border and helper text), and the ViewModel sends a one-off message through a `Channel`. The screen collects it and shows it in a Material 3 Snackbar (`HuellaSnackbarHost`).

| Screen | Rules | Example Snackbar messages |
|---|---|---|
| Login | Email required and valid (regex); password required | "Revisa los campos marcados", "¡Hola, Mariana!", login error from the repository, "Próximamente" (Google button) |
| Register | Name required; email valid and not already registered; city required; password ≥ 8 chars; confirmation must match; terms must be accepted | "Revisa los campos marcados", "Debes aceptar los términos y condiciones", "¡Cuenta creada! Bienvenido a Huella" |
| Forgot password | Email required, valid, and must belong to an existing account | "Enlace enviado a …" |
| Create post | Step 1: at least one photo. Step 2: breed and age required. Step 3: title ≥ 5 chars, description ≥ 20 chars, date required for lost/found/vet categories, city required | "Agrega al menos una foto", "Escribe la raza aproximada", "Publicación enviada" |
| Detail | Adoption request and report actions | "Solicitud enviada. Te avisaremos cuando Camila responda", "Gracias. Un moderador revisará tu reporte" |
| Moderation | Rejection reason is required | "Selecciona un motivo de rechazo" |
| Filters | Clear action | "Filtros eliminados" |

## Fake in-memory data

There is no backend. `domain/repository/FakeRepository.kt` is a Kotlin `object` that implements `HuellaRepository`:

- It is seeded with users, publications (adoption, lost, found, temporary home and vet), sent and received requests, a chat conversation, notifications, a points history, and moderation queues.
- `suspend` operations add a **700 ms** `delay` to simulate network latency, so loading states can be seen.
- Changes such as new accounts, publications, requests, messages or moderation decisions stay **only in memory** and are lost when the app process is killed.
- **Demo credentials:** `mariana@correo.com` / `12345678`. Mariana López has the *Protector* level and the moderator role. Accounts you create on the register screen also work until the app is restarted.

## Current limitations

- **The map is a placeholder.** The maps on the Buscar tab and the publication detail are drawn with a Compose `Canvas` (streets and pins), and the location in moderation detail is a striped placeholder. There is no Google Maps or real geolocation, and the "Marcar ubicación" field in create post is not saved yet.
- **Random images.** Pet photos come from `https://picsum.photos/seed/…`, so they are random stock photos, often not animals. They need internet; without it you see a striped placeholder. "Agregar foto" adds another random image rather than opening the camera or gallery.
- **No backend, auth or persistence.** Everything is in memory: Google sign-in only shows "Próximamente", password reset doesn't send an email, and notifications are not push notifications.
- **Spanish only.** Strings are hard-coded in the composables and are not in `strings.xml`.

## Course requirements mapping

| Requirement | Implementation |
|---|---|
| Project structure | Packages `core` (components, theme, utils), `domain` (model, repository), `features` (auth, feed, createpost, requests, profile, moderation) and `navigation`. See [Architecture](#architecture-and-folder-structure). |
| Onboarding with logo and description | `features/auth/SplashScreen.kt` shows the paw logo, the name "Huella" and the description "Red de adopción de mascotas". `features/auth/OnboardingScreen.kt` has 3 illustrated pages with title and description, a pager indicator, and Omitir/Siguiente/Comenzar. |
| App icon in mipmap + manifest | `res/mipmap-{mdpi…xxxhdpi}/ic_launcher(.png/_round.png)` and the adaptive icon in `res/mipmap-anydpi/`. `AndroidManifest.xml` sets `android:icon="@mipmap/ic_launcher"` and `android:roundIcon="@mipmap/ic_launcher_round"`. |
| Login + ViewModel | `features/auth/LoginScreen.kt` + `LoginViewModel.kt` (`LoginUiState`, email and password validation, show/hide password, loading state, `repo.login`). |
| Register + ViewModel | `features/auth/RegisterScreen.kt` + `RegisterViewModel.kt` (name, email, city dropdown, password + confirmation, terms checkbox). |
| Password recovery | `features/auth/ForgotPasswordScreen.kt` + `ForgotPasswordViewModel.kt` (email form, then a "Revisa tu correo" success state with "Usar otro correo"). It is also reachable from Settings → Cambiar contraseña. |
| Feed + detail + ViewModels | `features/feed/HomeScreen.kt` + `HomeViewModel.kt` (feed, categories, search, filters), `MapScreen.kt`, and `PublicationDetailScreen.kt` + `PublicationDetailViewModel.kt` (layouts per category, adoption request, report). |
| Create post + ViewModel | `features/createpost/CreatePostScreen.kt` + `CreatePostViewModel.kt` (3-step wizard checked at each step) and `PostSentScreen.kt`. |
| Navigation | `navigation/Routes.kt` (sealed class of routes with arguments) and `navigation/AppNavigation.kt` (`NavHost`, bottom bar shown only on tabs, tabs with saved state, back stack cleared on login and logout). |
| Snackbar | Each ViewModel sends messages through `Channel<String>` and each screen shows them in `SnackbarHostState` via `HuellaSnackbarHost`. See [Form validation and Snackbar feedback](#form-validation-and-snackbar-feedback). |
