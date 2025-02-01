package com.tachyonmusic.presentation.entry

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.tachyonmusic.presentation.home.HomeScreen
import com.tachyonmusic.presentation.library.LibraryScreen
import com.tachyonmusic.presentation.library.search.PlaybackSearchScreen
import com.tachyonmusic.presentation.profile.ProfileScreen

@Composable
fun NavigationGraph(
    navController: NavHostController,
    miniPlayerHeight: Dp,
    swipeableState: AnchoredDraggableState<SwipingStates>
) {
    AnimatedNavHost(navController, startDestination = HomeScreen.route()) {
        composable(HomeScreen.route()) {
            HomeScreen(miniPlayerHeight, swipeableState)
        }
        composable(LibraryScreen.route()) {
            LibraryScreen(swipeableState, navController)
        }
        composable(ProfileScreen.route()) {
            ProfileScreen()
        }
        composable(
            route = PlaybackSearchScreen.route(),
            arguments = PlaybackSearchScreen.arguments,

            enterTransition = {
                fadeIn() + slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Up,
                    spring(
                        stiffness = Spring.StiffnessMediumLow,
                        visibilityThreshold = IntOffset.VisibilityThreshold
                    )
                )
            },
            exitTransition = { fadeOut() + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
        ) { backStackEntry ->
            val arguments = requireNotNull(backStackEntry.arguments)
            PlaybackSearchScreen(arguments, swipeableState)
        }
    }
}