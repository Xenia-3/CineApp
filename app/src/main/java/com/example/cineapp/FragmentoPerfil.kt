package com.example.cineapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Button
import android.widget.ImageView
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import androidx.fragment.app.Fragment
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.ClearCredentialException
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch


class FragmentoPerfil : Fragment(R.layout.pantalla_perfil) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user == null) {
            startActivity(Intent(requireContext(), ActividadInicioSesion::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            requireActivity().finish()
            return
        }
        requireView().findViewById<TextView>(R.id.tvEmail).text = user.email
        requireView().findViewById<TextView>(R.id.tvUserName).text =
            user.displayName?.takeIf { it.isNotBlank() } ?: "Mi cuenta"
        view.findViewById<View>(R.id.btnEditProfile).setOnClickListener { editProfile() }
        Glide.with(this).load(user.photoUrl)
            .placeholder(R.drawable.icono_perfil).error(R.drawable.icono_perfil)
            .into(view.findViewById<ImageView>(R.id.imgProfile))
        view.findViewById<View>(R.id.btnProfileTickets).setOnClickListener {
            (requireActivity() as ActividadPrincipal).showSection(R.id.nav_tickets)
        }
        view.findViewById<View>(R.id.btnProfileFavorites).setOnClickListener {
            (requireActivity() as ActividadPrincipal).showSection(R.id.nav_favorites)
        }
        view.findViewById<View>(R.id.btnProfileSettings).setOnClickListener {
            val cinema = requireContext().getSharedPreferences("CinemaSelection", Context.MODE_PRIVATE)
                .getString("cinemaName", "Sin seleccionar")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Configuración")
                .setMessage("${user.email.orEmpty()}\n\nCine: $cinema")
                .setPositiveButton("Cambiar cine") { _, _ ->
                    startActivity(Intent(requireContext(), ActividadSeleccionCine::class.java))
                }
                .setNegativeButton("Cerrar", null)
                .show()
        }
        requireView().findViewById<Button>(R.id.btnLogout).setOnClickListener {
            auth.signOut()
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    CredentialManager.create(requireContext())
                        .clearCredentialState(ClearCredentialStateRequest())
                } catch (_: ClearCredentialException) {
                }
                requireContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE).edit().clear().apply()
                requireContext().getSharedPreferences("CinemaSelection", Context.MODE_PRIVATE).edit().clear().apply()
                startActivity(Intent(requireContext(), ActividadInicioSesion::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                requireActivity().finish()
            }
        }

    }

    override fun onResume() {
        super.onResume()
        if (FirebaseAuth.getInstance().currentUser != null) refreshProfile()
    }

    private fun refreshProfile() {
        try {
            val profile = GestorPerfil(requireContext()).getProfile()
            requireView().findViewById<TextView>(R.id.tvUserName).text =
                profile.first.ifBlank { getString(R.string.mi_cuenta) }
            requireView().findViewById<TextView>(R.id.tvUserAge).text =
                profile.second?.let { getString(R.string.edad_perfil, it) }
                    ?: getString(R.string.edad_sin_registrar)
        } catch (_: Exception) {
            Toast.makeText(requireContext(), R.string.error_consultar_perfil, Toast.LENGTH_LONG).show()
        }
    }

    private fun editProfile() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val manager = GestorPerfil(requireContext())
        val profile = try {
            manager.getProfile()
        } catch (_: Exception) {
            Toast.makeText(requireContext(), R.string.error_consultar_perfil, Toast.LENGTH_LONG).show()
            return
        }
        val form = layoutInflater.inflate(R.layout.dialogo_editar_perfil, null)
        val name = form.findViewById<EditText>(R.id.etProfileName)
        val age = form.findViewById<EditText>(R.id.etProfileAge)
        name.setText(profile.first)
        age.setText(profile.second?.toString().orEmpty())
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.DialogoPerfil)
            .setView(form)
            .setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.fondo_dialogo_perfil))
            .create()
        form.findViewById<View>(R.id.btnCancelProfile).setOnClickListener { dialog.dismiss() }
        dialog.setOnShowListener {
            form.findViewById<View>(R.id.btnSaveProfile).setOnClickListener {
                val newName = name.text.toString().trim()
                val newAge = age.text.toString().toIntOrNull()
                if (newName.isBlank()) {
                    name.error = getString(R.string.nombre_perfil_requerido)
                    name.requestFocus()
                    return@setOnClickListener
                }
                if (newAge == null || newAge !in 1..120) {
                    age.error = getString(R.string.edad_perfil_invalida)
                    age.requestFocus()
                    return@setOnClickListener
                }
                try {
                    check(uid != null && FirebaseAuth.getInstance().currentUser?.uid == uid)
                    check(manager.updateProfile(newName, newAge))
                    dialog.dismiss()
                    requireView().findViewById<TextView>(R.id.tvUserName).text = newName
                    requireView().findViewById<TextView>(R.id.tvUserAge).text = getString(R.string.edad_perfil, newAge)
                    Toast.makeText(requireContext(), R.string.perfil_actualizado, Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {
                    Toast.makeText(requireContext(), R.string.error_guardar_perfil, Toast.LENGTH_LONG).show()
                }
            }
        }
        dialog.show()
    }
}
