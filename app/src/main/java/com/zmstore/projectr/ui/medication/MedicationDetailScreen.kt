package com.zmstore.projectr.ui.medication

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zmstore.projectr.data.model.Medication
import com.zmstore.projectr.receiver.AlarmReceiver
import com.zmstore.projectr.ui.theme.*
import java.util.concurrent.TimeUnit

import androidx.compose.ui.res.stringResource
import com.zmstore.projectr.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationDetailScreen(
    viewModel: com.zmstore.projectr.ui.MainViewModel,
    medicationId: Int = -1,
    medicationName: String?,
    onBack: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onSave: (Medication) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(medicationName ?: "") }
    var dosage by remember { mutableStateOf("") }
    var stockCount by remember { mutableStateOf("") }
    var interval by remember { mutableStateOf("8") }
    var purpose by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    var sideEffects by remember { mutableStateOf("") }
    var alerts by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Outros") }
    var useCustomTimes by remember { mutableStateOf(false) }
    var customTimes by remember { mutableStateOf("") }
    var showOptionalFields by remember { mutableStateOf(false) }
    var isActive by remember { mutableStateOf(true) }
    var profileId by remember { mutableStateOf(0) }
    var lastTakenTimestamp by remember { mutableLongStateOf(0L) }

    val aiResult by viewModel.aiResearchResult.collectAsState()
    val isAiSearching by viewModel.isAiSearching.collectAsState()
    val interactionWarning by viewModel.interactionWarning.collectAsState()
    val userPrefs by viewModel.userPreferences.collectAsState()

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                name = results[0]
            }
        }
    }

    LaunchedEffect(aiResult) {
        aiResult?.let {
            if (it.name.isNotBlank()) {
                name = it.name
                viewModel.checkInteractions(it.name)
            }
            if (it.dosage.isNotBlank()) dosage = it.dosage
            if (it.purpose.isNotBlank()) purpose = it.purpose
            if (it.instructions.isNotBlank()) instructions = it.instructions
            if (it.sideEffects.isNotBlank()) sideEffects = it.sideEffects
            if (it.alerts.isNotBlank()) alerts = it.alerts
            
            if (it.purpose.isNotBlank() || it.instructions.isNotBlank() || it.sideEffects.isNotBlank() || it.alerts.isNotBlank()) {
                showOptionalFields = true
            }
            viewModel.clearAiResearch()
        }
    }

    LaunchedEffect(medicationId) {
        if (medicationId != -1) {
            val med = viewModel.getMedicationById(medicationId)
            med?.let {
                name = it.name
                dosage = it.dosage
                stockCount = it.stockCount.toString()
                interval = it.intervalHours.toString()
                purpose = it.purpose
                instructions = it.instructions
                sideEffects = it.sideEffects
                alerts = it.alerts
                category = it.category
                customTimes = it.customTimes ?: ""
                useCustomTimes = it.customTimes != null && it.customTimes.isNotBlank()
                isActive = it.isActive
                profileId = it.profileId
                lastTakenTimestamp = it.lastTakenTimestamp
            }
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
                CenterAlignedTopAppBar(
                    title = { Text(if (medicationId == -1) stringResource(R.string.detail_title_add) else stringResource(R.string.detail_title_edit), fontWeight = FontWeight.Bold, color = MedicleanDarkGreen) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = MedicleanDarkGreen)
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Warning Banner
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    color = Color.Red.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.Red.copy(alpha = 0.3f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color.Red)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(stringResource(R.string.detail_warning_title), style = MaterialTheme.typography.labelLarge, color = Color.Red, fontWeight = FontWeight.Black)
                            Text(stringResource(R.string.detail_warning_text), style = MaterialTheme.typography.bodySmall, color = MedicleanTextBlack.copy(alpha = 0.8f))
                        }
                    }
                }

                if (medicationId == -1) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InputOptionButton(icon = Icons.Default.Mic, label = "Voz", modifier = Modifier.weight(1f), onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            }
                            speechRecognizerLauncher.launch(intent)
                        })
                        InputOptionButton(icon = Icons.Default.CameraAlt, label = "Foto", modifier = Modifier.weight(1f), onClick = onNavigateToCamera)
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.detail_field_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MedicleanDarkGreen,
                        unfocusedTextColor = MedicleanDarkGreen,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = MedicleanTeal,
                        unfocusedBorderColor = MedicleanTeal.copy(alpha = 0.3f),
                        focusedLabelColor = MedicleanTeal,
                        unfocusedLabelColor = MedicleanDarkGreen.copy(alpha = 0.6f)
                    )
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = dosage,
                        onValueChange = { dosage = it },
                        label = { Text(stringResource(R.string.detail_field_dosage)) },
                        modifier = Modifier.weight(0.6f),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MedicleanDarkGreen,
                            unfocusedTextColor = MedicleanDarkGreen,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = MedicleanTeal,
                            unfocusedBorderColor = MedicleanTeal.copy(alpha = 0.3f)
                        )
                    )
                    OutlinedTextField(
                        value = stockCount,
                        onValueChange = { stockCount = it },
                        label = { Text(stringResource(R.string.detail_field_stock)) },
                        modifier = Modifier.weight(0.4f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MedicleanDarkGreen,
                            unfocusedTextColor = MedicleanDarkGreen,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = MedicleanTeal,
                            unfocusedBorderColor = MedicleanTeal.copy(alpha = 0.3f)
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = useCustomTimes, onCheckedChange = { useCustomTimes = it }, colors = SwitchDefaults.colors(checkedTrackColor = MedicleanTeal))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Usar horários específicos", color = MedicleanDarkGreen)
                }

                if (useCustomTimes) {
                    OutlinedTextField(
                        value = customTimes,
                        onValueChange = { customTimes = it },
                        label = { Text("Horários (ex: 08:00, 20:00)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MedicleanDarkGreen,
                            unfocusedTextColor = MedicleanDarkGreen,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = MedicleanTeal
                        )
                    )
                } else {
                    OutlinedTextField(
                        value = interval,
                        onValueChange = { interval = it },
                        label = { Text("Intervalo em Horas") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MedicleanDarkGreen,
                            unfocusedTextColor = MedicleanDarkGreen,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = MedicleanTeal
                        )
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Em uso (Receber lembretes)", fontWeight = FontWeight.Bold, color = MedicleanDarkGreen)
                    Switch(checked = isActive, onCheckedChange = { isActive = it }, colors = SwitchDefaults.colors(checkedTrackColor = MedicleanTeal))
                }

                Button(
                    onClick = {
                        val medication = Medication(
                            id = if (medicationId == -1) 0 else medicationId,
                            name = name,
                            dosage = dosage,
                            purpose = purpose,
                            instructions = instructions,
                            sideEffects = sideEffects,
                            alerts = alerts,
                            stockCount = stockCount.toIntOrNull() ?: 0,
                            intervalHours = if (useCustomTimes) 0 else (interval.toIntOrNull() ?: 0),
                            isActive = isActive,
                            category = category,
                            customTimes = if (useCustomTimes) customTimes.trim() else null,
                            profileId = profileId,
                            lastTakenTimestamp = lastTakenTimestamp
                        )
                        onSave(medication)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MedicleanTeal)
                ) {
                    Text(if (medicationId == -1) "SALVAR MEDICAMENTO" else "SALVAR ALTERAÇÕES", fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun InputOptionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.height(64.dp), shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp) {
        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = MedicleanTeal)
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontWeight = FontWeight.Bold, color = MedicleanDarkGreen)
        }
    }
}

@Composable
fun ResearchInfoField(label: String, value: String, onValueChange: (String) -> Unit, isWarning: Boolean = false) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = if (isWarning) Color.Red else MedicleanTeal, fontWeight = FontWeight.Bold)
        TextField(
            value = value, 
            onValueChange = onValueChange, 
            modifier = Modifier.fillMaxWidth(), 
            colors = TextFieldDefaults.colors(
                focusedTextColor = MedicleanDarkGreen, 
                unfocusedTextColor = MedicleanDarkGreen, 
                focusedContainerColor = Color.Transparent, 
                unfocusedContainerColor = Color.Transparent
            )
        )
    }
}
