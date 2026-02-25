package com.sbs.loaney.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sbs.loaney.ui.components.DonutChart
import com.sbs.loaney.ui.components.LineChart
import com.sbs.loaney.ui.theme.CoralRed
import com.sbs.loaney.ui.theme.NeonLime
import com.sbs.loaney.ui.theme.SkyBlue
import com.sbs.loaney.ui.theme.SurfaceDark
import com.sbs.loaney.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Mock data for the line chart (e.g. cumulative volume over recent days)
    // In a real app this would come from the ViewModel parsing historical payment dates
    val simulatedChartData = listOf(500f, 1200f, 1100f, 2500f, 3200f, 3000f, 4500f, 4800f, 5500f)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User section
            Text(
                text = "Mahadi", // Placeholder
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                textAlign = TextAlign.Center
            )

            // Lifetime Overview (Donut Chart)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SurfaceDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Lifetime Overview",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    DonutChart(
                        totalLent = uiState.totalLent,
                        totalBorrowed = uiState.totalBorrowed,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                    
                    // Legends
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NeonLime))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Total Lent", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CoralRed))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Total Borrowed", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // Summary Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCardDark(
                    title = "Total Lent",
                    amount = uiState.totalLent,
                    accentColor = NeonLime,
                    modifier = Modifier.weight(1f)
                )
                SummaryCardDark(
                    title = "Total Borrowed",
                    amount = uiState.totalBorrowed,
                    accentColor = SkyBlue,
                    modifier = Modifier.weight(1f)
                )
            }

            // Activity History (Line Chart)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SurfaceDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Activity Trend",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = "Last 30 Days",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    LineChart(
                        dataPoints = simulatedChartData,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(80.dp)) // Bottom nav padding
        }
    }
}
