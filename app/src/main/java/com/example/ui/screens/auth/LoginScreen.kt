package com.example.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Facebook
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.*

@Composable
fun LoginScreen(
    onSignInSuccess: (provider: String, identifier: String, displayName: String?) -> Unit
) {
    var activeMethod by remember { mutableStateOf<String?>(null) } // "GMAIL", "PHONE", "FACEBOOK"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070A16))
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_zyro_room_bg),
            contentDescription = "Zyro Lounge Backdrop",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF070A16).copy(alpha = 0.55f),
                            Color(0xFF070A16).copy(alpha = 0.94f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Brand Emblem
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(ZyroCyan, ZyroViolet)))
                        .border(2.dp, ZyroGold.copy(alpha = 0.8f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Z",
                        color = Color.White,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "ZYRO",
                    color = Color.White,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Carrom • Uno • Voice Rooms • VIP Lounge",
                    color = ZyroCyan,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            // Sign-In Options Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF12172E).copy(alpha = 0.94f))
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sign in to Zyro",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Choose how you want to enter the arena",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Option 1: Gmail
                    Button(
                        onClick = { activeMethod = "GMAIL" },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF111827)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("login_gmail_button")
                    ) {
                        Icon(Icons.Filled.Email, contentDescription = null, tint = Color(0xFFEA4335))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Continue with Gmail", fontWeight = FontWeight.Bold)
                    }

                    // Option 2: Phone Number
                    Button(
                        onClick = { activeMethod = "PHONE" },
                        colors = ButtonDefaults.buttonColors(containerColor = ZyroEmerald, contentColor = Color(0xFF031A11)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("login_phone_button")
                    ) {
                        Icon(Icons.Filled.PhoneAndroid, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Continue with Phone Number", fontWeight = FontWeight.Bold)
                    }

                    // Option 3: Facebook
                    Button(
                        onClick = { activeMethod = "FACEBOOK" },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2), contentColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("login_facebook_button")
                    ) {
                        Icon(Icons.Filled.Facebook, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Continue with Facebook", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text(
                text = "By signing in, you agree to Zyro fair-play & community rules",
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
    }

    if (activeMethod != null) {
        val method = activeMethod!!
        var inputValue by remember(method) {
            mutableStateOf(
                when (method) {
                    "GMAIL" -> "aarav.zyro@gmail.com"
                    "PHONE" -> "+91 98765 43210"
                    else -> "Aarav Zyro"
                }
            )
        }
        var otpCode by remember { mutableStateOf("4829") }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { activeMethod = null },
            title = {
                Text(
                    when (method) {
                        "GMAIL" -> "Sign in with Gmail"
                        "PHONE" -> "Sign in with Phone Number"
                        else -> "Sign in with Facebook"
                    }
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = inputValue,
                        onValueChange = {
                            inputValue = it
                            errorMsg = null
                        },
                        label = {
                            Text(
                                when (method) {
                                    "GMAIL" -> "Gmail Address"
                                    "PHONE" -> "Mobile Number"
                                    else -> "Facebook Name or Email"
                                }
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = when (method) {
                                "GMAIL" -> KeyboardType.Email
                                "PHONE" -> KeyboardType.Phone
                                else -> KeyboardType.Text
                            }
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (method == "PHONE") {
                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { otpCode = it },
                            label = { Text("Verification Code (OTP)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (errorMsg != null) {
                        Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = inputValue.trim()
                        when (method) {
                            "GMAIL" -> if (!trimmed.contains("@")) {
                                errorMsg = "Enter a valid Gmail address"
                                return@Button
                            }
                            "PHONE" -> if (trimmed.length < 7 || otpCode.trim().length < 4) {
                                errorMsg = "Enter valid phone number & 4+ digit code"
                                return@Button
                            }
                            else -> if (trimmed.isEmpty()) {
                                errorMsg = "Enter your Facebook profile name"
                                return@Button
                            }
                        }
                        val derivedName = when (method) {
                            "GMAIL" -> trimmed.substringBefore("@").replaceFirstChar { it.uppercase() }
                            "PHONE" -> null
                            else -> trimmed
                        }
                        activeMethod = null
                        onSignInSuccess(method, trimmed, derivedName)
                    },
                    modifier = Modifier.testTag("confirm_auth_button")
                ) {
                    Text("Sign In")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeMethod = null }) { Text("Cancel") }
            }
        )
    }
}
