package com.example.cineapp

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class ActividadInicioSesion : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getSharedPreferences("UserSession", MODE_PRIVATE).edit().clear().apply()
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            openCinemaSelection()
            return
        }
        setContentView(R.layout.pantalla_inicio_sesion)

        val emailInput = findViewById<EditText>(R.id.etEmail)
        val passwordInput = findViewById<EditText>(R.id.etPassword)
        val loginButton = findViewById<Button>(R.id.btnLogin)
        val registerLink = findViewById<TextView>(R.id.tvRegister)
        val googleButton = findViewById<Button>(R.id.btnGoogle)
        googleButton.setOnClickListener {
            lifecycleScope.launch {
                googleButton.isEnabled = false
                loginButton.isEnabled = false
                registerLink.isEnabled = false
                var signingIn = false
                try {
                    val googleOption = GetSignInWithGoogleOption.Builder(
                        getString(R.string.default_web_client_id)
                    ).build()
                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleOption)
                        .build()
                    val credential = CredentialManager.create(this@ActividadInicioSesion)
                        .getCredential(this@ActividadInicioSesion, request).credential
                    if (credential !is CustomCredential ||
                        credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        Toast.makeText(this@ActividadInicioSesion, "No se pudo iniciar sesión con Google", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val firebaseCredential = GoogleAuthProvider.getCredential(token, null)
                    auth.signInWithCredential(firebaseCredential).addOnCompleteListener(this@ActividadInicioSesion) { task ->
                        googleButton.isEnabled = true
                        loginButton.isEnabled = true
                        registerLink.isEnabled = true
                        if (task.isSuccessful) {
                            openCinemaSelection()
                        } else {
                            Toast.makeText(this@ActividadInicioSesion, "No se pudo iniciar sesión con Google. Intenta nuevamente", Toast.LENGTH_SHORT).show()
                        }
                    }
                    signingIn = true
                } catch (_: GetCredentialCancellationException) {
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    Toast.makeText(this@ActividadInicioSesion, "No se pudo iniciar sesión con Google. Intenta nuevamente", Toast.LENGTH_SHORT).show()
                } finally {
                    if (!signingIn) {
                        googleButton.isEnabled = true
                        loginButton.isEnabled = true
                        registerLink.isEnabled = true
                    }
                }
            }
        }
        findViewById<ImageView>(R.id.ivTogglePassword).setOnClickListener {
            val visible = passwordInput.inputType == android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            passwordInput.inputType = if (visible) {
                android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            } else {
                android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            }
            passwordInput.setSelection(passwordInput.text.length)
        }
        registerLink.setOnClickListener {
            startActivity(Intent(this, ActividadRegistro::class.java))
        }
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Ingresa tu correo y contraseña", Toast.LENGTH_SHORT).show()
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.error = "Ingresa un correo válido"
            } else {
                loginButton.isEnabled = false
                googleButton.isEnabled = false
                registerLink.isEnabled = false
                loginButton.text = "Iniciando sesión..."
                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
                    loginButton.isEnabled = true
                    googleButton.isEnabled = true
                    registerLink.isEnabled = true
                    loginButton.text = "Iniciar sesión"
                    if (task.isSuccessful) {
                        openCinemaSelection()
                    } else {
                        val message = when (task.exception) {
                            is FirebaseAuthInvalidCredentialsException, is FirebaseAuthInvalidUserException ->
                                "Correo o contraseña incorrectos"
                            is FirebaseNetworkException -> "Revisa tu conexión a internet"
                            is FirebaseTooManyRequestsException -> "Demasiados intentos. Intenta más tarde"
                            else -> "No se pudo iniciar sesión. Intenta nuevamente"
                        }
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun openCinemaSelection() {
        startActivity(Intent(this, ActividadSeleccionCine::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
