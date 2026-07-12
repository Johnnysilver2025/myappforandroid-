# Aegis AppLock 🛡️

Aegis AppLock is a high-performance, open-source, offline-first application protector for Android, designed in accordance with Material Design 3 and Android Security Best Practices. Similar to Xiaomi AppLock, Aegis allows users to secure selected system and installed applications with encrypted local credentials, featuring fingerprint, face, PIN, and pattern unlock methods.

---

## 🎨 Visual Identity & Key Highlights
- **Dynamic Color (Material 3)**: Deep integration with M3 dynamic palette colors, transitioning fluidly between dark and light themes.
- **Glassmorphic Security Cards**: Polished, tactile UI containers showcasing beautiful icons, progress badges, and diagnostic states.
- **Tactile Inputs**: Interactive, custom-drawn 3x3 pattern lock canvas and custom numeric PIN keypads with integrated haptic feedback.
- **100% Privacy-First**: 100% offline, requires zero cloud accounts, sends zero analytics or telemetry data, contains zero ads, and uses local Keystore-encrypted AES storage.

---

## 🚀 Key Features
1. **Onboarding Tutorial**: Interactive, step-by-step credentials config (PIN/Pattern), local security question setup, and guided permissions checker.
2. **Foreground App Detection**: Integrates with Android's system `AccessibilityService` to track foreground windows instantaneously and launch lock overlays without battery drain.
3. **Multi-Verification Options**: PIN Keypad, custom Pattern drawing, and official Android `BiometricPrompt` fingerprint and face verification.
4. **Local Password Recovery**: Secure, hashed local security question answers to reset credentials locally without remote recovery backdoors.
5. **Security Audit Log**: A local database log storing critical events like setting updates, password changes, and backup creations.
6. **Local Encrypted Backup**: Serialize settings and locked apps list, encrypt them via Android Keystore AES-GCM, and export them as a simple Base64 text string.
7. **Custom Lock Behaviors**:
   - **Immediately**: Relocks apps the moment they leave focus.
   - **Custom Delay**: Relocks apps 1 minute after exiting.
   - **Screen Off**: Relocks apps only when the screen turns off.
8. **App Self-Protection**: Toggle whether Aegis AppLock itself is locked.
9. **Auto-Restart**: Automatically reload logs and security configuration upon device reboot.

---

## 🏗️ Clean Software Architecture

The project is structured under **Clean Architecture** and **MVVM (Model-View-ViewModel)** guidelines to isolate business logic, enforce SOLID principles, and maximize testability.

### Architecture Layers
- **UI Layer (Jetpack Compose)**: Dynamic screens observing state flows from ViewModels, ensuring stateless, composable layouts.
- **ViewModel Layer**: Emits descriptive UI state schemas (`StateFlow`) and maps inputs back to business rules.
- **Data/Repository Layer**: Abstructs data access.
  - **Room Database**: Saves the list of protected packages and activity event logs.
  - **DataStore Preferences**: Handles application options, hashed password states, and salts.
- **Security Layer**: Handles Android Keystore operations (AES-GCM key derivation), password hashing (SHA-256 with salts), and active lock states.
- **Service Monitor Layer**: The core accessibility engine running in the background to monitor active foreground package transitions.

---

## 📁 Repository Folder Structure

