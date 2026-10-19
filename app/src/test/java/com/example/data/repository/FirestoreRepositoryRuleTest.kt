package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class FirestoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createUser_validPayload_succeeds() = runBlocking {
    val uid = signInTestUser("user_${UUID.randomUUID()}@test.com")
    val userDoc = mapOf(
      "id" to uid,
      "fullName" to "Test User",
      "email" to "test@example.com",
      "role" to "CLIENT"
    )

    withTimeout(5000L) {
      firestore.collection("users").document(uid).set(userDoc).await()
    }

    val snapshot = withTimeout(5000L) {
      firestore.collection("users").document(uid).get().await()
    }
    assertTrue(snapshot.exists())
    assertEquals("Test User", snapshot.getString("fullName"))
  }

  @Test
  fun getLoans_authenticatedUser_succeeds() = runBlocking {
    signInTestUser("user_${UUID.randomUUID()}@test.com")
    val result = withTimeout(5000L) {
      firestore.collection("loans").get().await()
    }
    assertNotNull(result)
  }

  @Test
  fun getProtectedData_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    try {
      withTimeout(5000L) {
        firestore.collection("users").document("non_existent").get().await()
      }
      fail("Expected FirebaseFirestoreException PERMISSION_DENIED")
    } catch (e: FirebaseFirestoreException) {
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
    } catch (e: Exception) {
      // In case wrapped
      val cause = e.cause
      if (cause is FirebaseFirestoreException) {
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, cause.code)
      } else {
        assertTrue(e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true)
      }
    }
  }
}
