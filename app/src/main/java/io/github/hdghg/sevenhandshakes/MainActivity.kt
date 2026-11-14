package io.github.hdghg.sevenhandshakes

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Main activity for the app. A FragmentActivity because the device unlock prompt needs one.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

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
