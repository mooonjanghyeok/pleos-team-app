
package com.example.pleosteamapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * K-DRIVE GUIDE - PC2 HOME UI
 *
 * PC1의 SDK 및 안내 엔진과 분리된 화면.
 *
 * interactionRestricted = true:
 * 복잡한 조작 메뉴 숨김.
 *
 * interactionRestricted = false:
 * 정차용 전체 메뉴 표시.
 */

@Composable
fun KDriveHomeScreen(
    navigationStatus: String,
    autoGuideEnabled: Boolean,
    demoMode: Boolean,
    onAutoGuideChange: (Boolean) -> Unit,
    onTrafficRulesClick: () -> Unit,
    onDrivingStatusClick: () -> Unit,
    onDemoClick: () -> Unit,
    onThemeSettingsClick: () -> Unit = {},
    interactionRestricted: Boolean = true
) {

    val colors = MaterialTheme.colorScheme

    // 주행 제한 상태에서는 상세 메뉴를 표시하지 않는다.
    if (interactionRestricted) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(24.dp),

            verticalArrangement =
                Arrangement.spacedBy(20.dp)
        ) {

            Text(
                text = "K-DRIVE",
                style = MaterialTheme.typography.headlineLarge,
                color = colors.onBackground
            )

            Text(
                text = "DRIVING GUIDANCE",
                style = MaterialTheme.typography.titleLarge,
                color = colors.primary
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = colors.surface
                )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),

                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    Text(
                        text = "Current status",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = kdriveSafeMessage(navigationStatus),
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = "Detailed menus are unavailable while driving.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onBackground,
                maxLines = 2
            )
        }

        return
    }

    // 정차 중 사용할 수 있는 전체 홈 화면
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {

        val wide = maxWidth >= 690.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),

            verticalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {

            // 상단 제목과 테마 설정
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "K-DRIVE",
                        style = MaterialTheme.typography.headlineLarge,
                        color = colors.onBackground
                    )

                    Text(
                        text = "DRIVING GUIDE",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primary
                    )
                }

                KDriveActionButton(
                    label = "Theme",
                    onClick = onThemeSettingsClick,
                    filled = false
                )
            }

            if (wide) {

                // 차량 가로 화면
                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(14.dp)
                ) {

                    KDriveHomeStatusCard(
                        navigationStatus = navigationStatus,
                        autoGuideEnabled = autoGuideEnabled,
                        demoMode = demoMode,
                        onAutoGuideChange = onAutoGuideChange,
                        modifier = Modifier.weight(1.1f)
                    )

                    Column(
                        modifier = Modifier.weight(0.9f),
                        verticalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {

                        KDriveHomeMenu(
                            number = "01",
                            title = "Traffic Rules",
                            subtitle = "Learn before driving",
                            onClick = onTrafficRulesClick
                        )

                        KDriveHomeMenu(
                            number = "02",
                            title = "Driving Status",
                            subtitle = "Current guidance",
                            onClick = onDrivingStatusClick
                        )

                        KDriveHomeMenu(
                            number = "03",
                            title = "Demo Mode",
                            subtitle = "Simulated guidance",
                            onClick = onDemoClick
                        )
                    }
                }

            } else {

                // 좁은 화면
                KDriveHomeStatusCard(
                    navigationStatus = navigationStatus,
                    autoGuideEnabled = autoGuideEnabled,
                    demoMode = demoMode,
                    onAutoGuideChange = onAutoGuideChange
                )

                KDriveHomeMenu(
                    number = "01",
                    title = "Traffic Rules",
                    subtitle = "Learn before driving",
                    onClick = onTrafficRulesClick
                )

                KDriveHomeMenu(
                    number = "02",
                    title = "Driving Status",
                    subtitle = "Current guidance",
                    onClick = onDrivingStatusClick
                )

                KDriveHomeMenu(
                    number = "03",
                    title = "Demo Mode",
                    subtitle = "Simulated guidance",
                    onClick = onDemoClick
                )
            }
        }
    }
}

// -------------------------------------------
// 자동 안내 상태 카드
// -------------------------------------------

@Composable
private fun KDriveHomeStatusCard(
    navigationStatus: String,
    autoGuideEnabled: Boolean,
    demoMode: Boolean,
    onAutoGuideChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {

    val colors = MaterialTheme.colorScheme

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),

            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {

            Text(
                text = "GUIDANCE CENTER",
                style = MaterialTheme.typography.labelLarge,
                color = colors.primary
            )

            Text(
                text = "Ready for your journey",
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "Navigation",
                style = MaterialTheme.typography.labelMedium
            )

            Text(
                text = kdriveSafeMessage(navigationStatus),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Automatic guide",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = if (autoGuideEnabled) "ON" else "OFF",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Switch(
                    checked = autoGuideEnabled,
                    onCheckedChange = onAutoGuideChange
                )
            }

            Text(
                text = if (demoMode) {
                    "DEMO - SIMULATED DATA"
                } else {
                    "NORMAL MODE"
                },
                color = colors.primary,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

// -------------------------------------------
// 기능 선택 카드
// -------------------------------------------

@Composable
private fun KDriveHomeMenu(
    number: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {

    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 110.dp)
            .clickable(onClick = onClick),

        shape = RoundedCornerShape(20.dp),

        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),

            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = number,
                style = MaterialTheme.typography.titleLarge,
                color = colors.primary
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(5.dp)
            ) {

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "›",
                style = MaterialTheme.typography.headlineLarge,
                color = colors.primary
            )
        }
    }
}

// -------------------------------------------
// 정차 상태 Preview
// -------------------------------------------

@Preview(
    name = "Home - Parked",
    widthDp = 1000,
    heightDp = 600,
    showBackground = true
)
@Composable
private fun KDriveHomeParkedPreview() {

    KDriveTheme(KDRIVE_THEME_PRESETS[0]) {

        KDriveHomeScreen(
            navigationStatus = "Waiting for navigation",
            autoGuideEnabled = false,
            demoMode = false,
            onAutoGuideChange = {},
            onTrafficRulesClick = {},
            onDrivingStatusClick = {},
            onDemoClick = {},
            onThemeSettingsClick = {},
            interactionRestricted = false
        )
    }
}

// -------------------------------------------
// 주행 제한 상태 Preview
// -------------------------------------------

@Preview(
    name = "Home - Restricted",
    widthDp = 1000,
    heightDp = 600,
    showBackground = true
)
@Composable
private fun KDriveHomeDrivingPreview() {

    KDriveTheme(KDRIVE_THEME_PRESETS[0]) {

        KDriveHomeScreen(
            navigationStatus = "Route guidance active",
            autoGuideEnabled = true,
            demoMode = false,
            onAutoGuideChange = {},
            onTrafficRulesClick = {},
            onDrivingStatusClick = {},
            onDemoClick = {},
            onThemeSettingsClick = {},
            interactionRestricted = true
        )
    }
}
