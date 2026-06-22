package com.example

import android.app.Application
import androidx.room.*
import com.example.data.database.AppDatabase
import com.example.data.repository.OsintRepository

class MainApplication : Application() {

    // Database and repository initialized lazily
    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "pdz_osint_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    val repository: OsintRepository by lazy {
        OsintRepository(database.dao(), applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: MainApplication
            private set
    }
}
