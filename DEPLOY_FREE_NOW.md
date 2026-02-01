# 🚀 Deploy Backend FREE (Right Now!)

## **Total Time: 10 minutes**
## **Cost: $0 (Forever Free)**

---

## **Step 1: Push Code to GitHub** (3 minutes)

### **If you haven't pushed to GitHub yet:**

```powershell
cd "C:\Learning\Python Project"

# Check status
git status

# Add all files
git add .

# Commit
git commit -m "Ready for deployment"

# Push to GitHub
git push origin main
```

### **If you don't have a GitHub remote yet:**

1. Go to: https://github.com/new
2. Create repository (name: `oneaccess-app`)
3. Make it **Private** (keep your code secret)
4. Don't initialize with README
5. Click "Create repository"

**Then run:**

```powershell
cd "C:\Learning\Python Project"
git remote add origin https://github.com/YOUR_USERNAME/oneaccess-app.git
git branch -M main
git add .
git commit -m "Initial commit"
git push -u origin main
```

---

## **Step 2: Deploy to Render.com** (5 minutes)

### **A. Sign Up for Render (FREE)**

1. Go to: https://render.com
2. Click **"Get Started for Free"**
3. Sign up with GitHub (easiest)
4. Authorize Render to access your repos

### **B. Create Web Service**

1. Click **"New +"** → **"Web Service"**

2. **Connect Repository:**
   - Find your `oneaccess-app` repo
   - Click **"Connect"**

3. **Configure Service:**
   ```
   Name: oneaccess-backend
   Region: Oregon (US West)
   Branch: main
   Root Directory: (leave blank)
   Runtime: Python 3
   Build Command: cd backend && pip install -r requirements.txt
   Start Command: cd backend && python -m flask run --host=0.0.0.0 --port=$PORT
   ```

4. **Plan:** Select **"Free"** (not Starter!)
   - 750 hours/month (plenty!)
   - Sleeps after 15 min inactivity
   - Wakes up on request (takes 30 sec)

5. **Environment Variables:**
   Click **"Add Environment Variable"** for each:
   ```
   FLASK_APP = app.main:app
   ONEACCESS_TOKEN_TTL_SECONDS = 20
   ```

6. Click **"Create Web Service"**

### **C. Wait for Deployment** (2-3 minutes)

You'll see:
```
==> Building...
==> Installing dependencies...
==> Starting service...
==> Your service is live at https://oneaccess-backend.onrender.com
```

**That's your backend URL!** 🎉

---

## **Step 3: Test Backend** (1 minute)

### **Test it works:**

```powershell
# Test login endpoint
curl https://oneaccess-backend.onrender.com/auth/login `
  -H "Content-Type: application/json" `
  -d '{"email":"alice@acme.com"}'

# Should return: {"accessToken":"eyJhbGc..."}
```

**If you see a token → SUCCESS!** ✅

---

## **Step 4: Update Android App** (2 minutes)

Now update your app to use the production backend:

### **Edit the default backend URL:**

File: `android/app/src/main/java/com/oneaccess/app/ui/AppState.kt`

Find this line (around line 32):
```kotlin
.getString("backend_url", "http://10.0.2.2:8000") ?: "http://10.0.2.2:8000"
```

Change to:
```kotlin
.getString("backend_url", "https://oneaccess-backend.onrender.com") ?: "https://oneaccess-backend.onrender.com"
//                         ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^         ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
//                         Your Render URL here                        And here
```

### **Rebuild APK:**

```powershell
cd android
./gradlew.bat assembleDebug
```

**Output:** New APK at `app/build/outputs/apk/debug/OneAccess-v1.0.0-debug.apk`

---

## **Step 5: Install on Phone & Test!** 📱

### **A. Install APK:**

1. **Copy APK to phone:**
   - USB transfer
   - Email to yourself
   - Upload to Drive/Dropbox

2. **On phone:**
   - Download/find APK
   - Enable "Install from Unknown Sources"
   - Tap APK → Install

### **B. Test the App:**

1. **Open OneAccess app**

