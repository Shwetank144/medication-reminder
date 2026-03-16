# EyeDrop Reminder - Android App

Post-operative cataract eye drop reminder with **real background alarms** that work even when your phone is locked.

## Features
- 3 medications: Moxigram DM (6-week taper), Micronac PF (1.5 months), Fomisa (ongoing)
- 15-minute staggered gap between drops at each time slot
- **Native Android alarms** - works in background, wakes phone from sleep
- Voice announcement of medication name using Text-to-Speech
- Visual schedule with tap-to-complete tracking
- Auto-reschedules after phone reboot

## How to Build (No Android Studio Needed!)

This repo uses **GitHub Actions** to automatically build the APK.

### Steps:
1. Create a **new public repository** on GitHub
2. Upload ALL these files (maintaining the folder structure)
3. Go to the **Actions** tab in your repo
4. You'll see "Build APK" workflow running
5. Wait 3-4 minutes for it to finish (green checkmark)
6. Click on the completed run → scroll down to **Artifacts**
7. Download **EyeDropReminder** zip → extract → install the APK

### Installing on your phone:
1. Transfer the APK to your phone
2. Open it → tap "Install" 
3. If blocked, go to Settings → allow "Install from unknown sources" for your browser/file manager
4. Open the app → go to Settings → set your surgery date
5. Grant notification and alarm permissions when prompted
