package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.AppViewModel

val Slate950 = Color(0xFF0F172A)
val Slate800 = Color(0xFF1E293B)
val Slate700 = Color(0xFF334155)
val Slate400 = Color(0xFF94A3B8)
val Slate200 = Color(0xFFE2E8F0)
val Indigo500 = Color(0xFF6366F1)
val Red500 = Color(0xFFEF4444)

@Composable
fun ConfigScreen(viewModel: AppViewModel) {
    val savedIp by viewModel.ipAddressState.collectAsState()
    val savedPort by viewModel.portState.collectAsState()

    var ipAddress by remember(savedIp) { mutableStateOf(savedIp) }
    var port by remember(savedPort) { mutableStateOf(savedPort) }
    var errorMsg by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950),
        containerColor = Slate950
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // SP Animated circular badge
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(Indigo500)
                    .border(3.dp, Slate200, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SP",
                    color = Color.White,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = (-1).sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "StreamPay Client",
                color = Slate200,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Ingresa la dirección IP de tu servidor de video streaming para conectarte de inmediato.",
                color = Slate400,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate800)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Configuración del Servidor",
                        color = Slate200,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // IP / URL textfield
                    OutlinedTextField(
                        value = ipAddress,
                        onValueChange = {
                            ipAddress = it
                            errorMsg = ""
                        },
                        label = { Text("IP del Servidor o URL") },
                        placeholder = { Text("Ej. 192.168.43.101") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = Slate400
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Indigo500,
                            unfocusedBorderColor = Slate700,
                            focusedTextColor = Slate200,
                            unfocusedTextColor = Slate200,
                            focusedContainerColor = Slate950,
                            unfocusedContainerColor = Slate950,
                            focusedLabelColor = Indigo500,
                            unfocusedLabelColor = Slate400
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ip_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Port textfield
                    OutlinedTextField(
                        value = port,
                        onValueChange = {
                            port = it
                            errorMsg = ""
                        },
                        label = { Text("Puerto (Opcional)") },
                        placeholder = { Text("Ej. 3001") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = Slate400
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Indigo500,
                            unfocusedBorderColor = Slate700,
                            focusedTextColor = Slate200,
                            unfocusedTextColor = Slate200,
                            focusedContainerColor = Slate950,
                            unfocusedContainerColor = Slate950,
                            focusedLabelColor = Indigo500,
                            unfocusedLabelColor = Slate400
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("port_input")
                    )

                    if (errorMsg.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMsg,
                            color = Red500,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val cleanIp = ipAddress.trim()
                            if (cleanIp.isBlank()) {
                                errorMsg = "La dirección IP o URL es obligatoria"
                            } else {
                                viewModel.saveConfig(cleanIp, port.trim())
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Indigo500,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("connect_button")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Conectar y Transmitir",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Storage Location Card
            val downloadLocation by viewModel.downloadLocationState.collectAsState()
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate800)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Almacenamiento de Descargas",
                        color = Slate200,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = "Elige dónde guardar los videos descargados en tu dispositivo.",
                        color = Slate400,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Internal Option
                        val isInternal = downloadLocation == com.example.data.pref.ServerConfig.VAL_LOCATION_INTERNAL
                        Button(
                            onClick = { viewModel.saveDownloadLocation(com.example.data.pref.ServerConfig.VAL_LOCATION_INTERNAL) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isInternal) Indigo500 else Slate700,
                                contentColor = Slate200
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("storage_internal_button")
                        ) {
                            Text(
                                text = "Interno",
                                fontWeight = if (isInternal) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        // SD Card Option
                        val isSd = downloadLocation == com.example.data.pref.ServerConfig.VAL_LOCATION_SD_CARD
                        Button(
                            onClick = { viewModel.saveDownloadLocation(com.example.data.pref.ServerConfig.VAL_LOCATION_SD_CARD) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSd) Indigo500 else Slate700,
                                contentColor = Slate200
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("storage_sd_button")
                        ) {
                            Text(
                                text = "Tarjeta SD",
                                fontWeight = if (isSd) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cache Control Card
            val keepCache by viewModel.keepCacheState.collectAsState()
            val cacheCleanInterval by viewModel.cacheCleanIntervalState.collectAsState()
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate800)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Configuración de Caché",
                        color = Slate200,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = "Guarda localmente el 100% de lo que veas para evitar lentitud en la carga y asegurar caching continuo.",
                        color = Slate400,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Keep Cache selector row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.saveKeepCache(true) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (keepCache) Indigo500 else Slate700,
                                contentColor = Slate200
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("cache_keep_enabled_button")
                        ) {
                            Text(
                                text = "Activar",
                                fontWeight = if (keepCache) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        Button(
                            onClick = { viewModel.saveKeepCache(false) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!keepCache) Indigo500 else Slate700,
                                contentColor = Slate200
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("cache_keep_disabled_button")
                        ) {
                            Text(
                                text = "Desactivar",
                                fontWeight = if (!keepCache) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    if (keepCache) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Limpieza Automática cada:",
                            color = Slate200,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // 4 options: Nunca, 1H, 24H, 7D
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val optNever = cacheCleanInterval == "NUNCA"
                                Button(
                                    onClick = { viewModel.saveCacheCleanInterval("NUNCA") },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (optNever) Indigo500 else Slate700,
                                        contentColor = Slate200
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .testTag("interval_never_button")
                                ) {
                                    Text("Nunca", fontSize = 12.sp, fontWeight = if (optNever) FontWeight.Bold else FontWeight.Normal)
                                }

                                val opt1h = cacheCleanInterval == "1H"
                                Button(
                                    onClick = { viewModel.saveCacheCleanInterval("1H") },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (opt1h) Indigo500 else Slate700,
                                        contentColor = Slate200
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .testTag("interval_1h_button")
                                ) {
                                    Text("1 hora", fontSize = 12.sp, fontWeight = if (opt1h) FontWeight.Bold else FontWeight.Normal)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val opt24h = cacheCleanInterval == "24H"
                                Button(
                                    onClick = { viewModel.saveCacheCleanInterval("24H") },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (opt24h) Indigo500 else Slate700,
                                        contentColor = Slate200
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .testTag("interval_24h_button")
                                ) {
                                    Text("24 horas", fontSize = 12.sp, fontWeight = if (opt24h) FontWeight.Bold else FontWeight.Normal)
                                }

                                val opt7d = cacheCleanInterval == "7D"
                                Button(
                                    onClick = { viewModel.saveCacheCleanInterval("7D") },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (opt7d) Indigo500 else Slate700,
                                        contentColor = Slate200
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .testTag("interval_7d_button")
                                ) {
                                    Text("7 días", fontSize = 12.sp, fontWeight = if (opt7d) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.clearWebCache() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Red500
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Red500, RoundedCornerShape(12.dp))
                            .height(46.dp)
                            .testTag("clear_cache_now_button")
                    ) {
                        Text(
                            text = "Limpiar Todo el Caché Ahora",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // User Info Setup Help Board
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Slate800.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Indigo500,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Ayuda de Conexión Local",
                            color = Slate200,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "• Asegúrate de que el servidor y tu teléfono estén en la misma red Wi-Fi.\n" +
                                    "• Habitualmente el puerto por defecto de StreamPay es el 3001.\n" +
                                    "• Si utilizas una URL segura externa (HTTPS), puedes dejar el puerto en blanco.",
                            color = Slate400,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
