package com.sbs.loaney.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object ManageLoans : Screen("manage_loans")
    object AddLoan : Screen("add_loan?type={type}") {
        fun createRoute(type: String) = "add_loan?type=$type"
    }
    object Settings : Screen("settings")
    object LoanDetail : Screen("loan_detail/{loanId}") {
        fun createRoute(loanId: Long) = "loan_detail/$loanId"
    }
}
