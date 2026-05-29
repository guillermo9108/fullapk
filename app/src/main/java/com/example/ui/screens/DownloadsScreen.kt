package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.DownloadItem
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.Screen
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val downloads by viewModel.allDownloads.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    var selectedItemForDetail by remember { mutableStateOf<DownloadItem?>(null) }

    val activeList = downloads.filter { it.status == "DOWNLOADING" }
    val completedList = downloads.filter { it.status != "DOWNLOADING" }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950),
        containerColor = Slate950,
        topBar = {
            MediumTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Gestor de Descargas",
                            color = Slate200,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "${downloads.size} archivos registrados",
                            color = Slate400,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.WebView) },
                        modifier = Modifier.testTag("back_to_web")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Volver a Transmisión",
                            tint = Slate200
                        )
                    }
                },
                actions = {
                    // Test Download generator tool
                    IconButton(
                        onClick = { viewModel.triggerTestDownload() },
                        modifier = Modifier.testTag("test_download_action")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Descarga de Prueba",
                            tint = Indigo500
                        )
                    }

                    // Three dots menu
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Opciones",
                                tint = Slate200
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(Slate800)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Limpiar historial", color = Slate200) },
                                onClick = {
                                    showMenu = false
                                    viewModel.clearHistoryOnly()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Eliminar todo", color = Red500, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    showMenu = false
                                    viewModel.clearAllHistoryAndFiles()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = Slate950,
                    titleContentColor = Slate200
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Test Download Helper Hint (Visible if list is empty)
            if (downloads.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Slate700,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No hay descargas registradas",
                            color = Slate200,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Cualquier archivo de video o PDF que descargues desde el reproductor web se listará aquí. Puedes presionar el botón de prueba superior (ícono de insecto) para descargar un fragmento de video de demostración.",
                            color = Slate400,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // SECTION 1: ACTIVE DOWNLOADS
                    if (activeList.isNotEmpty()) {
                        item {
                            Text(
                                text = "Descargando actualmente (${activeList.size})",
                                color = Indigo500,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }

                        items(activeList, key = { it.id }) { item ->
                            ActiveDownloadItemView(item = item, onDelete = { viewModel.deleteItem(item) })
                        }
                    }

                    // SECTION 2: HISTORIC ENTRIES
                    if (completedList.isNotEmpty()) {
                        item {
                            Text(
                                text = "Historial completo (${completedList.size})",
                                color = Slate400,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                            )
                        }

                        items(completedList, key = { it.id }) { item ->
                            HistoricDownloadItemView(
                                item = item,
                                onDelete = { viewModel.deleteItem(item) },
                                onOpen = { openDownloadedFile(context, item) },
                                onTap = { selectedItemForDetail = item }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // Modal Details Sheet Overlay For Offline Access
    selectedItemForDetail?.let { item ->
        DownloadDetailsDialog(
            item = item,
            onClose = { selectedItemForDetail = null },
            onOpen = { openDownloadedFile(context, item) },
            onShare = {
                selectedItemForDetail = null
                item.localUri?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "*/*"
                            putExtra(Intent.EXTRA_STREAM, androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            ))
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Compartir archivo"))
                    }
                }
            }
        )
    }
}

// 1. ACTIVE DOWNLOAD CARD Component
@Composable
fun ActiveDownloadItemView(
    item: DownloadItem,
    onDelete: (DownloadItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Spinning hourglass for progress loading
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Slate700),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Descargando",
                        tint = Indigo500,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.filename,
                        color = Slate200,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Tamaño: ${formatBytes(item.sizeBytes)}",
                        color = Slate400,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = { onDelete(item) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancelar",
                        tint = Slate400,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Percentage bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { item.progress / 100f },
                    color = Indigo500,
                    trackColor = Slate700,
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "${item.progress}%",
                    color = Slate200,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// 2. HISTORIC CARD Component
@Composable
fun HistoricDownloadItemView(
    item: DownloadItem,
    onDelete: (DownloadItem) -> Unit,
    onOpen: (DownloadItem) -> Unit,
    onTap: () -> Unit
) {
    val context = LocalContext.current
    val isCompleted = item.status == "COMPLETED"
    val colorAccent = if (isCompleted) Color(0xFF10B981) else Color(0xFFEF4444) // Emerald vs Red
    val statusIcon = if (isCompleted) Icons.Default.Check else Icons.Default.Warning

    val fileLocal = item.localUri?.let { File(it) }
    val isFileAvailable = fileLocal?.exists() == true

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail container
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Slate700),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted && isFileAvailable && item.localUri != null) {
                        val extension = fileLocal.extension.lowercase()
                        val isImage = extension in listOf("jpg", "jpeg", "png", "webp", "gif")
                        
                        if (isImage) {
                            coil.compose.AsyncImage(
                                model = coil.request.ImageRequest.Builder(LocalContext.current)
                                    .data(fileLocal)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Miniatura",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            val iconRes = when {
                                extension in listOf("mp4", "mkv", "webm", "3gp") -> Icons.Default.PlayArrow
                                extension in listOf("mp3", "wav", "ogg", "m4a") -> Icons.Default.PlayArrow
                                else -> Icons.Default.Info
                            }
                            Icon(
                                imageVector = iconRes,
                                contentDescription = null,
                                tint = Slate200,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = colorAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.filename,
                        color = Slate200,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatBytes(item.sizeBytes),
                        color = Slate400,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isCompleted) "Completado" else "Fallido",
                            color = colorAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(text = "•", color = Slate700, fontSize = 11.sp)
                        Text(
                            text = formatRelativeTime(item.timestamp),
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isCompleted && isFileAvailable) {
                    androidx.compose.material3.Button(
                        onClick = { onOpen(item) },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Indigo500
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("open_file_${item.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Abrir",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            val file = File(item.localUri!!)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "*/*"
                                putExtra(Intent.EXTRA_STREAM, androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                ))
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Compartir archivo"))
                        },
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = Slate200,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Compartir",
                                color = Slate200,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                IconButton(
                    onClick = { onDelete(item) },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = Slate400,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// 3. OFFLINE DETAIL OVERLAY DIALOG
@Composable
fun DownloadDetailsDialog(
    item: DownloadItem,
    onClose: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit
) {
    val isCompleted = item.status == "COMPLETED"
    val fileLocal = item.localUri?.let { File(it) }
    val isFileAvailable = fileLocal?.exists() == true

    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier.fillMaxWidth(0.95f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Slate800),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header details info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Detalles del Archivo",
                        color = Slate200,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Slate400,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Detail elements list
                DetailRow(label = "Nombre", value = item.filename)
                DetailRow(label = "Capacidad física", value = formatBytes(item.sizeBytes))
                DetailRow(
                    label = "Estado",
                    value = if (isCompleted) "Descargado con éxito" else "Descarga fallida",
                    valueColor = if (isCompleted) Color(0xFF10B981) else Color(0xFFEF4444)
                )

                // Render system path if file exists natively
                if (isCompleted && isFileAvailable && item.localUri != null) {
                    DetailRow(label = "Ruta local fija", value = item.localUri, maxLines = 3)
                } else if (isCompleted && !isFileAvailable) {
                    DetailRow(
                        label = "Ruta local fija",
                        value = "Archivo físico eliminado del disco",
                        valueColor = Color(0xFFEF4444)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Slate700, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Share / Play triggering
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCompleted && isFileAvailable) {
                        androidx.compose.material3.TextButton(
                            onClick = onOpen,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Indigo500,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Abrir", color = Indigo500, fontWeight = FontWeight.Bold)
                        }
                        
                        IconButton(onClick = onShare) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartir",
                                tint = Slate400
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Indigo500)
                            .clickable { onClose() }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Aceptar",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = Slate200,
    maxLines: Int = 1
) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(text = label, color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.sp,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Byte parser helper
fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 Bytes"
    val k = 1024.0
    val sizes = arrayOf("Bytes", "KB", "MB", "GB", "TB")
    val i = kotlin.math.floor(kotlin.math.log(bytes.toDouble(), k)).toInt()
    if (i >= sizes.size) return "Large file"
    return String.format(Locale.US, "%.1f %s", bytes / java.lang.Math.pow(k, i.toDouble()), sizes[i])
}

// Function to open downloaded files natively with Intents
private fun openDownloadedFile(context: android.content.Context, item: DownloadItem) {
    val path = item.localUri ?: return
    val file = File(path)
    if (!file.exists()) {
        android.widget.Toast.makeText(context, "El archivo físico no existe", android.widget.Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val ext = file.extension.lowercase()
        val mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: when {
            ext == "mp4" || ext == "mkv" || ext == "webm" || ext == "3gp" -> "video/*"
            ext == "mp3" || ext == "wav" || ext == "ogg" || ext == "m4a" -> "audio/*"
            ext == "jpg" || ext == "jpeg" || ext == "png" || ext == "webp" || ext == "gif" -> "image/*"
            ext == "pdf" -> "application/pdf"
            ext == "apk" -> "application/vnd.android.package-archive"
            else -> "*/*"
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "No se pudo abrir el archivo: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
    }
}

// Relative timestamp creator
fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    if (diff < 60000) return "Hace un momento"
    val minutes = diff / 60000
    if (minutes < 60) return "Hace $minutes m"
    val hours = minutes / 60
    if (hours < 24) return "Hace $hours h"
    val days = hours / 24
    if (days < 7) return "Hace $days d"

    // Absolute date fallback
    return try {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        "Reciente"
    }
}
