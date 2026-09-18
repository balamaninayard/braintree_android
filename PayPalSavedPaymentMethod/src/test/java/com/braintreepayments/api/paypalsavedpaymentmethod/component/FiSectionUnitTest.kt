package com.braintreepayments.api.paypalsavedpaymentmethod.component

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethod
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import com.braintreepayments.api.paypalsavedpaymentmethod.state.FiClusterState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalBetaApi::class)
@RunWith(RobolectricTestRunner::class)
class FiSectionUnitTest {

    private val context: Context = ContextThemeWrapper(
        RuntimeEnvironment.getApplication(),
        android.R.style.Theme_Material_Light
    )
    private val fiSection = FiSection(context)

    private val iconView get() = fiSection.findViewById<ImageView>(R.id.paypal_saved_payment_method_fi_icon)
    private val textView get() = fiSection.findViewById<TextView>(R.id.paypal_saved_payment_method_fi_text)
    private val editIconView get() = fiSection.findViewById<ImageView>(R.id.paypal_saved_payment_method_fi_edit_icon)
    private val shimmerView get() = fiSection.findViewById<View>(R.id.paypal_saved_payment_method_fi_shimmer)

    @Test
    fun `Loading state shows shimmer and hides content`() {
        fiSection.setState(FiClusterState.Loading)

        assertTrue(fiSection.isVisible())
        assertTrue(shimmerView.isVisible())
        assertFalse(iconView.isVisible())
        assertFalse(editIconView.isVisible())
        assertFalse(textView.isVisible())
    }

    @Test
    fun `Available state renders masked last four and shows edit pencil`() {
        val method = paymentMethod(lastDigits = "3339")

        fiSection.setState(FiClusterState.Available(method))

        assertTrue(fiSection.isVisible())
        assertFalse(shimmerView.isVisible())
        assertTrue(textView.isVisible())
        assertTrue(editIconView.isVisible())
        assertTrue(iconView.isVisible())
        assertEquals("••3339", textView.text.toString())
    }

    @Test
    fun `Available state falls back to label when lastDigits is null`() {
        val method = paymentMethod(lastDigits = null)

        fiSection.setState(FiClusterState.Available(method))

        assertEquals("label", textView.text.toString())
    }

    @Test
    fun `Available state falls back to label when lastDigits is blank`() {
        val method = paymentMethod(lastDigits = "   ")

        fiSection.setState(FiClusterState.Available(method))

        assertEquals("label", textView.text.toString())
    }

    @Test
    fun `NoFiLoad state renders the email instead of FI text and still shows edit pencil`() {
        fiSection.setState(FiClusterState.NoFiLoad(email = "buyer@example.com"))

        assertTrue(fiSection.isVisible())
        assertFalse(shimmerView.isVisible())
        assertTrue(textView.isVisible())
        assertTrue(editIconView.isVisible())
        assertFalse(iconView.isVisible())
        assertEquals("buyer@example.com", textView.text.toString())
    }

    @Test
    fun `NoNetworkLoad state hides the entire section`() {
        fiSection.setState(FiClusterState.NoNetworkLoad)

        assertFalse(fiSection.isVisible())
    }

    @Test
    fun `edit pencil click invokes the registered listener exactly once`() {
        var invocationCount = 0
        fiSection.setOnEditClickListener { invocationCount++ }
        fiSection.setState(FiClusterState.Available(paymentMethod(lastDigits = "1234")))

        editIconView.performClick()

        assertEquals(1, invocationCount)
    }

    private fun android.view.View.isVisible() = this.visibility == android.view.View.VISIBLE

    private fun paymentMethod(lastDigits: String?) = PayPalSavedPaymentMethod(
        label = "label",
        imageUrl = "https://example.com/icon.png",
        lastDigits = lastDigits,
        type = "CARD",
        subtype = null
    )
}
