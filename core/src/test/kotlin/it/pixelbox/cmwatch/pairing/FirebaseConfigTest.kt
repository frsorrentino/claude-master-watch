package it.pixelbox.cmwatch.pairing

import org.junit.Assert.*
import org.junit.Test

class FirebaseConfigTest {
    private val cfg = FirebaseConfig("k", "p", "1:42:android:ab", "https://p.firebaseio.com")

    @Test fun roundTripsAndDefaultsTheTopic() {
        assertEquals(cfg, FirebaseConfig.fromJson(cfg.toJson()))
        assertEquals("watch", cfg.topic)
        assertNull(FirebaseConfig.fromJson("{"))
        assertNull(FirebaseConfig.fromJson(null))
    }

    @Test fun senderIdComesFromTheAppId() {
        assertEquals("42", cfg.senderId)
        assertEquals("", cfg.copy(appId = "x").senderId)
    }

    @Test fun sameProjectIgnoresTheTopic() {
        assertTrue(cfg.sameProject(cfg.copy(topic = "other")))
        assertFalse(cfg.sameProject(cfg.copy(databaseUrl = "https://q.firebaseio.com")))
    }

    @Test fun fromGoogleServicesResources() {
        val v = mapOf("google_api_key" to "k", "project_id" to "p", "google_app_id" to "1:42:android:ab", "firebase_database_url" to "https://p.firebaseio.com")
        assertEquals(cfg, FirebaseConfig.fromResources(v))
        assertNull(FirebaseConfig.fromResources(v - "firebase_database_url"))
        assertNull(FirebaseConfig.fromResources(v + ("project_id" to "")))
    }

    @Test fun compactAndBack() = assertEquals(cfg, cfg.compact().config())
}
