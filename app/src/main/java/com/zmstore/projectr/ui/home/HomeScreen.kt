package com.zmstore.projectr.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zmstore.projectr.R
import com.zmstore.projectr.data.model.Medication
import com.zmstore.projectr.data.model.Profile
import com.zmstore.projectr.ui.MainViewModel
import com.zmstore.projectr.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
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

    if (medicationToConfirm != null) {
        AlertDialog(
            onDismissRequest = { medicationToConfirm = null; confirmationNote = "" },
            title = { Text("Confirmar Dose", fontWeight = FontWeight.Bold, color = MedicleanDarkGreen) },
            text = {
                Column {
                    Text("Você tomou ${medicationToConfirm?.name}?", color = MedicleanDarkGreen.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = confirmationNote,
                        onValueChange = { confirmationNote = it },
                        label = { Text("Observação (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MedicleanDarkGreen,
                            unfocusedTextColor = MedicleanDarkGreen,
                            focusedBorderColor = MedicleanTeal,
                            unfocusedBorderColor = MedicleanTeal.copy(alpha = 0.3f),
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
                    colors = ButtonDefaults.buttonColors(containerColor = MedicleanTeal)
                ) {
                    Text("Sim, tomei", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { medicationToConfirm = null; confirmationNote = "" }) {
                    Text("Cancelar", color = MedicleanDarkGreen.copy(alpha = 0.6f))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp)
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
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 60.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.home_fab_add_content),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(MedicleanWhite, MedicleanMint)
                    )
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                HomeHeader(
                    selectedProfile = selectedProfile,
                    onOpenDrawer = onOpenDrawer,
                    onProfileClick = { showProfileMenu = true }
                )

                HomeSearchSection(
                    searchQuery = searchQuery,
                    onSearchChange = { viewModel.updateSearchQuery(it) },
                    selectedCategory = selectedCategory,
                    onCategoryChange = { viewModel.updateSelectedCategory(it) }
                )

                // Barra de Acesso Rápido - Melhoria Implementada
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
                        label = "Alertas/Alarmes",
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
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(medications, key = { it.id }) { medication ->
                            MedicationCard(
                                medication = medication,
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
fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        modifier = modifier.height(64.dp),
        shadowElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MedicleanTeal.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = MedicleanTeal, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                label, 
                fontWeight = FontWeight.ExtraBold, 
                color = MedicleanDarkGreen, 
                fontSize = 14.sp,
                textAlign = TextAlign.Center
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
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onOpenDrawer,
            modifier = Modifier.background(Color.White, CircleShape)
        ) {
            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MedicleanTeal)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Olá, ${selectedProfile?.name ?: "Usuário"}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MedicleanDarkGreen
            )
            Text(
                text = "Sua saúde em primeiro lugar",
                style = MaterialTheme.typography.bodySmall,
                color = MedicleanDarkGreen.copy(alpha = 0.6f)
            )
        }

        Surface(
            onClick = onProfileClick,
            shape = CircleShape,
            color = Color.White,
            modifier = Modifier.size(40.dp),
            shadowElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Perfil",
                    tint = selectedProfile?.let { Color(it.color) } ?: MedicleanTeal
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
            placeholder = { Text("Buscar medicamento...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MedicleanTeal) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MedicleanDarkGreen,
                unfocusedTextColor = MedicleanDarkGreen,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = MedicleanTeal,
                unfocusedBorderColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categories = listOf("Todos", "Uso Contínuo", "Vitamina")
            categories.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategoryChange(category) },
                    label = { Text(category) },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MedicleanTeal,
                        selectedLabelColor = Color.White,
                        containerColor = Color.White,
                        labelColor = MedicleanDarkGreen
                    ),
                    border = null
                )
            }
        }
    }
}

@Composable
fun MedicationCard(
    medication: Medication,
    countdown: String,
    onConfirm: (Medication) -> Unit,
    onEdit: (Medication) -> Unit,
    onDelete: (Medication) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit(medication) },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MedicleanTeal.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MedicleanMint.copy(alpha = 0.4f),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Medication,
                                contentDescription = null,
                                tint = MedicleanTeal,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = medication.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MedicleanDarkGreen
                        )
                        Text(
                            text = medication.dosage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MedicleanTextBlack.copy(alpha = 0.7f)
                        )
                    }
                }

                if (medication.stockCount <= 5 && medication.stockCount > 0) {
                    Surface(
                        color = Color.Red.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Estoque Baixo",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (medication.isActive) "Próxima dose" else "Pausado",
                        style = MaterialTheme.typography.labelSmall,
                        color = MedicleanTextBlack.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = countdown,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (medication.isActive) MedicleanTeal else Color.Gray
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { onDelete(medication) },
                        modifier = Modifier.background(Color.Red.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color.Red, modifier = Modifier.size(20.dp))
                    }
                    
                    IconButton(
                        onClick = { onEdit(medication) },
                        modifier = Modifier.background(MedicleanMint.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MedicleanTeal, modifier = Modifier.size(20.dp))
                    }

                    if (medication.isActive) {
                        Button(
                            onClick = { onConfirm(medication) },
                            colors = ButtonDefaults.buttonColors(containerColor = MedicleanTeal),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("TOMAR", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.padding(8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
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
        Icon(
            if (isFiltering) Icons.Default.SearchOff else Icons.Default.AddCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MedicleanTeal.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isFiltering) "Nenhum medicamento encontrado" else "Comece a cuidar da sua saúde!",
            style = MaterialTheme.typography.titleMedium,
            color = MedicleanDarkGreen,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (isFiltering) "Tente buscar por outro nome" else "Adicione seu primeiro medicamento no botão abaixo",
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
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Selecione o Perfil",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MedicleanDarkGreen
                )
                Spacer(modifier = Modifier.height(16.dp))
                profiles.forEach { profile ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onProfileSelected(profile) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Color(profile.color), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            profile.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MedicleanDarkGreen
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingOverlay(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = Color.White, modifier = Modifier.size(80.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Bem-vindo ao Remédio Certo!",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Organize sua rotina de saúde de forma simples e segura. Adicione seus medicamentos e deixe que a gente te lembre.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MedicleanTeal),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Vamos Começar!", fontWeight = FontWeight.Bold)
            }
        }
    }
}
