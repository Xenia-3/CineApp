package com.example.cineapp

import android.os.Bundle
import android.os.SystemClock
import android.content.DialogInterface
import android.view.View
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DialogoProgresoPago : DialogFragment(R.layout.dialogo_progreso_pago) {
    companion object {
        fun newInstance(purchase: Bundle): DialogoProgresoPago {
            val dialog = DialogoProgresoPago()
            dialog.arguments = Bundle(purchase)
            return dialog
        }
    }

    private var completesAt = 0L
    private var completed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
        completesAt = savedInstanceState?.getLong("completesAt")
            ?: (SystemClock.elapsedRealtime() + 4000L)
        isCancelable = false
        completed = savedInstanceState?.getBoolean("completed") ?: false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val close = view.findViewById<View>(R.id.btnClosePaymentProgress)
        close.setOnClickListener {
            dismiss()
            reportResult()
        }
        close.isEnabled = false
        close.visibility = View.INVISIBLE
        val context = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                delay((completesAt - SystemClock.elapsedRealtime()).coerceAtLeast(0L))
                val uid = requireArguments().getString("purchaseUserId").orEmpty()
                check(uid.isNotBlank() && FirebaseAuth.getInstance().currentUser?.uid == uid) {
                    "Inicia sesión con la cuenta de la compra"
                }
                val ticket = Boleto.fromSelection(requireArguments(),
                    requireArguments().getString("purchaseId").orEmpty())
                withContext(Dispatchers.IO) { GestorBoletos(context).savePurchase(uid, ticket) }
                completed = true
                view.findViewById<TextView>(R.id.tvPaymentStatus).text = "Compra exitosa"
                view.findViewById<View>(R.id.paymentCheck).visibility = View.VISIBLE
                close.visibility = View.VISIBLE
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                completed = false
                view.findViewById<View>(R.id.paymentCheck).visibility = View.GONE
                view.findViewById<TextView>(R.id.tvPaymentStatus).text =
                    if (error.cause is android.database.sqlite.SQLiteConstraintException)
                        getString(R.string.error_guardar_compra) + "\n" + error.message
                    else getString(R.string.error_guardar_compra)
            }
            view.findViewById<View>(R.id.paymentSpinner).visibility = View.GONE
            close.visibility = View.VISIBLE
            close.isEnabled = true
            isCancelable = true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong("completesAt", completesAt)
        outState.putBoolean("completed", completed)
        super.onSaveInstanceState(outState)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        reportResult()
    }

    private fun reportResult() {
        parentFragmentManager.setFragmentResult("purchase_finished", Bundle().apply {
            putBoolean("completed", completed)
        })
    }
}
