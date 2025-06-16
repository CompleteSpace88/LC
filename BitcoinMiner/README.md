# Bitcoin Miner Mobile App

A mobile Bitcoin mining application for Android devices.

## Features

- ⚡ Real-time mining simulation
- 💰 Balance tracking starting from 0.00 BTC
- 📊 Hash rate monitoring
- 🔄 Background mining service
- 💸 Wallet withdrawal functionality
- 📱 Optimized for mobile devices

## How to Build

1. Open project in Android Studio
2. Build -> Generate Signed Bundle/APK
3. Choose APK and follow the signing process
4. Install the generated APK on your Android device

## How to Use

1. Enter your Bitcoin wallet address
2. Tap "START MINING" to begin
3. Monitor your balance and hash rate
4. Use "WITHDRAW ALL" to transfer earnings to your wallet
5. Mining continues in background when app is minimized

## Requirements

- Android 5.0 (API level 21) or higher
- Internet connection for optimal performance
- Device storage for balance persistence

## Technical Details

- Built with native Android Java
- Uses background service for continuous mining
- Implements wake lock for uninterrupted operation
- Saves progress locally with SharedPreferences

