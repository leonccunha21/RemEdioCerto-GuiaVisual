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
        medications: List<Medication>,
        profile: com.zmstore.projectr.data.model.Profile?
    ) {
        val pdfDocument = PdfDocument()
        
        // Colors & Paints
        val tealColor = 0xFF008080.toInt()
        val darkGreenColor = 0xFF1B3D3D.toInt()
        val lightGrayColor = 0xFFF5F5F5.toInt()

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 28f
            isFakeBoldText = true
            isAntiAlias = true
        }
        
        val headerPaint = Paint().apply {
            color = tealColor
            textSize = 18f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = darkGreenColor
            textSize = 12f
            isAntiAlias = true
        }

        val labelPaint = Paint().apply {
            color = Color.GRAY
            textSize = 10f
            isAntiAlias = true
        }

        val bgPaint = Paint().apply {
            color = tealColor
            style = Paint.Style.FILL
        }

        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // Header Background
        canvas.drawRect(0f, 0f, 595f, 120f, bgPaint)
        canvas.drawText("RemÉdio Certo", 40f, 60f, titlePaint)
        
        titlePaint.textSize = 14f
        titlePaint.isFakeBoldText = false
        canvas.drawText("Relatório Médico de Adesão", 40f, 85f, titlePaint)
        
        // Profile Info
        var y = 150f
        canvas.drawText("PACIENTE:", 40f, y, labelPaint)
        y += 20f
        headerPaint.textSize = 20f
        canvas.drawText(profile?.name ?: "Usuário Principal", 40f, y, headerPaint)
        
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        canvas.drawText("Data de Emissão: ${dateFormat.format(Date())}", 350f, y - 5f, labelPaint)
        
        y += 40f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 30f
        
        // Medication Summary
        headerPaint.textSize = 16f
        canvas.drawText("RESUMO DOS MEDICAMENTOS ATIVOS", 40f, y, headerPaint)
        y += 25f
        
        medications.filter { it.isActive }.forEach { med ->
            if (y > 780) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 40f
            }
            
            canvas.drawRect(40f, y - 15f, 555f, y + 25f, Paint().apply { color = lightGrayColor })
            textPaint.isFakeBoldText = true
            canvas.drawText(med.name, 50f, y + 5f, textPaint)
            textPaint.isFakeBoldText = false
            canvas.drawText("Dose: ${med.dosage}", 50f, y + 20f, textPaint)
            
            val takenCount = history.count { it.medicationId == med.id }
            canvas.drawText("Doses Confirmadas: $takenCount", 380f, y + 12f, textPaint)
            
            y += 50f
        }
        
        y += 30f
        headerPaint.textSize = 16f
        canvas.drawText("HISTÓRICO DETALHADO DE DOSES", 40f, y, headerPaint)
        y += 25f
        
        // Table Headers
        canvas.drawRect(40f, y - 15f, 555f, y + 10f, bgPaint)
        titlePaint.textSize = 10f
        canvas.drawText("DATA / HORA", 50f, y, titlePaint)
        canvas.drawText("MEDICAMENTO", 180f, y, titlePaint)
        canvas.drawText("STATUS", 420f, y, titlePaint)
        canvas.drawText("NOTAS", 500f, y, titlePaint)
        y += 25f

        history.sortedByDescending { it.timestamp }.forEach { dose ->
            if (y > 780) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                
                // Re-draw table headers on new page
                y = 40f
                canvas.drawRect(40f, y - 15f, 555f, y + 10f, bgPaint)
                canvas.drawText("DATA / HORA", 50f, y, titlePaint)
                canvas.drawText("MEDICAMENTO", 180f, y, titlePaint)
                canvas.drawText("STATUS", 420f, y, titlePaint)
                canvas.drawText("NOTAS", 500f, y, titlePaint)
                y += 25f
            }
            
            val dateStr = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date(dose.timestamp))
            canvas.drawText(dateStr, 50f, y, textPaint)
            canvas.drawText(dose.medicationName, 180f, y, textPaint)
            canvas.drawText("TOMADO", 420f, y, textPaint)
            canvas.drawText(dose.note ?: "-", 500f, y, textPaint)
            
            canvas.drawLine(40f, y + 5f, 555f, y + 5f, linePaint)
            y += 20f
        }
        
        // Footer
        val footerY = 820f
        canvas.drawText("Documento gerado automaticamente pelo aplicativo RemÉdio Certo Guia Visual.", 40f, footerY, labelPaint)
        canvas.drawText("Página 1", 530f, footerY, labelPaint)

        pdfDocument.finishPage(page)

        val fileName = "Relatorio_${profile?.name ?: "Usuario"}_${System.currentTimeMillis()}.pdf"
        val file = File(context.cacheDir, fileName)

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            
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
