package com.example.cineapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.google.firebase.auth.FirebaseAuth

class GestorPerfil(
    context: Context,
    private val helper: AyudanteBaseFavoritos = AyudanteBaseFavoritos.getInstance(context)
) {
    private val legacyProfiles = context.applicationContext
        .getSharedPreferences("UserProfiles", Context.MODE_PRIVATE)

    fun getProfile(): Pair<String, Int?> {
        val user = FirebaseAuth.getInstance().currentUser ?: error("Inicia sesión para consultar tu perfil")
        val age = legacyProfiles.getInt("age_${user.uid}", 0).takeIf { it > 0 }
        return getProfileForUser(user.uid, user.displayName.orEmpty(), age)
    }

    private fun getProfileForUser(uid: String, initialName: String, initialAge: Int?): Pair<String, Int?> {
        require(uid.isNotBlank())
        val db = helper.writableDatabase
        db.insertWithOnConflict("perfiles", null, ContentValues().apply {
            put("usuario_id", uid)
            put("nombre", initialName)
            put("edad", initialAge)
        }, SQLiteDatabase.CONFLICT_IGNORE)
        return db.query("perfiles", arrayOf("nombre", "edad"), "usuario_id = ?",
            arrayOf(uid), null, null, null).use { cursor ->
            check(cursor.moveToFirst()) { "No se pudo cargar el perfil" }
            cursor.getString(0) to if (cursor.isNull(1)) null else cursor.getInt(1)
        }
    }

    fun updateProfile(name: String, age: Int): Boolean {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: error("Inicia sesión para editar tu perfil")
        return updateProfileForUser(uid, name, age)
    }

    private fun updateProfileForUser(uid: String, name: String, age: Int): Boolean {
        require(uid.isNotBlank() && name.trim().isNotEmpty() && name.trim().length <= 80)
        require(age in 1..120)
        return helper.writableDatabase.update("perfiles", ContentValues().apply {
            put("nombre", name.trim())
            put("edad", age)
        }, "usuario_id = ?", arrayOf(uid)) == 1
    }
}
