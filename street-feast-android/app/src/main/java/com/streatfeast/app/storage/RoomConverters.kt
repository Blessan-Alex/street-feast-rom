package com.streatfeast.app.storage

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class RoomConverters {
    companion object {
        private val gson = Gson()

        @TypeConverter
        @JvmStatic
        fun fromDate(date: Date?): Long? = date?.time

        @TypeConverter
        @JvmStatic
        fun toDate(timestamp: Long?): Date? = timestamp?.let { Date(it) }

        @TypeConverter
        @JvmStatic
        fun fromStringList(value: String?): List<String>? {
            if (value == null || value.isEmpty()) {
                return emptyList()
            }
            val listType = object : TypeToken<List<String>>() {}.type
            return gson.fromJson(value, listType)
        }

        @TypeConverter
        @JvmStatic
        fun toStringList(list: List<String>?): String? {
            if (list == null || list.isEmpty()) {
                return null
            }
            return gson.toJson(list)
        }
    }
}
