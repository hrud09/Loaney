package com.sbs.loaney.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.unit.dp
import com.sbs.loaney.MainActivity
import com.sbs.loaney.data.local.AppDatabase
import com.sbs.loaney.data.model.LoanStatus
import com.sbs.loaney.data.model.LoanType
import com.sbs.loaney.data.repository.SettingsRepository
import com.sbs.loaney.data.repository.dataStore
import kotlinx.coroutines.flow.first
import java.util.Locale

/** What the user chose in the configuration screen when placing this widget. */
enum class WidgetDisplayMode { BOTH, GIVEN, TAKEN }

object LoaneyWidgetPrefs {
    val DISPLAY_MODE = stringPreferencesKey("display_mode")
    val HIDE_AMOUNTS = booleanPreferencesKey("hide_amounts")
    val SHOW_ACTIONS = booleanPreferencesKey("show_actions")
}

private val AlimGreen = Color(0xFF00A86B)
private val AlimWhite = Color(0xFFFFFFFF)
private val CoralRose = Color(0xFFFB7185)

/** Totals, computed the same way HomeViewModel.calculateSummary does. */
private data class WidgetTotals(val lent: Double, val borrowed: Double, val currency: String)

class LoaneyWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val totals = loadTotals(context)

        provideContent {
            val prefs = currentState<Preferences>()
            val mode = prefs[LoaneyWidgetPrefs.DISPLAY_MODE]
                ?.let { runCatching { WidgetDisplayMode.valueOf(it) }.getOrNull() }
                ?: WidgetDisplayMode.BOTH
            val hideAmounts = prefs[LoaneyWidgetPrefs.HIDE_AMOUNTS] ?: false
            val showActions = prefs[LoaneyWidgetPrefs.SHOW_ACTIONS] ?: true

            GlanceTheme {
                WidgetBody(totals, mode, hideAmounts, showActions)
            }
        }
    }

    private suspend fun loadTotals(context: Context): WidgetTotals {
        val currency = runCatching {
            SettingsRepository(context.dataStore).currencySymbolFlow.first()
        }.getOrDefault("৳")

        val loans = runCatching {
            AppDatabase.getDatabase(context).loanDao().getAllLoansOnce()
        }.getOrDefault(emptyList())

        var lent = 0.0
        var borrowed = 0.0

        loans.forEach { item ->
            val loan = item.loan
            if (loan.deleted) return@forEach
            if (loan.status == LoanStatus.FORGIVEN || loan.status == LoanStatus.FULLY_PAID) return@forEach

            val total = loan.amount + item.loanItems.sumOf { it.amount }
            val paid = item.payments.sumOf { it.amount }
            val balance = (total - paid).coerceAtLeast(0.0)

            if (loan.type == LoanType.LEND) lent += balance else borrowed += balance
        }

        return WidgetTotals(lent, borrowed, currency)
    }
}

@androidx.compose.runtime.Composable
private fun WidgetBody(
    totals: WidgetTotals,
    mode: WidgetDisplayMode,
    hideAmounts: Boolean,
    showActions: Boolean
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(AlimGreen)
            .cornerRadius(24.dp)
            .padding(16.dp)
            .clickable(actionStartActivity(openAppIntent())),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text(
            text = "Loaney",
            style = TextStyle(
                color = ColorProvider(AlimWhite),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        )

        Spacer(GlanceModifier.height(10.dp))

        Row(modifier = GlanceModifier.fillMaxWidth()) {
            if (mode != WidgetDisplayMode.TAKEN) {
                TotalBlock("Total Given", totals.lent, totals.currency, hideAmounts, AlimWhite)
            }
            if (mode == WidgetDisplayMode.BOTH) {
                Spacer(GlanceModifier.width(20.dp))
            }
            if (mode != WidgetDisplayMode.GIVEN) {
                TotalBlock("Total Taken", totals.borrowed, totals.currency, hideAmounts, CoralRose)
            }
        }

        if (showActions) {
            Spacer(GlanceModifier.height(14.dp))
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                WidgetAction("+ Give", LoanType.LEND)
                Spacer(GlanceModifier.width(10.dp))
                WidgetAction("− Take", LoanType.BORROW)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun TotalBlock(
    label: String,
    amount: Double,
    currency: String,
    hideAmounts: Boolean,
    amountColor: Color
) {
    Column {
        Text(
            text = label,
            style = TextStyle(
                color = ColorProvider(AlimWhite.copy(alpha = 0.85f)),
                fontSize = 12.sp
            )
        )
        Text(
            text = if (hideAmounts) "••••" else {
                "$currency${String.format(Locale.getDefault(), "%,.0f", amount)}"
            },
            style = TextStyle(
                color = ColorProvider(amountColor),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@androidx.compose.runtime.Composable
private fun WidgetAction(label: String, type: LoanType) {
    Text(
        text = label,
        modifier = GlanceModifier
            .background(AlimWhite.copy(alpha = 0.2f))
            .cornerRadius(12.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .clickable(actionStartActivity(addLoanIntent(type))),
        style = TextStyle(
            color = ColorProvider(AlimWhite),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    )
}

private fun openAppIntent(): Intent =
    Intent().apply {
        setClassName("com.sbs.loaney", "com.sbs.loaney.MainActivity")
        action = "com.sbs.loaney.widget.OPEN_APP"
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }

/**
 * Distinct [Intent.setAction] per button on purpose: PendingIntent equality ignores extras, so
 * two intents differing only by their extra would collapse into one and both buttons would open
 * the same screen.
 */
private fun addLoanIntent(type: LoanType): Intent =
    Intent().apply {
        setClassName("com.sbs.loaney", "com.sbs.loaney.MainActivity")
        action = "com.sbs.loaney.widget.ADD_LOAN_${type.name}"
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        putExtra(MainActivity.EXTRA_ADD_LOAN_TYPE, type.name)
    }

/** Redraw every placed widget. Call after anything that changes loan balances. */
suspend fun refreshLoaneyWidgets(context: Context) {
    runCatching { LoaneyWidget().updateAll(context) }
}
