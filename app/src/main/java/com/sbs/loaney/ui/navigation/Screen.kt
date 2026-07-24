package com.sbs.loaney.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Auth : Screen("auth?signup={signup}") {
        fun createRoute(signUp: Boolean = false) = "auth?signup=$signUp"
    }
    object Home : Screen("home")
    object ManageLoans : Screen("manage_loans?type={type}") {
        fun createRoute(type: String? = null) = if (type != null) "manage_loans?type=$type" else "manage_loans"
    }
    object AddLoan : Screen("add_loan?type={type}&amount={amount}&name={name}&purpose={purpose}&notes={notes}&witness={witness}&rel={rel}&guided={guided}") {
        fun createRoute(
            type: String,
            amount: String? = null,
            name: String? = null,
            purpose: String? = null,
            notes: String? = null,
            witness: String? = null,
            rel: String? = null,
            guided: Boolean = false
        ) = "add_loan?type=$type" +
                (if (amount != null) "&amount=${android.net.Uri.encode(amount)}" else "") +
                (if (name != null) "&name=${android.net.Uri.encode(name)}" else "") +
                (if (purpose != null) "&purpose=${android.net.Uri.encode(purpose)}" else "") +
                (if (notes != null) "&notes=${android.net.Uri.encode(notes)}" else "") +
                (if (witness != null) "&witness=${android.net.Uri.encode(witness)}" else "") +
                (if (rel != null) "&rel=${android.net.Uri.encode(rel)}" else "") +
                (if (guided) "&guided=true" else "")
    }
    object Settings : Screen("settings")
    object LoanDetail : Screen("loan_detail/{loanId}?remind={remind}&guided={guided}") {
        fun createRoute(loanId: Long, remind: Boolean = false, guided: Boolean = false) =
            "loan_detail/$loanId?remind=$remind&guided=$guided"
    }
    object Shop : Screen("shop")
    object History : Screen("history")
    object Notifications : Screen("notifications")
    object Emi : Screen("emi")
    object Deposit : Screen("deposit")
    object Tools : Screen("tools")
}
