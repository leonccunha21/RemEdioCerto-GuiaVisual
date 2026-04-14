package com.zmstore.projectr.ui.medication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
            title = { Text("Excluir Medicamento", fontWeight = FontWeight.Bold) },
            text = { Text("Tem certeza que deseja excluir '${medicationToDelete?.name}'? Esta ação não pode ser desfeita.") },
            confirmButton = {
                Button(
                    onClick = {
                        medicationToDelete?.let { viewModel.deleteMedication(it) }
                        medicationToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Excluir", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { medicationToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(MedicleanWhite, MedicleanMint)))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("MEUS MEDICAMENTOS", fontWeight = FontWeight.Black, color = MedicleanDarkGreen) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = MedicleanDarkGreen)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Procurar na lista...") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MedicleanTeal) },
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

                if (filteredMedications.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (searchQuery.isBlank()) "Nenhum medicamento cadastrado." else "Nenhum resultado encontrado.",
                            color = MedicleanDarkGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredMedications) { med ->
                            Card(
                                onClick = { onNavigateToDetail(med.id, med.name) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MedicleanTeal.copy(alpha = 0.1f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MedicleanMint.copy(alpha = 0.5f),
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Medication, contentDescription = null, tint = MedicleanTeal)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(med.name, fontWeight = FontWeight.Bold, color = MedicleanDarkGreen, fontSize = 18.sp)
                                            Text(med.dosage, color = MedicleanTextBlack.copy(alpha = 0.7f), fontSize = 14.sp)
                                        }
                                    }
                                    IconButton(onClick = { medicationToDelete = med }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color.Red.copy(alpha = 0.6f))
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
