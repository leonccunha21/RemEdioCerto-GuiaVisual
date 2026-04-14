package com.zmstore.projectr.ui.medication

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zmstore.projectr.ui.MainViewModel
import com.zmstore.projectr.ui.theme.*
import com.zmstore.projectr.data.model.Medication

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationListScreen(
    viewModel: MainViewModel,
    onNavigateToDetail: (Int, String) -> Unit,
    onBack: () -> Unit
) {
    val medications by viewModel.medications.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var medicationToDelete by remember { mutableStateOf<Medication?>(null) }
    
    val filteredMedications = if (searchQuery.isBlank()) {
        medications
    } else {
        medications.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    if (medicationToDelete != null) {
        AlertDialog(
            onDismissRequest = { medicationToDelete = null },
            title = { Text("Excluir Medicamento", fontWeight = FontWeight.Black, color = MedicleanDarkGreen) },
            text = { Text("Tem certeza que deseja excluir '${medicationToDelete?.name}'? Esta ação não pode ser desfeita.", color = MedicleanDarkGreen.copy(alpha = 0.7f)) },
            confirmButton = {
                Button(
                    onClick = {
                        medicationToDelete?.let { viewModel.deleteMedication(it) }
                        medicationToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MedicleanError),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Excluir", color = Color.White, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { medicationToDelete = null }) {
                    Text("Cancelar", color = MedicleanDarkGreen.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MedicleanWhite,
            shape = RoundedCornerShape(28.dp)
        )
    }

    Scaffold(
        containerColor = Color.White,
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "MEUS REMÉDIOS", 
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp), 
                        color = MedicleanDarkGreen
                    ) 
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(8.dp)
                            .background(if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.1f) else Color.White, RoundedCornerShape(12.dp))
                            .size(40.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = MedicleanTeal)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Premium Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("O que você procura?", color = MedicleanDarkGreen.copy(alpha = 0.4f)) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MedicleanTeal) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MedicleanDarkGreen,
                    unfocusedTextColor = MedicleanDarkGreen,
                    focusedContainerColor = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.05f) else Color.White,
                    unfocusedContainerColor = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.05f) else Color.White,
                    focusedBorderColor = MedicleanTeal,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = MedicleanTeal
                )
            )

            if (filteredMedications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Medication, contentDescription = null, modifier = Modifier.size(64.dp), tint = MedicleanTeal.copy(alpha = 0.2f))
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (searchQuery.isBlank()) "Sua lista está vazia" else "Remédio não encontrado",
                            color = MedicleanDarkGreen.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredMedications, key = { it.id }) { med ->
                        Surface(
                            onClick = { onNavigateToDetail(med.id, med.name) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = if (isSystemInDarkTheme()) Color(0xFF1E2A28) else MedicleanWhite,
                            shadowElevation = 4.dp,
                            border = BorderStroke(1.dp, MedicleanTeal.copy(alpha = 0.05f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(med.iconColor).copy(alpha = 0.1f),
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = when(med.iconType) {
                                                    "capsule" -> Icons.Default.Adjust
                                                    "drops" -> Icons.Default.WaterDrop
                                                    "liquid" -> Icons.Default.Vaccines
                                                    else -> Icons.Default.Medication
                                                },
                                                contentDescription = null, 
                                                tint = Color(med.iconColor),
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(med.name, fontWeight = FontWeight.Black, color = MedicleanDarkGreen, style = MaterialTheme.typography.titleMedium)
                                        Text(med.dosage, color = MedicleanDarkGreen.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                                IconButton(
                                    onClick = { medicationToDelete = med },
                                    modifier = Modifier.background(MedicleanError.copy(alpha = 0.08f), CircleShape).size(36.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MedicleanError, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

