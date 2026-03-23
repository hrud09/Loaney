package com.sbs.loaney.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object ManageLoans : Screen("manage_loans?type={type}") {
        fun createRoute(type: String? = null) = if (type != null) "manage_loans?type=$type" else "manage_loans"
    }
    object AddLoan : Screen("add_loan?type={type}") {
        fun createRoute(type: String) = "add_loan?type=$type"
    }
    object Settings : Screen("settings")
    object LoanDetail : Screen("loan_detail/{loanId}") {
        fun createRoute(loanId: Long) = "loan_detail/$loanId"
    }
    object Shop : Screen("shop")
    object History : Screen("history")
}
