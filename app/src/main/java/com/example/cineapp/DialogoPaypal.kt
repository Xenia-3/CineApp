package com.example.cineapp

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment

class DialogoPaypal : DialogFragment(R.layout.dialogo_paypal) {
    private var paypal: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val web = view.findViewById<WebView>(R.id.paypalWebView)
        paypal = web
        val progress = view.findViewById<View>(R.id.paypalProgress)
        val error = view.findViewById<View>(R.id.tvPaypalError)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = false
        web.settings.allowContentAccess = false
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(webView: WebView, request: WebResourceRequest): Boolean {
                return request.url.scheme != "https"
            }

            override fun onPageStarted(webView: WebView, url: String, favicon: Bitmap?) {
                progress.visibility = View.VISIBLE
                error.visibility = View.GONE
            }

            override fun onPageFinished(webView: WebView, url: String) {
                progress.visibility = View.GONE
                view.findViewById<TextView>(R.id.tvPaypalHost).text = android.net.Uri.parse(url).host ?: "PayPal"
            }

            override fun onReceivedError(webView: WebView, request: WebResourceRequest, failure: WebResourceError) {
                if (request.isForMainFrame) {
                    progress.visibility = View.GONE
                    error.visibility = View.VISIBLE
                }
            }
        }
        view.findViewById<ImageView>(R.id.btnPaypalBack).setOnClickListener {
            if (web.canGoBack()) web.goBack() else dismiss()
        }
        view.findViewById<Button>(R.id.btnPaypalClose).setOnClickListener { dismiss() }
        web.loadUrl("https://www.paypal.com/donate/?hosted_button_id=WWS6FLRH9RC88")
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(requireContext().getColor(R.color.bg_dark)))
            setLayout((resources.displayMetrics.widthPixels * 0.95).toInt(),
                (resources.displayMetrics.heightPixels * 0.85).toInt())
        }
    }

    override fun onDestroyView() {
        paypal?.apply {
            stopLoading()
            (parent as? android.view.ViewGroup)?.removeView(this)
            destroy()
        }
        paypal = null
        super.onDestroyView()
    }
}
