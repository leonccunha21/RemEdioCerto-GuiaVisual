package com.zmstore.projectr.ui.alarms

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.zmstore.projectr.ui.MainViewModel
import com.zmstore.projectr.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmManagementScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val medications by viewModel.medications.collectAsState()

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
                    Text(
                        "ALARMES", 
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        ),
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = MedicleanTeal)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (medications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(64.dp), tint = MedicleanTeal.copy(alpha = 0.2f))
                    Spacer(Modifier.height(16.dp))
                    Text("Nenhum alarme configurado", color = MedicleanDarkGreen.copy(alpha = 0.5f), fontWeight = FontWeight.Black)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(medications) { med ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(26.dp),
                        color = if (isSystemInDarkTheme()) Color(0xFF1E2A28) else MedicleanWhite,
                        shadowElevation = 4.dp,
                        border = BorderStroke(1.dp, MedicleanTeal.copy(alpha = 0.05f))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(20.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MedicleanTeal.copy(alpha = 0.08f),
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            if(med.isActive) Icons.Default.NotificationsActive else Icons.Default.NotificationsPaused,
                                            contentDescription = null, 
                                            tint = if(med.isActive) MedicleanTeal else Color.Gray,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(18.dp))
                                Column {
                                    Text(
                                        med.name, 
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                        color = MedicleanDarkGreen
                                    )
                                    val timeInfo = if (med.customTimes != null && med.customTimes.isNotBlank()) {
                                        med.customTimes
                                    } else {
                                        "A cada ${med.intervalHours} horas"
                                    }
                                    Text(
                                        timeInfo, 
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MedicleanDarkGreen.copy(alpha = 0.5f),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Switch(
                                checked = med.isActive,
                                onCheckedChange = { isActive ->
                                    viewModel.updateMedication(med.copy(isActive = isActive))
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MedicleanTeal,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color.LightGray.copy(alpha = 0.3f),
                                    uncheckedBorderColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

