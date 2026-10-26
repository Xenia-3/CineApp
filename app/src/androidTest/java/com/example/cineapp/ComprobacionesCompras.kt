package com.example.cineapp

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth


class ComprobacionesCompras : Instrumentation() {
    private var profilesOnly = false
    private var errorsOnly = false
    private var externalOnly = false
    private var persistenceStage: String? = null
    private var loginOnly = false

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        profilesOnly = arguments?.getString("profilesOnly") == "true"
        errorsOnly = arguments?.getString("errorsOnly") == "true"
        externalOnly = arguments?.getString("externalOnly") == "true"
        persistenceStage = arguments?.getString("persistenceStage")
        loginOnly = arguments?.getString("loginOnly") == "true"
        start()
    }

    override fun onStart() {
        if (loginOnly) {
            checkLoginNavigation()
            return
        }
        if (persistenceStage != null) {
            checkProcessPersistence()
            return
        }
        if (externalOnly) {
            checkExternalServices()
            return
        }
        if (errorsOnly) {
            checkDatabaseErrors()
            return
        }
        if (profilesOnly) {
            checkProfiles()
            return
        }
        val databaseName = "purchase_checks.db"
        val output = Bundle()
        var helper: AyudanteBaseFavoritos? = null
        try {
            targetContext.deleteDatabase(databaseName)
            createVersionOne(targetContext, databaseName)
            helper = AyudanteBaseFavoritos(targetContext, databaseName)
            val manager = GestorBoletos(targetContext, helper)
            val first = Boleto("checkout-1", 100, "Película de prueba", "/poster.jpg", "apopa",
                "Cine de prueba", "2099-01-01", "7:30 PM", "2D ESPAÑOL", listOf("A3", "A4"), 950)
            manager.savePurchase("account-a", first)
            manager.savePurchase("account-a", first)
            check(manager.getTickets("account-a") == listOf(first)) { "Duplicate or altered ticket" }
            check(manager.getTickets("account-b").isEmpty()) { "Tickets leaked across accounts" }
            val selection = Bundle().apply {
                putString("cinemaId", first.cinemaId)
                putInt("movieId", first.movieId)
                putString("date", first.date)
                putString("time", first.time)
                putString("format", first.format)
            }
            check(manager.occupiedSeats(selection) == setOf("A3", "A4"))
            var conflictRejected = false
            try {
                manager.savePurchase("account-b", first.copy(purchaseId = "conflict", seats = listOf("B1", "A3")))
            } catch (_: IllegalStateException) {
                conflictRejected = true
            }
            check(conflictRejected && manager.getTickets("account-b").isEmpty())
            check("B1" !in manager.occupiedSeats(selection)) { "Transaction did not roll back" }
            val second = first.copy(purchaseId = "checkout-2", date = "2099-01-02")
            manager.savePurchase("account-b", second)
            check(manager.getTickets("account-b") == listOf(second))
            helper.readableDatabase.rawQuery("SELECT titulo FROM favoritos WHERE usuario_id = ?",
                arrayOf("legacy-user")).use { check(it.moveToFirst() && it.getString(0) == "Favorito anterior") }
            helper.close()
            helper = AyudanteBaseFavoritos(targetContext, databaseName)
            val reopened = GestorBoletos(targetContext, helper)
            check(reopened.getTickets("account-a") == listOf(first)) { "Purchase lost after reopening" }
            check(reopened.getTickets("account-b") == listOf(second))
            val purchaseArguments = Bundle().apply {
                putString("purchaseId", "dialog-regression")
                putInt("movieId", 100)
                putStringArrayList("seats", arrayListOf("A3"))
            }
            val dialog = DialogoProgresoPago.newInstance(purchaseArguments)
            check(dialog.requireArguments().getString("purchaseId") == "dialog-regression")
            check(dialog.requireArguments().getInt("movieId") == 100)
            check(dialog.requireArguments().getStringArrayList("seats") == arrayListOf("A3"))
            purchaseArguments.putString("purchaseId", "changed")
            check(dialog.requireArguments().getString("purchaseId") == "dialog-regression")
            onUi {
                val themed = android.view.ContextThemeWrapper(targetContext, R.style.Theme_CineApp)
                val form = android.view.LayoutInflater.from(themed).inflate(R.layout.pantalla_pago, null, false)
                val expiry = form.findViewById<EditText>(R.id.etCardExpiry)
                val setup = FragmentoPago::class.java.getDeclaredMethod("setupExpiryField", EditText::class.java)
                setup.isAccessible = true
                setup.invoke(FragmentoPago(), expiry)
                checkExpiryField(expiry)
            }
            val uiResult = checkPurchaseUi(helper)
            output.putString("stream", "PASS: v1 migration preserves favorites; purchase persistence; account isolation; " +
                "idempotent checkout; occupied seats; conflict rollback; separate screenings; " +
                "payment dialog receives purchase arguments without crashing; " +
                "expiry MM/AAAA typing, paste, six-digit limit and backspace.\n$uiResult\n")
            finish(Activity.RESULT_OK, output)
        } catch (error: Throwable) {
            output.putString("stream", "FAIL: ${error.stackTraceToString()}\n")
            finish(Activity.RESULT_CANCELED, output)
        } finally {
            helper?.close()
            targetContext.deleteDatabase(databaseName)
        }
    }

    private fun checkProcessPersistence() {
        val name = "process_persistence_checks.db"
        val output = Bundle()
        var helper: AyudanteBaseFavoritos? = null
        try {
            val ticket = Boleto("persistent-ticket", 100, "Compra persistente de prueba", "",
                "apopa", "Cine de prueba", "2099-01-01", "7:30 PM", "2D", listOf("A3"), 550, 123)
            if (persistenceStage == "write") targetContext.deleteDatabase(name)
            helper = AyudanteBaseFavoritos(targetContext, name)
            val manager = GestorBoletos(targetContext, helper)
            if (persistenceStage == "write") {
                manager.savePurchase("fixture-user", ticket)
                helper.writableDatabase.execSQL(
                    "INSERT INTO perfiles(usuario_id, nombre, edad) VALUES (?, ?, ?)",
                    arrayOf<Any>("fixture-user", "Perfil persistente", 25))
            } else {
                check(persistenceStage == "read")
                check(manager.getTickets("fixture-user") == listOf(ticket))
                helper.readableDatabase.rawQuery(
                    "SELECT nombre, edad FROM perfiles WHERE usuario_id = ?",
                    arrayOf("fixture-user")).use {
                    check(it.moveToFirst() && it.getString(0) == "Perfil persistente" && it.getInt(1) == 25)
                }
            }
            helper.close()
            if (persistenceStage == "read") targetContext.deleteDatabase(name)
            output.putString("stream", "PASS persistence $persistenceStage: PID ${android.os.Process.myPid()}; " +
                "purchase and profile in disposable on-disk SQLite database.\n")
            finish(Activity.RESULT_OK, output)
        } catch (error: Throwable) {
            helper?.close()
            output.putString("stream", "FAIL: ${error.stackTraceToString()}\n")
            finish(Activity.RESULT_CANCELED, output)
        }
    }

    private fun checkLoginNavigation() {
        val output = Bundle()
        var login: ActividadInicioSesion? = null
        try {
            check(FirebaseAuth.getInstance().currentUser == null) { "Existing session: no personal account will be used" }
            login = startActivitySync(Intent(targetContext, ActividadInicioSesion::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as ActividadInicioSesion
            val activity = login
            onUi {
                val email = activity.findViewById<EditText>(R.id.etEmail)
                val password = activity.findViewById<EditText>(R.id.etPassword)
                email.text.clear()
                password.text.clear()
                activity.findViewById<View>(R.id.btnLogin).performClick()
                check(FirebaseAuth.getInstance().currentUser == null && !activity.isFinishing)
                email.setText("correo-invalido")
                password.setText("x")
                activity.findViewById<View>(R.id.btnLogin).performClick()
                check(email.error.toString() == "Ingresa un correo válido")
                email.text.clear()
                password.text.clear()
            }
            val monitor = addMonitor(ActividadRegistro::class.java.name, null, false)
            onUi { activity.findViewById<View>(R.id.tvRegister).performClick() }
            val register = waitForMonitorWithTimeout(monitor, 5000) as? ActividadRegistro
                ?: error("Registration screen did not open")
            removeMonitor(monitor)
            onUi { register.findViewById<View>(R.id.tvLogin).performClick() }
            waitForIdleSync()
            onUi { check(register.isFinishing && !activity.isFinishing) }
            output.putString("stream", "PASS: actual login Activity opens; empty fields rejected; invalid email rejected; " +
                "registration link opens ActivityRegistro; back link returns to login. No credentials sent.\n")
            onUi { activity.finish() }
            finish(Activity.RESULT_OK, output)
        } catch (error: Throwable) {
            onUi { login?.finish() }
            output.putString("stream", "FAIL: ${error.stackTraceToString()}\n")
            finish(Activity.RESULT_CANCELED, output)
        }
    }

    private fun checkExternalServices() {
        val output = Bundle()
        val results = mutableListOf<String>()
        var failed = false
        fun verify(label: String, block: () -> String) {
            try {
                results.add("PASS $label: ${block()}")
            } catch (error: Throwable) {
                failed = true
                results.add("FAIL $label: ${error.javaClass.simpleName}")
            }
        }
        var first: PeliculaTmdb? = null
        verify("TMDB genres") {
            val response = ClienteTmdb.api.genres().execute()
            check(response.isSuccessful)
            val count = response.body()!!.genres.size
            check(count > 0)
            "HTTP ${response.code()}, $count genres"
        }
        verify("TMDB now_playing") {
            val response = ClienteTmdb.api.movies("now_playing").execute()
            check(response.isSuccessful)
            val movies = response.body()!!.results
            check(movies.isNotEmpty())
            first = movies.first()
            "HTTP ${response.code()}, ${movies.size} movies"
        }
        verify("TMDB upcoming") {
            val response = ClienteTmdb.api.movies("upcoming").execute()
            check(response.isSuccessful && response.body()!!.results.isNotEmpty())
            "HTTP ${response.code()}, ${response.body()!!.results.size} movies"
        }
        verify("TMDB search") {
            val response = ClienteTmdb.api.search(first?.title ?: "Dune").execute()
            check(response.isSuccessful && response.body()!!.results.isNotEmpty())
            "HTTP ${response.code()}, ${response.body()!!.results.size} results"
        }
        verify("TMDB detail and poster") {
            val movie = first ?: error("No movie available")
            val response = ClienteTmdb.api.detail(movie.id).execute()
            check(response.isSuccessful && response.body()!!.id == movie.id)
            val detail = response.body()!!
            check(!detail.overview.isNullOrBlank())
            val poster = java.net.URL("https://image.tmdb.org/t/p/w500${detail.posterPath}")
                .openConnection() as java.net.HttpURLConnection
            try {
                poster.connectTimeout = 15000
                poster.readTimeout = 15000
                check(poster.responseCode == 200)
                val size = poster.inputStream.use { it.readBytes().size }
                check(size > 0)
                "HTTP ${response.code()}, movie ID ${detail.id}, synopsis present, poster HTTP 200 ($size bytes)"
            } finally { poster.disconnect() }
        }
        verify("Firebase Auth endpoint without credentials") {
            val key = com.google.firebase.FirebaseApp.getInstance().options.apiKey
            val connection = java.net.URL("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$key")
                .openConnection() as java.net.HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write("{}".toByteArray()) }
                val code = connection.responseCode
                val body = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val message = org.json.JSONObject(body).getJSONObject("error").getString("message")
                check(code == 400 && message == "MISSING_EMAIL")
                "HTTP $code, MISSING_EMAIL; service reachable, no account created or authenticated"
            } finally { connection.disconnect() }
        }
        output.putString("stream", results.joinToString("\n", postfix = "\n"))
        finish(if (failed) Activity.RESULT_CANCELED else Activity.RESULT_OK, output)
    }

    private fun checkDatabaseErrors() {
        val name = "database_error_checks.db"
        val output = Bundle()
        targetContext.deleteDatabase(name)
        val helper = AyudanteBaseFavoritos(targetContext, name)
        val instance = AyudanteBaseFavoritos::class.java.getDeclaredField("instance").apply { isAccessible = true }
        val original = instance.get(null)
        var host: ActividadRegistro? = null
        try {
            val db = helper.writableDatabase
            val manager = GestorBoletos(targetContext, helper)
            val first = Boleto("error-test-1", 100, "Prueba", "", "apopa", "Cine de prueba",
                "2099-01-01", "7:30 PM", "2D", listOf("A3"), 550)
            val selection = Bundle().apply {
                putInt("movieId", first.movieId)
                putString("cinemaId", first.cinemaId)
                putString("date", first.date)
                putString("time", first.time)
                putString("format", first.format)
            }
            check(manager.getTickets("fixture-user").isEmpty())
            manager.savePurchase("fixture-user", first)
            db.execSQL("""CREATE TRIGGER fail_seat BEFORE INSERT ON asientos_comprados
                WHEN NEW.asiento = 'B2' BEGIN SELECT RAISE(ABORT, 'Forced test failure'); END""")
            val retry = first.copy(purchaseId = "error-test-retry", seats = listOf("B1", "B2"))
            check(runCatching { manager.savePurchase("fixture-user", retry) }.isFailure)
            check(manager.getTickets("fixture-user") == listOf(first))
            check(manager.occupiedSeats(selection) == setOf("A3")) { "Partial seats were committed" }
            db.execSQL("DROP TRIGGER fail_seat")
            manager.savePurchase("fixture-user", retry)
            manager.savePurchase("fixture-user", retry)
            check(manager.getTickets("fixture-user").size == 2) { "Retry duplicated a purchase" }
            db.execSQL("ALTER TABLE compras RENAME TO compras_test_backup")
            check(runCatching { manager.getTickets("fixture-user") }.isFailure) { "Read error hidden as empty tickets" }
            db.execSQL("ALTER TABLE compras_test_backup RENAME TO compras")
            db.execSQL("ALTER TABLE asientos_comprados RENAME TO asientos_test_backup")
            check(runCatching { manager.occupiedSeats(selection) }.isFailure) { "Read error hidden as available seats" }
            instance.set(null, helper)
            android.os.ParcelFileDescriptor.AutoCloseInputStream(
                uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP")
            ).use { it.readBytes() }
            host = startActivitySync(Intent(targetContext, ActividadRegistro::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as ActividadRegistro
            val activity = host
            val seats = FragmentoAsientos().apply { arguments = selection }
            onUi {
                if (android.os.Build.VERSION.SDK_INT >= 27) {
                    activity.setShowWhenLocked(true)
                    activity.setTurnScreenOn(true)
                }
                val container = android.widget.FrameLayout(activity).apply { id = View.generateViewId() }
                activity.setContentView(container)
                activity.supportFragmentManager.beginTransaction().add(container.id, seats).commitNow()
            }
            waitForIdleSync()
            onUi {
                val button = seats.requireView().findViewById<android.widget.Button>(R.id.btnContinueSeats)
                check(button.text.toString() == activity.getString(R.string.reintentar_consulta))
                check(seats.requireView().findViewById<View>(R.id.layoutSeats).visibility == View.INVISIBLE)
                button.performClick()
                check(activity.supportFragmentManager.fragments.single() === seats)
                check(button.text.toString() == activity.getString(R.string.reintentar_consulta))
                db.execSQL("ALTER TABLE asientos_test_backup RENAME TO asientos_comprados")
                button.performClick()
                check(seats.requireView().findViewById<View>(R.id.layoutSeats).visibility == View.VISIBLE)
                check(!button.isEnabled)
                val seatMap = FragmentoAsientos::class.java.getDeclaredField("seatViews").apply { isAccessible = true }
                @Suppress("UNCHECKED_CAST")
                val seatViews = seatMap.get(seats) as Map<String, TextView>
                fun backgroundColor(seat: View): Int {
                    val bitmap = android.graphics.Bitmap.createBitmap(20, 20, android.graphics.Bitmap.Config.ARGB_8888)
                    val drawable = seat.background.constantState!!.newDrawable().mutate()
                    drawable.setBounds(0, 0, 20, 20)
                    drawable.draw(android.graphics.Canvas(bitmap))
                    val color = bitmap.getPixel(10, 10)
                    bitmap.recycle()
                    return color
                }
                check(!seatViews.getValue("A3").isEnabled)
                check(backgroundColor(seatViews.getValue("A3")) == activity.getColor(R.color.asiento_ocupado))
                check(backgroundColor(seatViews.getValue("C1")) == activity.getColor(R.color.asiento_disponible))
                seatViews.getValue("A3").performClick()
                check(!button.isEnabled)
                seatViews.getValue("C1").performClick()
                check(backgroundColor(seatViews.getValue("C1")) == activity.getColor(R.color.asiento_seleccionado))
                check(button.isEnabled)
                db.execSQL("ALTER TABLE asientos_comprados RENAME TO asientos_test_backup")
                button.performClick()
                check(button.text.toString() == activity.getString(R.string.reintentar_consulta))
                check(activity.supportFragmentManager.fragments.single() === seats) { "Payment opened after failed occupancy query" }
                db.execSQL("ALTER TABLE asientos_test_backup RENAME TO asientos_comprados")
            }
            var completedResult: Boolean? = null
            val dialog = DialogoProgresoPago.newInstance(Bundle(selection).apply {
                putString("purchaseUserId", "deliberately-not-the-signed-in-user")
                putString("purchaseId", "must-not-be-saved")
            })
            onUi {
                activity.supportFragmentManager.setFragmentResultListener("purchase_finished", activity) { _, result ->
                    completedResult = result.getBoolean("completed")
                }
                dialog.showNow(activity.supportFragmentManager, "failure_dialog")
            }
            Thread.sleep(4500)
            waitForIdleSync()
            onUi {
                val content = dialog.requireView()
                check(content.findViewById<TextView>(R.id.tvPaymentStatus).text.toString() == activity.getString(R.string.error_guardar_compra))
                check(content.findViewById<View>(R.id.paymentCheck).visibility == View.GONE)
                val close = content.findViewById<View>(R.id.btnClosePaymentProgress)
                check(close.visibility == View.VISIBLE && close.isEnabled)
                close.performClick()
            }
            waitForIdleSync()
            onUi { check(completedResult == false) { "Failed purchase reported success" } }
            check(manager.getTickets("fixture-user").size == 2)
            output.putString("stream", "PASS: SQLite failures propagate instead of empty data; purchase rollback; " +
                "successful retry without duplicate; seats hidden and payment blocked after query failures; " +
                "retry restores seat selection; failure dialog has visible close button, no success check, completed=false. " +
                "Only a disposable database was used; authentication unchanged.\n")
            finish(Activity.RESULT_OK, output)
        } catch (error: Throwable) {
            output.putString("stream", "FAIL: ${error.stackTraceToString()}\n")
            finish(Activity.RESULT_CANCELED, output)
        } finally {
            onUi { host?.finish() }
            waitForIdleSync()
            instance.set(null, original)
            helper.close()
            targetContext.deleteDatabase(name)
        }
    }

    private fun checkProfiles() {
        val name = "profile_checks.db"
        val output = Bundle()
        var helper: AyudanteBaseFavoritos? = null
        try {
            targetContext.deleteDatabase(name)
            val previous = object : SQLiteOpenHelper(targetContext, name, null, 3) {
                override fun onCreate(db: SQLiteDatabase) {
                    AyudanteBaseFavoritos(targetContext, name).onCreate(db)
                    db.execSQL("DROP TABLE perfiles")
                    db.execSQL("INSERT INTO favoritos(usuario_id, pelicula_id, titulo, nota) VALUES ('legacy-user', 42, 'Favorito anterior', 'Dato anterior')")
                    db.execSQL("""INSERT INTO compras VALUES (
                        'old-purchase', 'legacy-user', 42, 'Compra anterior', '', 'apopa',
                        'Cine anterior', '2099-01-01', '7:30 PM', '2D', 'A3', 550, 123)""")
                    db.execSQL("""INSERT INTO asientos_comprados VALUES (
                        'old-purchase', 'apopa', 42, '2099-01-01', '7:30 PM', '2D', 'A3')""")
                }
                override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            }
            fun rows(db: SQLiteDatabase, sql: String): List<List<String?>> =
                db.rawQuery(sql, null).use { cursor ->
                    val result = mutableListOf<List<String?>>()
                    while (cursor.moveToNext()) result.add((0 until cursor.columnCount).map { cursor.getString(it) })
                    result
                }
            val queries = listOf("SELECT * FROM favoritos", "SELECT * FROM compras", "SELECT * FROM asientos_comprados")
            val before = queries.map { rows(previous.writableDatabase, it) }
            previous.close()
            helper = AyudanteBaseFavoritos(targetContext, name)
            check(helper.writableDatabase.version == 4)
            check(queries.map { rows(helper!!.readableDatabase, it) } == before)
            var manager = GestorPerfil(targetContext, helper)
            val read = GestorPerfil::class.java.getDeclaredMethod("getProfileForUser",
                String::class.java, String::class.java, Int::class.javaObjectType).apply { isAccessible = true }
            val update = GestorPerfil::class.java.getDeclaredMethod("updateProfileForUser",
                String::class.java, String::class.java, Int::class.javaPrimitiveType).apply { isAccessible = true }
            @Suppress("UNCHECKED_CAST")
            fun profile(uid: String, initialName: String = "", initialAge: Int? = null): Pair<String, Int?> =
                read.invoke(manager, uid, initialName, initialAge) as Pair<String, Int?>
            fun save(uid: String, value: String, age: Int): Boolean = update.invoke(manager, uid, value, age) as Boolean
            check(profile("account-a", "Nombre inicial", 20) == ("Nombre inicial" to 20))
            check(profile("account-b", "Otra cuenta", 30) == ("Otra cuenta" to 30))
            check(profile("account-google") == ("" to null))
            check(save("account-a", "  Nombre editado  ", 25))
            check(profile("account-a", "Nombre inicial", 20) == ("Nombre editado" to 25))
            check(profile("account-b") == ("Otra cuenta" to 30))
            check(save("account-a", "Otro nombre", 26))
            check(!save("missing-account", "No existe", 20))
            check(runCatching { save("account-a", "", 20) }.isFailure)
            check(runCatching { save("account-a", "Nombre", 0) }.isFailure)
            check(runCatching { save("account-a", "Nombre", 121) }.isFailure)
            helper.close()
            helper = AyudanteBaseFavoritos(targetContext, name)
            manager = GestorPerfil(targetContext, helper)
            check(profile("account-a", "Nombre anterior", 20) == ("Otro nombre" to 26))
            check(profile("account-b") == ("Otra cuenta" to 30))
            check(queries.map { rows(helper!!.readableDatabase, it) } == before)
            helper.writableDatabase.execSQL("ALTER TABLE perfiles RENAME TO perfiles_test_backup")
            check(runCatching { profile("account-a") }.isFailure)
            check(runCatching { save("account-a", "No guardar", 25) }.isFailure)
            helper.writableDatabase.execSQL("ALTER TABLE perfiles_test_backup RENAME TO perfiles")
            if (FirebaseAuth.getInstance().currentUser == null) {
                check(runCatching { manager.getProfile() }.isFailure)
                check(runCatching { manager.updateProfile("Sin sesion", 20) }.isFailure)
            }
            onUi {
                val themed = android.view.ContextThemeWrapper(targetContext, R.style.Theme_CineApp)
                val inflater = android.view.LayoutInflater.from(themed)
                val form = inflater.inflate(R.layout.dialogo_editar_perfil, null, false)
                check(form.findViewById<EditText>(R.id.etProfileName) != null)
                check(form.findViewById<EditText>(R.id.etProfileAge) != null)
                form.findViewById<EditText>(R.id.etProfileName).setText("María López")
                form.findViewById<EditText>(R.id.etProfileAge).setText("24")
                val width = (320 * targetContext.resources.displayMetrics.density).toInt()
                form.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
                form.layout(0, 0, width, form.measuredHeight)
                val preview = android.graphics.Bitmap.createBitmap(width, form.measuredHeight,
                    android.graphics.Bitmap.Config.ARGB_8888)
                form.draw(android.graphics.Canvas(preview))
                java.io.FileOutputStream(java.io.File(targetContext.cacheDir, "profile_dialog_preview.png")).use {
                    preview.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                }
                preview.recycle()
                val screen = inflater.inflate(R.layout.pantalla_perfil, null, false)
                check(screen.findViewById<View>(R.id.btnEditProfile) != null)
                check(screen.findViewById<TextView>(R.id.tvUserAge) != null)
            }
            helper.close()
            targetContext.deleteDatabase(name)
            output.putString("stream", "PASS: v3 to v4 migration preserves favorites, purchases and seats; profile creation from initial name/age; " +
                "real SQLite UPDATE name and age; repeated edit; account isolation; persistence after reopening; " +
                "validation; database errors propagate; unauthenticated access rejected; profile layouts inflate. " +
                "SQL used fixture UIDs without changing Firebase authentication.\n")
            finish(Activity.RESULT_OK, output)
        } catch (error: Throwable) {
            helper?.close()
            output.putString("stream", "FAIL: ${error.stackTraceToString()}\n")
            finish(Activity.RESULT_CANCELED, output)
        }
    }
    private fun checkPurchaseUi(helper: AyudanteBaseFavoritos): String {
        if (FirebaseAuth.getInstance().currentUser == null) return "UI skipped: no signed-in account."
        if (targetContext.getSharedPreferences("CinemaSelection", Context.MODE_PRIVATE)
                .getString("cinemaId", null) !in listOf("apopa", "soyapango")) {
            return "UI skipped: no selected cinema."
        }
        val instance = AyudanteBaseFavoritos::class.java.getDeclaredField("instance").apply { isAccessible = true }
        val original = instance.get(null)
        instance.set(null, helper)
        var activity: ActividadPrincipal? = null
        try {
            android.os.ParcelFileDescriptor.AutoCloseInputStream(
                uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP")
            ).use { it.readBytes() }
            activity = startActivitySync(Intent(targetContext, ActividadPrincipal::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as ActividadPrincipal
            val main = activity
            onUi {
                main.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                if (android.os.Build.VERSION.SDK_INT >= 27) {
                    main.setShowWhenLocked(true)
                    main.setTurnScreenOn(true)
                }
            }
            var ready = false
            for (attempt in 0..30) {
                onUi { ready = main.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) }
                if (ready) break
                Thread.sleep(100)
            }
            if (!ready) return "UI skipped: phone did not resume the test activity."
            val selection = Bundle().apply {
                putInt("movieId", 100)
                putString("title", "Prueba de interfaz")
                putString("cinemaId", "apopa")
                putString("cinemaName", "Cine de prueba")
                putString("date", "2099-01-03")
                putString("time", "7:30 PM")
                putString("format", "2D ESPAÑOL")
                putStringArrayList("seats", arrayListOf("B2"))
                putInt("ticketCount", 1)
                putInt("totalCents", 400)
            }
            onUi {
                main.showPayment(selection)
                main.supportFragmentManager.executePendingTransactions()
                val payment = main.supportFragmentManager.primaryNavigationFragment as FragmentoPago
                payment.requireView().findViewById<View>(R.id.btnMethodCard).performClick()
                val expiry = payment.requireView().findViewById<EditText>(R.id.etCardExpiry)
                checkExpiryField(expiry)
                val fields = mapOf(R.id.etCardHolder to "Prueba", R.id.etCardNumber to "4111111111111111",
                    R.id.etCardExpiry to "12/2099", R.id.etCardCvv to "123")
                fields.forEach { (id, value) -> payment.requireView().findViewById<EditText>(id).setText(value) }
                payment.requireView().findViewById<View>(R.id.btnPayNow).performClick()
                payment.childFragmentManager.executePendingTransactions()
                val dialog = payment.childFragmentManager.findFragmentByTag("payment_progress")!!
                check(dialog.requireView().findViewById<TextView>(R.id.tvPaymentStatus).text == "Procesando su pago")
                check(!dialog.requireView().findViewById<View>(R.id.btnClosePaymentProgress).isEnabled)
                check(dialog.requireView().findViewById<View>(R.id.btnClosePaymentProgress).visibility == View.INVISIBLE)
            }
            Thread.sleep(4500)
            waitForIdleSync()
            onUi {
                val payment = main.supportFragmentManager.primaryNavigationFragment as FragmentoPago
                val dialog = payment.childFragmentManager.findFragmentByTag("payment_progress")!!
                check(dialog.requireView().findViewById<View>(R.id.paymentCheck).visibility == View.VISIBLE)
                check(dialog.requireView().findViewById<TextView>(R.id.tvPaymentStatus).text == "Compra exitosa")
                check(dialog.requireView().findViewById<View>(R.id.btnClosePaymentProgress).visibility == View.VISIBLE)
                dialog.requireView().findViewById<View>(R.id.btnClosePaymentProgress).performClick()
                check(main.supportFragmentManager.primaryNavigationFragment is FragmentoBoletos)
                main.showSection(R.id.nav_profile)
                val profile = main.supportFragmentManager.primaryNavigationFragment!!
                profile.requireView().findViewById<View>(R.id.btnProfileFavorites).performClick()
                check(main.supportFragmentManager.primaryNavigationFragment is FragmentoFavoritos)
                main.showSection(R.id.nav_profile)
                main.supportFragmentManager.primaryNavigationFragment!!.requireView()
                    .findViewById<View>(R.id.btnProfileTickets).performClick()
                check(main.supportFragmentManager.primaryNavigationFragment is FragmentoBoletos)
            }
            check(GestorBoletos(targetContext, helper).getTickets(FirebaseAuth.getInstance().currentUser!!.uid)
                .single().totalCents == 550)
            checkTicketDetails(main, helper)
            return "PASS UI: processing, green check, close X, Tickets navigation, profile shortcuts; " +
                "two ticket details open as independent Activities with their own data, same original QR, " +
                "back arrow returns to Tickets, opening details does not alter purchases."
        } finally {
            onUi { activity?.finish() }
            waitForIdleSync()
            instance.set(null, original)
        }
    }

    private fun checkTicketDetails(main: ActividadPrincipal, helper: AyudanteBaseFavoritos) {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        val manager = GestorBoletos(targetContext, helper)
        val first = manager.getTickets(uid).single()
        val second = first.copy(purchaseId = "ui-detail-2", title = "Otra película de prueba",
            date = "2099-01-04", seats = listOf("C2", "C6"), totalCents = 1150)
        manager.savePurchase(uid, second)
        val before = manager.getTickets(uid)
        val entries = listOf(first, second)
        for ((index, ticket) in entries.withIndex()) {
            val monitor = addMonitor(ActividadDetalleBoleto::class.java.name, null, false)
            var detail: ActividadDetalleBoleto? = null
            try {
                onUi {
                    val list = main.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvTickets)
                    val adapter = AdaptadorBoletos(entries)
                    val holder = adapter.createViewHolder(list, 0)
                    adapter.bindViewHolder(holder, index)
                    holder.itemView.performClick()
                }
                detail = waitForMonitorWithTimeout(monitor, 5000) as? ActividadDetalleBoleto
                    ?: error("Tapping a ticket did not open ActividadDetalleBoleto")
                val opened = detail
                var loaded = false
                for (attempt in 0..30) {
                    onUi { loaded = opened.findViewById<View>(R.id.ticketDetailContent).visibility == View.VISIBLE }
                    if (loaded) break
                    Thread.sleep(100)
                }
                check(loaded) { "Boleto detail did not load" }
                onUi {
                    check(opened.findViewById<TextView>(R.id.tvTicketMovie).text.toString() == ticket.title)
                    check(opened.findViewById<TextView>(R.id.tvTicketCinema).text.toString() == ticket.cinema)
                    check(opened.findViewById<TextView>(R.id.tvTicketDateTime).text.toString() == "${ticket.date} · ${ticket.time}")
                    check(opened.findViewById<TextView>(R.id.tvTicketSeats).text.toString() == ticket.seats.joinToString(", "))
                    val qrView = opened.findViewById<android.widget.ImageView>(R.id.imgTicketQr)
                    val qr = qrView.drawable as android.graphics.drawable.BitmapDrawable
                    val originalQr = opened.getDrawable(R.drawable.qr_boleto) as android.graphics.drawable.BitmapDrawable
                    check(qr.bitmap.sameAs(originalQr.bitmap)) { "The original QR was replaced" }
                    check(qrView.width > 0 && qrView.height > 0) { "QR is not visible" }
                    opened.findViewById<View>(R.id.btnBackTicketDetail).performClick()
                }
                waitForIdleSync()
                onUi { check(main.supportFragmentManager.primaryNavigationFragment is FragmentoBoletos) }
            } finally {
                onUi { detail?.finish() }
                removeMonitor(monitor)
            }
        }
        check(manager.getTickets(uid) == before) { "Opening a detail changed purchases" }
    }

    private fun checkExpiryField(expiry: EditText) {
        expiry.setText("")
        expiry.setSelection(0)
        for (digit in "032027") {
            expiry.text.insert(expiry.selectionStart, digit.toString())
        }
        check(expiry.text.toString() == "03/2027") { "Expiry typing did not apply MM/AAAA: ${expiry.text}" }
        expiry.text.insert(expiry.selectionStart, "8")
        check(expiry.text.toString() == "03/2027") { "Expiry accepted more than six digits" }
        for (expected in listOf("03/202", "03/20", "03/2", "03/", "03", "0", "")) {
            val cursor = expiry.selectionStart
            expiry.text.delete(cursor - 1, cursor)
            check(expiry.text.toString() == expected) { "Expiry backspace got stuck at the slash" }
        }
        expiry.setText("032027")
        check(expiry.text.toString() == "03/2027" && expiry.error == null)
    }

    private fun onUi(block: () -> Unit) {
        var failure: Throwable? = null
        runOnMainSync {
            try { block() } catch (error: Throwable) { failure = error }
        }
        failure?.let { throw it }
    }

    private fun createVersionOne(context: Context, name: String) {
        val previous = object : SQLiteOpenHelper(context, name, null, 1) {
            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL("""CREATE TABLE favoritos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT, usuario_id TEXT NOT NULL,
                    pelicula_id INTEGER NOT NULL, titulo TEXT NOT NULL, poster TEXT,
                    genero TEXT, calificacion TEXT, UNIQUE(usuario_id, pelicula_id))""")
                db.execSQL("INSERT INTO favoritos(usuario_id, pelicula_id, titulo) VALUES (?, ?, ?)",
                    arrayOf<Any>("legacy-user", 42, "Favorito anterior"))
            }
            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        previous.writableDatabase
        previous.close()
    }
}
