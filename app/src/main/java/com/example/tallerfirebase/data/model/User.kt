package com.example.tallerfirebase.data.model

// Usuario guardado en Realtime Database bajo /users/{uid}.
// Usa "var" y valores por defecto para que Firebase pueda leerlo y escribirlo.
// La contraseña NO se guarda aquí: la administra Firebase Authentication.
data class User(
    var uid: String = "",
    var nombre: String = "",
    var identificacion: String = "",
    var correo: String = "",
    var telefono: String = "",
    var latitud: Double = 0.0,
    var longitud: Double = 0.0,
    var conectado: Boolean = false,
    var fotoUrl: String = ""
)
