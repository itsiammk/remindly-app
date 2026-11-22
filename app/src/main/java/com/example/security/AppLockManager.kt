package com.example.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object AppLockManager {

  private const val ITERATIONS = 25_000
  private const val KEY_LENGTH_BITS = 256
  private val random = SecureRandom()

  fun hashPin(pin: String): String {
    val salt = ByteArray(16).also { random.nextBytes(it) }
    val derived = derive(pin.toCharArray(), salt)
    return "${toHex(salt)}:${toHex(derived)}"
  }

  fun verifyPin(pin: String, storedHash: String): Boolean {
    if (pin.length < 4 || !storedHash.contains(':')) return false
    val parts = storedHash.split(':', limit = 2)
    val salt = fromHex(parts[0]) ?: return false
    val expected = fromHex(parts[1]) ?: return false
    val actual = derive(pin.toCharArray(), salt)
    return MessageDigest.isEqual(expected, actual)
  }

  fun canUseBiometric(context: Context): Boolean {
    val manager = BiometricManager.from(context)
    val result =
      manager.canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
          BiometricManager.Authenticators.BIOMETRIC_WEAK
      )
    return result == BiometricManager.BIOMETRIC_SUCCESS
  }

  fun showBiometricPrompt(
    activity: FragmentActivity,
    title: String,
    subtitle: String,
    negativeText: String,
    onAuthenticated: () -> Unit,
    onError: () -> Unit = {},
  ) {
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt =
      BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
          override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            onAuthenticated()
          }

          override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            onError()
          }
        },
      )

    val info =
      BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setNegativeButtonText(negativeText)
        .setAllowedAuthenticators(
          BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        .build()

    runCatching { prompt.authenticate(info) }.onFailure { onError() }
  }

  private fun derive(pin: CharArray, salt: ByteArray): ByteArray {
    val spec = PBEKeySpec(pin, salt, ITERATIONS, KEY_LENGTH_BITS)
    val factory =
      runCatching { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256") }
        .getOrElse { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1") }
    return try {
      factory.generateSecret(spec).encoded
    } finally {
      spec.clearPassword()
    }
  }

  private fun toHex(bytes: ByteArray): String =
    bytes.joinToString("") { "%02x".format(it) }

  private fun fromHex(hex: String): ByteArray? {
    if (hex.length % 2 != 0) return null
    return runCatching {
      ByteArray(hex.length / 2) { i ->
        hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
      }
    }.getOrNull()
  }
}
