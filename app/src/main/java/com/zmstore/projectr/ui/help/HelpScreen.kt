package com.zmstore.projectr.ui.help

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zmstore.projectr.ui.theme.MedicleanDarkGreen
import com.zmstore.projectr.ui.theme.MedicleanMint
import com.zmstore.projectr.ui.theme.MedicleanTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Central de Ajuda", fontWeight = FontWeight.Bold, color = MedicleanDarkGreen) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = MedicleanDarkGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MedicleanMint)
            )
        },
        containerColor = MedicleanMint
    ) { padding ->
        val helpItems = listOf(
            Pair("Como adicionar um medicamento?", "Vá para a tela Inicial e clique no botão circular azul (+) no canto inferior direito. Você pode adicionar por foto, voz ou digitando."),
            Pair("Onde configuro a IA do aplicativo?", "Vá para 'Meus Dados' no menu lateral e adicione a sua chave no campo 'Chave API Gemini'."),
            Pair("Como funcionam os Lembretes?", "Ative o botão 'Em uso' na tela do medicamento e configure o Intervalo em horas, ou escolha horários específicos (ex: 08:00, 20:00)."),
            Pair("O que é o aviso de Estoque Baixo?", "Ao adicionar o seu medicamento, você pode colocar a quantidade atual. Cada vez que clica em 'Confirmar' na tela inicial, reduz um. Se ficar 5 ou menos, a tela te avisa na cor vermelha."),
            Pair("Privacidade", "Todos os seus dados de saúde estão salvos apenas no seu próprio celular. Você pode acessar nossa Política de Privacidade na aba Meus Dados.")
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(helpItems) { (title, description) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QuestionMark, contentDescription = null, tint = MedicleanTeal, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MedicleanDarkGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MedicleanDarkGreen.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MedicleanTeal.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MedicleanTeal)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Dúvidas extras? Consulte seu médico de confiança. O app é um auxiliador e não substitui a orientação profissional.", style = MaterialTheme.typography.bodySmall, color = MedicleanDarkGreen)
                    }
                }
            }
        }
    }
}
