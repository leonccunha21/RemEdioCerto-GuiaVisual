package com.zmstore.projectr.ui.help

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.zmstore.projectr.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
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
                        "CENTRAL DE AJUDA", 
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp), 
                        color = MedicleanDarkGreen
                    ) 
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
    ) { padding ->
        val helpItems = listOf(
            Pair("Como adicionar um medicamento?", "Vá para a tela Inicial e clique no botão (+). Você pode adicionar por foto (IA), voz ou digitando manualmente."),
            Pair("Onde configuro a IA do aplicativo?", "Vá para 'Meus Dados' e insira sua chave no campo 'Chave API Gemini'. Isso habilita a análise inteligente."),
            Pair("Como funcionam os Lembretes?", "Ative o botão 'Em uso' no detalhe do medicamento e configure o intervalo ou horários fixos."),
            Pair("O que é o aviso de Estoque Baixo?", "O app monitora sua quantidade restante. Quando chegar em 5 unidades, o card ficará vermelho no dashboard."),
            Pair("Meus dados estão seguros?", "Sim! Todos os seus dados de saúde são armazenados localmente no seu dispositivo, garantindo total privacidade.")
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(helpItems) { (title, description) ->
                HelpItemCard(title, description)
            }
            
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MedicleanTeal.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MedicleanTeal.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MedicleanTeal, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "Dúvidas extras? Consulte seu médico. O app é um auxiliador e não substitui a orientação profissional.", 
                            style = MaterialTheme.typography.bodySmall, 
                            color = MedicleanDarkGreen.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HelpItemCard(title: String, description: String) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = if (isSystemInDarkTheme()) Color(0xFF1E2A28) else Color.White,
        shadowElevation = if (expanded) 4.dp else 1.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(shape = CircleShape, color = MedicleanTeal.copy(alpha = 0.1f), modifier = Modifier.size(32.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.QuestionMark, contentDescription = null, tint = MedicleanTeal, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MedicleanDarkGreen
                    )
                }
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MedicleanDarkGreen.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

