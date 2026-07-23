package com.sbs.loaney.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sbs.loaney.ui.components.ToolInfoDialog
import com.sbs.loaney.ui.components.ToolInfoType
import com.sbs.loaney.ui.theme.*

private data class Tool(
    val type: ToolInfoType,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val accent: Color,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEmi: () -> Unit,
    onNavigateToDeposit: () -> Unit
) {
    var activeInfoTool by remember { mutableStateOf<ToolInfoType?>(null) }

    val tools = listOf(
        Tool(
            type = ToolInfoType.EMI_HUB,
            icon = Icons.Default.Calculate,
            title = "EMI Hub",
            subtitle = "Plan instalments and track what you owe",
            accent = CyberIndigo,
            onClick = onNavigateToEmi
        ),
        Tool(
            type = ToolInfoType.DPS_FDR,
            icon = Icons.Default.Savings,
            title = "DPS & FDR",
            subtitle = "Project maturity value and track deposits",
            accent = AlimGreen,
            onClick = onNavigateToDeposit
        )
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Tools",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AlimWhite
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AlimWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AlimDark,
                    titleContentColor = AlimWhite
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            tools.forEach { tool ->
                ToolCard(
                    tool = tool,
                    onInfoClick = { activeInfoTool = tool.type }
                )
            }
        }
    }

    if (activeInfoTool != null) {
        ToolInfoDialog(
            toolType = activeInfoTool!!,
            onDismiss = { activeInfoTool = null }
        )
    }
}

@Composable
private fun ToolCard(
    tool: Tool,
    onInfoClick: () -> Unit
) {
    Card(
        onClick = tool.onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = tool.accent
            ) {
                Icon(
                    tool.icon,
                    contentDescription = null,
                    tint = AlimWhite,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(28.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tool.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    tool.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onInfoClick) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Tool Info",
                    tint = tool.accent
                )
            }
        }
    }
}

