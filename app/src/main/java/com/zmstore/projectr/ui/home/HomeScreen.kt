package com.zmstore.projectr.ui.home

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zmstore.projectr.R
import kotlinx.coroutines.launch
import com.zmstore.projectr.data.model.Medication
import com.zmstore.projectr.data.model.Profile
import com.zmstore.projectr.ui.MainViewModel
import com.zmstore.projectr.ui.theme.*

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onNavigateToCamera: () -> Unit,
    onNavigateToDetail: (Int, String) -> Unit,
    onNavigateToHistory: () -> Unit,
    onOpenDrawer: () -> Unit,
    onNavigateToMedicationList: () -> Unit,
    onNavigateToAlarms: () -> Unit
) {
    val medications by viewModel.medications.collectAsState()
    val userPrefs by viewModel.userPreferences.collectAsState()
    val profiles by viewModel.allProfiles.collectAsState()
    val selectedProfile by viewModel.selectedProfile.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    var medicationToConfirm by remember { mutableStateOf<Medication?>(null) }
    var confirmationNote by remember { mutableStateOf("") }
    var showProfileMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.stockAlert.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.streakAlert.collect { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long
            )
        }
    }

    val speechRecognizerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.get(0) ?: ""
            if (spokenText.isNotBlank()) {
                viewModel.confirmDoseByVoice(spokenText) { message, success ->
                    scope.launch {
                        snackbarHostState.showSnackbar(message)
                    }
                }
            }
        }
    }

    if (medicationToConfirm != null) {
        AlertDialog(
            onDismissRequest = { medicationToConfirm = null; confirmationNote = "" },
            icon = { 
                Surface(
                    shape = CircleShape,
                    color = MedicleanTeal.copy(alpha = 0.1f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MedicleanTeal, modifier = Modifier.size(32.dp))
                    }
                }
            },
            title = { 
                Text(
                    "Confirmar Dose", 
                    style = MaterialTheme.typography.headlineSmall, 
                    fontWeight = FontWeight.Black, 
                    color = MedicleanDarkGreen,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                ) 
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Você tomou ${medicationToConfirm?.name}?", 
                        style = MaterialTheme.typography.bodyLarge,
                        color = MedicleanDarkGreen.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedTextField(
                        value = confirmationNote,
                        onValueChange = { confirmationNote = it },
                        placeholder = { Text("Alguma observação? (ex: com água)", color = MedicleanDarkGreen.copy(alpha = 0.3f)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MedicleanDarkGreen,
                            unfocusedTextColor = MedicleanDarkGreen,
                            focusedContainerColor = MedicleanMint.copy(alpha = 0.05f),
                            unfocusedContainerColor = MedicleanMint.copy(alpha = 0.05f),
                            focusedBorderColor = MedicleanTeal,
                            unfocusedBorderColor = Color.Transparent,
                            focusedLabelColor = MedicleanTeal,
                            unfocusedLabelColor = MedicleanDarkGreen.copy(alpha = 0.6f)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        medicationToConfirm?.let { med ->
                            viewModel.confirmDose(med.id, med.name, confirmationNote.ifBlank { null })
                        }
                        medicationToConfirm = null
                        confirmationNote = ""
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MedicleanTeal)
                ) {
                    Text("SIM, ESTÁ TOMADO", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { medicationToConfirm = null; confirmationNote = "" },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("CANCELAR", color = MedicleanDarkGreen.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = if (isSystemInDarkTheme()) Color(0xFF1E2A28) else Color.White,
            shape = RoundedCornerShape(32.dp),
            tonalElevation = 0.dp
        )
    }


    if (userPrefs.isFirstRun) {
        OnboardingOverlay(onDismiss = { viewModel.completeOnboarding() })
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToDetail(-1, "") },
                containerColor = MedicleanTeal,
                contentColor = Color.White,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(bottom = 60.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.home_fab_add_content),
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .background(Color.White)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                val adherence by viewModel.todaysAdherence.collectAsState()

                HomeHeader(
                    selectedProfile = selectedProfile,
                    onOpenDrawer = onOpenDrawer,
                    onProfileClick = { showProfileMenu = true }
                )

                HealthDashboard(
                    adherence = adherence,
                    onVoiceClick = {
                        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Diga o nome do remédio")
                        }
                        try {
                            speechRecognizerLauncher.launch(intent)
                        } catch (e: Exception) {
                            scope.launch { snackbarHostState.showSnackbar("Erro no reconhecimento de voz") }
                        }
                    }
                )

                HomeSearchSection(
                    searchQuery = searchQuery,
                    onSearchChange = { viewModel.updateSearchQuery(it) },
                    selectedCategory = selectedCategory,
                    onCategoryChange = { viewModel.updateSelectedCategory(it) }
                )

                // Barra de Acesso Rápido - Premium Styling
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        icon = Icons.AutoMirrored.Filled.List,
                        label = "Meus Remédios",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToMedicationList
                    )
                    QuickActionCard(
                        icon = Icons.Default.NotificationsActive,
                        label = "Alarmes",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToAlarms
                    )
                }

                if (medications.isEmpty()) {
                    EmptyState(searchQuery.isNotBlank())
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        items(medications, key = { it.id }) { medication ->
                            MedicationCard(
                                medication = medication,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                countdown = viewModel.getMedicationCountdown(medication),
                                onConfirm = { medicationToConfirm = it },
                                onEdit = { onNavigateToDetail(it.id, it.name) },
                                onDelete = { viewModel.deleteMedication(it) }
                            )
                        }
                    }
                }
            }

            if (showProfileMenu) {
                ProfileSelectionOverlay(
                    profiles = profiles,
                    onProfileSelected = {
                        viewModel.selectProfile(it)
                        showProfileMenu = false
                    },
                    onDismiss = { showProfileMenu = false }
                )
            }
        }
    }
}

