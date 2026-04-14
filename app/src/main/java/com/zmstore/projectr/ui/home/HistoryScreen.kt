package com.zmstore.projectr.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zmstore.projectr.ui.theme.*
import com.zmstore.projectr.data.model.DoseHistory
import com.zmstore.projectr.data.model.Medication
import com.zmstore.projectr.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.res.stringResource
import com.zmstore.projectr.R
import androidx.compose.runtime.*
import com.zmstore.projectr.util.PdfExportHelper
import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val history by viewModel.doseHistory.collectAsState()
    val medications by viewModel.medications.collectAsState()
    val selectedProfile by viewModel.selectedProfile.collectAsState()
    var showQrDialog by remember { mutableStateOf(false) }
    
    val adherenceRate = remember(history, medications) {
        if (medications.isEmpty()) 0f
        else {
            val totalTaken = history.size
            if (totalTaken == 0) 0f else 100f // Placeholder
        }
    }

    val upcomingDoses = remember(medications) {
        medications.filter { it.isActive && (it.intervalHours > 0 || !it.customTimes.isNullOrBlank()) }
            .sortedBy { 
                if (it.lastTakenTimestamp == 0L) 0L 
                else it.lastTakenTimestamp + (it.intervalHours * 3600 * 1000)
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MedicleanMint)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { 
                        Column {
                            Text(stringResource(R.string.history_title), fontWeight = FontWeight.Bold, color = MedicleanDarkGreen)
                            selectedProfile?.let {
                                Text(it.name, style = MaterialTheme.typography.labelSmall, color = MedicleanTeal)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.camera_btn_cancel), tint = MedicleanDarkGreen)
                        }
                    }
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Statistics Section
                item {
                    AdherenceStatsCard(
                        totalTaken = history.size,
                        activeMeds = medications.count { it.isActive },
                        history = history,
                        onExportPdf = {
                            PdfExportHelper.exportAdherenceReport(context, history, medications, selectedProfile)
                        },
                        onShareQr = { showQrDialog = true }
                    )
                }

                // Upcoming Section
                item {
                    Text(
                        text = "PRÓXIMAS DOSES",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MedicleanDarkGreen.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                }
                
                if (upcomingDoses.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "Nenhuma dose agendada para hoje.",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MedicleanDarkGreen.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    items(upcomingDoses) { med ->
                        UpcomingDoseItem(med, viewModel.getMedicationCountdown(med))
                    }
                }

                // History Section
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "HISTÓRICO RECENTE",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MedicleanDarkGreen.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                }

                if (history.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.history_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MedicleanDarkGreen.copy(alpha = 0.6f),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    items(history.reversed()) { dose ->
                        HistoryItem(dose)
                    }
                }
            }
        }
        
        if (showQrDialog) {
            QrCodeDialog(onDismiss = { showQrDialog = false }, userName = selectedProfile?.name ?: "Usuário")
        }
    }
}

@Composable
fun QrCodeDialog(onDismiss: () -> Unit, userName: String) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", color = MedicleanTeal, fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Text("Compartilhar Histórico", fontWeight = FontWeight.Bold, color = MedicleanDarkGreen)
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "O médico pode escanear este código para acessar seu histórico clínico resumido gerado pela IA.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MedicleanDarkGreen.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                Surface(
                    modifier = Modifier.size(200.dp),
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, MedicleanTeal.copy(alpha = 0.2f))
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.fillMaxSize(), tint = MedicleanTeal)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val dotSize = 4.dp.toPx()
                            val spacing = 8.dp.toPx()
                            drawCircle(MedicleanDarkGreen, dotSize, Offset(spacing, spacing))
                            drawCircle(MedicleanDarkGreen, dotSize, Offset(size.width - spacing, spacing))
                            drawCircle(MedicleanDarkGreen, dotSize, Offset(spacing, size.height - spacing))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "remediocerto.ai/v1/${userName.replace(" ", "").lowercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MedicleanTeal,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White
    )
}

