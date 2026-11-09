package com.jinatra.wishplay

import android.security.NetworkSecurityPolicy
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Test
import java.net.URL
import java.util.UUID
import javax.net.ssl.HttpsURLConnection

class AuthenticationTransportTest {
    @Test fun googleAuthenticationAndDatabaseEndpointsAcceptValidSystemCertificates() {
        val failures = mutableListOf<String>()
        listOf("identitytoolkit.googleapis.com", "securetoken.googleapis.com", "firestore.googleapis.com").forEach { host ->
            val connection = URL("https://$host/").openConnection() as HttpsURLConnection
            try {
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                // An HTTP error at the root URL is fine: reaching HTTP proves the TLS handshake worked.
                assertTrue("No HTTP response from $host", connection.responseCode in 100..599)
                assertTrue("No server certificate from $host", connection.serverCertificates.isNotEmpty())
            } catch (error: Exception) {
                failures += "$host: ${error.javaClass.simpleName}: ${error.message}"
            } finally { connection.disconnect() }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test fun firebaseAuthenticationReachesTheServerWithoutUsingARealAccount() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = FirebaseApp.initializeApp(context, FirebaseApp.getInstance().options, "auth-transport-${UUID.randomUUID()}")
        try {
            val failure = runCatching {
                FirebaseAuth.getInstance(app).signInWithCustomToken("deliberately-invalid-test-token").await()
            }.exceptionOrNull()
            // The backend must reject the deliberately invalid token, not fail TLS pin verification.
            assertTrue("Expected Firebase's invalid-token response, got $failure", failure is FirebaseAuthException)
            assertEquals("ERROR_INVALID_CUSTOM_TOKEN", (failure as FirebaseAuthException).errorCode)
            assertNull(FirebaseAuth.getInstance(app).currentUser)
        } finally { app.delete() }
    }

    @Test fun plaintextConnectionsRemainBlocked() {
        assertFalse(NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted("identitytoolkit.googleapis.com"))
        assertFalse(NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted("firestore.googleapis.com"))
    }
}
