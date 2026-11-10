package com.example.myapplication

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
// Removed material-icons-extended imports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

// Mock user database
val userDatabase = mutableMapOf<String, String>()

@Composable
fun StarryBackground() {
    val stars = remember {
        val random = Random(42)
        List(150) {
            Triple(
                random.nextFloat(), // x
                random.nextFloat(), // y
                random.nextFloat() * 2f // radius
            )
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05050A))
    ) {
        stars.forEach { (x, y, radius) ->
            drawCircle(
                color = Color.White.copy(alpha = 0.6f + (Random.nextFloat() * 0.4f)),
                radius = radius,
                center = Offset(x * size.width, y * size.height)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Fondo estrellado simulando el mockup
        StarryBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo y Título
            Text(
                text = "🐱", // Simulando el logo del gato
                fontSize = 64.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Aviso\nGato",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 36.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Iniciar Sesión",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Campo Correo
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                placeholder = { Text("Correo", color = Color.Gray) },
                leadingIcon = { Text("✉️", modifier = Modifier.padding(start = 12.dp)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White,
                    focusedContainerColor = Color.White.copy(alpha = 0.1f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo Contraseña
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("Contraseña", color = Color.Gray) },
                leadingIcon = { Text("🔒", modifier = Modifier.padding(start = 12.dp)) },
                trailingIcon = {
                    TextButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(
                            text = if (passwordVisible) "👁" else "👁‍🗨",
                            color = Color.Gray
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White,
                    focusedContainerColor = Color.White.copy(alpha = 0.1f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Mensajes de error/éxito
            errorMessage?.let {
                Text(text = it, color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
            }
            successMessage?.let {
                Text(text = it, color = Color(0xFF69F0AE), fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón Entrar
            Button(
                onClick = {
                    errorMessage = null
                    successMessage = null
                    if (username.isBlank() || password.isBlank()) {
                        errorMessage = "Por favor ingrese ambos campos."
                        return@Button
                    }
                    val storedPassword = userDatabase[username]
                    if (storedPassword == null) {
                        errorMessage = "Usuario no registrado. Regístrese primero."
                    } else if (storedPassword == password) {
                        onAuthSuccess()
                    } else {
                        errorMessage = "Contraseña incorrecta."
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Entrar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botón Registrarse (Destacado)
            OutlinedButton(
                onClick = {
                    errorMessage = null
                    successMessage = null

                    if (username.isBlank()) {
                        errorMessage = "El correo no puede estar vacío."
                        return@OutlinedButton
                    }
                    if (password.length < 9) {
                        errorMessage = "La contraseña debe tener al menos 9 caracteres."
                        return@OutlinedButton
                    }
                    if (!password.any { it.isUpperCase() }) {
                        errorMessage = "La contraseña debe contener al menos una letra mayúscula."
                        return@OutlinedButton
                    }
                    if (userDatabase.containsKey(username)) {
                        errorMessage = "El usuario ya existe. Inicie sesión."
                    } else {
                        userDatabase[username] = password
                        successMessage = "¡Cuenta creada con éxito! Ya puede iniciar sesión."
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Registrarse", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
