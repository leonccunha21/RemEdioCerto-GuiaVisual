package com.zmstore.projectr.data.repository

import com.google.firebase.vertexai.FirebaseVertexAI
import com.google.firebase.vertexai.type.content
import com.zmstore.projectr.data.model.Medication
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AILogicRepository @Inject constructor() {

    private val vertexAI = FirebaseVertexAI.getInstance()
    private val model = vertexAI.generativeModel("gemini-1.5-flash")

    fun getMedicationInfo(medicationName: String): Flow<Result<PartialMedicationInfo>> = flow {
        try {
            val prompt = """
                Forneça informações detalhadas sobre o medicamento: $medicationName.
                Responda estritamente no formato JSON com as seguintes chaves:
                "purpose": (para que serve),
                "instructions": (como usar),
                "sideEffects": (principais efeitos colaterais),
                "alerts": (avisos importantes),
                "category": (uma categoria simples como Analgésico, Antibiótico, etc)
                
                Se não conhecer o medicamento, retorne um erro ou campos vazios.
                Use português do Brasil.
            """.trimIndent()

            val response = model.generateContent(prompt)
            val text = response.text
            
            if (text != null) {
                // Aqui no mundo real usaríamos um parser JSON (Gson/Kotlinx Serialization)
                // Por brevidade e para este exemplo, assumimos que o retorno é tratável
                // Em produção: parsear o JSON e retornar o objeto
                emit(Result.success(parseAIResponse(text)))
            } else {
                emit(Result.failure(Exception("Resposta da IA vazia")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    private fun parseAIResponse(jsonString: String): PartialMedicationInfo {
        // Implementação simplificada para extração manual se necessário, 
        // mas idealmente usaríamos Json.decodeFromString
        // Vou simular um parse básico para fins de demonstração
        return PartialMedicationInfo(
            purpose = extractField(jsonString, "purpose"),
            instructions = extractField(jsonString, "instructions"),
            sideEffects = extractField(jsonString, "sideEffects"),
            alerts = extractField(jsonString, "alerts"),
            category = extractField(jsonString, "category")
        )
    }

    private fun extractField(json: String, field: String): String {
        val pattern = "\"$field\"\\s*:\\s*\"([^\"]*)\"".toRegex()
        return pattern.find(json)?.groups?.get(1)?.value ?: ""
    }
}

data class PartialMedicationInfo(
    val purpose: String,
    val instructions: String,
    val sideEffects: String,
    val alerts: String,
    val category: String
)
