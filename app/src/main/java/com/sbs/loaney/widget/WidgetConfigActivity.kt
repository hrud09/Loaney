package com.sbs.loaney.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.MutablePreferences
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import com.sbs.loaney.ui.theme.AlimGreen
import com.sbs.loaney.ui.theme.AlimWhite
import com.sbs.loaney.ui.theme.CoralRose
import com.sbs.loaney.ui.theme.LoaneyTheme
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Shown by the launcher when the user drops the widget onto their home screen. Must call
 * setResult(RESULT_OK) with the widget id, otherwise the system silently discards the widget.
 */
class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // Back-out must leave no widget behind.
        setResult(RESULT_CANCELED, resultIntent())

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            LoaneyTheme(darkTheme = isSystemInDarkTheme()) {
                WidgetConfigScreen(
                    onCancel = { finish() },
                    onSave = { mode, hideAmounts, showActions ->
                        save(mode, hideAmounts, showActions)
                    }
                )
            }
        }
    }

    private fun save(mode: WidgetDisplayMode, hideAmounts: Boolean, showActions: Boolean) {
        lifecycleScope.launch {
            // getGlanceIdBy throws if the widget vanished while this screen was open (the user
            // removed it, or the launcher restarted). Don't take the app down over it.
            runCatching {
                val glanceId = GlanceAppWidgetManager(this@WidgetConfigActivity)
                    .getGlanceIdBy(appWidgetId)

                updateAppWidgetState(
                    context = this@WidgetConfigActivity,
                    definition = PreferencesGlanceStateDefinition,
                    glanceId = glanceId
                ) { prefs ->
                    val mutable: MutablePreferences = prefs.toMutablePreferences()
                    mutable[LoaneyWidgetPrefs.DISPLAY_MODE] = mode.name
                    mutable[LoaneyWidgetPrefs.HIDE_AMOUNTS] = hideAmounts
                    mutable[LoaneyWidgetPrefs.SHOW_ACTIONS] = showActions
                    mutable
                }

                LoaneyWidget().update(this@WidgetConfigActivity, glanceId)
            }

            setResult(RESULT_OK, resultIntent())
            finish()
        }
    }

    private fun resultIntent() =
        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}

@Composable
private fun WidgetConfigScreen(
    onCancel: () -> Unit,
    onSave: (WidgetDisplayMode, Boolean, Boolean) -> Unit
) {
    var mode by remember { mutableStateOf(WidgetDisplayMode.BOTH) }
    var hideAmounts by remember { mutableStateOf(false) }
    var showActions by remember { mutableStateOf(true) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "Set up your widget",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            WidgetPreview(mode, hideAmounts, showActions)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Show",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.selectableGroup()
                ) {
                    ModeChip("Both", mode == WidgetDisplayMode.BOTH) { mode = WidgetDisplayMode.BOTH }
                    ModeChip("Given only", mode == WidgetDisplayMode.GIVEN) { mode = WidgetDisplayMode.GIVEN }
                    ModeChip("Taken only", mode == WidgetDisplayMode.TAKEN) { mode = WidgetDisplayMode.TAKEN }
                }
            }

            ConfigSwitch(
                title = "Hide amounts",
                subtitle = "Show •••• instead of balances, so people can't read your finances over your shoulder.",
                checked = hideAmounts,
                onCheckedChange = { hideAmounts = it }
            )

            ConfigSwitch(
                title = "Quick actions",
                subtitle = "Give and Take buttons that jump straight into a new loan.",
                checked = showActions,
                onCheckedChange = { showActions = it }
            )

            Spacer(Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = { onSave(mode, hideAmounts, showActions) },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AlimGreen,
                        contentColor = AlimWhite
                    )
                ) {
                    Text("Add widget", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** A live mock of the widget, so the choices above mean something before committing. */
@Composable
private fun WidgetPreview(
    mode: WidgetDisplayMode,
    hideAmounts: Boolean,
    showActions: Boolean
) {
    val sample = 5000.0
    fun money(v: Double) =
        if (hideAmounts) "••••" else "৳${String.format(Locale.getDefault(), "%,.0f", v)}"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(AlimGreen)
            .padding(16.dp)
    ) {
        Text("Loaney", style = MaterialTheme.typography.labelMedium, color = AlimWhite)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            if (mode != WidgetDisplayMode.TAKEN) {
                Column {
                    Text(
                        "Total Given",
                        style = MaterialTheme.typography.bodySmall,
                        color = AlimWhite.copy(alpha = 0.85f)
                    )
                    Text(
                        money(sample),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AlimWhite
                    )
                }
            }
            if (mode != WidgetDisplayMode.GIVEN) {
                Column {
                    Text(
                        "Total Taken",
                        style = MaterialTheme.typography.bodySmall,
                        color = AlimWhite.copy(alpha = 0.85f)
                    )
                    Text(
                        money(0.0),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CoralRose
                    )
                }
            }
        }
        if (showActions) {
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PreviewChip("+ Give")
                PreviewChip("− Take")
            }
        }
    }
}

@Composable
private fun PreviewChip(label: String) {
    Text(
        label,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AlimWhite.copy(alpha = 0.2f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = AlimWhite
    )
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AlimGreen,
            selectedLabelColor = AlimWhite
        )
    )
}

@Composable
private fun ConfigSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AlimWhite,
                checkedTrackColor = AlimGreen
            )
        )
    }
}