@Composable
fun HealthDashboard(adherence: Pair<Int, Int>, onVoiceClick: () -> Unit) {
    val (taken, total) = adherence
    val progress = if (total > 0) taken.toFloat() / total else 0f
    val percentage = (progress * 100).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSystemInDarkTheme()) Color(0xFF1E2A28) else MedicleanTeal
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Background organic shape decoration
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = MedicleanTeal.copy(alpha = 0.05f),
                    radius = size.minDimension * 0.8f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.1f)
                )
            }

            Row(
                modifier = Modifier
                    .padding(28.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "DASHBOARD HOJE",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "$taken de $total",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "doses concluídas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Premium Voice Action Button
                    Surface(
                        onClick = onVoiceClick,
                        color = MedicleanTeal,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Confirmar por Voz", style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.Black)
                        }
                    }
                }

                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(110.dp)) {
                    // Track
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxSize(),
                        color = Color.White.copy(alpha = 0.2f),
                        strokeWidth = 14.dp,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    // Progress
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        color = Color.White,
                        strokeWidth = 14.dp,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$percentage%",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSystemInDarkTheme()) Color(0xFF1E2A28) else MedicleanWhite,
        modifier = modifier.height(70.dp),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Surface(
                color = MedicleanTeal.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MedicleanTeal, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                label, 
                fontWeight = FontWeight.Bold, 
                color = MedicleanDarkGreen, 
                fontSize = 13.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun HomeHeader(
    selectedProfile: Profile?,
    onOpenDrawer: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onOpenDrawer,
            modifier = Modifier
                .background(if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.08f) else Color.White, RoundedCornerShape(16.dp))
                .size(48.dp)
                .border(1.dp, if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(16.dp))
        ) {
            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MedicleanTeal, modifier = Modifier.size(26.dp))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Olá, ${selectedProfile?.name ?: "Usuário"}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MedicleanDarkGreen
            )
            Text(
                text = "Sua rotina de saúde hoje",
                style = MaterialTheme.typography.labelMedium,
                color = MedicleanDarkGreen.copy(alpha = 0.5f),
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }

        Surface(
            onClick = onProfileClick,
            shape = RoundedCornerShape(16.dp),
            color = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.08f) else Color.White,
            modifier = Modifier.size(48.dp),
            shadowElevation = 4.dp,
            border = BorderStroke(2.dp, selectedProfile?.let { Color(it.color) } ?: MedicleanTeal)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Perfil",
                    tint = selectedProfile?.let { Color(it.color) } ?: MedicleanTeal,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeSearchSection(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("O que você procura?", color = MedicleanDarkGreen.copy(alpha = 0.4f)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MedicleanTeal) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MedicleanDarkGreen,
                unfocusedTextColor = MedicleanDarkGreen,
                focusedContainerColor = if (isSystemInDarkTheme()) Color(0xFF1A2624) else MedicleanWhite,
                unfocusedContainerColor = if (isSystemInDarkTheme()) Color(0xFF1A2624) else MedicleanWhite,
                focusedBorderColor = MedicleanTeal,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = MedicleanTeal
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val categories = listOf("Todos", "Uso Contínuo", "Vitamina")
            categories.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategoryChange(category) },
                    label = { 
                        Text(
                            category, 
                            fontWeight = if(selectedCategory == category) FontWeight.Black else FontWeight.Medium,
                            fontSize = 13.sp
                        ) 
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MedicleanTeal,
                        selectedLabelColor = Color.White,
                        containerColor = if (isSystemInDarkTheme()) Color(0xFF1A1A1A) else MedicleanWhite,
                        labelColor = MedicleanDarkGreen
                    ),
                    border = null
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MedicationCard(
    medication: Medication,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    countdown: String,
    onConfirm: (Medication) -> Unit,
    onEdit: (Medication) -> Unit,
    onDelete: (Medication) -> Unit
) {
    with(sharedTransitionScope) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEdit(medication) },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSystemInDarkTheme()) Color(0xFF1E2A28) else Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = if (isSystemInDarkTheme()) null else BorderStroke(1.dp, Color(0xFFE8ECEB))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile/Icon Container with soft glow
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(medication.iconColor).copy(alpha = 0.12f),
                        modifier = Modifier
                            .size(64.dp)
                            .sharedElement(
                                rememberSharedContentState(key = "medication_icon_${medication.id}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            ),
                        border = BorderStroke(1.dp, Color(medication.iconColor).copy(alpha = 0.2f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when(medication.iconType) {
                                    "capsule" -> Icons.Default.Adjust
                                    "drops" -> Icons.Default.WaterDrop
                                    "liquid" -> Icons.Default.Vaccines
                                    else -> Icons.Default.Medication
                                },
                                contentDescription = null,
                                tint = Color(medication.iconColor),
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(18.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = medication.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MedicleanDarkGreen,
                                modifier = Modifier.sharedElement(
                                    rememberSharedContentState(key = "medication_title_${medication.id}"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                            )
                            if (medication.streakCount > 1) {
                                Surface(
                                    color = Color(0xFFFF9800).copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.padding(start = 12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
                                        Text("🔥", fontSize = 14.sp)
                                        Spacer(Modifier.width(6.dp))
                                        Text(medication.streakCount.toString(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color(0xFFE65100))
                                    }
                                }
                            }
                        }
                        Text(
                            text = medication.dosage,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MedicleanDarkGreen.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (medication.stockCount <= 5 && medication.stockCount > 0) {
                        Surface(
                            color = MedicleanError.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text("ESTOQUE BAIXO", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MedicleanError, letterSpacing = 0.5.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (medication.isActive) "PRÓXIMA DOSE" else "STATUS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MedicleanDarkGreen.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = if (medication.isActive) countdown else "Pausado",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = if (medication.isActive) MedicleanTeal else Color.Gray.copy(alpha = 0.6f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconButton(
                            onClick = { onDelete(medication) },
                            modifier = Modifier
                                .background(MedicleanError.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                .size(48.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MedicleanError, modifier = Modifier.size(20.dp))
                        }
                        
                        if (medication.isActive) {
                            Button(
                                onClick = { onConfirm(medication) },
                                colors = ButtonDefaults.buttonColors(containerColor = MedicleanTeal),
                                shape = RoundedCornerShape(18.dp),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("TOMAR AGORA", fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(isFiltering: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            color = MedicleanTeal.copy(alpha = 0.05f),
            shape = CircleShape
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (isFiltering) Icons.Default.SearchOff else Icons.Default.MedicalServices,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = MedicleanTeal.copy(alpha = 0.3f)
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (isFiltering) "Sem resultados" else "Sua jornada começa aqui",
            style = MaterialTheme.typography.headlineSmall,
            color = MedicleanDarkGreen,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isFiltering) "Tente ajustar sua busca" else "Adicione seu primeiro remédio para começar o acompanhamento.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MedicleanDarkGreen.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun ProfileSelectionOverlay(
    profiles: List<Profile>,
    onProfileSelected: (Profile) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = if (isSystemInDarkTheme()) Color(0xFF121A18) else MedicleanWhite,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color.Gray.copy(alpha = 0.3f), CircleShape)
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Selecione o Perfil",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MedicleanDarkGreen
                )
                Spacer(modifier = Modifier.height(20.dp))
                profiles.forEach { profile ->
                    Surface(
                        onClick = { onProfileSelected(profile) },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(16.dp),
                                color = Color(profile.color),
                                shape = CircleShape,
                                border = BorderStroke(2.dp, Color.White)
                            ) {}
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                profile.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MedicleanDarkGreen
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun OnboardingOverlay(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.8f), Color.Black.copy(alpha = 0.95f))))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(120.dp),
                color = MedicleanTeal.copy(alpha = 0.2f),
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = MedicleanTeal, modifier = Modifier.size(60.dp))
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "Bem-vindo ao\nRemédio Certo",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                lineHeight = 40.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Sua saúde organizada de forma inteligente. Simples, seguro e essencial para sua rotina.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MedicleanTeal),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("COMEÇAR AGORA", fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        }
    }
}