2. **Sign in:**
   - Backend URL should already be: `https://oneaccess-backend.onrender.com`
   - Email: `alice@acme.com`
   - Gate: `BLD_ACME`
   - Click "Sign in"

3. **Check Access tab:**
   - Should see QR code generating ✅

4. **Check Time tab:**
   - Should see your test sessions ✅

5. **Try all features:**
   - Delegation
   - Visitor passes
   - Time tracking

---

## **🎉 You're Live!**

**Your app is now:**
- ✅ Running on real backend (FREE)
- ✅ Accessible from anywhere
- ✅ Working on real phones
- ✅ Ready for real users

---

## **📊 Free Tier Limits:**

| Resource | Free Tier |
|----------|-----------|
| **Monthly Hours** | 750 (unlimited for 1 service) |
| **Sleep After** | 15 min inactivity |
| **Wake Up Time** | ~30 seconds |
| **Bandwidth** | 100 GB/month |
| **Cost** | **$0/month** |

**Perfect for:**
- Testing
- Demo
- Small office (<50 users)
- Side projects

---

## **⚠️ Important Notes:**

### **About Free Tier "Sleep":**

**What happens:**
- After 15 min of no requests, service sleeps
- First request wakes it up (takes 30 sec)
- Subsequent requests are instant

**For your app:**
- First tap of the day might take 30 sec
- After that, instant!
- This is fine for most use cases

**To prevent sleep (optional):**
- Set up a cron job to ping every 14 minutes
- Or upgrade to paid plan ($7/month - never sleeps)

---

## **🔧 Updating Your Backend:**

**When you make code changes:**

```powershell
# 1. Make your changes
# 2. Commit and push
git add .
git commit -m "Update backend"
git push

# 3. Render auto-deploys!
# Check: https://dashboard.render.com
```

**Deployment takes:** ~2-3 minutes

---

## **🆘 Troubleshooting:**

### **"Build failed on Render"**
- Check build logs in Render dashboard
- Make sure requirements.txt is correct
- Verify Python version matches

### **"Service not responding"**
- Service might be sleeping (wait 30 sec)
- Check Render dashboard for errors
- Verify environment variables are set

### **"App can't connect"**
- Check URL is correct: `https://` not `http://`
- No trailing slash: ❌ `.../` ✅ `.com`
- Phone must have internet

### **"Token expired" errors**
- Normal! Tokens last 20 seconds
- App auto-refreshes QR codes
- Just tap again

---

## **💰 When to Upgrade:**

**Stick with FREE if:**
- ✅ Testing/demo
- ✅ <50 users
- ✅ Don't mind 30sec wake-up

**Upgrade to $7/month if:**
- ⬆️ 50+ active users
- ⬆️ Need instant response 24/7
- ⬆️ Running production business

---

## **Next Steps After Deployment:**

1. ✅ **Test all features** on real phone
2. ✅ **Share APK** with friends/colleagues
3. ✅ **Get feedback**
4. ✅ **Iterate and improve**
5. 🚀 **Optional:** Deploy to Google Play ($25)

---

## **📱 Sharing Your App:**

### **Internal Distribution (FREE):**

**Upload APK to:**
- Google Drive → Share link
- Dropbox → Share link
- GitHub Releases → Download link
- Your company intranet

**Send to users:**
```
Hi! Install our new access app:

1. Download APK from: [LINK]
2. Enable "Install from Unknown Sources"
3. Install APK
4. Open app and sign in with your email

Backend URL is already configured!
No setup needed - just install and use.
```

---

## **🎯 Quick Checklist:**

```
[ ] Pushed code to GitHub
[ ] Created Render account
[ ] Deployed backend to Render
[ ] Got backend URL (https://...)
[ ] Updated Android app with URL
[ ] Rebuilt APK
[ ] Installed on phone
[ ] Tested all features
[ ] Shared with users!
```

---

**Ready to deploy?** Follow Step 1 above! 🚀

**Questions?** Check the troubleshooting section!

**Cost so far:** Still $0! 🎉
