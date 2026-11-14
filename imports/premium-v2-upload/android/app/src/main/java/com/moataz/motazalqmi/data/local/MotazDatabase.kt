package com.moataz.motazalqmi.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CategoryEntity::class, ArticleEntity::class, QuoteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MotazDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun articleDao(): ArticleDao
    abstract fun quoteDao(): QuoteDao
}
