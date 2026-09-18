package com.braintreepayments.api.paypalsavedpaymentmethod.state

import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypalsavedpaymentmethod.Payer
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodSummary
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodSummaryResult
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalBetaApi::class)
class FiClusterStateUnitTest {

    @Test
    fun `Failure result maps to NoNetworkLoad`() {
        val result = PayPalSavedPaymentMethodSummaryResult.Failure(Exception("network error"))

        assertTrue(result.toFiClusterState() is FiClusterState.NoNetworkLoad)
    }

    @Test
    fun `Success with a payment method maps to Available with the first method`() {
        val first = paymentMethod(lastDigits = "1111")
        val second = paymentMethod(lastDigits = "2222")
        val summary = PayPalSavedPaymentMethodSummary(
            paypalPayer = null,
            paypalSavedPaymentMethods = listOf(first, second)
        )
        val result = PayPalSavedPaymentMethodSummaryResult.Success(summary)

        val state = result.toFiClusterState()

        assertTrue(state is FiClusterState.Available)
        assertEquals(first, (state as FiClusterState.Available).paymentMethod)
    }

    @Test
    fun `Success with no payment methods but a payer maps to NoFiLoad with the payer email`() {
        val summary = PayPalSavedPaymentMethodSummary(
            paypalPayer = Payer(email = "buyer@example.com", editable = true),
            paypalSavedPaymentMethods = emptyList()
        )
        val result = PayPalSavedPaymentMethodSummaryResult.Success(summary)

        val state = result.toFiClusterState()

        assertTrue(state is FiClusterState.NoFiLoad)
        assertEquals("buyer@example.com", (state as FiClusterState.NoFiLoad).email)
    }

    @Test
    fun `Success with neither a payment method nor a payer maps to NoNetworkLoad`() {
        val summary = PayPalSavedPaymentMethodSummary(paypalPayer = null, paypalSavedPaymentMethods = emptyList())
        val result = PayPalSavedPaymentMethodSummaryResult.Success(summary)

        assertTrue(result.toFiClusterState() is FiClusterState.NoNetworkLoad)
    }

    private fun paymentMethod(lastDigits: String) = PayPalSavedPaymentMethod(
        label = "label",
        imageUrl = "https://example.com/icon.png",
        lastDigits = lastDigits,
        type = "CARD",
        subtype = null
    )
}
