package io.github.hdghg.sevenhandshakes.util

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import timber.log.Timber

/**
 * Verifies that the person holding the device is its owner, by biometrics or the screen lock PIN, pattern or password.
 */
object DeviceUnlock {
    private const val AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    /**
     * Asks the user to unlock the device.
     *
     * [onSuccess] runs after a successful unlock. [onUnavailable] runs if the unlock cannot be asked for,
     * most often because the device has no screen lock. Nothing runs if the user cancels.
     */
    fun confirm(
        context: Context,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onUnavailable: () -> Unit,
    ) {
        val activity = context.findFragmentActivity()
        if (activity == null) {
            Timber.w("Device unlock needs a FragmentActivity")
            onUnavailable()
            return
        }
        val status = BiometricManager.from(activity).canAuthenticate(AUTHENTICATORS)
        if (status != BiometricManager.BIOMETRIC_SUCCESS) {
            Timber.w("Device unlock unavailable: status %d", status)
            onUnavailable()
            return
        }
        val callback =
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    Timber.d("Device unlock confirmed")
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    Timber.d("Device unlock not confirmed: %d %s", errorCode, errString)
                }
            }
        val promptInfo =
            BiometricPrompt.PromptInfo
                .Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(AUTHENTICATORS)
                .build()
        BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback).authenticate(promptInfo)
    }

    private fun Context.findFragmentActivity(): FragmentActivity? {
        var context = this
        while (context is ContextWrapper) {
            if (context is FragmentActivity) return context
            context = context.baseContext
        }
        return null
    }
}
