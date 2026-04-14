package com.zmstore.projectr.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import com.zmstore.projectr.data.model.DoseHistory
import com.zmstore.projectr.data.model.Medication
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfExportHelper {

    fun exportAdherenceReport(
        context: Context,
        history: List<DoseHistory>,
        medications: List<Medication>
    ) {
        val pdfDocument = PdfDocument()
        // ... (existing paint setup)
        val paint = Paint()
        val titlePaint = Paint().apply {
            textSize = 24f
            isFakeBoldText = true
        }
        val textPaint = Paint().apply {
            textSize = 14f
        }
        val headerPaint = Paint().apply {
            textSize = 16f
            isFakeBoldText = true
        }

        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        var y = 40f
        canvas.drawText("Relatório de Adesão - ProjectR", 40f, y, titlePaint)
        
        y += 40f
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        canvas.drawText("Data do Relatório: ${dateFormat.format(Date())}", 40f, y, textPaint)
        
        y += 30f
        canvas.drawText("Resumo de Medicamentos:", 40f, y, headerPaint)
        
        y += 20f
        medications.forEach { med ->
            val dosesTaken = history.count { it.medicationId == med.id }
            canvas.drawText("- ${med.name}: $dosesTaken doses confirmadas", 60f, y, textPaint)
            y += 20f
        }

        y += 20f
        canvas.drawText("Histórico Recente (Últimas 20 doses):", 40f, y, headerPaint)
        y += 20f
        
        history.takeLast(20).reversed().forEach { dose ->
            val dateStr = dateFormat.format(Date(dose.timestamp))
            canvas.drawText("$dateStr - ${dose.medicationName} ${if (dose.note != null) " (Nota: ${dose.note})" else ""}", 60f, y, textPaint)
            y += 20f
            
            if (y > 800) { 
                return@forEach 
            }
        }

        pdfDocument.finishPage(page)

        val fileName = "Relatorio_Adesao_${System.currentTimeMillis()}.pdf"
        val file = File(context.cacheDir, fileName) // Use cache for easier sharing

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            
            // Share the PDF
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "Compartilhar Relatório"))
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Erro ao gerar PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }
}
