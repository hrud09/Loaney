package com.sbs.loaney.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sbs.loaney.R
import com.sbs.loaney.ui.theme.AlimDark
import com.sbs.loaney.ui.theme.AlimWhite
import java.util.*

@Composable
fun AlimHeader(
    userName: String,
    userProfilePhoto: String? = null,
    unreadNotificationsCount: Int = 0,
    onProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onPositionedNotification: (LayoutCoordinates) -> Unit = {},
    onPositionedProfile: (LayoutCoordinates) -> Unit = {}
) {
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> stringResource(id = R.string.good_morning)
        in 12..16 -> stringResource(id = R.string.good_afternoon)
        else -> stringResource(id = R.string.good_evening)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AlimDark)
            .padding(top = 0.dp, start = 20.dp, end = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = AlimWhite.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = userName,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = AlimWhite,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
            )
        }

        // Notifications Button
        Box(
            modifier = Modifier.onGloballyPositioned { onPositionedNotification(it) },
            contentAlignment = Alignment.TopEnd
        ) {
            IconButton(onClick = onNotificationsClick) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = AlimWhite
                )
            }
            if (unreadNotificationsCount > 0) {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp, end = 8.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Profile Photo
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(AlimWhite.copy(alpha = 0.15f))
                .onGloballyPositioned { onPositionedProfile(it) }
                .clickable { onProfileClick() }
        ) {
            if (userProfilePhoto != null) {
                AsyncImage(
                    model = userProfilePhoto,
                    contentDescription = "Profile Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Person, 
                    contentDescription = null, 
                    tint = AlimWhite,
                    modifier = Modifier.fillMaxSize().padding(8.dp)
                )
            }
        }
    }
}

