package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppLockDatabase
import com.example.data.AppLockRepository
import com.example.data.PreferencesManager
import com.example.security.CryptoManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class AppLockRepositoryTest {

    private lateinit var db: AppLockDatabase
    private lateinit var repository: AppLockRepository
    private lateinit var context: Context

    @Before
    fun createDb() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppLockDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        
        val prefManager = PreferencesManager(context)
        val cryptoManager = CryptoManager()
        
        repository = AppLockRepository(db.appLockDao(), prefManager, cryptoManager)
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testAppLockAndUnlockPersistence() = runBlocking {
        val targetPkg = "com.example.messenger"
        
        // Assert initially not protected
        assertFalse(repository.isAppProtected(targetPkg))
        
        // Protect it
        repository.protectApp(targetPkg)
        assertTrue(repository.isAppProtected(targetPkg))
        
        // Retrieve list flow
        val protectedApps = repository.protectedApps.first()
        assertEquals(1, protectedApps.size)
        assertEquals(targetPkg, protectedApps[0].packageName)
        
        // Unprotect it
        repository.unprotectApp(targetPkg)
        assertFalse(repository.isAppProtected(targetPkg))
    }

    @Test
    fun testActivityLogsInsertion() = runBlocking {
        // Assert logs initially empty (or contains setup logs)
        repository.clearLogs()
        var logs = repository.activityLogs.first()
        assertEquals(0, logs.size)
        
        // Log an event
        repository.logEvent("TEST_EVENT", "This is an audit log message.")
        logs = repository.activityLogs.first()
        
        assertEquals(1, logs.size)
        assertEquals("TEST_EVENT", logs[0].eventType)
        assertEquals("This is an audit log message.", logs[0].details)
    }

    @Test
    fun testSecureBackupAndRestoreFlow() = runBlocking {
        // Setup state to backup
        repository.clearLogs()
        repository.setPasswordDetails("PIN", "hashed_pwd", "salt_pwd")
        repository.setSecurityQuestionDetails(2, "hashed_answer")
        repository.setLockBehavior("CUSTOM_DELAY")
        repository.protectApp("com.target.secret")
        
        // Export backup
        val backupString = repository.exportBackup()
        assertTrue(backupString.isNotEmpty())
        
        // Reset everything to default
        repository.resetAll()
        assertEquals(0, repository.protectedApps.first().size)
        assertEquals("PIN", repository.passwordType.first())
        
        // Import backup
        val importSuccess = repository.importBackup(backupString)
        assertTrue(importSuccess)
        
        // Verify state restored
        assertEquals("PIN", repository.passwordType.first())
        assertEquals("hashed_pwd", repository.passwordHash.first())
        assertEquals("hashed_answer", repository.securityQuestionAnswerHash.first())
        assertEquals("CUSTOM_DELAY", repository.lockBehavior.first())
        
        val protectedApps = repository.protectedApps.first()
        assertEquals(1, protectedApps.size)
        assertEquals("com.target.secret", protectedApps[0].packageName)
    }
}