```
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt            # Entrypoint & Jetpack Compose NavHost
│   │   │   │   ├── LockScreenActivity.kt      # Secure fullscreen overlay activity
│   │   │   │   ├── AppLockApplication.kt      # Application class & DI Container
│   │   │   │   ├── data/                      # Local Persistence Layer
│   │   │   │   │   ├── AppLockDatabase.kt     # Room entities (ProtectedApp, ActivityLog) & DAO
│   │   │   │   │   ├── AppLockRepository.kt   # Unified repository, backup serializer
│   │   │   │   │   └── PreferencesManager.kt  # Datastore Preferences settings manager
│   │   │   │   ├── security/                  # Encryption & Cryptography
│   │   │   │   │   ├── CryptoManager.kt       # Keystore AES-GCM engine
│   │   │   │   │   ├── HashUtils.kt           # SHA-256 salting & hashing
│   │   │   │   │   └── AppLockManager.kt      # Whitelist & lockout delay manager
│   │   │   │   ├── service/                   # Background Services & Receivers
│   │   │   │   │   ├── AppLockAccessibilityService.kt # Foreground app monitor service
│   │   │   │   │   └── BootReceiver.kt        # Startup protection reboot receiver
│   │   │   │   ├── ui/                        # User Interfaces
│   │   │   │   │   ├── apps/                  # App Selection ViewModels & lists
│   │   │   │   │   ├── lock/                  # Secure keypad/canvas layout views
│   │   │   │   │   ├── onboarding/            # First-time user setup state views
│   │   │   │   │   ├── settings/              # Settings & Backup/Restore view views
│   │   │   │   │   └── theme/                 # Material 3 typography & themes
│   │   │   │   └── utils/                     # Helper utilities
│   │   │   │       └── ServiceUtils.kt        # Accessibility service checker
│   │   │   └── res/
│   │   │       ├── values/strings.xml         # XML String resources
│   │   │       └── xml/                       # Accessibility config xmls
│   │   └── test/                              # Local JVM unit & integration tests
│   │       └── java/com/example/
│   │           ├── ExampleUnitTest.kt
│   │           ├── ExampleRobolectricTest.kt
│   │           ├── AppLockSecurityTest.kt     # Cryptography & hashing tests
│   │           └── AppLockRepositoryTest.kt   # In-memory Room & backup restore tests
│   └── build.gradle.kts                       # App-level dependencies configurations
├── gradle/libs.versions.toml                  # Central Version Catalog dependencies
├── settings.gradle.kts                        # Root project config
└── LICENSE                                    # MIT License documentation
```

---

## 🛠️ Development Setup & Build Instructions

### Prerequisites
- **Android Studio Ladybug** (or newer)
- **JDK 17** or **JDK 21** configured in build environments
- **Android SDK Level 31+** (minimum required for biometrics & target runtimes)

### Compiling and Running
1. Clone the repository locally:
   ```bash
   git clone https://github.com/yourusername/aegis-applock.git
   ```
2. Open the project in Android Studio. Gradle will automatically sync using the `gradle/libs.versions.toml` version catalog.
3. Build the Debug APK:
   ```bash
   gradle assembleDebug
   ```
4. Run the app on a connected device or streaming emulator:
   ```bash
   gradle installDebug
   ```

### Running Tests
Aegis AppLock contains extensive unit and integration tests written with **Robolectric** so they execute instantly on the local JVM without an emulator.

To run the security, repository, and storage tests:
```bash
gradle :app:testDebugUnitTest
```

---

## 🔒 Security Operations & Safeguards
- **Master Key Derivation**: Aegis generates a custom, non-exportable hardware-backed symmetric Key (`AegisAppLockMasterKey`) in the Android Keystore system.
- **Zero Plaintext Storage**: PINs, patterns, and security question answers are combined with random cryptographic `salts` and hashed using `SHA-256` before writing to disk.
- **Fail-Safe Override**: If back pressure is detected or the user tries to back out of the overlay `LockScreenActivity`, Aegis redirects the focus to the Android `Home Launcher` automatically, preventing bypass.
- **Brute-Force Lockout**: 5 failed unlock attempts trigger a 30-second cooldown timer. Cooldown increases dynamically to 60s and 120s upon subsequent failures to mitigate automated guessing.

---

## 🤝 Contribution Guide
We welcome contributions to Aegis AppLock!
1. Fork the Project.
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`).
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`).
4. Push to the Branch (`git push origin feature/AmazingFeature`).
5. Open a Pull Request.

---

## 📄 License
This project is open-source and licensed under the **MIT License**. See the [LICENSE](./LICENSE) file for details.
