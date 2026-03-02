package com.sbs.loaney.ui.screens

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.sbs.loaney.ui.screens.CustomLightTextField
import com.sbs.loaney.ui.theme.*
import com.sbs.loaney.ui.viewmodel.SettingsViewModel
import androidx.compose.ui.res.stringResource
import com.sbs.loaney.R
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Dialog & Sheet States
    var showNameDialog by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var showCurrencySheet by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(id = R.string.settings), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (isLoading) {
            com.sbs.loaney.ui.components.AnimatedLoadingScreen(modifier = Modifier.padding(paddingValues))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // -- GROUP 1: PROFILE --
            SettingsGroup(title = stringResource(id = R.string.profile)) {
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = stringResource(id = R.string.display_name),
                    subtitle = uiState.userName,
                    onClick = { showNameDialog = true }
                )
            }

            // -- GROUP 2: PREFERENCES --
            SettingsGroup(title = stringResource(id = R.string.preferences)) {
                val themeSubtitle = when (uiState.themeMode) {
                    0 -> stringResource(id = R.string.theme_system)
                    1 -> stringResource(id = R.string.theme_light)
                    2 -> stringResource(id = R.string.theme_dark)
                    3 -> "Colorful"
                    else -> stringResource(id = R.string.theme_system)
                }
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = stringResource(id = R.string.theme),
                    subtitle = themeSubtitle,
                    onClick = { showThemeSheet = true }
                )

                // Accent color picker (only when Colorful is active)
                if (uiState.themeMode == 3) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            "Accent Color",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            colorfulAccents.forEachIndexed { index, accent ->
                                val isSelected = uiState.accentColor == index
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(accent.primary)
                                        .then(
                                            if (isSelected) Modifier.border(3.dp, Color.White, CircleShape)
                                            else Modifier.border(1.dp, accent.primary.copy(alpha = 0.5f), CircleShape)
                                        )
                                        .clickable { viewModel.setAccentColor(index) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                SettingsItem(
                    icon = Icons.Default.AttachMoney,
                    title = stringResource(id = R.string.currency),
                    subtitle = uiState.currencySymbol,
                    onClick = { showCurrencySheet = true }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = stringResource(id = R.string.language),
                    subtitle = if (uiState.appLanguage == "bn") stringResource(id = R.string.bangla) else stringResource(id = R.string.english),
                    onClick = { showLanguageSheet = true }
                )
            }

            // -- GROUP 3: NOTIFICATIONS --
            SettingsGroup(title = stringResource(id = R.string.system_group)) {
                SettingsToggleItem(
                    icon = Icons.Default.Notifications,
                    title = stringResource(id = R.string.push_notifications),
                    isChecked = uiState.notificationsEnabled,
                    onToggle = { viewModel.setNotificationsEnabled(it) }
                )
            }
        }
        }
    }

    // --- MODALS & DIALOGS ---

    if (showNameDialog) {
        var tempName by remember { mutableStateOf(uiState.userName) }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text(stringResource(id = R.string.edit_profile_name)) },
            text = {
                CustomLightTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = stringResource(id = R.string.display_name),
                    leadingIcon = Icons.Default.Person
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (tempName.isNotBlank()) viewModel.setUserName(tempName)
                    showNameDialog = false
                }) { Text(stringResource(id = R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text(stringResource(id = R.string.cancel)) }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showThemeSheet) {
        ModalBottomSheet(onDismissRequest = { showThemeSheet = false }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
                Text(stringResource(id = R.string.select_theme), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                ThemeOption(stringResource(id = R.string.theme_system), uiState.themeMode == 0) { viewModel.setThemeMode(0); showThemeSheet = false }
                ThemeOption(stringResource(id = R.string.theme_light), uiState.themeMode == 1) { viewModel.setThemeMode(1); showThemeSheet = false }
                ThemeOption(stringResource(id = R.string.theme_dark), uiState.themeMode == 2) { viewModel.setThemeMode(2); showThemeSheet = false }
                ThemeOption("Colorful", uiState.themeMode == 3) { viewModel.setThemeMode(3); showThemeSheet = false }
            }
        }
    }

    if (showCurrencySheet) {
        val currencies = listOf(
            "৳" to stringResource(id = R.string.currency_bdt),
            "$" to stringResource(id = R.string.currency_usd),
            "€" to stringResource(id = R.string.currency_eur),
            "£" to stringResource(id = R.string.currency_gbp),
            "₹" to stringResource(id = R.string.currency_inr)
        )
        ModalBottomSheet(onDismissRequest = { showCurrencySheet = false }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
                Text(stringResource(id = R.string.select_currency), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                currencies.forEach { (symbol, name) ->
                    ThemeOption(name, uiState.currencySymbol == symbol) { 
                        viewModel.setCurrencySymbol(symbol)
                        showCurrencySheet = false 
                    }
                }
            }
        }
    }

    if (showLanguageSheet) {
        ModalBottomSheet(onDismissRequest = { showLanguageSheet = false }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
                Text(stringResource(id = R.string.app_language), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                ThemeOption(stringResource(id = R.string.english), uiState.appLanguage == "en") {
                    showLanguageSheet = false 
                    if (uiState.appLanguage != "en") {
                        coroutineScope.launch {
                            isLoading = true
                            kotlinx.coroutines.delay(800)
                            viewModel.setAppLanguage("en")
                            setAppLocale(context, "en")
                            isLoading = false
                        }
                    }
                }
                ThemeOption(stringResource(id = R.string.bangla), uiState.appLanguage == "bn") {
                    showLanguageSheet = false 
                    if (uiState.appLanguage != "bn") {
                        coroutineScope.launch {
                            isLoading = true
                            kotlinx.coroutines.delay(800)
                            viewModel.setAppLanguage("bn")
                            setAppLocale(context, "bn")
                            isLoading = false
                        }
                    }
                }
            }
        }
    }
}

// --- NATIVE LOCALE HANDLER ---
private fun setAppLocale(context: Context, languageTag: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(languageTag)
    } else {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
    }
}

// --- COMPONENTS ---

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp),
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground))
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SettingsToggleItem(icon: ImageVector, title: String, isChecked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isChecked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground), modifier = Modifier.weight(1f))
        Switch(
            checked = isChecked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.surface, checkedTrackColor = MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
fun ThemeOption(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
        }
    }
}
