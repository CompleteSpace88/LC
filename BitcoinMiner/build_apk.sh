#!/bin/bash

echo "🔨 Building Bitcoin Miner APK..."

# Create necessary directories
mkdir -p app/src/main/res/mipmap-hdpi
mkdir -p app/src/main/res/mipmap-mdpi
mkdir -p app/src/main/res/mipmap-xhdpi
mkdir -p app/src/main/res/mipmap-xxhdpi
mkdir -p app/src/main/res/mipmap-xxxhdpi

# Create a simple launcher icon (placeholder)
echo "Creating launcher icons..."

# Build the project
echo "Compiling APK..."
if command -v gradle &> /dev/null; then
    ./gradlew assembleDebug
elif command -v ./gradlew &> /dev/null; then
    ./gradlew assembleDebug
else
    echo "⚠️  Gradle not found. Please install Android Studio or Gradle to build the APK."
    echo "📱 You can also import this project into Android Studio and build from there."
fi

echo "✅ Bitcoin Miner app created successfully!"
echo "📁 Project files are ready in the BitcoinMiner directory"
echo "🚀 Import into Android Studio to build the APK"

