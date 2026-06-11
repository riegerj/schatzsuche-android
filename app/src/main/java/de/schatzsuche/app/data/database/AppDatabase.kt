package de.schatzsuche.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import de.schatzsuche.app.data.dao.HuntSessionDao
import de.schatzsuche.app.data.dao.HuntStepDao
import de.schatzsuche.app.data.dao.QrCodeDao
import de.schatzsuche.app.data.dao.StepCompletionDao
import de.schatzsuche.app.data.dao.TreasureHuntDao
import de.schatzsuche.app.data.model.HuntSessionEntity
import de.schatzsuche.app.data.model.HuntSessionStatus
import de.schatzsuche.app.data.model.HuntStepEntity
import de.schatzsuche.app.data.model.HuntTheme
import de.schatzsuche.app.data.model.QrCodeEntity
import de.schatzsuche.app.data.model.StepCompletionEntity
import de.schatzsuche.app.data.model.TreasureHuntEntity

class Converters {
    @TypeConverter
    fun fromTheme(theme: HuntTheme): String = theme.name

    @TypeConverter
    fun toTheme(value: String): HuntTheme = HuntTheme.valueOf(value)

    @TypeConverter
    fun fromStatus(status: HuntSessionStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): HuntSessionStatus = HuntSessionStatus.valueOf(value)
}

@Database(
    entities = [
        QrCodeEntity::class,
        TreasureHuntEntity::class,
        HuntStepEntity::class,
        HuntSessionEntity::class,
        StepCompletionEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun qrCodeDao(): QrCodeDao
    abstract fun treasureHuntDao(): TreasureHuntDao
    abstract fun huntStepDao(): HuntStepDao
    abstract fun huntSessionDao(): HuntSessionDao
    abstract fun stepCompletionDao(): StepCompletionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "schatzsuche.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
