package com.comunidadconectada.app.ui;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

/** Traduce excepciones de Firebase Auth a mensajes comprensibles. */
final class AuthErrors {
    private AuthErrors() { }
    static String friendly(Exception e) {
        if (e instanceof FirebaseNetworkException) return "Sin conexión. Revisa tu internet e intenta de nuevo.";
        if (e instanceof FirebaseTooManyRequestsException) return "Demasiados intentos. Espera unos minutos.";
        if (e instanceof FirebaseAuthInvalidUserException) return "No existe una cuenta con ese correo.";
        if (e instanceof FirebaseAuthInvalidCredentialsException) return "Correo o contraseña incorrectos.";
        if (e instanceof FirebaseAuthUserCollisionException) return "Ese correo ya está registrado.";
        if (e instanceof FirebaseAuthWeakPasswordException) return "La contraseña es muy débil (mínimo 6 caracteres).";
        if (e instanceof FirebaseAuthException) return "No se pudo completar la autenticación.";
        return "Ocurrió un error inesperado. Intenta de nuevo.";
    }
}
