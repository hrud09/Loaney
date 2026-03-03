package com.sbs.loaney.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.mess_title), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
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
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )

            ToolCard(
                title = stringResource(id = R.string.tool_tracker_title),
                description = stringResource(id = R.string.tool_tracker_desc),
                icon = Icons.Default.Info,
                backgroundColor = CoralPink,
                onClick = onNavigateToTracker
            )
            
            ToolCard(
                title = stringResource(id = R.string.tool_settings_title),
                description = stringResource(id = R.string.tool_settings_desc),
                icon = Icons.Default.Settings,
                backgroundColor = SecondaryOrange,
                onClick = {}
            )

            ToolCard(
                title = stringResource(id = R.string.tool_coming_soon_title),
                description = stringResource(id = R.string.tool_coming_soon_desc),
                icon = Icons.Default.Construction,
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
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
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = Color.Black
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title, 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = description, 
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black.copy(alpha = 0.7f)
                )
            }
        }
    }
}
