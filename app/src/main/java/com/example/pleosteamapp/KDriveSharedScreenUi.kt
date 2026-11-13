package com.example.pleosteamapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * K-DRIVE GUIDE
 * Shared Screen UI - Design 5/6
 */

internal object KDriveScreenStyle {
    val pageHorizontal = 24.dp
    val pageVertical = 18.dp
    val sectionGap = 14.dp
    val cardRadius = 18.dp
    val buttonRadius = 14.dp
    val touchHeight = 76.dp
    val cardPadding = 20.dp
}

@Composable
internal fun KDriveScreenHeader(
    title: String,
    subtitle: String,
    backLabel: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = KDriveScreenStyle.touchHeight),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 30.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onBackground
            )

            Text(
                text = subtitle,
                fontSize = 17.sp,
                lineHeight = 24.sp,
                color = colors.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        KDriveSecondaryAction(
            label = backLabel,
            onClick = onBack,
            modifier = Modifier.widthIn(min = 126.dp)
        )
    }
}

@Composable
internal fun KDrivePrimaryAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(
            min = KDriveScreenStyle.touchHeight
        ),
        shape = RoundedCornerShape(
            KDriveScreenStyle.buttonRadius
        ),
        contentPadding = PaddingValues(
            horizontal = 20.dp,
            vertical = 12.dp
        )
    ) {
        Text(
            text = label,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun KDriveSecondaryAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(
            min = KDriveScreenStyle.touchHeight
        ),
        shape = RoundedCornerShape(
            KDriveScreenStyle.buttonRadius
        ),
        contentPadding = PaddingValues(
            horizontal = 20.dp,
            vertical = 12.dp
        )
    ) {
        Text(
            text = label,
            fontSize = 18.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}