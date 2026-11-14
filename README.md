# pleos-team-app

Hyundai Vehicle App Development Challenge - Team Project

pc2 github 연결테스트성공


<!-- KDRIVE_CONTEST_INFO_BEGIN -->

## K-DRIVE GUIDE - Contest Submission

**앱 소개:** 외국인을 위한 한국 교통규칙 비교 및 다국어 차량용 가이드 앱

### 핵심 기능
- 6개국 운전규칙 비교 및 8개 비교 주제
- 한국 교통규칙 11개 상황 안내
- 한국어, 영어, 프랑스어, 스페인어 지원
- 다국어 TTS 음성 안내
- 사고 및 긴급상황 대응
- 주행 상태에 따른 상세 학습 화면 제한

### 개발 환경
- Kotlin / Jetpack Compose
- Android Automotive OS
- Pleos Connect 에뮬레이터
- 패키지명: com.example.pleosteamapp

### 테스트 환경
- 플랫폼: Pleos Connect / Android Automotive OS
- 에뮬레이터 모델: Connect on x86_64 emulator
- 에뮬레이터 빌드: RELEASE.release_connect_v2.1.0 test-keys
- Android 버전: 14
- 화면 해상도: 2560 × 1440

### APK
- 파일 경로: apk/카더라_K-DRIVE_GUIDE_release.apk
- 빌드 유형: Release
- 서명: 개발용 테스트 인증서

### 설치 및 실행
1. Pleos Connect 에뮬레이터를 실행합니다.
2. 현재 Android 사용자 ID를 확인합니다.
3. APK를 해당 사용자에게 설치합니다.
4. 차량을 P / 0km/h로 설정합니다.
5. K-DRIVE GUIDE 앱을 실행합니다.

PowerShell 실행 예시:

    adb -s emulator-5554 shell am get-current-user
    adb -s emulator-5554 install -r --user 10 apk/카더라_K-DRIVE_GUIDE_release.apk
    adb -s emulator-5554 shell am start --user 10 -n com.example.pleosteamapp/.KDriveSituationActivity

사용자 번호 10은 예시이며 실제 사용자 번호로 변경해야 합니다.

### 테스트
Pleos Connect 에뮬레이터에서 주요 화면, 다국어 안내,
긴급상황 안내 및 P-D-P 주행 제한 동작을 검증했습니다.

이 APK는 공모전 심사용 테스트 버전이며,
정식 상용 배포용 서명이 아닙니다.

<!-- KDRIVE_CONTEST_INFO_END -->
