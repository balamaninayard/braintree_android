package com.braintreepayments.api.paypalsavedpaymentmethod.component

import android.content.Context
import android.graphics.Color
import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import com.braintreepayments.api.paypalsavedpaymentmethod.state.CreditMessagingContent
import com.braintreepayments.api.paypalsavedpaymentmethod.state.CreditMessagingState
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodStyleResolver
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.ComponentAppearance
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.ContainerStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.CreditMessagingStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodViewStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CreditMessagingViewUnitTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val view = CreditMessagingView(context)

    private val shimmerView
        get() = view.findViewById<View>(R.id.paypal_saved_payment_method_credit_messaging_shimmer)
    private val textView
        get() = view.findViewById<TextView>(R.id.paypal_saved_payment_method_credit_messaging_text)

    @Test
    fun `Loading state shows the shimmer bar and hides the message`() {
        view.setState(CreditMessagingState.Loading)

        assertTrue(view.visibility == View.VISIBLE)
        assertTrue(shimmerView.visibility == View.VISIBLE)
        assertFalse(textView.visibility == View.VISIBLE)
    }

    @Test
    fun `Content state hides the shimmer and renders the message plus learn-more text`() {
        val content = CreditMessagingContent(
            message = "Pay in 4 of \$25",
            learnMoreText = "Learn more",
            learnMoreUrl = "https://paypal.com/pay-later"
        )

        view.setState(CreditMessagingState.Content(content))

        assertTrue(view.visibility == View.VISIBLE)
        assertFalse(shimmerView.visibility == View.VISIBLE)
        assertTrue(textView.visibility == View.VISIBLE)
        assertTrue(textView.text.contains("Pay in 4 of \$25"))
        assertTrue(textView.text.contains("Learn more"))
    }

    @Test
    fun `Content state with blank learn-more text still renders the message`() {
        val content = CreditMessagingContent(message = "Pay in 4 of \$25", learnMoreText = "", learnMoreUrl = "")

        view.setState(CreditMessagingState.Content(content))

        assertTrue(textView.text.contains("Pay in 4 of \$25"))
    }

    @Test
    fun `Hidden state hides the whole row`() {
        view.setState(CreditMessagingState.Hidden)

        assertFalse(view.visibility == View.VISIBLE)
    }

    @Test
    fun `applyStyle sets text color and font size from the resolved style`() {
        val resolvedStyle = PayPalSavedPaymentMethodStyleResolver(
            context,
            PayPalSavedPaymentMethodViewStyle(
                componentAppearance = ComponentAppearance(textColor = Color.RED),
                container = ContainerStyle(creditMessaging = CreditMessagingStyle(fontSizeSp = 18f))
            )
        )

        view.applyStyle(resolvedStyle)
        view.setState(CreditMessagingState.Content(CreditMessagingContent("msg", "", "")))

        assertEquals(Color.RED, textView.currentTextColor)
        assertEquals(18f, textView.textSize / context.resources.displayMetrics.scaledDensity, 0.01f)
    }

    @Test
    fun `tapping learn-more twice reuses the same lander instance`() {
        val content = CreditMessagingContent(
            message = "Pay in 4 of \$25",
            learnMoreText = "Learn more",
            learnMoreUrl = "https://paypal.com/pay-later"
        )
        view.setState(CreditMessagingState.Content(content))

        val spanned = textView.text as Spanned
        val span = spanned.getSpans(0, spanned.length, ClickableSpan::class.java).first()

        span.onClick(textView)
        val landerAfterFirstTap = getPrivateField(view, "lander\$delegate").let {
            (it as Lazy<*>).value
        }

        span.onClick(textView)
        val landerAfterSecondTap = getPrivateField(view, "lander\$delegate").let {
            (it as Lazy<*>).value
        }

        assertTrue(landerAfterFirstTap === landerAfterSecondTap)
    }

    private fun getPrivateField(target: Any, name: String): Any? {
        val field = target::class.java.getDeclaredField(name)
        field.isAccessible = true
        return field.get(target)
    }
}
