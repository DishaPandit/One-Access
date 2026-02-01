# 🚀 Deploy OneAccess to Google Play Store

Complete step-by-step guide to publish your app on Google Play.

---

## **Prerequisites**

- ✅ Google Account
- ✅ $25 one-time Google Play Developer registration fee
- ✅ Android app working (you have this!)
- ✅ Production backend server (see Backend Deployment below)

---

## **PHASE 1: Prepare App for Release** 📱

### **Step 1: Update App Details**

1. **Change Package Name** (Important!)
   
   Open `android/app/build.gradle.kts` and change:
   ```kotlin
   applicationId = "com.yourcompany.oneaccess"  // Make it unique!
   ```
   
   Examples:
   - `com.acmecorp.oneaccess`
   - `com.yourname.officeaccess`
   - `com.yourorg.timetracker`

2. **Update Version Numbers**
   ```kotlin
   versionCode = 1        // Android version (increment for each release)
   versionName = "1.0.0"  // User-visible version
   ```

3. **Update App Name**
   
   Edit `android/app/src/main/res/values/strings.xml`:
   ```xml
   <string name="app_name">OneAccess</string>
   ```

### **Step 2: Create Signing Key**

**This is CRITICAL!** You need a signing key to publish your app.

**In PowerShell/Terminal:**

```powershell
# Navigate to android folder
cd "C:\Learning\Python Project\android"

# Generate keystore (one-time setup)
keytool -genkey -v -keystore release-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias oneaccess-key

# You'll be asked:
# - Keystore password: [create a strong password]
# - Key password: [create a strong password]
# - Your name, organization, etc.
```

**⚠️ IMPORTANT:**
- **Save these passwords!** You'll never be able to recover them
- **Backup `release-keystore.jks`** file - you need it for ALL future updates
- **NEVER share or commit this file to Git!**

### **Step 3: Configure Signing**

Edit `android/app/build.gradle.kts`:

1. Update the signing config with your passwords:
   ```kotlin
   signingConfigs {
       create("release") {
           storeFile = file("../release-keystore.jks")
           storePassword = "YOUR_STORE_PASSWORD"  // The password you created
           keyAlias = "oneaccess-key"
           keyPassword = "YOUR_KEY_PASSWORD"      // The key password you created
       }
   }
   ```

2. Enable signing in release build:
   ```kotlin
   buildTypes {
       release {
           signingConfig = signingConfigs.getByName("release")  // Uncomment this line
           // ... rest stays the same
       }
   }
   ```

### **Step 4: Build Release APK/AAB**

**Option A: Android App Bundle (AAB) - RECOMMENDED**

```powershell
cd android
./gradlew.bat bundleRelease
```

**Output:** `android/app/build/outputs/bundle/release/app-release.aab`

**Option B: APK (for testing)**

```powershell
cd android
./gradlew.bat assembleRelease
```

**Output:** `android/app/build/outputs/apk/release/app-release.apk`

---

## **PHASE 2: Create Assets** 🎨

Google Play requires specific assets. Create these:

### **Required Graphics:**

1. **App Icon** (512 x 512 px, PNG)
   - Your app's main icon
   - No transparency

2. **Feature Graphic** (1024 x 500 px, PNG/JPG)
   - Banner for your store listing
   - Showcases your app

3. **Screenshots** (At least 2)
   - Phone: 1080 x 1920 px or 1080 x 2340 px
   - Take screenshots from your emulator:
     - Access tab with QR code
     - Time tracking tab showing statistics
     - Settings screen

4. **Optional:**
   - Promo video (YouTube)
   - Tablet screenshots

### **Quick Screenshot Tips:**

In Android Emulator:
1. Open your app
2. Click the camera icon (📷) on the emulator toolbar
3. Save screenshots to `android/screenshots/` folder

---

## **PHASE 3: Google Play Console Setup** 🎮

### **Step 1: Create Developer Account**

1. Go to: https://play.google.com/console
2. Sign in with your Google Account
3. Pay $25 registration fee (one-time)
4. Complete developer profile

### **Step 2: Create New App**

1. Click **"Create app"**
2. Fill in details:
   - **App name:** OneAccess (or your name)
   - **Default language:** English (United States)
   - **App/Game:** App
   - **Free/Paid:** Free
3. Accept declarations
4. Click **"Create app"**

### **Step 3: Set Up Store Listing**

Fill in all required fields:

**App Details:**
```
Short Description (80 chars max):
NFC-based office access control with automatic time tracking

Full Description (4000 chars max):
OneAccess is a modern office access control system that uses NFC technology 
to grant building access and automatically track employee time.

Features:
• NFC phone-based access (no physical cards needed)
• Automatic time tracking - tap in, tap out
• Real-time statistics and reporting
• Delegation - share access temporarily
• Visitor pass management
• Secure JWT-based authentication
• Complete audit trail

Perfect for:
- Office buildings
- Corporate campuses
- Co-working spaces
- Facilities requiring time tracking

How it works:
1. Tap your phone on the building door reader (Entry)
2. System tracks your time inside
3. Tap when leaving (Exit)
4. View complete time statistics in the app

Security & Privacy:
- Server-side authentication
- Encrypted communication
- No location tracking inside building
- Complete audit logs

Requirements:
- Android 8.0 (Oreo) or higher
- NFC-capable device
- Backend server (self-hosted or cloud)
```

**Category:** Business > Productivity

**Contact Details:**
- Email: your-email@example.com
- Privacy Policy URL: (see below)

**Graphics:**
- Upload all assets you created

### **Step 4: Content Rating**

1. Go to **"Content rating"** section
2. Fill out questionnaire
3. Most likely rating: **Everyone**
4. Submit for rating

### **Step 5: App Access**

