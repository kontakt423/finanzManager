package com.example.finanzmanager.ui

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** Auf API 30+ ist Geräte-PIN als Fallback erlaubt, darunter nur Biometrie. */
private fun biometricAuthenticators(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    else
        BiometricManager.Authenticators.BIOMETRIC_WEAK

/** True, wenn das Gerät Biometrie (bzw. PIN-Fallback) für die App-Sperre nutzen kann. */
fun canUseBiometric(context: Context): Boolean =
    BiometricManager.from(context).canAuthenticate(biometricAuthenticators()) ==
        BiometricManager.BIOMETRIC_SUCCESS

/** Zeigt den System-Biometrie-Dialog. Muss von einer FragmentActivity aus aufgerufen werden. */
fun FragmentActivity.showBiometricPrompt(
    onSuccess: () -> Unit,
    onFailed: () -> Unit = {}
) {
    val executor = ContextCompat.getMainExecutor(this)
    val prompt = BiometricPrompt(
        this, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onFailed()
        }
    )
    val builder = BiometricPrompt.PromptInfo.Builder()
        .setTitle("FinanzManager entsperren")
        .setSubtitle("Bitte bestätige deine Identität")
        .setAllowedAuthenticators(biometricAuthenticators())
    // Ein Abbrechen-Button ist nur (und zwingend) ohne Geräte-PIN-Fallback erlaubt.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        builder.setNegativeButtonText("Abbrechen")
    }
    try {
        prompt.authenticate(builder.build())
    } catch (e: Exception) {
        onFailed()
    }
}

@Composable
fun AppLockScreen(onUnlock: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text("FinanzManager ist gesperrt", fontWeight = FontWeight.Black, fontSize = 18.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "Zum Entsperren authentifizieren",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(28.dp))
            Button(onClick = onUnlock, modifier = Modifier.height(52.dp)) {
                Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Entsperren", fontWeight = FontWeight.Bold)
            }
        }
    }
}
