package com.zmstore.projectr.ui.medication

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
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
import com.zmstore.projectr.util.TtsHelper

import androidx.compose.ui.res.stringResource
import com.zmstore.projectr.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MedicationDetailScreen(
    viewModel: com.zmstore.projectr.ui.MainViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
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
    var iconType by remember { mutableStateOf("pill") }
    var iconColor by remember { mutableIntStateOf(0xFF00A79D.toInt()) }

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

    LaunchedEffect(medicationId, medicationName) {
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
                iconType = it.iconType
                iconColor = it.iconColor
            }
        }
    }

    with(sharedTransitionScope) {
        Scaffold(
            containerColor = Color.White,
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            if (medicationId == -1) "ADICIONAR" else "EDITAR", 
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp),
                            color = MedicleanDarkGreen
                        ) 
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White),
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.padding(8.dp).background(if (false) Color.White.copy(alpha = 0.1f) else Color.White, RoundedCornerShape(12.dp)).size(40.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = MedicleanTeal)
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
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Shared Visual Header
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (false) Color(0xFF1E2A28) else Color.White,
                    shape = RoundedCornerShape(28.dp),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(iconColor).copy(alpha = 0.1f),
                            modifier = Modifier
                                .size(80.dp)
                                .sharedElement(
                                    rememberSharedContentState(key = "medication_icon_$medicationId"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when(iconType) {
                                        "capsule" -> Icons.Default.Adjust
                                        "drops" -> Icons.Default.WaterDrop
                                        "liquid" -> Icons.Default.Vaccines
                                        else -> Icons.Default.Medication
                                    },
                                    contentDescription = null,
                                    tint = Color(iconColor),
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(20.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name.ifBlank { "Nome do Remédio" },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = MedicleanDarkGreen,
                                modifier = Modifier.sharedElement(
                                    rememberSharedContentState(key = "medication_title_$medicationId"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                            )
                            Text(
                                text = dosage.ifBlank { "Defina a dosagem" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MedicleanDarkGreen.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // AI Tools & Input Methods
                if (medicationId == -1) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InputOptionButton(
                            icon = Icons.Default.Mic, 
                            label = "Voz", 
                            color = MedicleanTeal,
                            modifier = Modifier.weight(1f), 
                            onClick = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                }
                                speechRecognizerLauncher.launch(intent)
                            }
                        )
                        InputOptionButton(
                            icon = Icons.Default.CameraAlt, 
                            label = "Foto", 
                            color = MedicleanGold,
                            modifier = Modifier.weight(1f), 
                            onClick = onNavigateToCamera
                        )
                    }
                }

                // Visual Customizer Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (false) Color.White.copy(alpha = 0.05f) else MedicleanMint.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                        .padding(16.dp)
                ) {
                    Text("IDENTIDADE VISUAL", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = MedicleanTeal, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val types = listOf("pill" to Icons.Default.Medication, "capsule" to Icons.Default.Adjust, "drops" to Icons.Default.WaterDrop, "liquid" to Icons.Default.Vaccines)
                        types.forEach { (type, icon) ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(if (iconType == type) MedicleanTeal else Color.White.copy(alpha = 0.3f), CircleShape)
                                    .clickable { iconType = type },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = if (iconType == type) Color.White else MedicleanDarkGreen, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val colors = listOf(0xFF00A79D.toInt(), 0xFFE91E63.toInt(), 0xFF2196F3.toInt(), 0xFF4CAF50.toInt(), 0xFFFF9800.toInt(), 0xFF9C27B0.toInt())
                        colors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(color), CircleShape)
                                    .clickable { iconColor = color }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (iconColor == color) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }

                // Main Fields
                PremiumTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nome do Medicamento",
                    icon = Icons.Default.Medication,
                    trailingIcon = {
                        if (isAiSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MedicleanTeal)
                        } else if (name.isNotBlank() && userPrefs.geminiApiKey.isNotBlank()) {
                            IconButton(onClick = { viewModel.fetchMedicationInfo(name) }) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "IA", tint = MedicleanTeal)
                            }
                        }
                    }
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PremiumTextField(
                        value = dosage,
                        onValueChange = { dosage = it },
                        label = "Dosagem",
                        modifier = Modifier.weight(1f)
                    )
                    PremiumTextField(
                        value = stockCount,
                        onValueChange = { stockCount = it },
                        label = "Estoque",
                        modifier = Modifier.weight(0.8f),
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                }

                // Scheduling Section
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (false) Color.White.copy(alpha = 0.03f) else Color.White.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MedicleanTeal.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = MedicleanTeal, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("AGENDAMENTO", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = MedicleanTeal)
                            }
                            Switch(
                                checked = useCustomTimes, 
                                onCheckedChange = { useCustomTimes = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = MedicleanTeal)
                            )
                        }
                        
                        Text(
                            if(useCustomTimes) "Defina horários exatos (ex: 08:00, 20:00)" else "Defina o intervalo entre as doses (horas)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MedicleanDarkGreen.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        if (useCustomTimes) {
                            PremiumTextField(value = customTimes, onValueChange = { customTimes = it }, label = "Ex: 08:30, 20:30")
                        } else {
                            PremiumTextField(value = interval, onValueChange = { interval = it }, label = "Horas", keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        }
                    }
                }

                // Active Status
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isActive) MedicleanTeal.copy(alpha = 0.05f) else Color.Gray.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(20.dp),
                    onClick = { isActive = !isActive }
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (isActive) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff, contentDescription = null, tint = if (isActive) MedicleanTeal else Color.Gray)
                            Spacer(Modifier.width(12.dp))
                            Text(if (isActive) "Lembretes Ativos" else "Lembretes Silenciados", fontWeight = FontWeight.Bold, color = MedicleanDarkGreen)
                        }
                        Switch(checked = isActive, onCheckedChange = { isActive = it }, colors = SwitchDefaults.colors(checkedTrackColor = MedicleanTeal))
                    }
                }

                // AI Advanced Section
                Column(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = { showOptionalFields = !showOptionalFields },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MedicleanTeal, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (showOptionalFields) "Ocultar Assistente IA" else "Ver Detalhes Assistidos por IA", color = MedicleanTeal, fontWeight = FontWeight.Black)
                            Icon(if (showOptionalFields) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = MedicleanTeal)
                        }
                    }

                    AnimatedVisibility(visible = showOptionalFields) {
                        val ttsHelper = remember { TtsHelper(context) }
                        DisposableEffect(Unit) {
                            onDispose { ttsHelper.shutdown() }
                        }

                        Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Surface(
                                onClick = {
                                    val textToSpeak = buildString {
                                        append("Informações sobre $name. ")
                                        if (purpose.isNotBlank()) append("Serve para: $purpose. ")
                                        if (instructions.isNotBlank()) append("Como usar: $instructions. ")
                                        if (alerts.isNotBlank()) append("Atenção: $alerts. ")
                                    }
                                    ttsHelper.speak(textToSpeak)
                                },
                                color = MedicleanTeal,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Ouvir Orientação da IA", color = Color.White, fontWeight = FontWeight.Black)
                                }
                            }

                            PremiumTextField(value = purpose, onValueChange = { purpose = it }, label = "Para que serve", isMultiLine = true)
                            PremiumTextField(value = instructions, onValueChange = { instructions = it }, label = "Como tomar / Instruções", isMultiLine = true)
                            PremiumTextField(value = sideEffects, onValueChange = { sideEffects = it }, label = "Efeitos Colaterais", isMultiLine = true)
                            
                            // Interaction Warning Card
                            if (interactionWarning != null) {
                                Surface(
                                    color = MedicleanError.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(1.dp, MedicleanError.copy(alpha = 0.2f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = MedicleanError, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("CONFLITO DETECTADO", fontWeight = FontWeight.Black, color = MedicleanError, style = MaterialTheme.typography.labelLarge)
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Text(interactionWarning!!, color = MedicleanDarkGreen, style = MaterialTheme.typography.bodySmall)
                                        TextButton(onClick = { viewModel.clearInteractionWarning() }, modifier = Modifier.align(Alignment.End)) {
                                            Text("IGNORAR", color = MedicleanError, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

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
                            lastTakenTimestamp = lastTakenTimestamp,
                            iconType = iconType,
                            iconColor = iconColor
                        )
                        onSave(medication)
                    },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MedicleanTeal)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(if (medicationId == -1) "SALVAR MEDICAMENTO" else "ATUALIZAR DADOS", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            }
        }
    }
}

@Composable
fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
    isMultiLine: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = MedicleanDarkGreen.copy(alpha = 0.5f), fontWeight = FontWeight.Bold) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        singleLine = !isMultiLine,
        minLines = if (isMultiLine) 3 else 1,
        leadingIcon = icon?.let { { Icon(it, contentDescription = null, tint = MedicleanTeal) } },
        trailingIcon = trailingIcon,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MedicleanDarkGreen,
            unfocusedTextColor = MedicleanDarkGreen,
            focusedContainerColor = if (false) Color.White.copy(alpha = 0.05f) else Color.White,
            unfocusedContainerColor = if (false) Color.White.copy(alpha = 0.05f) else Color.White,
            focusedBorderColor = MedicleanTeal,
            unfocusedBorderColor = Color.Transparent,
            cursorColor = MedicleanTeal
        )
    )
}

@Composable
fun InputOptionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick, 
        modifier = modifier.height(60.dp), 
        shape = RoundedCornerShape(18.dp), 
        color = if (false) Color.White.copy(alpha = 0.05f) else Color.White, 
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.width(10.dp))
            Text(label, fontWeight = FontWeight.Black, color = MedicleanDarkGreen)
        }
    }
}
