package io.github.hdghg.sevenhandshakes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Main activity for the app
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("MainActivity created (restored=%b)", savedInstanceState != null)
        enableEdgeToEdge()
        setContent {
            MainTheme {
                AppMainNavGraph()
            }
        }
    }
}
