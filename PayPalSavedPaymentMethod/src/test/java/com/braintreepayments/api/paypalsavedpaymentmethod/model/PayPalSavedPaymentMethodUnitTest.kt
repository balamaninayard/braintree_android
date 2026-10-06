package com.braintreepayments.api.paypalsavedpaymentmethod.model

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PayPalSavedPaymentMethodUnitTest {

    @Test
    fun `fromJson parses all fields`() {
        val json = JSONObject()
            .put("label", "PayPal Credit")
            .put("imageUrl", "https://example.com/paypal-credit.png")
            .put("lastDigits", "3357")
            .put("type", "PAYPAL_CREDIT")
            .put("subtype", "PAY_LATER_US")

        val result = PayPalSavedPaymentMethod.fromJson(json)

        assertEquals("PayPal Credit", result?.label)
        assertEquals("https://example.com/paypal-credit.png", result?.imageUrl)
        assertEquals("3357", result?.lastDigits)
        assertEquals(PayPalSavedPaymentMethodType.PAYPAL_CREDIT, result?.type)
        assertEquals("PAY_LATER_US", result?.subtype)
    }

    @Test
    fun `fromJson sets type to null for an unrecognized type and keeps the rest of the instrument`() {
        val result = PayPalSavedPaymentMethod.fromJson(JSONObject().put("type", "SOMETHING_NEW").put("label", "Visa"))

        assertNull(result?.type)
        assertEquals("Visa", result?.label)
    }

    @Test
    fun `fromJson returns null for missing and explicitly null optional fields`() {
        val json = JSONObject().put("type", "CARD").put("subtype", JSONObject.NULL)

        val result = PayPalSavedPaymentMethod.fromJson(json)

        assertNull(result?.label)
        assertNull(result?.imageUrl)
        assertNull(result?.lastDigits)
        assertNull(result?.subtype)
    }

    @Test
    fun `fromJson returns null for a null json object`() {
        assertNull(PayPalSavedPaymentMethod.fromJson(null))
    }
}
