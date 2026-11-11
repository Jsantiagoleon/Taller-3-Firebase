package com.example.tallerfirebase.data.repository

import com.example.tallerfirebase.data.model.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository(
    database: FirebaseDatabase = FirebaseDatabase.getInstance()
) {
    private val usersRef = database.getReference(AuthRepository.USERS_NODE)

    // Escucha en tiempo real los datos de un usuario
    fun observeUser(uid: String): Flow<User?> = callbackFlow {
        val ref = usersRef.child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(User::class.java))
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // Escucha en tiempo real los usuarios conectados (máximo 100)
    fun observeOnlineUsers(): Flow<List<User>> = callbackFlow {
        val query = usersRef
            .orderByChild(FIELD_CONECTADO)
            .equalTo(true)
            .limitToFirst(MAX_ONLINE_USERS)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.children.mapNotNull { it.getValue(User::class.java) })
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    // Cambia el estado conectado/desconectado.
    // Al conectarse se programa onDisconnect para que, si la app se cierra o
    // pierde conexión, Firebase marque automáticamente al usuario como desconectado.
    suspend fun setConnected(uid: String, connected: Boolean) {
        val ref = usersRef.child(uid).child(FIELD_CONECTADO)
        if (connected) {
            ref.onDisconnect().setValue(false).await()
        } else {
            ref.onDisconnect().cancel().await()
        }
        ref.setValue(connected).await()
    }

    suspend fun updateLocation(uid: String, latitud: Double, longitud: Double) {
        usersRef.child(uid).updateChildren(
            mapOf(
                FIELD_LATITUD to latitud,
                FIELD_LONGITUD to longitud
            )
        ).await()
    }

    suspend fun updateProfile(
        uid: String,
        nombre: String,
        identificacion: String,
        telefono: String,
        fotoUrl: String? = null
    ) {
        val updates = mutableMapOf<String, Any>(
            FIELD_NOMBRE to nombre,
            FIELD_IDENTIFICACION to identificacion,
            FIELD_TELEFONO to telefono
        )
        fotoUrl?.let { updates[FIELD_FOTO_URL] = it }
        usersRef.child(uid).updateChildren(updates).await()
    }

    companion object {
        const val FIELD_NOMBRE = "nombre"
        const val FIELD_IDENTIFICACION = "identificacion"
        const val FIELD_TELEFONO = "telefono"
        const val FIELD_FOTO_URL = "fotoUrl"
        const val FIELD_CONECTADO = "conectado"
        const val FIELD_LATITUD = "latitud"
        const val FIELD_LONGITUD = "longitud"
        const val MAX_ONLINE_USERS = 100
    }
}
