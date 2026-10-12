package io.github.hdghg.sevenhandshakes.ui.newgratitude

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hdghg.sevenhandshakes.ui.NewGratitudeTopAppBar

@Composable
fun NewGratitudeScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NewGratitudeViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { NewGratitudeTopAppBar(onBack = onBack, onComplete = onComplete) },
    ) { paddingValues ->
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = modifier.padding(paddingValues),
        ) {
            Text(
                "Your comment:",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
            )
            OutlinedTextField(
                value = uiState.text,
                onValueChange = viewModel::updateText,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Text("Stars:", modifier = Modifier.fillMaxWidth())
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                    ) {
                        MyStarButton(
                            number = 1,
                            active = uiState.stars > 0,
                            updater = viewModel::setStars,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        MyStarButton(
                            number = 2,
                            active = uiState.stars > 1,
                            updater = viewModel::setStars,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        MyStarButton(
                            number = 3,
                            active = uiState.stars > 2,
                            updater = viewModel::setStars,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    when (uiState.stars) {
                        1 -> {
                            Text(
                                "One star. Good small gratitude",
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        2 -> {
                            Text("Two stars. Usual gratitude", modifier = Modifier.fillMaxWidth())
                        }

                        else -> {
                            Text(
                                "Three stars. Outstanding gratitude",
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
            Text(
                "Receiver:",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
            )
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
            ) {
                OutlinedTextField(
                    value = uiState.receiver,
                    onValueChange = viewModel::updateReceiver,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = { },
                ) {
                    Text("Scan QR")
                }
            }
            Button(
                onClick = viewModel::complete,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
            ) {
                Text("Complete")
            }
            Text("Edit Complete: " + uiState.editComplete)
            LaunchedEffect(uiState.editComplete) {
                if (uiState.editComplete) {
                    onComplete()
                }
            }
        }
    }
}

@Composable
fun MyStarButton(number: Int, active: Boolean, updater: (Int) -> Unit, modifier: Modifier) {
    IconButton(
        onClick = { updater(number) },
        modifier =
            modifier
                .size(48.dp),
    ) {
        Icon(
            imageVector = if (active) Icons.Filled.Star else Icons.Filled.StarOutline,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview
@Composable
fun NewGratitudeScreenPreview() {
    Surface {
        NewGratitudeScreen({}, {})
    }
}
