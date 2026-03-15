# Publishing 4D Market Intelligence to Google Play Store

## Pre-requisites

- [x] Google Play Developer Account ($25 one-time fee) — https://play.google.com/console/signup
- [x] Signed release AAB: `app/build/outputs/bundle/release/app-release.aab`
- [x] Privacy Policy URL (host `store-listing/privacy-policy.html` on your website)
- [x] Store listing text (in `store-listing/play-store-listing.md`)

---

## Step 1: Create Google Play Developer Account

1. Go to https://play.google.com/console/signup
2. Sign in with your Google account
3. Pay the **$25 one-time registration fee**
4. Complete identity verification (takes 1-2 business days)

## Step 2: Create the App in Play Console

1. Open **Google Play Console** → **All apps** → **Create app**
2. Fill in:
   - **App name**: `4D Market Intelligence`
   - **Default language**: English (United States)
   - **App or game**: App
   - **Free or paid**: Free
3. Accept the declarations and click **Create app**

## Step 3: Set Up Store Listing

Go to **Grow** → **Store presence** → **Main store listing**:

1. **App name**: `4D Market Intelligence`
2. **Short description**: Copy from `store-listing/play-store-listing.md`
3. **Full description**: Copy from `store-listing/play-store-listing.md`
4. **App icon**: Upload a 512x512 PNG (high-res icon)
5. **Feature graphic**: Upload a 1024x500 PNG (promotional banner)
6. **Screenshots**: Upload at least 2 phone screenshots (min 320px, max 3840px on each side)
   - Take screenshots on your device or emulator showing: Dashboard, Watchlist, Signal Center, Correlations
7. **Category**: Finance
8. **Contact email**: contact@4ddigital.solutions

### How to take screenshots:
```powershell
# From connected device via ADB
adb shell screencap /sdcard/screenshot.png
adb pull /sdcard/screenshot.png ./store-listing/screenshots/
```

## Step 4: Content Rating

Go to **Policy** → **App content** → **Content rating**:

1. Start the questionnaire
2. Select category: **Utility, Productivity, Communication, or other**
3. Answer all questions (no violence, no sexual content, no gambling, no user-generated content)
4. You'll receive a rating of **Everyone** / **PEGI 3**

## Step 5: Privacy Policy & Data Safety

### Privacy Policy
Go to **Policy** → **App content** → **Privacy policy**:
- Enter your privacy policy URL: `https://4ddigital.solutions/privacy-policy`
- You MUST host `store-listing/privacy-policy.html` at this URL before submitting

### Data Safety
Go to **Policy** → **App content** → **Data safety**:
1. Does your app collect or share user data? → **No** (the app stores data locally only)
2. Data types collected: **None**
3. Data encrypted in transit: **Yes** (HTTPS only)
4. Users can request data deletion: **Yes** (uninstalling the app deletes all data)

## Step 6: Target Audience & App Access

Go to **Policy** → **App content**:

- **Target audience**: 18+ (financial markets content)
- **App access**: All functionality available without special access
- **Ads**: This app does not contain ads
- **Government apps**: No
- **Financial features**: The app provides market data display and analysis tools. It does NOT facilitate trading, payments, or financial transactions.

## Step 7: Create a Release

Go to **Release** → **Production** → **Create new release**:

1. **App signing**: Accept Google Play App Signing (recommended). Google manages the upload key for you.
2. **Upload AAB**: Upload `app/build/outputs/bundle/release/app-release.aab`
3. **Release name**: `1.0.0`
4. **Release notes**:
   ```
   Initial release of 4D Market Intelligence.

   • Multi-market watchlists (DAX, B3, crypto, commodities)
   • AI Signal Center with explainable technical analysis
   • Cross-asset correlation engine (11+ market pairs)
   • World market clock with exchange session status
   • Smart alerts (price, %, volatility, correlation)
   • Prediction engine with entry/target/stop-loss levels
   • Professional dark terminal UI
   • Free — no ads, no tracking, no account required
   ```
5. Click **Review release** → **Start rollout to Production**

## Step 8: Review Process

- Google reviews all new apps before publishing
- Typical review time: **1-3 business days** (can be up to 7 days for new accounts)
- You'll receive an email when approved or if action is needed

---

## After Publishing

### Update the App
1. Increment `versionCode` and `versionName` in `build.gradle.kts` (root)
2. Build new AAB: `.\gradlew.bat bundleRelease`
3. Upload to Play Console → Release → Production → Create new release

### Monitor
- **Play Console Dashboard**: Downloads, ratings, crashes (Android Vitals)
- **Pre-launch report**: Google automatically tests on real devices

---

## Graphic Assets Checklist

| Asset | Size | Required |
|-------|------|----------|
| App icon (hi-res) | 512 x 512 px PNG | Yes |
| Feature graphic | 1024 x 500 px PNG | Yes |
| Phone screenshots | min 2, 16:9 or 9:16 | Yes (min 2) |
| 7" tablet screenshots | 16:9 or 9:16 | Recommended |
| 10" tablet screenshots | 16:9 or 9:16 | Recommended |

---

## Build Commands Reference

```powershell
# Build release AAB (for Play Store)
cd "d:\4D trader"
.\gradlew.bat bundleRelease

# Build release APK (for direct distribution)
.\gradlew.bat assembleRelease

# Output locations:
# AAB: app\build\outputs\bundle\release\app-release.aab
# APK: app\build\outputs\apk\release\app-release.apk
```
