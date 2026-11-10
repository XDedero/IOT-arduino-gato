package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Función para comunicarse con el ESP8266/ESP32 en un hilo de fondo (Dispatchers.IO)
fun enviarComandoArduino(endpoint: String, coroutineScope: kotlinx.coroutines.CoroutineScope) {
    coroutineScope.launch(Dispatchers.IO) {
        try {
            val url = URL("http://192.168.1.50/$endpoint")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                println("Comando $endpoint enviado con éxito")
            } else {
                println("Error al enviar $endpoint. Código HTTP: $responseCode")
            }
            connection.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

suspend fun consultarContadorGatoArduino(): Int? {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("http://192.168.1.50/estado_gato")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 3000
            connection.readTimeout = 3000

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().readText()
                responseText.trim().toIntOrNull()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArduinoControlScreen(onLogout: () -> Unit = {}) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    
    // Estado del sistema: 0 = Reposo (Verde), 1 = Alerta Reciente (Amarillo), 2 = Alerta Detectada (Rojo)
    var isSystemActive by remember { mutableStateOf(false) } // Falso = apagado, Verdadero = Escuchando arduino

    // Contadores e historial
    var catExitCount by remember { mutableStateOf(24) }
    val historyLog = remember { mutableStateListOf<String>() }
    
    // UI State para saber qué pantalla se muestra dentro del drawer
    var currentScreen by remember { mutableStateOf("inicio") }
    
    // Estado visual del círculo (0 = Apagado, 1 = Verde, 2 = Amarillo, 3 = Rojo)
    var visualState by remember { mutableIntStateOf(1) }
    
    // Polling automático (Preguntar al Arduino cada 3 segundos)
    LaunchedEffect(isSystemActive) {
        if (isSystemActive) {
            visualState = 1 // Verde - Reposo
            while (true) {
                val arduinoCount = consultarContadorGatoArduino()
                if (arduinoCount != null && arduinoCount > catExitCount) {
                    catExitCount = arduinoCount
                    val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    historyLog.add(0, "[$currentTime] ¡Gato detectado! Total: $catExitCount")

                    // Cambiar a Rojo temporalmente
                    visualState = 3
                }
                delay(3000)

                // Si estaba en rojo, tras un rato volver a Amarillo o Verde
                if (visualState == 3) {
                    delay(5000) // Mostrar el rojo por 5 segundos
                    visualState = 2 // Pasar a amarillo (Actividad reciente)
                }
            }
        } else {
            visualState = 0 // Apagado
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF111116),
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(Modifier.height(32.dp))
                
                // Cabecera del Menu
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🐱", fontSize = 32.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Aviso Gato",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = { coroutineScope.launch { drawerState.close() } }) {
                        Text("❌", color = Color.White)
                    }
                }
                
                Spacer(Modifier.height(48.dp))
                
                // Elementos del menú
                NavigationDrawerItem(
                    icon = { Text("🏠", fontSize = 24.sp) },
                    label = { Text("Inicio", color = Color.White) },
                    selected = currentScreen == "inicio",
                    onClick = {
                        currentScreen = "inicio"
                        coroutineScope.launch { drawerState.close() }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color.White.copy(alpha = 0.15f),
                        unselectedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                
                NavigationDrawerItem(
                    icon = { Text("📊", fontSize = 24.sp) },
                    label = { Text("Historial", color = Color.White) },
                    selected = currentScreen == "historial",
                    onClick = {
                        currentScreen = "historial"
                        coroutineScope.launch { drawerState.close() }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color.White.copy(alpha = 0.15f),
                        unselectedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                Spacer(Modifier.weight(1f))

                NavigationDrawerItem(
                    icon = { Text("🚪", fontSize = 24.sp) },
                    label = { Text("Cerrar Sesión", color = Color.White) },
                    selected = false,
                    onClick = {
                        onLogout()
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("Aviso Gato", color = Color.White, fontWeight = FontWeight.Bold)
                    },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Text("☰", color = Color.White, fontSize = 24.sp)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color(0xFF05050A)
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                StarryBackground() // Fondo compartido del mockup

                if (currentScreen == "inicio") {
                    // PANTALLA PRINCIPAL
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {

                        // Círculo de Estado
                        val (circleColor, statusText) = when (visualState) {
                            1 -> Pair(Color(0xFF00FF00), "En Reposo")
                            2 -> Pair(Color(0xFFFFD700), "Estado Alerta")
                            3 -> Pair(Color(0xFFFF0000), "Alerta\n¡Gato detectado!")
                            else -> Pair(Color.Gray, "Sistema Apagado")
                        }

                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .background(circleColor.copy(alpha = 0.15f), CircleShape)
                                .padding(16.dp)
                                .background(Color.Black, CircleShape)
                                .border(8.dp, circleColor, CircleShape)
                                .clip(CircleShape)
                                .clickable {
                                    // Click para encender/apagar o forzar prueba
                                    if (!isSystemActive) {
                                        isSystemActive = true
                                        enviarComandoArduino("activar", coroutineScope)
                                    } else {
                                        // Forzar registro para pruebas
                                        catExitCount++
                                        visualState = 3 // Rojo
                                        enviarComandoArduino("registrar_gato", coroutineScope)
                                        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                                        historyLog.add(0, "[$currentTime] Prueba Manual (Total: $catExitCount)")
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Círculo sólido interno
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                            colors = listOf(circleColor, circleColor.copy(alpha = 0.5f))
                                        ),
                                        shape = CircleShape
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Texto de Estado
                        Text(
                            text = statusText,
                            color = if (visualState == 2) circleColor else Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        if (visualState == 0) {
                            Text(
                                text = "Presiona el círculo para activar",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(48.dp))

                        // Tarjeta de Detecciones
                        Card(
                            modifier = Modifier
                                .width(200.dp)
                                .height(120.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.08f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🐾", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Detecciones", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = catExitCount.toString(),
                                    color = Color.White,
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        if (isSystemActive) {
                            TextButton(onClick = {
                                isSystemActive = false
                                enviarComandoArduino("desactivar", coroutineScope)
                            }) {
                                Text("Apagar Sistema", color = Color.Red.copy(alpha = 0.8f))
                            }
                        }
                    }
                } else if (currentScreen == "historial") {
                    // PANTALLA DE HISTORIAL
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Text("Historial de Detecciones", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        if (historyLog.isEmpty()) {
                            Text("No hay detecciones recientes.", color = Color.Gray)
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(historyLog) { log ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
                                    ) {
                                        Text(text = log, color = Color.White, modifier = Modifier.padding(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}