package com.example.tallerfirebase.data.repository

import com.example.tallerfirebase.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    database: FirebaseDatabase = FirebaseDatabase.getInstance()
) {
    private val usersRef = database.getReference(USERS_NODE)

    val currentUid: String?
        get() = auth.currentUser?.uid

    fun isLoggedIn(): Boolean = auth.currentUser != null

    // Crea el usuario en Firebase Auth y guarda sus datos en Realtime Database
    suspend fun register(
        nombre: String,
        identificacion: String,
        correo: String,
        password: String,
        telefono: String
    ) {
        val result = auth.createUserWithEmailAndPassword(correo, password).await()
        val uid = result.user?.uid ?: throw IllegalStateException("UID nulo")
        val user = User(
            uid = uid,
            nombre = nombre,
            identificacion = identificacion,
            correo = correo,
            telefono = telefono
        )
        usersRef.child(uid).setValue(user).await()
    }

    suspend fun login(correo: String, password: String) {
        auth.signInWithEmailAndPassword(correo, password).await()
    }

    fun logout() {
        auth.signOut()
    }

    companion object {
        const val USERS_NODE = "users"
    }
}
