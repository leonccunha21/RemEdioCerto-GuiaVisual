package com.zmstore.projectr.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.zmstore.projectr.data.model.DoseHistory
import com.zmstore.projectr.data.model.Medication

import com.zmstore.projectr.data.model.Profile

@Database(
    entities = [Medication::class, DoseHistory::class, Profile::class], 
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "remedio_certo_db"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Insert default profile on creation
                        val contentValues = android.content.ContentValues().apply {
                            put("name", "Meu Perfil")
                            put("color", 0xFF008080.toInt())
                            put("isDefault", 1)
                        }
                        db.insert("profiles", android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, contentValues)
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
