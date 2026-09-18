package com.braintreepayments.api.paypalsavedpaymentmethod.component

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.ComponentAppearance
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.ContainerStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.FundingInstrumentStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalLabelStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalLogoStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodViewStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PayPalSavedPaymentMethodViewUnitTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val view = PayPalSavedPaymentMethodView(context)

    private val logoView get() = view.findViewById<ImageView>(R.id.paypal_saved_payment_method_paypal_logo)
    private val labelView get() = view.findViewById<TextView>(R.id.paypal_saved_payment_method_paypal_label)
    private val fiSection get() = view.findViewById<FiSection>(R.id.paypal_saved_payment_method_fi_section)
    private val creditMessagingView
        get() = view.findViewById<CreditMessagingView>(R.id.paypal_saved_payment_method_credit_messaging)

    @Test
    fun `default style shows the logo and label`() {
        assertTrue(logoView.isVisible())
        assertTrue(labelView.isVisible())
    }

    @Test
    fun `setStyle with showPayPalLogo false hides the logo`() {
        view.setStyle(PayPalSavedPaymentMethodViewStyle(showPayPalLogo = false))

        assertFalse(logoView.isVisible())
    }

    @Test
    fun `setStyle with showPayPalLabel false hides the label`() {
        view.setStyle(PayPalSavedPaymentMethodViewStyle(showPayPalLabel = false))

        assertFalse(labelView.isVisible())
    }

    @Test
    fun `setStyle applies text color and font size to the label`() {
        view.setStyle(
            PayPalSavedPaymentMethodViewStyle(
                componentAppearance = ComponentAppearance(textColor = Color.RED, baseFontSizeSp = 20f)
            )
        )

        assertEquals(Color.RED, labelView.currentTextColor)
        assertEquals(20f, labelView.textSize / context.resources.displayMetrics.scaledDensity, 0.01f)
    }

    @Test
    fun `setStyle applies logo width as a square bounding box`() {
        view.setStyle(
            PayPalSavedPaymentMethodViewStyle(container = ContainerStyle(logo = PayPalLogoStyle(widthDp = 32f)))
        )

        val density = context.resources.displayMetrics.density
        val expectedPx = (32f * density).toInt()
        assertEquals(expectedPx, logoView.layoutParams.width)
        assertEquals(expectedPx, logoView.layoutParams.height)
    }

    @Test
    fun `setStyle zeroes the label margin start when the logo is hidden`() {
        view.setStyle(
            PayPalSavedPaymentMethodViewStyle(
                showPayPalLogo = false,
                container = ContainerStyle(label = PayPalLabelStyle(marginStartDp = 12f))
            )
        )

        val marginStart = (labelView.layoutParams as LinearLayout.LayoutParams).marginStart
        assertEquals(0, marginStart)
    }

    @Test
    fun `setStyle applies background color and corner radius to the container`() {
        view.setStyle(
            PayPalSavedPaymentMethodViewStyle(
                componentAppearance = ComponentAppearance(backgroundColor = Color.BLUE),
                container = ContainerStyle(cornerRadiusDp = 8f)
            )
        )

        val background = view.background as GradientDrawable
        assertEquals(Color.BLUE, background.color?.defaultColor)

        val density = context.resources.displayMetrics.density
        assertEquals(8f * density, background.cornerRadius, 0.01f)
    }

    @Test
    fun `setStyle applies the funding instrument margin start to the fi section`() {
        view.setStyle(
            PayPalSavedPaymentMethodViewStyle(
                container = ContainerStyle(
                    fundingInstrument = FundingInstrumentStyle(
                        marginStartDp = 6f
                    )
                )
            )
        )

        val density = context.resources.displayMetrics.density
        val marginStart = (fiSection.layoutParams as LinearLayout.LayoutParams).marginStart
        assertEquals((6f * density).toInt(), marginStart)
    }

    @Test
    fun `hiding both logo and label zeroes the credit messaging anchor margin`() {
        view.setStyle(PayPalSavedPaymentMethodViewStyle(showPayPalLogo = false, showPayPalLabel = false))

        val marginStart = (creditMessagingView.layoutParams as LinearLayout.LayoutParams).marginStart
        assertEquals(0, marginStart)
    }

    private fun android.view.View.isVisible() = this.visibility == android.view.View.VISIBLE
}
