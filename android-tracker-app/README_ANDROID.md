# 📱 Quantum GPS Native Android Tracker (Solution 3)

A lightweight (~2MB) native Android Application that runs an unkillable **`ForegroundService`** with **`FusedLocationProviderClient`** and **HiveMQ Cloud MQTT**, enabling continuous 1-second GPS location broadcasting **even after the app is swiped away from Recent Apps, the screen is locked, or the phone is rebooted**.

---

## 🚀 Features
- **Unkillable Background Service (`START_STICKY`)**: Android OS keeps the GPS broadcasting engine alive 24/7 even if the user swipes away the app.
- **Hardware GNSS Precision**: Uses `Priority.PRIORITY_HIGH_ACCURACY` directly from satellite receivers.
- **Auto-Start on Boot (`BOOT_COMPLETED`)**: Automatically resumes broadcasting if the phone restarts.
- **Direct MQTT Integration**: Broadcasts to the same HiveMQ / EMQX topic (`antigravity_tracker_v3/{roomId}/{clientId}`) used by your Google Satellite Web Map (`admin.html`).

---

## 🛠️ How to Build the APK in Android Studio

### Step 1: Open the Project in Android Studio
1. Open **Android Studio**.
2. Click **Open** (or `File > Open`).
3. Select the folder:
   ```
   C:\Users\jashwanth ck\.gemini\antigravity\scratch\map-location-finder\android-tracker-app
   ```
4. Allow Gradle to sync dependencies (takes ~1-2 minutes on first load).

---

### Step 2: Build the Installable `.apk`
1. In Android Studio, go to the top menu: **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
2. Once the build finishes, click **locate** in the popup notification at the bottom right.
3. Your installable file will be located at:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

---

### Step 3: Install and Use on Any Android Phone
1. Send the `app-debug.apk` file to the target phone via **WhatsApp**, **Google Drive**, or **Bluetooth**.
2. Tap the APK to install it (enable "Install from Unknown Sources" if prompted).
3. Open **Quantum GPS**, type the **Room ID** (e.g. `room-alpha`), and enter the person's name.
4. Tap **"Start 24/7 Tracking Service"** and allow location permissions (**"Allow all the time"**).
5. The friend can now **swipe away the app, lock their phone, or put it in their pocket** — their live moving marker on your **Admin Web Map (`admin.html`)** will never stop!