@Composable
fun WeeklyAdherenceChart(history: List<DoseHistory>) {
    val calendar = Calendar.getInstance()
    val today = calendar.get(Calendar.DAY_OF_YEAR)
    val currentYear = calendar.get(Calendar.YEAR)
    
    // Last 7 days counts
    val counts = IntArray(7) { 0 }
    val daysLabels = Array(7) { "" }
    val dayFormatter = SimpleDateFormat("EEE", Locale.getDefault())
    
    for (i in 0..6) {
        val checkCal = Calendar.getInstance()
        checkCal.add(Calendar.DAY_OF_YEAR, - (6 - i))
        val dayOfYear = checkCal.get(Calendar.DAY_OF_YEAR)
        val year = checkCal.get(Calendar.YEAR)
        
        daysLabels[i] = dayFormatter.format(checkCal.time).uppercase()
        counts[i] = history.count { 
            val doseCal = Calendar.getInstance()
            doseCal.timeInMillis = it.timestamp
            doseCal.get(Calendar.DAY_OF_YEAR) == dayOfYear && doseCal.get(Calendar.YEAR) == year
        }
    }
    
    val maxCount = (counts.maxOrNull() ?: 1).coerceAtLeast(1)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Text(
            text = "Doses nos Últimos 7 Dias",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val spacing = size.width / 7
                val barWidth = spacing * 0.4f
                val chartHeight = size.height
                
                for (i in 0..6) {
                    val barHeight = (counts[i].toFloat() / maxCount) * chartHeight
                    val x = i * spacing + (spacing - barWidth) / 2
                    
                    // Draw Bar
                    drawRoundRect(
                        color = if (i == 6) Color.White else Color.White.copy(alpha = 0.3f),
                        topLeft = Offset(x, chartHeight - barHeight),
                        size = Size(barWidth, barHeight.coerceAtLeast(4.dp.toPx())),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
            }
        }
        
        ChartLabels(daysLabels)
    }
}

// Fixed Row implementation for labels
@Composable
fun ChartLabels(labels: Array<String>) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        labels.forEachIndexed { index, label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (index == 6) Color.White else Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun AdherenceStatsCard(
    totalTaken: Int, 
    activeMeds: Int, 
    history: List<DoseHistory>,
    onExportPdf: () -> Unit,
    onShareQr: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MedicleanTeal),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ADESÃO GERAL",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (activeMeds == 0) "100%" else "${(totalTaken.coerceAtMost(100))}%", // Placeholder logic
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                fontWeight = FontWeight.Black
            )
            
            WeeklyAdherenceChart(history)
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Total Doses", totalTaken.toString())
                StatItem("Ativos", activeMeds.toString())
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onExportPdf,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = MedicleanTeal, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Exportar Relatório (PDF)", color = MedicleanTeal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedButton(
                onClick = onShareQr,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compartilhar QR Code (IA Link)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
fun HistoryItem(dose: DoseHistory) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val formattedDate = dateFormat.format(Date(dose.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MedicleanTeal.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dose.medicationName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MedicleanDarkGreen
                )
                Text(
                    text = stringResource(R.string.history_confirmed_at, formattedDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MedicleanDarkGreen.copy(alpha = 0.6f)
                )
                
                if (!dose.note.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sintoma/Nota: ${dose.note}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MedicleanTeal,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            // Badge design
            Surface(
                color = MedicleanTeal,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.history_status_taken),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun UpcomingDoseItem(medication: Medication, countdown: String) {
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val nextDoseTimestamp = medication.lastTakenTimestamp + (medication.intervalHours * 3600 * 1000)
    val nextTime = if (medication.lastTakenTimestamp == 0L) "--:--" else dateFormat.format(Date(nextDoseTimestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MedicleanTeal.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = medication.name, fontWeight = FontWeight.Bold, color = MedicleanDarkGreen)
                Text(text = "Hoje às $nextTime", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Surface(
                color = if (countdown == "Atrasado") Color.Red.copy(alpha = 0.1f) else MedicleanTeal.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = countdown,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (countdown == "Atrasado") Color.Red else MedicleanTeal,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
