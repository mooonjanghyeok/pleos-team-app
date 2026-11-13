# K-DRIVE GUIDE

K-DRIVE GUIDE is a multilingual driving guide
for international drivers visiting South Korea.

## Main Features

- Driving rule comparisons for six countries
- Eight comparison topics
- Eleven traffic-rule scenarios
- Korean, English, French and Spanish support
- Multilingual speech guidance
- Accident and emergency assistance
- Driving-state safety restrictions

## Development Environment

- Kotlin
- Jetpack Compose
- Android Automotive OS
- Pleos Connect
- Package: com.example.pleosteamapp

## Installation

1. Launch the Pleos Connect emulator.
2. Check the current Android user ID.
3. Install the supplied test APK for that user.
4. Launch KDriveSituationActivity.
5. Set the vehicle to Park for detailed learning.

Example commands:

adb -s emulator-5554 shell am get-current-user

adb -s emulator-5554 install -r --user 10 KDRIVE_GUIDE_Release_Test.apk

adb -s emulator-5554 shell am start --user 10 -n com.example.pleosteamapp/.KDriveSituationActivity

Replace user 10 with the actual emulator user.

## Verification

Tested on the Pleos Connect emulator:
- Main learning screens
- Country comparison
- Multilingual speech
- Emergency assistance
- Park-to-Drive-to-Park restrictions

## Important Note

The provided APK is signed with a development
certificate for demonstration and review.
It is not a commercial distribution build.