If your app requires login:
- Provide demo credentials:
  - Email: `alice@acme.com`
  - Note: "Demo account, no password required"

### **Step 6: Privacy Policy**

**You MUST have a privacy policy!**

Create a simple one at: https://app-privacy-policy-generator.firebaseapp.com/

Or use this template:

```
Privacy Policy for OneAccess

This app collects:
- Email address (for authentication)
- Device ID (for access control)
- Entry/exit timestamps (for time tracking)
- Gate access events (for security audit)

Data is stored on your organization's server.
No data is shared with third parties.
No location tracking beyond building entry/exit.

Contact: your-email@example.com
```

Host this on a public URL (GitHub Pages, your website, etc.)

### **Step 7: Upload App Bundle**

1. Go to **"Production"** → **"Create new release"**
2. Upload your **AAB file** (`app-release.aab`)
3. Fill in release notes:
   ```
   Initial release
   - NFC-based building access
   - Automatic time tracking
   - Delegation & visitor management
   - Real-time statistics
   ```
4. Save (don't submit yet!)

### **Step 8: Complete All Sections**

Check the dashboard - all sections must be ✅ green:
- Store listing
- Content rating
- App access
- Ads (select "No" if no ads)
- Target audience
- News apps (select "No")
- Data safety
- Privacy policy
- App content

### **Step 9: Submit for Review**

1. Review everything
2. Click **"Submit for review"**
3. Wait 1-3 days for Google's review
4. You'll get an email when approved/rejected

---

## **PHASE 4: Backend Deployment** 🖥️

### **Your App Needs a Production Server!**

The Flask backend must be deployed to a public server.

### **Option A: Cloud Deployment (Recommended)**

**1. Deploy to Heroku (Free tier available):**

```powershell
# Install Heroku CLI
# Download from: https://devcenter.heroku.com/articles/heroku-cli

cd backend

# Create Procfile
echo "web: python -m flask run --host=0.0.0.0 --port=$PORT" > Procfile

# Create runtime.txt
echo "python-3.12" > runtime.txt

# Deploy
heroku create oneaccess-backend
heroku config:set FLASK_APP=app.main:app
git push heroku main
```

**2. Deploy to Google Cloud Run:**

```powershell
# Create Dockerfile
cd backend
# (Create Dockerfile - see below)

# Deploy
gcloud run deploy oneaccess-backend \
  --source . \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated
```

**3. Deploy to AWS EC2, DigitalOcean, etc.**

### **Option B: Self-Hosted Server**

- Set up a server with static IP
- Install Python, Flask, nginx
- Configure SSL certificate (Let's Encrypt)
- Keep it running 24/7

### **Update App Backend URL**

Once deployed, update the default backend URL in your app:

Edit `android/app/src/main/java/com/oneaccess/app/ui/AppState.kt`:

```kotlin
fun backendUrl(context: Context): String {
    return context.getSharedPreferences("oneaccess", Context.MODE_PRIVATE)
        .getString("backend_url", "https://your-backend-url.com") ?: "https://your-backend-url.com"
}
```

---

## **PHASE 5: Post-Launch** 🎉

### **After Approval:**

1. **App is Live!** 🎊
   - Available on Google Play Store
   - Users can search and download

2. **Monitor:**
   - Google Play Console → Statistics
   - Check crash reports
   - Read user reviews

3. **Update App:**
   ```powershell
   # Increment version in build.gradle.kts
   versionCode = 2
   versionName = "1.0.1"
   
   # Build new AAB
   ./gradlew.bat bundleRelease
   
   # Upload to Play Console → Production → Create new release
   ```

---

## **Testing Before Launch** 🧪

### **Internal Testing Track**

Before going live, test with real users:

1. **Play Console** → **"Internal testing"**
2. Upload AAB
3. Add testers (email addresses)
4. They get a link to install
5. Fix any issues
6. Promote to Production when ready

---

## **Common Issues & Solutions** ⚠️

### **Issue: "Upload failed - Duplicate package"**
**Solution:** Change `applicationId` in build.gradle.kts to be unique

### **Issue: "Signing key mismatch"**
**Solution:** You must use the SAME keystore for all updates. Backup your keystore!

### **Issue: "App not compatible with any devices"**
**Solution:** Check minSdk (26 = Android 8.0). Lower if needed.

### **Issue: "Backend not accessible"**
**Solution:** 
- Ensure backend has public URL with HTTPS
- Update backend URL in app
- Test with real device (not emulator)

---

## **Costs** 💰

| Item | Cost | Frequency |
|------|------|-----------|
| Google Play Developer Account | $25 | One-time |
| Backend Hosting (Heroku/AWS) | $5-50/month | Monthly |
| Domain Name (optional) | $10-15/year | Yearly |
| SSL Certificate | Free (Let's Encrypt) | - |

---

## **Checklist Before Submitting** ✅

- [ ] Changed package name to unique ID
- [ ] Created and backed up signing keystore
- [ ] Built signed AAB file
- [ ] Created all required graphics (icon, screenshots, etc.)
- [ ] Deployed backend to production server
- [ ] Updated backend URL in app
- [ ] Created privacy policy
- [ ] Filled out complete store listing
- [ ] Tested app on real device
- [ ] Completed content rating
- [ ] Submitted for review

---

## **Next Steps**

1. **Create your signing key** (Step 2 above)
2. **Build release AAB** (Step 4 above)
3. **Create Play Console account** (Phase 3)
4. **Deploy backend** (Phase 4)
5. **Submit app!** 🚀

---

## **Need Help?**

- **Google Play Help:** https://support.google.com/googleplay/android-developer
- **Android Publishing Guide:** https://developer.android.com/studio/publish

**Good luck with your launch!** 🎉📱
