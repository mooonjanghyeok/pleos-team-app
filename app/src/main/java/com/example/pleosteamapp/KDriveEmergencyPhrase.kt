package com.example.pleosteamapp

enum class KDriveEmergencyPhrase(val koreanText: String) {
    ACCIDENT("교통사고가 났습니다. 저는 한국어를 잘 못합니다. 도와주세요."),
    INJURY("다친 사람이 있습니다. 119에 신고해 주세요."),
    POLICE("경찰의 도움이 필요합니다. 112에 신고해 주세요."),
    INTERPRETER("한국어를 잘 못합니다. 통역을 도와주세요.")
}

enum class KDriveEmergencyVoiceResult {
    REQUESTED,
    PARKED_REQUIRED,
    UNAVAILABLE,
    BUSY,
    NAVIGATION_PRIORITY,
    FAILED
}
