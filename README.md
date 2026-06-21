# CleanSpace

Android Storage & Cache Cleaner — Jetpack Compose + MVVM + Shizuku

## ফিচার লিস্ট

1. **Dashboard (Home Hub)** — মোট/ব্যবহৃত/খালি storage, ক্যাটাগরি অনুযায়ী Donut Chart, এবং সব টুলের grid
2. **Junk Scanner** — .tmp/.log/.bak/.old/.temp/.cache ফাইল ও খালি ফোল্ডার
3. **Large File Finder** — ৫০ MB+ ফাইল sort করে দেখায়
4. **Duplicate Finder** — একই ফাইলের নকল কপি খুঁজে বের করে (size+hash matching), ছবি হলে thumbnail দেখায়
5. **Orphaned Data Finder** — uninstall করা App-এর রেখে যাওয়া leftover folder (Shizuku লাগে)
6. **Media Cleaner** — WhatsApp/Telegram/Messenger/Facebook-এর Images/Video/Status আলাদা করে দেখায়
7. **App-wise Storage** — প্রতিটা App-এর storage ব্যবহার
8. **সব App-এর Cache এক ক্লিকে Clean** — Shizuku দিয়ে
9. **Theme** — Light / Dark / System

### Batch 1 আপডেট নোট
- Navigation নতুন করে সাজানো হয়েছে: আগে bottom-tab ছিল, এখন Home স্ক্রিন থেকে card ট্যাপ করে প্রতিটা টুলে যাওয়া যায় (টুল বেশি হয়ে যাওয়ায় bottom-tab আর কাজে দিচ্ছিল না)
- Duplicate Finder-এ ছবির thumbnail দেখানোর জন্য Coil লাইব্রেরি যোগ হয়েছে
- Orphaned Data Finder **শুধু Shizuku চালু থাকলেই** কাজ করবে — কারণ Android 11+ এ All Files Access থাকলেও `Android/data` ফোল্ডারে সরাসরি ঢোকা যায় না, এটা Google-এর ইচ্ছাকৃত সীমাবদ্ধতা
- পরের batch-এ আসবে: App Manager (batch uninstall/disable) এবং Auto-Clean Scheduler

## Termux থেকে Build করার ধাপ (তোমার established workflow)

```bash
cd ~/storage/downloads   # যেখানে ZIP নামিয়েছ
unzip CleanSpace.zip
cd CleanSpace
git init
git config --global --add safe.directory $(pwd)
git add .
git commit -m "Initial commit: CleanSpace"
git branch -M main
git remote add origin https://github.com/<তোমার-username>/cleanspace-app.git
git push -u origin main
```

GitHub Actions নিজে থেকে চলবে, **Actions** ট্যাবে গিয়ে build শেষ হলে **Artifacts** থেকে `CleanSpace-debug-apk` ডাউনলোড করো।

## App চালু করার পর Permission Setup

App প্রথমবার খুললে দুটো permission চাইবে (System Settings পেজে গিয়ে Allow করতে হবে):

1. **All Files Access** — Settings পেজ খুলে toggle অন করো
2. **Usage Access** — Settings পেজ খুলে CleanSpace-কে Allow করো

দুটো দেওয়ার পর App-এ ফিরে "Permission চেক করো" বাটনে ট্যাপ করলেই মূল App চালু হয়ে যাবে।

## Shizuku Setup — সব App-এর Cache সত্যিকারের ভাবে Clean করার জন্য

তোমার ফোন যদি **Android 11 বা তার বেশি** হয় (Realme C25Y সাধারণত Android 11/12 নিয়ে আসে), তাহলে এই পদ্ধতি **পুরোপুরি ফোনের মধ্যেই** করা যাবে — PC বা Termux কোনোটাই লাগবে না।

### ধাপ ১: Shizuku App ইনস্টল করো
Play Store থেকে "Shizuku" সার্চ করে ইনস্টল করো, অথবা:
https://shizuku.rikka.app/download/

### ধাপ ২: Developer Options চালু করো
Settings > About Phone > "Build Number" এ পরপর ৭ বার ট্যাপ করো (Developer Options চালু হবে)

### ধাপ ৩: Wireless Debugging অন করো
Settings > Additional Settings (বা Developer Options) > **Wireless debugging** চালু করো

### ধাপ ৪: Shizuku App থেকে Pair করো
Shizuku App খুলে "Wireless debugging" সেকশনে যাও > "Pair device with pairing code" ট্যাপ করো — এটা system-এর pairing screen খুলবে, সেখানের কোড মিলিয়ে Pair হয়ে যাবে।

### ধাপ ৫: Start করো
Pairing হয়ে গেলে Shizuku App-এর মূল স্ক্রিনে ফিরে **Start** বাটনে ট্যাপ করো। স্ট্যাটাস "Running" দেখালে কাজ শেষ।

### ধাপ ৬: CleanSpace-এ Permission দাও
CleanSpace App-এর **Apps** ট্যাবে গিয়ে "Permission দাও" বাটনে ট্যাপ করো, Shizuku-এর popup-এ Allow করো।

এখন "সব App-এর Cache Clean করো" বাটন সত্যিকারের ভাবে কাজ করবে।

**মনে রাখবে:** ফোন রিস্টার্ট করলে ধাপ ৫ (Shizuku App খুলে Start) আবার করতে হবে — এটাই non-root পদ্ধতির একমাত্র অসুবিধা। Root করা থাকলে এই ঝামেলা থাকতো না, কিন্তু root করা risky তাই এটা এড়িয়ে গেছি।

যদি ফোনে Android 10 বা তার কম থাকে, তাহলে wireless debugging নেই — Shizuku চালু করতে একটা PC থেকে এক-বারের জন্য ADB কমান্ড চালাতে হবে (Shizuku-এর official guide দেখো)।

## টেকনিক্যাল নোট
- Kotlin 1.9.10, Compose BOM 2023.10.01, compileSdk/targetSdk 34, minSdk 24, Java 17, Gradle 8.4
- Shizuku API ব্যবহার করা হয়েছে `dev.rikka.shizuku:api:13.1.5` — যদি Gradle এই ভার্সন খুঁজে না পায়, https://github.com/RikkaApps/Shizuku-API -এ গিয়ে latest version `app/build.gradle`-এ বসিয়ে দাও
- Android 13+ এ `pm clear --cache-only <package>` কমান্ড ব্যবহার হয় (প্রতিটা App আলাদাভাবে), তার নিচের ভার্সনে `pm trim-caches` ব্যবহার হয় (সিস্টেম পুরোটা একসাথে trim করে)
