package com.braintreepayments.api.paypalsavedpaymentmethod.component

import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.braintreepayments.api.paypalsavedpaymentmethod.R

/**
 * The "Learn more" destination for [CreditMessagingView] -- an in-app WebView presented as a
 * bottom sheet.
 *
 * Hand-rolled [Dialog] rather than a `BottomSheetDialogFragment`: no Material Components
 * dependency added for a single dialog. No drag-to-dismiss gesture -- dismissed via the close
 * button or a backdrop tap only.
 */
internal class CreditMessagingLander(context: Context) :
    Dialog(context, R.style.PayPalSavedPaymentMethod_BottomSheetDialog) {

    private val webView: WebView

    init {
        setContentView(R.layout.credit_messaging_lander)
        webView = findViewById(R.id.paypal_saved_payment_method_lander_webview)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = false
        }
        findViewById<ImageView>(R.id.paypal_saved_payment_method_lander_close).also { closeButton ->
            closeButton.setOnClickListener { dismiss() }
            ViewCompat.setOnApplyWindowInsetsListener(closeButton) { view, insets ->
                val statusBarInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
                view.updateLayoutParams<LinearLayout.LayoutParams> { topMargin = statusBarInset }
                insets
            }
        }
        setCanceledOnTouchOutside(true)

        window?.apply {
            setGravity(Gravity.BOTTOM)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    /** Loads [url] and shows the sheet. */
    fun load(url: String) {
        webView.settings.javaScriptEnabled = true
        webView.loadUrl(url)
        show()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
