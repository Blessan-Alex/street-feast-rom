package com.streatfeast.app.storage

import androidx.room.TypeConverter
import java.util.Date

class RoomConverters {

    @TypeConverter
    fun fromDate(date: Date?): Long? = date?.time

    @TypeConverter
    fun toDate(timestamp: Long?): Date? = timestamp?.let { Date(it) }
}
