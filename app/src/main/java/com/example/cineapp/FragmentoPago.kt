package com.example.cineapp

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID
import java.util.Locale

class FragmentoPago : Fragment(R.layout.pantalla_pago) {
    private var selectedMethod: String? = null
    private var paymentPending = false
    private val cardFields = listOf(R.id.etCardHolder, R.id.etCardNumber, R.id.etCardExpiry, R.id.etCardCvv)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupExpiryField(view.findViewById(R.id.etCardExpiry))
        val purchase = requireArguments()
        val seats = purchase.getStringArrayList("seats").orEmpty()
        val subtotalCents = purchase.getInt("totalCents")
        val serviceFeeCents = Boleto.SERVICE_FEE_CENTS
        val totalCents = subtotalCents + serviceFeeCents
        selectedMethod = savedInstanceState?.getString("method")
        if (!purchase.containsKey("purchaseId")) {
            purchase.putString("purchaseId", UUID.randomUUID().toString())
            purchase.putString("purchaseUserId", FirebaseAuth.getInstance().currentUser?.uid)
        }

        view.findViewById<ImageView>(R.id.btnBackPayment).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        Glide.with(this).load(purchase.getString("image"))
            .placeholder(R.color.card_dark).error(R.color.card_dark)
            .into(view.findViewById<ImageView>(R.id.imgPaymentPoster))
        view.findViewById<TextView>(R.id.tvPaymentTitle).text = purchase.getString("title")
        view.findViewById<TextView>(R.id.tvPaymentCinema).text = purchase.getString("cinemaName")
        view.findViewById<TextView>(R.id.tvPaymentFunction).text =
            "${purchase.getString("date")} · ${purchase.getString("time")}\n${purchase.getString("format")}"
        view.findViewById<TextView>(R.id.tvPaymentSeats).text = "Asientos: ${seats.joinToString(", ")}"
        val count = purchase.getInt("ticketCount")
        view.findViewById<TextView>(R.id.tvPaymentCount).text = "$count ${if (count == 1) "boleto" else "boletos"}"
        view.findViewById<TextView>(R.id.tvPaymentSubtotal).text = money(subtotalCents)
        view.findViewById<TextView>(R.id.tvPaymentFee).text = money(serviceFeeCents)
        view.findViewById<TextView>(R.id.tvPaymentTotal).text = money(totalCents)

        val cardButton = view.findViewById<MaterialButton>(R.id.btnMethodCard)
        val paypalButton = view.findViewById<MaterialButton>(R.id.btnMethodPaypal)
        val cardForm = view.findViewById<View>(R.id.cardForm)
        val payButton = view.findViewById<Button>(R.id.btnPayNow)
        paymentPending = childFragmentManager.findFragmentByTag("payment_progress") != null
        fun updateMethod() {
            cardForm.visibility = if (selectedMethod == "card") View.VISIBLE else View.GONE
            highlightMethod(cardButton, selectedMethod == "card")
            highlightMethod(paypalButton, selectedMethod == "paypal")
            payButton.isEnabled = selectedMethod != null && !paymentPending
            payButton.text = if (selectedMethod == "paypal") "Abrir PayPal" else "Pagar ahora"
        }
        cardButton.setOnClickListener {
            selectedMethod = "card"
            updateMethod()
        }
        paypalButton.setOnClickListener {
            selectedMethod = "paypal"
            clearCardForm()
            updateMethod()
            openPaypal()
        }
        updateMethod()
        childFragmentManager.setFragmentResultListener("purchase_finished", viewLifecycleOwner) { _, result ->
            paymentPending = false
            if (result.getBoolean("completed")) {
                (requireActivity() as ActividadPrincipal).showSection(R.id.nav_tickets)
            } else {
                updateMethod()
            }
        }
        payButton.setOnClickListener {
            when (selectedMethod) {
                "card" -> simulateCardPayment()
                "paypal" -> openPaypal()
            }
        }
    }

    private fun setupExpiryField(field: EditText) {
        field.addTextChangedListener(object : TextWatcher {
            private var formatting = false
            private var deletingSlash = false

            override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) {
                if (!formatting) {
                    deletingSlash = count == 1 && after == 0 && text?.getOrNull(start) == '/'
                }
            }

            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(text: Editable?) {
                if (formatting || text == null) return
                val raw = text.toString()
                val numbers = raw.filter { it in '0'..'9' }.take(6)
                val beforeCursor = raw.take(field.selectionStart.coerceAtLeast(0))
                    .count { it in '0'..'9' }.coerceAtMost(numbers.length)
                val formatted = when {
                    numbers.length > 2 -> numbers.take(2) + "/" + numbers.drop(2)
                    numbers.length == 2 && !deletingSlash -> "$numbers/"
                    else -> numbers
                }
                if (raw != formatted) {
                    formatting = true
                    text.replace(0, text.length, formatted)
                    val slashOffset = if ('/' in formatted &&
                        (beforeCursor > 2 || (beforeCursor == 2 && !deletingSlash))) 1 else 0
                    field.setSelection((beforeCursor + slashOffset).coerceAtMost(formatted.length))
                    formatting = false
                }
            }
        })
    }

    private fun highlightMethod(button: MaterialButton, selected: Boolean) {
        button.isSelected = selected
        val color = if (selected) requireContext().getColor(R.color.accent_red)
            else android.graphics.Color.rgb(58, 58, 58)
        button.strokeColor = ColorStateList.valueOf(color)
    }

    private fun simulateCardPayment() {
        if (paymentPending) return
        for (id in cardFields) {
            val field = requireView().findViewById<EditText>(id)
            if (field.text.isBlank()) {
                field.error = "Completa este campo"
                field.requestFocus()
                return
            }
        }
        clearCardForm()
        if (childFragmentManager.findFragmentByTag("payment_progress") == null) {
            val paymentDialog = DialogoProgresoPago.newInstance(requireArguments())
            paymentPending = true
            requireView().findViewById<Button>(R.id.btnPayNow).isEnabled = false
            paymentDialog.show(childFragmentManager, "payment_progress")
        }
    }

    private fun openPaypal() {
        if (childFragmentManager.findFragmentByTag("paypal") == null) {
            DialogoPaypal().show(childFragmentManager, "paypal")
        }
    }

    private fun clearCardForm() {
        cardFields.forEach { id ->
            view?.findViewById<EditText>(id)?.apply {
                text.clear()
                error = null
                clearFocus()
            }
        }
    }

    private fun money(cents: Int) = String.format(Locale.US, "$%.2f", cents / 100.0)

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("method", selectedMethod)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        clearCardForm()
        super.onDestroyView()
    }
}
