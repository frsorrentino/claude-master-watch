package it.pixelbox.cmwatch.transport

import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Orologio anonimo su Firebase: uid registrato al pairing, idToken per le chiamate REST. Solo con google-services.json. */
object FirebaseAuthToken {
    fun databaseUrl(): String? = runCatching { FirebaseApp.getInstance().options.databaseUrl }.getOrNull()

    fun uid(): String? = runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull()

    private val lock = Mutex()

    /** Un solo sign-in anonimo alla volta: tre chiamate concorrenti all'avvio creavano tre utenti (claude-master, 12/09 15:53). */
    suspend fun token(): String? = lock.withLock {
        withContext(Dispatchers.IO) {
            runCatching {
                val auth = FirebaseAuth.getInstance()
                val user = auth.currentUser ?: Tasks.await(auth.signInAnonymously()).user ?: return@runCatching null
                Tasks.await(user.getIdToken(false)).token
            }.getOrNull()
        }
    }
}
