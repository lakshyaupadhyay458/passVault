package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.security.SecureRandom
import kotlin.math.roundToInt

@Composable
fun PasswordGeneratorWidget(
    onPasswordCopied: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var passwordLength by remember { mutableStateOf(16f) }
    var useUppercase by remember { mutableStateOf(true) }
    var useLowercase by remember { mutableStateOf(true) }
    var useDigits by remember { mutableStateOf(true) }
    var useSymbols by remember { mutableStateOf(true) }
    var generatedPassword by remember { mutableStateOf("") }

    fun generateSecurePassword(
        length: Int,
        upper: Boolean,
        lower: Boolean,
        digits: Boolean,
        symbols: Boolean
    ): String {
        val uppercaseChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val lowercaseChars = "abcdefghijklmnopqrstuvwxyz"
        val digitChars = "0123456789"
        val symbolChars = "!@#$%^&*()_+-=[]{}|;:,.<>?"
        
        var charPool = ""
        val mandatoryChars = mutableListOf<Char>()
        val random = SecureRandom()
        
        if (upper) {
            charPool += uppercaseChars
            mandatoryChars.add(uppercaseChars[random.nextInt(uppercaseChars.length)])
        }
        if (lower) {
            charPool += lowercaseChars
            mandatoryChars.add(lowercaseChars[random.nextInt(lowercaseChars.length)])
        }
        if (digits) {
            charPool += digitChars
            mandatoryChars.add(digitChars[random.nextInt(digitChars.length)])
        }
        if (symbols) {
            charPool += symbolChars
            mandatoryChars.add(symbolChars[random.nextInt(symbolChars.length)])
        }
        
        if (charPool.isEmpty()) return ""

        val passwordBuilder = StringBuilder()
        // Add mandatory ones first to ensure complexity
        passwordBuilder.append(mandatoryChars.joinToString(""))
        
        val remainingLength = length - mandatoryChars.size
        for (i in 0 until remainingLength) {
            passwordBuilder.append(charPool[random.nextInt(charPool.length)])
        }
        
        // Shuffle the characters
        val list = passwordBuilder.toList().shuffled(random)
        return list.joinToString("")
    }

    // Trigger generate on launch and whenever criteria change
    LaunchedEffect(passwordLength, useUppercase, useLowercase, useDigits, useSymbols) {
        if (useUppercase || useLowercase || useDigits || useSymbols) {
            generatedPassword = generateSecurePassword(
                passwordLength.roundToInt(),
                useUppercase,
                useLowercase,
                useDigits,
                useSymbols
            )
        } else {
            generatedPassword = ""
        }
    }

    val entropyScore = remember(generatedPassword) {
        val len = generatedPassword.length
        if (len == 0) "None"
        else if (len < 8) "Extremely Weak"
        else if (len < 12) "Average"
        else if (len < 16) "Strong"
        else "Military Grade"
    }

    val entropyColor = remember(generatedPassword) {
        val len = generatedPassword.length
        if (len < 8) Color(0xFFEB5757) // Red
        else if (len < 12) Color(0xFFF2C94C) // Orange/Yellow
        else if (len < 16) Color(0xFF2F80ED) // Blue
        else Color(0xFF27AE60) // Emerald Green
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF131722))
            .border(1.dp, Color(0xFF232B3A), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Secure Key Generator",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(entropyColor.copy(alpha = 0.15f))
                    .border(1.dp, entropyColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = entropyScore,
                    fontSize = 11.sp,
                    color = entropyColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Generated Password Box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0D0F14))
                .border(1.dp, Color(0xFF232B3A), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = generatedPassword.ifEmpty { "Select options below" },
                fontSize = 15.sp,
                color = if (generatedPassword.isEmpty()) Color(0xFF5A6981) else Color(0xFF2F80ED),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .testTag("generated_pass_text")
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (useUppercase || useLowercase || useDigits || useSymbols) {
                            generatedPassword = generateSecurePassword(
                                passwordLength.roundToInt(),
                                useUppercase,
                                useLowercase,
                                useDigits,
                                useSymbols
                            )
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Regenerate Keys",
                        tint = Color(0xFF8B9BB4),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = {
                        if (generatedPassword.isNotEmpty()) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("PassVault Generation", generatedPassword)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Password copied securely!", Toast.LENGTH_SHORT).show()
                            onPasswordCopied(generatedPassword)
                        }
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("copy_generated_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Password string",
                        tint = Color(0xFF27AE60),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Length Slider
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Key Byte Length",
                    fontSize = 12.sp,
                    color = Color(0xFF8B9BB4)
                )
                Text(
                    text = "${passwordLength.roundToInt()} characters",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Slider(
                value = passwordLength,
                onValueChange = { passwordLength = it },
                valueRange = 8f..32f,
                steps = 24,
                colors = SliderDefaults.colors(
                    activeTrackColor = Color(0xFF2F80ED),
                    inactiveTrackColor = Color(0xFF1E2532),
                    thumbColor = Color(0xFF2F80ED)
                ),
                modifier = Modifier.testTag("length_slider")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Options Toggles
        val toggleOptions = listOf(
            Triple("Uppercase [A-Z]", useUppercase) { valState: Boolean -> useUppercase = valState },
            Triple("Lowercase [a-z]", useLowercase) { valState: Boolean -> useLowercase = valState },
            Triple("Numerals [0-9]", useDigits) { valState: Boolean -> useDigits = valState },
            Triple("Special Symbols [&%#]", useSymbols) { valState: Boolean -> useSymbols = valState }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                CustomToggleBox(label = "Uppercase", checked = useUppercase, onCheckedChange = { useUppercase = it })
                Spacer(modifier = Modifier.height(6.dp))
                CustomToggleBox(label = "Lowercase", checked = useLowercase, onCheckedChange = { useLowercase = it })
            }

            Column(modifier = Modifier.weight(1f)) {
                CustomToggleBox(label = "Numbers", checked = useDigits, onCheckedChange = { useDigits = it })
                Spacer(modifier = Modifier.height(6.dp))
                CustomToggleBox(label = "Symbols", checked = useSymbols, onCheckedChange = { useSymbols = it })
            }
        }
    }
}

@Composable
fun CustomToggleBox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (checked) Color(0xFF2F80ED).copy(alpha = 0.08f) else Color(0xFF0F111A))
            .border(
                1.dp,
                if (checked) Color(0xFF2F80ED).copy(alpha = 0.3f) else Color(0xFF1D222F),
                RoundedCornerShape(8.dp)
            )
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (checked) Color.White else Color(0xFF8B9BB4),
            fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal
        )
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF2F80ED),
                uncheckedColor = Color(0xFF3B485F),
                checkmarkColor = Color.White
            ),
            modifier = Modifier
                .scale(0.7f)
                .size(16.dp)
        )
    }
}
