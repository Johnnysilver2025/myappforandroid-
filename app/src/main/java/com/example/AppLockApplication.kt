package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.example.data.AppLockDao
import com.example.data.AppLockDatabase
import com.example.data.AppLockRepository
import com.example.data.PreferencesManager
import com.example.security.CryptoManager
import com.example.security.AppLockManager

class AppContainer(private val context: Context) {
    val database: AppLockDatabase by lazy {
        Room.databaseBuilder(
            context,
            AppLockDatabase::class.java,
            "aegis_applock_db"
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    val appLockDao: AppLockDao by lazy {
        database.appLockDao()
    }

    val preferencesManager: PreferencesManager by lazy {
        PreferencesManager(context)
    }

    val cryptoManager: CryptoManager by lazy {
        CryptoManager()
    }

    val appLockManager: AppLockManager by lazy {
        AppLockManager(context)
    }

    val repository: AppLockRepository by lazy {
        AppLockRepository(appLockDao, preferencesManager, cryptoManager)
    }
}

class AppLockApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
