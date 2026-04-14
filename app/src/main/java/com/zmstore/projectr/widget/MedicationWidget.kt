package com.zmstore.projectr.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.zmstore.projectr.R
import com.zmstore.projectr.data.repository.MedicationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

import android.app.PendingIntent
import android.content.Intent
import android.view.View
import com.zmstore.projectr.data.model.DoseHistory

@AndroidEntryPoint
class MedicationWidget : AppWidgetProvider() {

    @Inject
    lateinit var repository: MedicationRepository

    companion object {
        private const val ACTION_CONFIRM_DOSE = "com.zmstore.projectr.ACTION_CONFIRM_DOSE"
        private const val EXTRA_MED_ID = "EXTRA_MED_ID"
        private const val EXTRA_MED_NAME = "EXTRA_MED_NAME"
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_CONFIRM_DOSE) {
            val medId = intent.getIntExtra(EXTRA_MED_ID, -1)
            val medName = intent.getStringExtra(EXTRA_MED_NAME) ?: ""
            if (medId != -1) {
                confirmDose(context, medId, medName)
            }
        }
    }

    private fun confirmDose(context: Context, medId: Int, medName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            repository.insertDoseHistory(DoseHistory(medicationId = medId, medicationName = medName))
            repository.getMedicationById(medId)?.let { med ->
                val updatedStock = if (med.stockCount > 0) med.stockCount - 1 else 0
                val updatedMed = med.copy(
                    lastTakenTimestamp = System.currentTimeMillis(),
                    stockCount = updatedStock
                )
                repository.updateMedication(updatedMed)
                com.zmstore.projectr.util.MedicationAlarmHelper.scheduleAlarm(context, updatedMed)
                
                // Refresh all widgets
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = android.content.ComponentName(context, MedicationWidget::class.java)
                onUpdate(context, appWidgetManager, appWidgetManager.getAppWidgetIds(componentName))
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val medications = repository.allMedications.first()
                val nextMed = medications
                    .filter { it.isActive }
                    .minByOrNull { 
                        val nextDose = if (it.lastTakenTimestamp == 0L) 0L 
                                      else it.lastTakenTimestamp + (it.intervalHours.toLong() * 3600 * 1000)
                        if (nextDose == 0L) Long.MAX_VALUE else nextDose
                    }

                if (nextMed != null) {
                    val nextDoseTime = if (nextMed.lastTakenTimestamp == 0L) 0L 
                                      else nextMed.lastTakenTimestamp + (nextMed.intervalHours.toLong() * 3600 * 1000)
                    
                    val timeStr = if (nextDoseTime == 0L) "Pendente" 
                                 else SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nextDoseTime))

                    views.setTextViewText(R.id.widget_med_name, nextMed.name)
                    views.setTextViewText(R.id.widget_time, "Próxima dose: $timeStr")

                    // Show confirm button and set action
                    views.setViewVisibility(R.id.widget_btn_confirm, View.VISIBLE)
                    val intent = Intent(context, MedicationWidget::class.java).apply {
                        action = ACTION_CONFIRM_DOSE
                        putExtra(EXTRA_MED_ID, nextMed.id)
                        putExtra(EXTRA_MED_NAME, nextMed.name)
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        nextMed.id,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_btn_confirm, pendingIntent)
                } else {
                    views.setTextViewText(R.id.widget_med_name, "Nenhum agendado")
                    views.setTextViewText(R.id.widget_time, "Abra o app para adicionar")
                    views.setViewVisibility(R.id.widget_btn_confirm, View.GONE)
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
