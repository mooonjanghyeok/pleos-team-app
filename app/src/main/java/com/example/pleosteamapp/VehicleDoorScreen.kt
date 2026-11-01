
package com.example.pleosteamapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pleosteamapp.ui.theme.PleosTeamAppTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
data class DoorUiState(
    val busy: Boolean = false,
    val canRead: Boolean = false,
    val status: String = "연결 전",
    val capability: String = "미확인",
    val doorLabel: String = "미조회",
    val rawValue: String = "—",
    val readAt: String = "—",
    val error: String = ""
)

@Composable
fun VehicleDoorScreen(
    state: DoorUiState,
    onConnect: () -> Unit,
    onRead: () -> Unit
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "우리 앱 · Vehicle SDK 연결 테스트",
                style = MaterialTheme.typography.headlineSmall
            )

            Text("조회 대상: 앞쪽 왼쪽 문 / ROW_1_LEFT")
            Text("진행 상태: ${state.status}")
            Text("SDK 지원 정보: ${state.capability}")

            Text(
                text = "마지막 조회 결과: ${state.doorLabel}",
                style = MaterialTheme.typography.headlineMedium
            )

            Text("SDK 원시값: ${state.rawValue}")
            Text("조회 시각: ${state.readAt}")

            Button(
                onClick = onConnect,
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E40AF),
                    contentColor = Color.White,
                    disabledContainerColor = Color.LightGray,
                    disabledContentColor = Color.DarkGray
                )
            ) {
                Text("① SDK 연결 · 지원 확인")
            }

            Button(
                onClick = onRead,
                enabled = state.canRead && !state.busy,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E40AF),
                    contentColor = Color.White,
                    disabledContainerColor = Color.LightGray,
                    disabledContentColor = Color.DarkGray
                )
            ) {
                Text("② 문 상태 조회")
            }

            if (state.busy) {
                Text("차량 서비스 응답을 확인하는 중입니다.")
            }

            if (state.error.isNotBlank()) {
                Text(
                    text = "확인할 내용\n${state.error}",
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text(
                "이 화면은 조회 버튼을 누를 때만 갱신됩니다. " +
                        "문을 여닫는 제어 기능이나 자동 갱신 기능은 없습니다."
            )

            Text(
                "Virtual Console에서 Front left의 Close/Open을 바꾼 뒤 " +
                        "② 문 상태 조회를 다시 눌러주세요."
            )
        }
    }
}

// PC 2의 화면 확인용입니다. 실제 차량에서 읽은 결과가 아닙니다.
@Preview(showBackground = true, widthDp = 800, heightDp = 650)
@Composable
private fun VehicleDoorScreenPreview() {
    PleosTeamAppTheme {
        VehicleDoorScreen(
            state = DoorUiState(
                canRead = true,
                status = "미리보기용 예시 데이터",
                capability = "READ_WRITE / 범위 0..1",
                doorLabel = "닫힘",
                rawValue = "0",
                readAt = "예시"
            ),
            onConnect = {},
            onRead = {}
        )
    }
}