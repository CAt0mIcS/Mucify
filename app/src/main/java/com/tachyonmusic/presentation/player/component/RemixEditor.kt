package com.tachyonmusic.presentation.player.component

import androidx.activity.ComponentActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.tachyonmusic.app.R
import com.tachyonmusic.domain.model.RewardAd
import com.tachyonmusic.domain.use_case.player.SaveRemixToDatabase
import com.tachyonmusic.presentation.core_components.ErrorDialog
import com.tachyonmusic.presentation.player.RemixEditorViewModel
import com.tachyonmusic.presentation.theme.Theme
import com.tachyonmusic.presentation.util.asString
import com.tachyonmusic.util.ms
import com.tachyonmusic.util.toReadableString
import kotlinx.coroutines.launch

@Composable
fun RemixEditor(
    modifier: Modifier = Modifier,
    viewModel: RemixEditorViewModel = hiltViewModel()
) {
    val timingData = viewModel.timingData
    val scope = rememberCoroutineScope()
    val error by viewModel.remixError.collectAsState()

    if (error != null && error?.code == -1)
        ErrorDialog(
            title = stringResource(R.string.warning),
            subtitle = error?.message?.asString() ?: "Unknown"
        )

    Column(modifier = modifier) {
        val duration by viewModel.duration.collectAsState()

        for (i in timingData.indices) {

            val sliderColor =
                if (i == viewModel.currentIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        Theme.padding.extremelySmall,
                        MaterialTheme.colorScheme.primary,
                        Theme.shapes.medium
                    )
                    .padding(Theme.padding.medium)

            ) {
                val settings by viewModel.settings.collectAsState()
                val deltaDuration = settings.audioUpdateInterval

                LazyRow(modifier = Modifier.fillMaxWidth()) {
                    item {
                        Button(
                            onClick = {
                                viewModel.updateTimingData(
                                    i,
                                    timingData[i].startTime - deltaDuration,
                                    timingData[i].endTime
                                )
                                viewModel.setAndPlayTimingDataAt(i)
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(text = "<")
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                viewModel.updateTimingData(
                                    i,
                                    timingData[i].startTime + deltaDuration,
                                    timingData[i].endTime
                                )
                                viewModel.setAndPlayTimingDataAt(i)
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(text = ">")
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                viewModel.updateStartToCurrentPosition(i)
                                viewModel.setNewTimingData()
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("<>")
                        }
                    }


                    item {
                        IconButton(
                            onClick = { viewModel.setAndPlayTimingDataAt(i) },
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_play),
                                contentDescription = "Seek to timing data"
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(timingData[i].startTime.toReadableString(true))
                    Text(timingData[i].endTime.toReadableString(true))
                }

                RangeSlider(
                    modifier = Modifier
                        .systemGestureExclusion()
                        .align(Alignment.CenterHorizontally),
                    value = timingData[i].startTime.inWholeMilliseconds.toFloat()..timingData[i].endTime.inWholeMilliseconds.toFloat(),
                    onValueChange = {
                        viewModel.updateTimingData(i, it.start.ms, it.endInclusive.ms)
                    },
                    onValueChangeFinished = viewModel::setNewTimingData,
                    valueRange = 0f..duration.inWholeMilliseconds.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = sliderColor,
                        activeTrackColor = sliderColor,
                        inactiveTrackColor = if (i == viewModel.currentIndex)
                            MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.onSecondary
                    )
                )

                LazyRow(modifier = Modifier.align(Alignment.End)) {
                    item {
                        var timingDataInfoDropdownExpanded by remember { mutableStateOf(false) }

                        IconButton(
                            onClick = {
                                timingDataInfoDropdownExpanded = !timingDataInfoDropdownExpanded
                            }
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                "Additional Loop options for timing data $i"
                            )
                        }

                        DropdownMenu(
                            expanded = timingDataInfoDropdownExpanded,
                            onDismissRequest = { timingDataInfoDropdownExpanded = false }
                        ) {
                            var isMoveUpValid by remember { mutableStateOf(false) }
                            var isMoveDownValid by remember { mutableStateOf(false) }
                            LaunchedEffect(timingData) {
                                isMoveUpValid = viewModel.isValidTimingDataMove(i, i - 1)
                                isMoveDownValid = viewModel.isValidTimingDataMove(i, i + 1)
                            }

                            DropdownMenuItem(
                                text = { Text("Insert before") },
                                onClick = {
                                    viewModel.addNewTimingData(i)
                                    timingDataInfoDropdownExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Insert after") },
                                onClick = {
                                    viewModel.addNewTimingData(i + 1)
                                    timingDataInfoDropdownExpanded = false
                                }
                            )


                            DropdownMenuItem(
                                text = { Text("Move up") },
                                onClick = {
                                    viewModel.moveTimingData(i, i - 1)
                                    timingDataInfoDropdownExpanded = false
                                },
                                enabled = isMoveUpValid
                            )
                            DropdownMenuItem(
                                text = { Text("Move down") },
                                onClick = {
                                    viewModel.moveTimingData(i, i + 1)
                                    timingDataInfoDropdownExpanded = false
                                },
                                enabled = isMoveDownValid
                            )


                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    viewModel.removeTimingData(i)
                                    timingDataInfoDropdownExpanded = false
                                }
                            )
                        }
                    }


                    item {
                        Button(
                            onClick = {
                                viewModel.updateEndToCurrentPosition(i)
                                viewModel.setNewTimingData()
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("<>")
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                viewModel.updateTimingData(
                                    i,
                                    timingData[i].startTime,
                                    timingData[i].endTime - deltaDuration
                                )
                                viewModel.setAndPlayTimingDataAt(i, startFromEnd = true)
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(text = "<")
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                viewModel.updateTimingData(
                                    i,
                                    timingData[i].startTime,
                                    timingData[i].endTime + deltaDuration
                                )
                                viewModel.setAndPlayTimingDataAt(i, startFromEnd = true)
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(text = ">")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.padding(top = 6.dp))
        }

        var openRemixSaveDialog by remember { mutableStateOf(false) }
        val currentName by viewModel.currentRemixName.collectAsState()
        var remixName by rememberSaveable { mutableStateOf(currentName ?: "") }
        val remixError by viewModel.remixError.collectAsState()

        // TODO: Is this a good way to keep states synced?
        LaunchedEffect(currentName) {
            remixName = currentName ?: ""
        }

        Button(onClick = {
            openRemixSaveDialog = true
        }) {
            Text("Save")
        }

        val activity = LocalContext.current as? ComponentActivity
        if (openRemixSaveDialog) {
            // TODO: Show error

            BasicAlertDialog(
                onDismissRequest = { openRemixSaveDialog = false },
            ) {
                Column {
                    TextField(
                        value = remixName,
                        onValueChange = { remixName = it },
                        singleLine = true)
                    Button(
                        onClick = {
                            viewModel.saveNewRemix(
                                remixName,
                                ignoreMaxRemixCount = activity == null
                            )
                            openRemixSaveDialog = false
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }

        if (remixError?.code == SaveRemixToDatabase.ERROR_NEEDS_TO_SHOW_AD) {
            Dialog(
                onDismissRequest = { viewModel.clearRemixError() },
                DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                    usePlatformDefaultWidth = false
                )
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth(.8f)
                        .clip(Theme.shapes.large)
                        .background(MaterialTheme.colorScheme.background, Theme.shapes.large)
                        .border(
                            BorderStroke(2.dp, MaterialTheme.colorScheme.surfaceContainer),
                            Theme.shapes.large
                        )
                        .padding(
                            horizontal = Theme.padding.large,
                            vertical = Theme.padding.medium
                        )
                ) {
                    Column {
                        val rewardText = when (val type = viewModel.rewardAdType) {
                            is RewardAd.Type.NewRemixes ->
                                if (type.amount == 1)
                                    stringResource(R.string.watch_reward_ad_for_one_remix)
                                else
                                    stringResource(R.string.watch_reward_ad_for_more_remixes, type.amount)
                            else -> {
                                // No reward ad available, ignore max remix count (TODO: Should be checked in ViewModel)
                                LaunchedEffect(Unit) {
                                    viewModel.saveNewRemix(remixName, ignoreMaxRemixCount = true)
                                }
                                return@Box
                            }
                        }

                        Text(rewardText)
                        Row(
                            modifier = Modifier
                                .padding(top = Theme.padding.medium)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Button(onClick = {
                                scope.launch {
                                    viewModel.playAd(activity)
                                    viewModel.saveNewRemix(
                                        remixName,
                                        ignoreMaxRemixCount = activity == null
                                    )
                                }
                            }) {
                                Text("Watch Ad")
                            }

                            Button(onClick = { viewModel.clearRemixError() }) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }
        }


        if (remixError?.code == SaveRemixToDatabase.ERROR_REMIX_ALREADY_EXISTS) {
            Dialog(
                onDismissRequest = { viewModel.clearRemixError() },
                DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                    usePlatformDefaultWidth = false
                )
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth(.8f)
                        .clip(Theme.shapes.large)
                        .background(MaterialTheme.colorScheme.background, Theme.shapes.large)
                        .border(
                            BorderStroke(2.dp, MaterialTheme.colorScheme.surfaceContainer),
                            Theme.shapes.large
                        )
                        .padding(
                            horizontal = Theme.padding.large,
                            vertical = Theme.padding.medium
                        )
                ) {
                    Column {
                        Text("Remix with name $remixName already exists")
                        Row(
                            modifier = Modifier
                                .padding(top = Theme.padding.medium)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Button(onClick = {
                                viewModel.saveNewRemix(
                                    remixName,
                                    replaceExisting = true
                                )
                            }) {
                                Text("Replace")
                            }

                            Button(onClick = { viewModel.clearRemixError() }) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }
        }
    }
}