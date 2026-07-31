package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.YamahaBlue
import com.example.ui.theme.YamahaBlueDark
import com.example.ui.theme.YamahaRed

@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit,
    errorMessage: String?,
    mustChangePassword: Boolean = false,
    onChangePasswordSubmit: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("Admin@123") }
    var passwordVisible by remember { mutableStateOf(false) }

    // First Login Password Change Modal State
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordChangeError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(YamahaBlueDark)
    ) {
        // Hero Background Image with Gradient Overlay
        Image(
            painter = painterResource(id = R.drawable.img_yamaha_hero_1785404947296),
            contentDescription = "Yamaha Welding Plant",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            YamahaBlueDark.copy(alpha = 0.7f),
                            YamahaBlueDark
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(140.dp))

            // Brand Emblem & Title
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(YamahaRed),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Yamaha Shield",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "YAMAHA MOTOR INDIA",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = YamahaRed,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            )

            Text(
                text = "WELDING PATROL MANAGEMENT SYSTEM",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )

            Text(
                text = "Enterprise Maintenance Portal",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.7f)
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Login Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Authorized Personnel Login",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Username Input
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        placeholder = { Text("e.g. admin, supervisor, operator") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Username") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = YamahaBlue,
                            focusedLabelColor = YamahaBlue
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_input")
                    )

                    val mappedEmail = if (username.isNotBlank()) {
                        "${username.trim().lowercase().removeSuffix("@yamaha-motor-india.com")}@yamaha-motor-india.com"
                    } else "username@yamaha-motor-india.com"

                    Text(
                        text = "Mapped Enterprise Email: $mappedEmail",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(start = 4.dp, top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Input
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = YamahaBlue,
                            focusedLabelColor = YamahaBlue
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input")
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall.copy(color = YamahaRed)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Submit Login Button
                    Button(
                        onClick = { onLoginClick(username, password) },
                        colors = ButtonDefaults.buttonColors(containerColor = YamahaBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_button")
                    ) {
                        Text(
                            text = "SECURE LOG IN",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            username = "admin"
                            password = "Admin@123"
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = YamahaBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Fill Default Admin Credentials (admin / Admin@123)", fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Default Super Admin: admin / Admin@123",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = YamahaRed,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Yamaha Motor Company © 2026. Confidential Industrial System.",
                style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.5f))
            )
        }
    }

    // Mandatory First Login Password Change Modal
    if (mustChangePassword) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { /* Modal force change, cannot dismiss */ },
            title = {
                Text(
                    text = "First Login - Password Change Mandatory",
                    fontWeight = FontWeight.Bold,
                    color = YamahaRed
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Initial setup detected using default credentials (Admin@123). Security policy requires creating a new secure password before continuing.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = YamahaBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_password_input")
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm New Password") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = YamahaBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_password_input")
                    )

                    if (passwordChangeError != null) {
                        Text(
                            text = passwordChangeError!!,
                            style = MaterialTheme.typography.labelSmall.copy(color = YamahaRed)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPassword.isBlank()) {
                            passwordChangeError = "New password cannot be empty"
                        } else if (newPassword == "Admin@123") {
                            passwordChangeError = "New password must be different from default Admin@123"
                        } else if (newPassword != confirmPassword) {
                            passwordChangeError = "Passwords do not match"
                        } else {
                            passwordChangeError = null
                            onChangePasswordSubmit(newPassword)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = YamahaBlue),
                    modifier = Modifier.testTag("submit_password_change_button")
                ) {
                    Text("SAVE NEW PASSWORD")
                }
            }
        )
    }
}
