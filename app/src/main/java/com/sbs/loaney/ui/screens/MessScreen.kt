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
import com.sbs.loaney.ui.theme.PrimaryLime
import com.sbs.loaney.ui.theme.SecondaryOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessScreen(
    onNavigateToTracker: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Mess (Tools Hub)", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = Color.White
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
                text = "Available Tools",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            ToolCard(
                title = "Single-Screen Loan Tracker",
                description = "Deep dive into a single loan's history and status.",
                icon = Icons.Default.Info,
                backgroundColor = PrimaryLime,
                onClick = onNavigateToTracker
            )
            
            ToolCard(
                title = "Settings",
                description = "Configure app preferences.",
                icon = Icons.Default.Settings,
                backgroundColor = SecondaryOrange,
                onClick = {}
            )

            ToolCard(
                title = "Coming Soon...",
                description = "More financial tools are under development.",
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
        shape = RoundedCornerShape(24.dp),
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
