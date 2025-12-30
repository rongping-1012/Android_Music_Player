package com.example.music_player.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.Date

@Database(
    entities = [User::class, PlayHistory::class, FavoriteMusic::class],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun playHistoryDao(): PlayHistoryDao
    abstract fun favoriteMusicDao(): FavoriteMusicDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS play_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        username TEXT NOT NULL,
                        songPath TEXT NOT NULL,
                        songName TEXT NOT NULL,
                        playTime INTEGER NOT NULL
                    )
                """)
            }
        }
        
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS favorite_music (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        username TEXT NOT NULL,
                        songPath TEXT NOT NULL,
                        songName TEXT NOT NULL
                    )
                """)
            }
        }
        
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 添加 darkMode 字段到 user_table
                database.execSQL("""
                    ALTER TABLE user_table ADD COLUMN darkMode INTEGER NOT NULL DEFAULT 0
                """)
            }
        }
        
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 添加 gender 字段到 user_table，默认为 MALE (0)
                database.execSQL("""
                    ALTER TABLE user_table ADD COLUMN gender TEXT NOT NULL DEFAULT 'MALE'
                """)
            }
        }
        
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 添加 lastLoginTime 字段到 user_table
                database.execSQL("""
                    ALTER TABLE user_table ADD COLUMN lastLoginTime INTEGER
                """)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "user_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    @TypeConverter
    fun fromGender(gender: Gender): String {
        return gender.name
    }
    
    @TypeConverter
    fun toGender(value: String): Gender {
        return Gender.valueOf(value)
    }
}
