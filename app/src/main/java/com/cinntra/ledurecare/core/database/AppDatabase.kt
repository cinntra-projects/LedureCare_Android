package com.cinntra.ledurecare.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cinntra.ledurecare.feature.auth.data.local.UserDao
import com.cinntra.ledurecare.feature.auth.data.local.UserEntity

@Database(entities = [UserEntity::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}
