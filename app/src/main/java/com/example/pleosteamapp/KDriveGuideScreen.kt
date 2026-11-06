package com.example.pleosteamapp   // ⚠️ MainActivity.kt 첫 줄의 package 줄로 바꾸세요

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * K-DRIVE GUIDE 메인 화면 (Jetpack Compose)
 *
 * ┌───────────────────────────────────────────┐
 * │ K-DRIVE GUIDE                  AUTO [스위치] │  ← 자동 안내 켜기/끄기
 * │ Next: 남산1호터널톨게이트 · 2.3 km              │  ← 내비에서 받은 다음 시설·거리
 * │ ┌───────────────────────────────────────┐ │
 * │ │ DEMO                                  │ │
 * │ │ Toll Gate Ahead · 2 km                │ │  ← 안내 카드 (크게)
 * │ │ • Hi-Pass lanes are only for ...      │ │
 * │ └───────────────────────────────────────┘ │
 * │ [Demo: Toll Gate]  [Replay]  [Stop]       │
 * └───────────────────────────────────────────┘
 *
 * 프로젝트가 Compose가 아니라 XML 화면이면 알려주세요. XML 버전으로 바꿔 드립니다.
 */

data class GuideUiState(
    val autoMode: Boolean = false,
    val status: GuideSignal? = null,
    val current: Announcement? = null
)

private val Bg = Color(0xFF0E1116)
private val CardBg = Color(0xFF1A2230)
private val Sub = Color(0xFF9AA4B2)
private val DemoColor = Color(0xFFFFB020)

@Composable
fun KDriveGuideScreen(
    state: GuideUiState,
    onAutoModeChange: (Boolean) -> Unit,
    onDemoToll: () -> Unit,
    onReplay: () -> Unit,
    onStop: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .padding(24.dp)
    ) {
        // 상단: 앱 이름 + 자동 안내 스위치
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("K-DRIVE GUIDE", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(if (state.autoMode) "AUTO" else "MANUAL", color = Sub, fontSize = 18.sp)
            Spacer(Modifier.width(12.dp))
            Switch(checked = state.autoMode, onCheckedChange = onAutoModeChange)
        }

        Spacer(Modifier.height(12.dp))

        // 상태줄: 다음 시설 + 남은 거리 (AUTO 모드에서 이 숫자가 줄어드는지로 distance 검증 가능)
        val s = state.status
        Text(
            text = if (s == null) "No upcoming road facility" else "Next: ${s.name} · ${formatDistance(s.distanceM)}",
            color = Sub,
            fontSize = 20.sp
        )

        Spacer(Modifier.height(20.dp))

        // 안내 카드
        val a = state.current
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(Modifier.padding(28.dp)) {
                if (a == null) {
                    Text("Drive safely. Guidance will appear here.", color = Color.White, fontSize = 26.sp)
                } else {
                    if (a.isDemo) {
                        Text("DEMO", color = DemoColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(a.title, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(a.placeName, color = Sub, fontSize = 18.sp)
                    Spacer(Modifier.height(20.dp))
                    a.body.forEach { line ->
                        Text("• $line", color = Color.White, fontSize = 24.sp, lineHeight = 32.sp)
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // 하단 버튼 (차량 화면이라 크게)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = onDemoToll,
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
            ) { Text("Demo: Toll Gate", fontSize = 20.sp) }

            OutlinedButton(
                onClick = onReplay,
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
            ) { Text("Replay", fontSize = 20.sp) }

            OutlinedButton(
                onClick = onStop,
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
            ) { Text("Stop", fontSize = 20.sp) }
        }
    }
}

private fun formatDistance(m: Double): String =
    if (m >= 1000) String.format("%.1f km", m / 1000) else "${m.toInt()} m"
