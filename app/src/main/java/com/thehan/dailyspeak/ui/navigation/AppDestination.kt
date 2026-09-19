package com.thehan.dailyspeak.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.graphics.vector.ImageVector
import com.thehan.dailyspeak.R

enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    TODAY(
        route = "today",
        labelRes = R.string.nav_today,
        icon = Icons.Default.Home,
    ),
    REVIEW(
        route = "review",
        labelRes = R.string.nav_review,
        icon = Icons.Default.Refresh,
    ),
    PROFILE(
        route = "profile",
        labelRes = R.string.nav_profile,
        icon = Icons.Default.Person,
    ),
}
object AppDestination {
    const val ATTEMPT_ID_ARG = "attemptId"
    const val FEEDBACK_ROUTE = "feedback/{$ATTEMPT_ID_ARG}"
    const val NEXT_QUESTION_KEY = "next_question"

    fun feedback(attemptId: Long): String = "feedback/$attemptId"
}