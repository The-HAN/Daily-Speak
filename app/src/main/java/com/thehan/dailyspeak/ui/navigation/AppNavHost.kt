package com.thehan.dailyspeak.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.thehan.dailyspeak.ui.feature.feedback.FeedbackRoute
import com.thehan.dailyspeak.ui.feature.profile.ProfileRoute
import com.thehan.dailyspeak.ui.feature.review.ReviewRoute
import com.thehan.dailyspeak.ui.feature.today.TodayRoute

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.TODAY.route,
        modifier = modifier,
    ) {
        composable(TopLevelDestination.TODAY.route) { entry ->
            val nextQuestionRequested by entry.savedStateHandle
                .getStateFlow(AppDestination.NEXT_QUESTION_KEY, false)
                .collectAsStateWithLifecycle()

            TodayRoute(
                onOpenFeedback = { attemptId ->
                    navController.navigate(AppDestination.feedback(attemptId))
                },
                nextQuestionRequested = nextQuestionRequested,
                onNextQuestionRequestConsumed = {
                    entry.savedStateHandle[AppDestination.NEXT_QUESTION_KEY] = false
                },
            )
        }
        composable(TopLevelDestination.REVIEW.route) {
            ReviewRoute(
                onOpenFeedback = { attemptId ->
                    navController.navigate(AppDestination.feedback(attemptId))
                },
            )
        }
        composable(TopLevelDestination.PROFILE.route) {
            ProfileRoute()
        }
        composable(
            route = AppDestination.FEEDBACK_ROUTE,
            arguments = listOf(
                navArgument(AppDestination.ATTEMPT_ID_ARG) {
                    type = NavType.LongType
                },
            ),
        ) {
            FeedbackRoute(
                onBack = { navController.popBackStack() },
                onRetryRecording = { navController.popBackStack() },
                onNextQuestion = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(AppDestination.NEXT_QUESTION_KEY, true)
                    navController.popBackStack()
                },
            )
        }
    }
}