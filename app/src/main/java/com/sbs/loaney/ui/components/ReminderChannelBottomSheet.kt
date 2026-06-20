package com.sbs.loaney.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbs.loaney.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderChannelBottomSheet(
    onDismiss: () -> Unit,
    onChannelSelected: (ReminderChannel) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Send Reminder Via",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Choose a communication channel to notify the recipient.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            val channels = listOf(
                ReminderChannel.WhatsApp,
                ReminderChannel.Messenger,
                ReminderChannel.SMS,
                ReminderChannel.Email,
                ReminderChannel.Other
            )
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(channels) { channel ->
                    ChannelItem(
                        channel = channel,
                        onClick = {
                            onChannelSelected(channel)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChannelItem(
    channel: ReminderChannel,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(channel.color.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = channel.icon,
                contentDescription = channel.label,
                tint = channel.color,
                modifier = Modifier.size(32.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Text(
            text = channel.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}

sealed class ReminderChannel(
    val label: String,
    val icon: ImageVector,
    val color: Color
) {
    object WhatsApp : ReminderChannel("WhatsApp", Icons.Default.Chat, Color(0xFF25D366))
    object Messenger : ReminderChannel("Messenger", Icons.Default.Send, Color(0xFF0084FF))
    object SMS : ReminderChannel("SMS", Icons.Default.Message, Color(0xFF4285F4))
    object Email : ReminderChannel("Email", Icons.Default.Email, Color(0xFFEA4335))
    object Other : ReminderChannel("Other", Icons.Default.Share, Color(0xFF673AB7))
}
