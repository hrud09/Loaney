package com.sbs.loaney.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sbs.loaney.ui.theme.*
import androidx.compose.ui.res.stringResource
import com.sbs.loaney.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessScreen(
    onNavigateToTracker: () -> Unit
) {
    Scaffold(
        containerColor = AlimCream,
        topBar = {
            Column(modifier = Modifier.background(AlimDark)) {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            stringResource(id = R.string.mess_title), 
                            fontWeight = FontWeight.SemiBold,
                            color = AlimWhite
                        ) 
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = AlimDark,
                        titleContentColor = AlimWhite
                    )
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.available_tools),
                style = MaterialTheme.typography.titleLarge,
                color = AlimDark,
                fontWeight = FontWeight.Bold
            )

            ToolCard(
                title = stringResource(id = R.string.tool_tracker_title),
                description = stringResource(id = R.string.tool_tracker_desc),
                icon = Icons.Default.Info,
                accentColor = AlimGreen,
                onClick = onNavigateToTracker
            )
            
            ToolCard(
                title = stringResource(id = R.string.tool_settings_title),
                description = stringResource(id = R.string.tool_settings_desc),
                icon = Icons.Default.Settings,
                accentColor = AlimGreen,
                onClick = {}
            )

            ToolCard(
                title = stringResource(id = R.string.tool_coming_soon_title),
                description = stringResource(id = R.string.tool_coming_soon_desc),
                icon = Icons.Default.Construction,
                accentColor = AlimDark.copy(alpha = 0.4f),
                onClick = {}
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolCard(
    title: String, 
    description: String, 
    icon: ImageVector, 
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = AlimWhite,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(accentColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = accentColor
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title, 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold,
                    color = AlimDark
                )
                Text(
                    text = description, 
                    style = MaterialTheme.typography.bodyMedium,
                    color = AlimDark.copy(alpha = 0.6f)
                )
            }
        }
    }
}
