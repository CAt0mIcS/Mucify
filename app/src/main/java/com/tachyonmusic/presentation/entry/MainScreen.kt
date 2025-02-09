package com.tachyonmusic.presentation.entry

import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.MotionLayout
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.tachyonmusic.app.R
import com.tachyonmusic.database.data.data_source.Database
import com.tachyonmusic.presentation.core_components.UriPermissionDialog
import com.tachyonmusic.presentation.entry.component.LoadingBox
import com.tachyonmusic.presentation.home.HomeScreen
import com.tachyonmusic.presentation.library.LibraryScreen
import com.tachyonmusic.presentation.onboarding.OnboardingScreen
import com.tachyonmusic.presentation.player.PlayerLayout
import com.tachyonmusic.presentation.profile.component.OpenDocumentDialog
import com.tachyonmusic.presentation.theme.TachyonTheme
import com.tachyonmusic.presentation.theme.Theme
import com.tachyonmusic.util.EventSeverity
import kotlinx.coroutines.launch

enum class SwipingStates {
    EXPANDED,
    COLLAPSED
}

@Composable
fun MainScreen(
    updateReadyToInstall: Boolean,
    updateSnackbarResult: (Boolean) -> Unit,
    miniplayerSnapPosition: SwipingStates?,
    onMiniplayerSnapCompleted: () -> Unit,
    onboardingCompleted: Boolean,
    viewModel: MainViewModel = hiltViewModel()
) {
    val settings by viewModel.composeSettings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    TachyonTheme(settings = settings) {
        Surface {
            /***************************************************************************************
             * Show Messages from [EventChannel]
             **************************************************************************************/
            val snackbarHostState = remember { SnackbarHostState() }

            val context = LocalContext.current
            LaunchedEffect(key1 = true) {
                viewModel.eventChannel.collect { event ->
                    if (event.severity > EventSeverity.Debug) {
                        snackbarHostState.showSnackbar(
                            event.message.asString(context),
                            withDismissAction = true,
                            duration = when (event.severity) {
                                EventSeverity.Error, EventSeverity.Fatal -> SnackbarDuration.Long
                                else -> SnackbarDuration.Short
                            }
                        )
                    }
                }
            }

            if (isLoading) {
                LoadingBox()
            }

            var startDestination by rememberSaveable { mutableStateOf(HomeScreen.route()) }
            if (!onboardingCompleted) {
                OnboardingScreen(
                    onCompleted = { startDestination = LibraryScreen.route() }
                )
                return@Surface
            }

            if (updateReadyToInstall) {
                val appRestartQuestionStr = stringResource(R.string.request_app_restart_for_update)
                val restartStr = stringResource(R.string.restart)
                LaunchedEffect(true) {
                    val result = snackbarHostState.showSnackbar(
                        appRestartQuestionStr,
                        restartStr,
                        true,
                        SnackbarDuration.Indefinite
                    )

                    updateSnackbarResult(result == SnackbarResult.ActionPerformed)
                }
            }

            var miniPlayerHeight by remember { mutableStateOf(0.dp) }
            val navController = rememberAnimatedNavController()
            val localDensity = LocalDensity.current

            val decayAnimationSpec = rememberSplineBasedDecay<Float>()
            val anchoredDraggableState = remember {
                AnchoredDraggableState(
                    initialValue = SwipingStates.COLLAPSED,
                    anchors = DraggableAnchors {},
                    positionalThreshold = { distance: Float -> distance * 0.5f },
                    velocityThreshold = { with(localDensity) { 100.dp.toPx() } },
                    snapAnimationSpec = tween(),
                    decayAnimationSpec = decayAnimationSpec
                )
            }

            // Allow ActivityMain to override player position (extended, collapsed)
            LaunchedEffect(miniplayerSnapPosition) {
                miniplayerSnapPosition?.let {
                    anchoredDraggableState.snapTo(it)
                    onMiniplayerSnapCompleted()
                }
            }


            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    BottomNavigation(anchoredDraggableState, navController)
                }
            ) { innerPaddingScaffold ->

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPaddingScaffold)
                ) {
                    val heightInPx =
                        with(LocalDensity.current) { maxHeight.toPx() } // Get height of screen
                    val miniPlayerHeightInPx =
                        with(LocalDensity.current) { miniPlayerHeight.toPx() }

                    // https://www.strv.com/blog/collapsing-toolbar-using-jetpack-compose-motion-layout-engineering
                    // https://medium.com/@AungThiha3/jetpack-compose-anchored-draggable-item-in-motionlayout-part-1-8d5a1cde880f

                    val anchors = DraggableAnchors {
                        SwipingStates.COLLAPSED at heightInPx - miniPlayerHeightInPx
                        SwipingStates.EXPANDED at 0f
                    }

                    anchoredDraggableState.updateAnchors(anchors)
                    val offset =
                        if (anchoredDraggableState.offset.isNaN()) 0f else anchoredDraggableState.offset
                    val progress =
                        (1 - (offset / heightInPx)).coerceIn(0f, 1f)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        Column {
                            MotionLayoutHeader(
                                progress,
                                mainContent = { modifier ->
                                    Box(modifier = modifier.fillMaxHeight()) {
                                        NavigationGraph(
                                            navController,
                                            miniPlayerHeight,
                                            anchoredDraggableState,
                                            startDestination
                                        )
                                    }
                                },
                                scrollableBody = {
                                    // TODO: Animate properly: Image moving from left corner to big screen, text moving, ...

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .anchoredDraggable(
                                                anchoredDraggableState,
                                                Orientation.Vertical
                                            )
                                    ) {
                                        // TODO: When MiniPlayer is collapsed [progress] is still something like 0.079...
                                        //      we're correcting it here and it seems to be missing [miniPlayerHeight]
                                        //      and extra small padding to make it 0 when collapsed and 1 when expanded.
                                        //      This may not be true for all devices and densities and paddings and size and ...
                                        val extraSmallPaddingInPx = with(LocalDensity.current) {
                                            Theme.padding.extraSmall.toPx()
                                        }

                                        val correctedProgress =
                                            (1 - (offset / (heightInPx - miniPlayerHeightInPx - extraSmallPaddingInPx)))
                                                .coerceIn(0f, 1f)

                                        PlayerLayout(
                                            miniPlayerHeight,
                                            { miniPlayerHeight = it },
                                            anchoredDraggableState,
                                            correctedProgress,
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MotionLayoutHeader(
    progress: Float,
    mainContent: @Composable (Modifier) -> Unit,
    scrollableBody: @Composable () -> Unit
) {
    MotionLayout(
        start = jsonConstraintSetStart(),
        end = jsonConstraintSetEnd(),
        progress = progress,
        modifier = Modifier.fillMaxWidth()
    ) {
        mainContent(
            Modifier
                .layoutId("homeScreen")
                .fillMaxWidth()
        )

        Box(Modifier.layoutId("content")) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Theme.shapes.large)
                    .background(MaterialTheme.colorScheme.background, Theme.shapes.large)
            ) {
                scrollableBody()
            }
        }
    }
}


private fun jsonConstraintSetStart() = ConstraintSet(
    """ {
	homeScreen: { 
		width: "spread",
		start: ['parent', 'start', 0],
		end: ['parent', 'end', 0],
		top: ['parent', 'top', 0],
	},
	content: {
		width: "spread",
		start: ['parent', 'start', 0],
		end: ['parent', 'end', 0],
		top: ['homeScreen', 'bottom', 0],
	}
} """
)

private fun jsonConstraintSetEnd() = ConstraintSet(
    """ {
	homeScreen: { 
		width: "spread",
		height: 0,
		start: ['parent', 'start', 0],
		end: ['parent', 'end', 0],
		top: ['parent', 'top', 0],
	},
	content: {
		width: "spread",
		start: ['parent', 'start', 0],
		end: ['parent', 'end', 0],
		top: ['homeScreen', 'bottom', 0],
	}  
} """
)

