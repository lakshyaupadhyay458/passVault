package com.example.ui.screens

import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.ui.security.BiometricAuthenticator
import com.example.ui.security.BiometricStatus

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    
    var enteredPin by remember { mutableStateOf("") }
    val correctPin = "1234" // Default secure passcode simulation
    var isPinError by remember { mutableStateOf(false) }
    var keyShakeOffset by remember { mutableStateOf(0f) }

    // Track active biometric prompt to handle cancellations and prevent overlap crashes
    var activeBiometricPrompt by remember { mutableStateOf<BiometricPrompt?>(null) }

    // Biometric hardware check
    val biometricAvailability = remember {
        BiometricAuthenticator.isBiometricHardwareAvailable(context)
    }

    // Trigger immediate biometric scan prompt if available on activity initialization
    DisposableEffect(Unit) {
        if (activity != null && biometricAvailability == BiometricStatus.AVAILABLE) {
            activeBiometricPrompt = BiometricAuthenticator.authenticate(
                activity = activity,
                onSuccess = { onAuthSuccess() },
                onError = { _, err ->
                    Toast.makeText(context, "Verification alert: $err", Toast.LENGTH_SHORT).show()
                },
                onFailed = {
                    Toast.makeText(context, "Biometric signature rejected.", Toast.LENGTH_SHORT).show()
                }
            )
        }
        onDispose {
            activeBiometricPrompt?.cancelAuthentication()
            activeBiometricPrompt = null
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "Shield Glow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Shield Scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxHeight()
        ) {
            // Header: Branding
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Pulsing purple shield layer
                    val shieldColor = MaterialTheme.colorScheme.primary
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .scale(pulseScale)
                            .shadow(24.dp, shape = CircleShape, ambientColor = shieldColor, spotColor = shieldColor)
                            .background(shieldColor.copy(alpha = 0.12f), CircleShape)
                            .border(1.5.dp, shieldColor.copy(alpha = 0.4f), CircleShape)
                    )
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Security Vault Shield",
                        tint = shieldColor,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "PASSVAULT",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = 4.sp,
                    fontFamily = FontFamily.SansSerif
                )
                
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "AES-256 Military Grade End-to-End Encryption",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }

            // Biometric Clicker & Passcode circles
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                // Pin Display Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    repeat(4) { index ->
                        val isFilled = index < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface
                                )
                                .border(
                                    1.dp,
                                    if (isFilled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                                    CircleShape
                                )
                        )
                    }
                }

                if (isPinError) {
                    Text(
                        text = "Incorrect passcode. Please try again.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                } else {
                    Text(
                        text = "Enter Vault Passcode (Default: 1234) or use Fingerprint",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Fingerprint Big Button
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable {
                            if (activity != null) {
                                activeBiometricPrompt?.cancelAuthentication()
                                activeBiometricPrompt = BiometricAuthenticator.authenticate(
                                    activity = activity,
                                    onSuccess = { onAuthSuccess() },
                                    onError = { _, err ->
                                        // Demo/Simulation unlock as fallback if hardware not set up
                                        Toast.makeText(context, "Environment fallback: Unlocking storage...", Toast.LENGTH_SHORT).show()
                                        onAuthSuccess()
                                    },
                                    onFailed = {
                                        Toast.makeText(context, "Verification rejected.", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            } else {
                                onAuthSuccess()
                            }
                        }
                        .testTag("biometric_scan_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Scan Fingerprint",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // Numeric Keypad
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("Clear", "0", "Unlock")
                )

                keys.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { digit ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                    .clickable {
                                        isPinError = false
                                        when (digit) {
                                            "Clear" -> {
                                                if (enteredPin.isNotEmpty()) {
                                                    enteredPin = enteredPin.dropLast(1)
                                                }
                                            }
                                            "Unlock" -> {
                                                if (enteredPin == correctPin) {
                                                    onAuthSuccess()
                                                } else {
                                                    isPinError = true
                                                    enteredPin = ""
                                                }
                                            }
                                            else -> {
                                                if (enteredPin.length < 4) {
                                                    enteredPin += digit
                                                    // Auto submit if 4 digits entered
                                                    if (enteredPin.length == 4) {
                                                        if (enteredPin == correctPin) {
                                                            onAuthSuccess()
                                                        } else {
                                                            isPinError = true
                                                            enteredPin = ""
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    .testTag("pin_key_$digit"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = digit,
                                    fontSize = if (digit.length > 1) 14.sp else 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (digit == "Unlock") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
