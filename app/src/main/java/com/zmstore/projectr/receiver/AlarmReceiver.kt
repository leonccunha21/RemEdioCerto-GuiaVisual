package com.zmstore.projectr.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.zmstore.projectr.R

import android.app.PendingIntent
import com.zmstore.projectr.data.model.DoseHistory
import com.zmstore.projectr.data.repository.MedicationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.zmstore.projectr.util.MedicationAlarmHelper

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: MedicationRepository

    companion object {
        const val ACTION_CONFIRM = "com.zmstore.projectr.ACTION_CONFIRM"
        const val ACTION_SNOOZE = "com.zmstore.projectr.ACTION_SNOOZE"
        const val EXTRA_MEDICATION_ID = "MEDICATION_ID"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getIntExtra(EXTRA_MEDICATION_ID, -1)
        if (medicationId == -1) return

        when (intent.action) {
            ACTION_CONFIRM -> handleConfirm(context, medicationId)
            ACTION_SNOOZE -> handleSnooze(context, medicationId)
            else -> showNotification(context, medicationId)
        }
    }

    private fun showNotification(context: Context, medicationId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val medication = repository.getMedicationById(medicationId) ?: return@launch
            if (!medication.isActive) return@launch

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "medication_reminders"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Lembretes de Medicamentos",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notificações para tomar seus remédios"
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val lowStockAlert = when {
                medication.stockCount in 1..5 -> "\n\n⚠️ Atenção: Estoque baixo (${medication.stockCount} restantes)!"
                medication.stockCount <= 0 -> "\n\n🚨 Alerta: Medicamento sem estoque!"
                else -> ""
            }

            // Action: Confirm
            val confirmIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_CONFIRM
                putExtra(EXTRA_MEDICATION_ID, medicationId)
            }
            val confirmPendingIntent = PendingIntent.getBroadcast(
                context, medicationId * 10 + 1, confirmIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Action: Snooze
            val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_SNOOZE
                putExtra(EXTRA_MEDICATION_ID, medicationId)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                context, medicationId * 10 + 2, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.iconeapp)
                .setContentTitle("Hora do Remédio: ${medication.name}")
                .setContentText("Não esqueça de tomar sua dose agora.$lowStockAlert")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_check_circle, context.getString(R.string.notification_action_confirm), confirmPendingIntent)
                .addAction(R.drawable.ic_notifications, context.getString(R.string.notification_action_snooze), snoozePendingIntent)
                .setStyle(NotificationCompat.BigTextStyle().bigText("Não esqueça de tomar sua dose de ${medication.name} agora.$lowStockAlert"))
                .build()

            notificationManager.notify(medicationId, notification)
 
            // Reschedule next regular alarm
            MedicationAlarmHelper.scheduleAlarm(context, medication)
        }
    }

    private fun handleConfirm(context: Context, medicationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(medicationId)

        CoroutineScope(Dispatchers.IO).launch {
            repository.getMedicationById(medicationId)?.let { med ->
                repository.insertDoseHistory(DoseHistory(medicationId = med.id, medicationName = med.name))
                val updatedStock = if (med.stockCount > 0) med.stockCount - 1 else 0
                val updatedMed = med.copy(
                    lastTakenTimestamp = System.currentTimeMillis(),
                    stockCount = updatedStock
                )
                repository.updateMedication(updatedMed)
                MedicationAlarmHelper.scheduleAlarm(context, updatedMed)
            }
        }
    }

    private fun handleSnooze(context: Context, medicationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(medicationId)

        CoroutineScope(Dispatchers.IO).launch {
            repository.getMedicationById(medicationId)?.let { med ->
                MedicationAlarmHelper.scheduleSnooze(context, med)
            }
        }
    }
}
