package com.streatfeast.app.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        OrderEntity::class,
        OrderItemEntity::class,
        CategoryEntity::class,
        ItemEntity::class,
        FrequentItemEntity::class,
        MenuMetadataEntity::class
    ],
    version = 6,
    exportSchema = true
)
@TypeConverters(RoomConverters::class)
abstract class StreetFeastDatabase : RoomDatabase() {

    abstract fun orderDao(): OrderDao
    abstract fun menuDao(): MenuDao

    companion object {
        @Volatile
        private var instance: StreetFeastDatabase? = null

        fun getInstance(context: Context): StreetFeastDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    StreetFeastDatabase::class.java,
                    "streetfeast.db"
                ).fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}



