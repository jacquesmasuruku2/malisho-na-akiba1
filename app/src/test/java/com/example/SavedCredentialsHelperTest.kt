package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.UserRole
import com.example.util.SavedCredentialsHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SavedCredentialsHelperTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        SavedCredentialsHelper.clearAll(context)
    }

    @Test
    fun testSaveAndRetrieveCredential() {
        SavedCredentialsHelper.saveCredential(
            context = context,
            email = "masuruku@malaki.cd",
            password = "SecretPassword123",
            fullName = "Jacques Masuruku",
            phone = "+243 999 123 456",
            role = UserRole.CLIENT
        )

        val list = SavedCredentialsHelper.getSavedCredentials(context)
        assertEquals(1, list.size)

        val cred = list.first()
        assertEquals("masuruku@malaki.cd", cred.email)
        assertEquals("SecretPassword123", cred.password)
        assertEquals("Jacques Masuruku", cred.fullName)
        assertEquals(UserRole.CLIENT, cred.role)

        val lastCred = SavedCredentialsHelper.getLastSavedCredential(context)
        assertNotNull(lastCred)
        assertEquals("masuruku@malaki.cd", lastCred?.email)
        assertEquals("SecretPassword123", lastCred?.password)
    }

    @Test
    fun testUpdateExistingCredential() {
        SavedCredentialsHelper.saveCredential(
            context = context,
            email = "client@malaki.cd",
            password = "old_password",
            fullName = "Client Test",
            role = UserRole.CLIENT
        )

        // Sauvegarde d'un nouveau mot de passe pour le même compte
        SavedCredentialsHelper.saveCredential(
            context = context,
            email = "client@malaki.cd",
            password = "new_secure_password",
            fullName = "Client Test",
            role = UserRole.CLIENT
        )

        val list = SavedCredentialsHelper.getSavedCredentials(context)
        assertEquals(1, list.size)
        assertEquals("new_secure_password", list.first().password)
    }

    @Test
    fun testRemoveCredential() {
        SavedCredentialsHelper.saveCredential(
            context = context,
            email = "user1@malaki.cd",
            password = "pwd1",
            fullName = "User One"
        )
        SavedCredentialsHelper.saveCredential(
            context = context,
            email = "user2@malaki.cd",
            password = "pwd2",
            fullName = "User Two"
        )

        assertEquals(2, SavedCredentialsHelper.getSavedCredentials(context).size)

        SavedCredentialsHelper.removeCredential(context, "user1@malaki.cd")
        val remaining = SavedCredentialsHelper.getSavedCredentials(context)
        assertEquals(1, remaining.size)
        assertEquals("user2@malaki.cd", remaining.first().email)
    }

    @Test
    fun testClearAllCredentials() {
        SavedCredentialsHelper.saveCredential(
            context = context,
            email = "test@malaki.cd",
            password = "pwd",
            fullName = "Test User"
        )
        SavedCredentialsHelper.clearAll(context)
        assertTrue(SavedCredentialsHelper.getSavedCredentials(context).isEmpty())
        assertNull(SavedCredentialsHelper.getLastSavedCredential(context))
    }
}
