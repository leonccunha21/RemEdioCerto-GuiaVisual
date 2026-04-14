package com.zmstore.projectr.ui.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
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
    
    val upcomingDoses = remember(medications) {
        medications.filter { it.isActive && (it.intervalHours > 0 || !it.customTimes.isNullOrBlank()) }
            .sortedBy { 
                if (it.lastTakenTimestamp == 0L) 0L 
                else it.lastTakenTimestamp + (it.intervalHours * 3600 * 1000)
            }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    if (isSystemInDarkTheme())
                        listOf(Color(0xFF0F1716), Color(0xFF17201F))
                    else
                        listOf(MedicleanWhite, MedicleanMint)
                )
            ),
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("HISTÓRICO", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp), color = MedicleanDarkGreen)
                        selectedProfile?.let {
                            Text(it.name.uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), color = MedicleanTeal)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.padding(8.dp).background(if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.1f) else Color.White, RoundedCornerShape(12.dp)).size(40.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = MedicleanTeal)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
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
                SectionHeader("PRÓXIMAS DOSES", Icons.Default.Schedule)
            }
            
            if (upcomingDoses.isEmpty()) {
                item {
                    EmptyStatePlaceholder("Nenhuma dose agendada")
                }
            } else {
                items(upcomingDoses) { med ->
                    UpcomingDoseItem(med, viewModel.getMedicationCountdown(med))
                }
            }

            // History Section
            item {
                SectionHeader("ATIVIDADES RECENTES", Icons.Default.History)
            }

            if (history.isEmpty()) {
                item {
                    EmptyStatePlaceholder("Nenhum registro encontrado")
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

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MedicleanTeal, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp),
            fontWeight = FontWeight.Black,
            color = MedicleanDarkGreen
        )
    }
}

@Composable
fun EmptyStatePlaceholder(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.3f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MedicleanTeal.copy(alpha = 0.05f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MedicleanDarkGreen.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun QrCodeDialog(onDismiss: () -> Unit, userName: String) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MedicleanTeal),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("ENTENDI", fontWeight = FontWeight.Black)
            }
        },
        title = {
            Text("COMPARTILHAR", fontWeight = FontWeight.Black, color = MedicleanDarkGreen, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "O médico pode escanear para acessar seu prontuário resumido.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MedicleanDarkGreen.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                Surface(
                    modifier = Modifier.size(220.dp),
                    color = Color.White,
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(20.dp)) {
                        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.fillMaxSize(), tint = MedicleanDarkGreen)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "ID: ${userName.replace(" ", "").lowercase()}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MedicleanTeal,
                    fontWeight = FontWeight.Black
                )
            }
        },
        shape = RoundedCornerShape(32.dp),
        containerColor = MedicleanWhite
    )
}

@Composable
fun WeeklyAdherenceChart(history: List<DoseHistory>) {
    val counts = IntArray(7) { 0 }
    val daysLabels = Array(7) { "" }
    val dayFormatter = SimpleDateFormat("EEE", Locale.getDefault())
    
    for (i in 0..6) {
        val checkCal = Calendar.getInstance()
        checkCal.add(Calendar.DAY_OF_YEAR, - (6 - i))
        daysLabels[i] = dayFormatter.format(checkCal.time).uppercase()
        counts[i] = history.count { 
            val doseCal = Calendar.getInstance()
            doseCal.timeInMillis = it.timestamp
            doseCal.get(Calendar.DAY_OF_YEAR) == checkCal.get(Calendar.DAY_OF_YEAR)
        }
    }
    
    val maxCount = (counts.maxOrNull() ?: 1).coerceAtLeast(1)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(80.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            counts.forEachIndexed { index, count ->
                val barHeight = (count.toFloat() / maxCount) * 80f
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .height(barHeight.dp.coerceAtLeast(4.dp))
                            .background(if (index == 6) Color.White else Color.White.copy(alpha = 0.3f), CircleShape)
                    )
                }
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            daysLabels.forEach { label ->
                Text(
                    label, 
                    modifier = Modifier.weight(1f), 
                    textAlign = TextAlign.Center, 
                    style = MaterialTheme.typography.labelSmall, 
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Black
                )
            }
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MedicleanTeal,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("PONTUAÇÃO", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Black)
                    Text("${(totalTaken * 10).coerceAtMost(100)}%", style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.Black)
                }
                Surface(color = Color.White.copy(alpha = 0.2f), shape = CircleShape) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.padding(12.dp).size(24.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            WeeklyAdherenceChart(history)
            
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    onClick = onExportPdf,
                    modifier = Modifier.weight(1f),
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = MedicleanTeal, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("PDF", color = MedicleanTeal, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
                Surface(
                    onClick = onShareQr,
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QrCode, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("SHARE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItem(dose: DoseHistory) {
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dayFormat = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = if (isSystemInDarkTheme()) Color(0xFF1E2A28) else Color.White,
        border = BorderStroke(1.dp, MedicleanTeal.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MedicleanTeal.copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = MedicleanTeal)
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(dose.medicationName, fontWeight = FontWeight.Black, color = MedicleanDarkGreen)
                Text(
                    "${dayFormat.format(Date(dose.timestamp))} às ${dateFormat.format(Date(dose.timestamp))}", 
                    style = MaterialTheme.typography.bodySmall, 
                    color = MedicleanDarkGreen.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (!dose.note.isNullOrBlank()) {
                Icon(Icons.Default.Info, contentDescription = "Nota", tint = MedicleanGold, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun UpcomingDoseItem(medication: Medication, countdown: String) {
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val nextDoseTimestamp = medication.lastTakenTimestamp + (medication.intervalHours * 3600 * 1000)
    val nextTime = if (medication.lastTakenTimestamp == 0L) "--:--" else dateFormat.format(Date(nextDoseTimestamp))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.05f) else Color.White,
        border = BorderStroke(1.dp, MedicleanTeal.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MedicleanGold.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MedicleanGold, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(medication.name, fontWeight = FontWeight.Black, color = MedicleanDarkGreen)
                    Text("Hoje às $nextTime", style = MaterialTheme.typography.bodySmall, color = MedicleanDarkGreen.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                }
            }
            
            Surface(
                color = if (countdown == "Atrasado") MedicleanError.copy(alpha = 0.1f) else MedicleanTeal.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = countdown.uppercase(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (countdown == "Atrasado") MedicleanError else MedicleanTeal,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

