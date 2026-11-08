
package com.example.pleosteamapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment

import androidx.compose.ui.tooling.preview.Preview

/**
 * K-DRIVE GUIDE
 * PC2 담당 차량용 홈 화면
 *
 * NaviHelper, GPS, TTS 및 안내 엔진을 직접 실행하지 않는다.
 * PC1이 실제 상태와 콜백을 연결하도록 설계한다.
 */
@Composable
fun KDriveHomeScreen(
    navigationStatus: String,
    autoGuideEnabled: Boolean,
    demoMode: Boolean,
    onAutoGuideChange: (Boolean) -> Unit,
    onTrafficRulesClick: () -> Unit,
    onDrivingStatusClick: () -> Unit,
    onDemoClick: () -> Unit
) {

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        val isWideScreen = maxWidth >= 700.dp

        if (isWideScreen) {

            // 차량용 가로 화면
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement =
                    Arrangement.spacedBy(20.dp)
            ) {

                HomeInformation(
                    navigationStatus = navigationStatus,
                    autoGuideEnabled = autoGuideEnabled,
                    demoMode = demoMode,
                    onAutoGuideChange = onAutoGuideChange,
                    modifier = Modifier.weight(1f)
                )

                HomeActions(
                    onTrafficRulesClick = onTrafficRulesClick,
                    onDrivingStatusClick = onDrivingStatusClick,
                    onDemoClick = onDemoClick,
                    modifier = Modifier.weight(1f)
                )
            }

        } else {

            // 세로 화면 또는 작은 화면
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),

                verticalArrangement =
                    Arrangement.spacedBy(16.dp)
            ) {

                HomeInformation(
                    navigationStatus = navigationStatus,
                    autoGuideEnabled = autoGuideEnabled,
                    demoMode = demoMode,
                    onAutoGuideChange = onAutoGuideChange
                )

                HomeActions(
                    onTrafficRulesClick = onTrafficRulesClick,
                    onDrivingStatusClick = onDrivingStatusClick,
                    onDemoClick = onDemoClick
                )
            }
        }
    }
}

@Composable
private fun HomeInformation(
    navigationStatus: String,
    autoGuideEnabled: Boolean,
    demoMode: Boolean,
    onAutoGuideChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "K-DRIVE GUIDE",
            style = MaterialTheme.typography.headlineLarge
        )

        Text(
            text = "Your Driving Companion in Korea",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement =
                    Arrangement.spacedBy(14.dp)
            ) {

                Text(
                    text = "AUTOMATIC GUIDE",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "Voice Guidance",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = if (autoGuideEnabled) {
                                "ON"
                            } else {
                                "OFF"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Switch(
                        checked = autoGuideEnabled,
                        onCheckedChange = onAutoGuideChange
                    )
                }

                Text(
                    text = "Navigation: $navigationStatus",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = if (demoMode) {
                        "MODE: DEMO"
                    } else {
                        "MODE: NORMAL"
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun HomeActions(
    onTrafficRulesClick: () -> Unit,
    onDrivingStatusClick: () -> Unit,
    onDemoClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "Choose a Feature",
            style = MaterialTheme.typography.titleLarge
        )

        HomeActionCard(
            title = "Traffic Rules",
            description =
                "Roundabouts, right turns, " +
                        "left turns and other Korean driving rules.",
            onClick = onTrafficRulesClick
        )

        HomeActionCard(
            title = "Driving Status",
            description =
                "View navigation status " +
                        "and current guidance information.",
            onClick = onDrivingStatusClick
        )

        HomeActionCard(
            title = "Demo Mode",
            description =
                "Preview driving guidance " +
                        "without a live navigation route.",
            onClick = onDemoClick
        )
    }
}

@Composable
private fun HomeActionCard(
    title: String,
    description: String,
    onClick: () -> Unit
) {

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 독립적인 Compose 미리보기
 *
 * 실제 차량 SDK 연결 없이 화면을 확인하기 위한 용도.
 */
@Preview(
    name = "K-DRIVE Vehicle Home",
    showBackground = true,
    widthDp = 900,
    heightDp = 520
)
@Composable
private fun KDriveHomeScreenPreview() {

    var autoEnabled by remember {
        mutableStateOf(false)
    }

    MaterialTheme {

        KDriveHomeScreen(
            navigationStatus = "Waiting for navigation",
            autoGuideEnabled = autoEnabled,
            demoMode = false,

            onAutoGuideChange = {
                autoEnabled = it
            },

            // 화면 이동은 추후 PC1 통합
            onTrafficRulesClick = {},
            onDrivingStatusClick = {},
            onDemoClick = {}
        )
    }
}
