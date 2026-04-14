package com.zmstore.projectr.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zmstore.projectr.ui.MainViewModel
import com.zmstore.projectr.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val userPrefs by viewModel.userPreferences.collectAsState()
    val context = LocalContext.current
    
    var name by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var emergency by remember { mutableStateOf("") }
    var geminiApiKey by remember { mutableStateOf("") }
    var isBiometricEnabled by remember { mutableStateOf(false) }
    var isDarkModeEnabled by remember { mutableStateOf(false) }

    // Sync local state when prefs load
    LaunchedEffect(userPrefs) {
        name = userPrefs.name
        weight = userPrefs.weight
        height = userPrefs.height
        emergency = userPrefs.emergencyContact
        geminiApiKey = userPrefs.geminiApiKey
        isBiometricEnabled = userPrefs.isBiometricEnabled
        // Assume dark mode is managed in userPrefs - let's verify UserPreferencesRepository
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isSystemInDarkTheme()) 
                        listOf(Color(0xFF1B2B28), Color(0xFF121D1B)) 
                    else 
                        listOf(MedicleanWhite, MedicleanMint)
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "MEUS DADOS", 
                            fontWeight = FontWeight.Black, 
                            color = if (isSystemInDarkTheme()) Color.White else MedicleanDarkGreen,
                            letterSpacing = 1.sp
                        ) 
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, 
                                contentDescription = "Voltar", 
                                tint = if (isSystemInDarkTheme()) Color.White else MedicleanDarkGreen
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Section Title
                Text(
                    text = "Informações Pessoais",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSystemInDarkTheme()) MedicleanMint else MedicleanDarkGreen
                )

                // Profile Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSystemInDarkTheme()) Color(0xFF243A36) else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, 
                        MedicleanTeal.copy(alpha = if (isSystemInDarkTheme()) 0.3f else 0.1f)
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nome Completo") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MedicleanTeal) },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = if (isSystemInDarkTheme()) Color.White else MedicleanDarkGreen,
                                unfocusedTextColor = if (isSystemInDarkTheme()) Color.White else MedicleanDarkGreen,
                                focusedBorderColor = MedicleanTeal,
                                unfocusedBorderColor = MedicleanTeal.copy(alpha = 0.3f)
                            )
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val fieldColors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = if (isSystemInDarkTheme()) Color.White else MedicleanDarkGreen,
                                unfocusedTextColor = if (isSystemInDarkTheme()) Color.White else MedicleanDarkGreen,
                                focusedBorderColor = MedicleanTeal,
                                unfocusedBorderColor = MedicleanTeal.copy(alpha = 0.3f)
                            )
                            OutlinedTextField(
                                value = weight,
                                onValueChange = { weight = it },
                                label = { Text("Peso (kg)") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = fieldColors
                            )
                            OutlinedTextField(
                                value = height,
                                onValueChange = { height = it },
                                label = { Text("Altura (cm)") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = fieldColors
                            )
                        }
                    }
                }

                // Emergency Section
                Text(
                    text = "Segurança e Emergência",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSystemInDarkTheme()) MedicleanMint else MedicleanDarkGreen,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSystemInDarkTheme()) Color(0xFF1B2B28) else MedicleanTeal.copy(alpha = 0.05f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MedicleanTeal.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = emergency,
                            onValueChange = { emergency = it },
                            label = { Text("Contato de Emergência") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Emergency, contentDescription = null, tint = MedicleanTeal) },
                            shape = RoundedCornerShape(16.dp),
                            placeholder = { Text("(00) 00000-0000") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = if (isSystemInDarkTheme()) Color.White else MedicleanDarkGreen,
                                unfocusedTextColor = if (isSystemInDarkTheme()) Color.White else MedicleanDarkGreen,
                                focusedBorderColor = MedicleanTeal,
                                unfocusedBorderColor = MedicleanTeal.copy(alpha = 0.3f)
                            )
                        )
                        
                        Text(
                            text = "Este contato será exibido em notificações críticas ou em caso de emergência médica salvos no app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.6f) else MedicleanDarkGreen.copy(alpha = 0.6f)
                        )
                    }
                }

                // AI Settings Section
                Text(
                    text = "Configurações de IA e Privacidade",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSystemInDarkTheme()) MedicleanMint else MedicleanDarkGreen,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSystemInDarkTheme()) Color(0xFF243A36) else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, 
                        MedicleanTeal.copy(alpha = if (isSystemInDarkTheme()) 0.3f else 0.1f)
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = geminiApiKey,
                            onValueChange = { geminiApiKey = it },
                            label = { Text("Chave de API Gemini") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Insira sua chave aqui...") },
                            shape = RoundedCornerShape(16.dp),
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = if (isSystemInDarkTheme()) Color.White else MedicleanDarkGreen,
                                unfocusedTextColor = if (isSystemInDarkTheme()) Color.White else MedicleanDarkGreen,
                                focusedBorderColor = MedicleanTeal,
                                unfocusedBorderColor = MedicleanTeal.copy(alpha = 0.3f)
                            )
                        )
                        
                        androidx.compose.material3.HorizontalDivider(
                            color = MedicleanTeal.copy(alpha = if (isSystemInDarkTheme()) 0.3f else 0.1f)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Proteção Biométrica",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSystemInDarkTheme()) Color.White else MedicleanDarkGreen
                                )
                                Text(
                                    text = "Exigir digital/face ao abrir o app",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.6f) else MedicleanDarkGreen.copy(alpha = 0.6f)
                                )
                            }
                            Switch(
                                checked = isBiometricEnabled,
                                onCheckedChange = { isBiometricEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = MedicleanTeal
                                )
                            )
                        }
                    }
                }

                Button(
                    onClick = { 
                        viewModel.updateProfile(name, weight, height, emergency, geminiApiKey, isBiometricEnabled)
                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MedicleanTeal),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("SALVAR ALTERAÇÕES", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }

                // Legal Section (Mandatory for Health Apps)
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextButton(onClick = { 
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://sites.google.com/view/projectr-privacy-policy"))
                        context.startActivity(intent)
                    }) {
                        Text(
                            "Política de Privacidade",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MedicleanTeal,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "Versão 1.4.0 • ProjectR Digital Health",
                        style = MaterialTheme.typography.labelSmall,
                        color = (if (isSystemInDarkTheme()) Color.White else MedicleanDarkGreen).copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}
