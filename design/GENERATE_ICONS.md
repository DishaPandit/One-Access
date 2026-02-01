# App Icon Generation Guide

Your logo has been designed! Now let's generate all the required Android icon sizes.

## 🎨 Logo Design

I've created two versions:
1. **`logo.svg`** - Full featured version with door, NFC waves, checkmark, and "1" badge
2. **`logo_simple.svg`** - Clean minimal version (recommended for app icon)

## 🚀 Quick Generation (Recommended)

### Option 1: Android Studio (Easiest)

1. **Open Android Studio**
2. **Right-click** on `app/src/main/res`
3. Select **New → Image Asset**
4. Choose **Launcher Icons (Adaptive and Legacy)**
5. **Source Asset:** 
   - Select **Image** 
   - Browse to `design/logo_simple.svg`
6. **Background Layer:**
   - Color: `#2196F3`
7. Click **Next** → **Finish**

This automatically generates all required sizes!

### Option 2: Online Tool (No software needed)

1. Go to: **https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html**
2. Upload `logo_simple.svg`
3. Set background color: `#2196F3`
4. Download the ZIP
5. Extract and copy to `android/app/src/main/res/`

### Option 3: Using ImageMagick (Command line)

If you have ImageMagick installed:

```bash
cd design

# Generate all sizes
convert logo_simple.svg -resize 48x48 ../android/app/src/main/res/mipmap-mdpi/ic_launcher.png
convert logo_simple.svg -resize 72x72 ../android/app/src/main/res/mipmap-hdpi/ic_launcher.png
convert logo_simple.svg -resize 96x96 ../android/app/src/main/res/mipmap-xhdpi/ic_launcher.png
convert logo_simple.svg -resize 144x144 ../android/app/src/main/res/mipmap-xxhdpi/ic_launcher.png
convert logo_simple.svg -resize 192x192 ../android/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png

# Round versions
convert logo_simple.svg -resize 48x48 ../android/app/src/main/res/mipmap-mdpi/ic_launcher_round.png
convert logo_simple.svg -resize 72x72 ../android/app/src/main/res/mipmap-hdpi/ic_launcher_round.png
convert logo_simple.svg -resize 96x96 ../android/app/src/main/res/mipmap-xhdpi/ic_launcher_round.png
convert logo_simple.svg -resize 144x144 ../android/app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png
convert logo_simple.svg -resize 192x192 ../android/app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png
```

## 📏 Required Icon Sizes

Android needs icons in multiple densities:

| Density | Size | Path |
|---------|------|------|
| mdpi | 48×48 | `mipmap-mdpi/ic_launcher.png` |
| hdpi | 72×72 | `mipmap-hdpi/ic_launcher.png` |
| xhdpi | 96×96 | `mipmap-xhdpi/ic_launcher.png` |
| xxhdpi | 144×144 | `mipmap-xxhdpi/ic_launcher.png` |
| xxxhdpi | 192×192 | `mipmap-xxxhdpi/ic_launcher.png` |

Same sizes for `ic_launcher_round.png` (circular version)

## ✅ What's Already Done

I've created:
- ✅ **Adaptive icon XML** (works on Android 8.0+)
- ✅ **Vector drawable** for foreground
- ✅ **Background color** definition
- ✅ **Monochrome icon** support (Android 13+ themed icons)

Your app will automatically have a modern adaptive icon!

## 🎨 Logo Elements

The icon features:
- **Blue background** (#2196F3) - Trust and technology
- **White door** - Access and entry
- **NFC waves** - Contactless technology
- **Modern style** - Clean and professional

## 🔧 Customization

Want to change colors? Edit:
```xml
<!-- android/app/src/main/res/values/ic_launcher_background.xml -->
<color name="ic_launcher_background">#2196F3</color>
```

Common alternatives:
- **Green:** `#4CAF50` (eco, growth)
- **Purple:** `#9C27B0` (premium, modern)
- **Dark Blue:** `#1976D2` (professional)
- **Orange:** `#FF9800` (energetic)

## 📱 Testing

After generating icons:

1. **Rebuild app:**
   ```bash
   cd android
   ./gradlew clean assembleDebug
   ```

2. **Install on device**

3. **Check home screen** - icon should appear!

4. **Check app drawer** - make sure icon looks good

## 🎉 Pro Tips

1. **Keep it simple** - Icons should be recognizable at small sizes
2. **High contrast** - Make sure icon stands out
3. **Test on device** - Emulator might not show true colors
4. **Round corners** - Modern Android uses rounded icons
5. **Adaptive icon** - Will look good on any device

## 🐛 Troubleshooting

**Icon not updating?**
```bash
# Clean build
./gradlew clean

# Reinstall app
adb uninstall com.oneaccess.app
./gradlew installDebug
```

**Wrong colors?**
- Check `ic_launcher_background.xml`
- Make sure SVG colors are correct
- Try regenerating from Android Studio

**Blurry icon?**
- Use vector (SVG) source
- Generate high-res PNG (512×512 minimum)
- Use proper density folders

## 📚 Resources

- [Android Icon Guidelines](https://developer.android.com/guide/practices/ui_guidelines/icon_design_launcher)
- [Material Design Icons](https://material.io/design/iconography/product-icons.html)
- [Adaptive Icons](https://developer.android.com/guide/practices/ui_guidelines/icon_design_adaptive)

---

Need help? Open an issue on GitHub!
