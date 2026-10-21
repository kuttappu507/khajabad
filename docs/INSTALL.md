# Installing khajabad

## 1. Download

**Direct APK (recommended):**

👉 https://github.com/kuttappu507/khajabad/releases/download/latest/khajabad.apk

That link always serves the newest signed build (≈16 MB). It is a plain `.apk` file — **do not** download it as a zip.

From the terminal instead:

```bash
curl -L -o khajabad.apk https://github.com/kuttappu507/khajabad/releases/download/latest/khajabad.apk
adb install -r khajabad.apk
```

## 2. Open it

1. Tap the downloaded file (Files app → Downloads → `khajabad.apk`).
2. If Android asks, allow **Install unknown apps** for the app you opened it with (Files / Chrome).
3. If Play Protect shows a "scan" dialog, choose **Install anyway** / **More details → Install anyway**.
4. The app is called **khajabad**, cream icon with a mint mihrab arch.

Requires **Android 6.0 (API 23)** or newer. No account, no ads; the books work fully offline.

## "App not installed" — what actually causes it

| Cause | Fix |
|---|---|
| **You installed a `.zip`** (the GitHub *artifact* download is a zip that contains the APK) | Use the direct `.apk` link above. Unzip → pick `app-release.apk` if you already downloaded the zip. |
| **An older khajabad is installed with a different signature** (e.g. an earlier debug build) | Settings → Apps → khajabad → **Uninstall**, then install again. In a terminal: `adb uninstall com.khajabad.app`. |
| **Play Protect / OEM security blocking the file** | Open the file, tap **More details → Install anyway**. On MIUI/Realme: Settings → Privacy → *Install unknown apps* off for the source, or turn off "Security scan" for that install. |
| **Download was truncated** (size should be ≈16 MB) | Re-download on a stable connection, or use `curl -L` / `adb install`. |
| **Android version below 6.0** | The app needs API 23+. |
| **Free storage below ~40 MB** | Free some space; the installer needs room for the APK *and* the unpacked copy. |

Anything else? Get the real error code with:

```bash
adb install -r khajabad.apk
# e.g. INSTALL_FAILED_UPDATE_INCOMPATIBLE  → uninstall the old copy
#      INSTALL_PARSE_FAILED_NO_CERTIFICATES → corrupt download
#      INSTALL_FAILED_OLDER_SDK             → Android < 6.0
```

## Build it yourself

```bash
git clone https://github.com/kuttappu507/khajabad.git
cd khajabad
./gradlew assembleRelease        # app/build/outputs/apk/release/app-release.apk
./gradlew installRelease         # with a device connected
```

The project is signed with the committed key `app/keystore.p12`
(alias `khajabad`, store/key password `khajabad`) so CI and local builds install
over each other. Replace it with your own key before publishing anywhere.
