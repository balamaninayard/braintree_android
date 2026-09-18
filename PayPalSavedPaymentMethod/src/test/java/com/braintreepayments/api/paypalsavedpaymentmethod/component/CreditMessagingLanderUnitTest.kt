package com.braintreepayments.api.paypalsavedpaymentmethod.component

import android.content.Context
import android.view.ContextThemeWrapper
import android.widget.ImageView
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowWebView

@RunWith(RobolectricTestRunner::class)
class CreditMessagingLanderUnitTest {

    private val context: Context = ContextThemeWrapper(
        RuntimeEnvironment.getApplication(),
        android.R.style.Theme_Material_Light
    )
    private val lander = CreditMessagingLander(context)

    @Test
    fun `load enables JavaScript, loads the given url, and shows the dialog`() {
        lander.load("https://paypal.com/pay-later")

        val webView = lander.findViewById<android.webkit.WebView>(R.id.paypal_saved_payment_method_lander_webview)
        assertTrue(webView.settings.javaScriptEnabled)
        val shadowWebView: ShadowWebView = shadowOf(webView)
        assertEquals("https://paypal.com/pay-later", shadowWebView.lastLoadedUrl)
        assertTrue(lander.isShowing)
    }

    @Test
    fun `tapping the close button dismisses the dialog`() {
        lander.load("https://paypal.com/pay-later")

        val closeButton = lander.findViewById<ImageView>(R.id.paypal_saved_payment_method_lander_close)
        closeButton.performClick()

        assertFalse(lander.isShowing)
    }
}
