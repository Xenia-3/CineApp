package com.example.cineapp

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

class ActividadRegistro : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pantalla_registro)

        val emailInput = findViewById<EditText>(R.id.etRegisterEmail)
        val ageInput = findViewById<EditText>(R.id.etRegisterAge)
        val passwordInput = findViewById<EditText>(R.id.etRegisterPassword)
        val confirmPasswordInput = findViewById<EditText>(R.id.etRegisterConfirmPassword)
        val registerButton = findViewById<Button>(R.id.btnRegister)
        val loginLink = findViewById<TextView>(R.id.tvLogin)
        loginLink.setOnClickListener { finish() }

        registerButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()
            val age = ageInput.text.toString().toIntOrNull()
            val confirmPassword = confirmPasswordInput.text.toString()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Ingresa tu correo y contraseña", Toast.LENGTH_SHORT).show()
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.error = "Ingresa un correo válido"
            } else if (age == null || age <= 0) {
                ageInput.error = "Ingresa tu edad en años"
            } else if (password.length < 6) {
                passwordInput.error = "Utiliza al menos 6 caracteres"
            } else if (confirmPassword.isEmpty() || password != confirmPassword) {
                confirmPasswordInput.error = "Las contraseñas deben coincidir"
            } else {
                registerButton.isEnabled = false
                loginLink.isEnabled = false
                registerButton.text = "Creando cuenta..."
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        registerButton.isEnabled = true
                        loginLink.isEnabled = true
                        registerButton.text = "Registrarse"
                        if (task.isSuccessful) {
                            val uid = task.result.user!!.uid
                            getSharedPreferences("UserProfiles", MODE_PRIVATE).edit()
                                .putInt("age_$uid", age)
                                .apply()
                            Toast.makeText(this, "Cuenta creada", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, ActividadSeleccionCine::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                            finish()
                        } else {
                            val message = when (task.exception) {
                                is FirebaseAuthUserCollisionException -> "Este correo ya está registrado"
                                is FirebaseAuthWeakPasswordException -> "La contraseña no cumple los requisitos de seguridad"
                                is FirebaseNetworkException -> "Revisa tu conexión a internet"
                                is FirebaseTooManyRequestsException -> "Demasiados intentos. Intenta más tarde"
                                else -> "No se pudo crear la cuenta. Revisa los datos e intenta nuevamente"
                            }
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }
    }
}

