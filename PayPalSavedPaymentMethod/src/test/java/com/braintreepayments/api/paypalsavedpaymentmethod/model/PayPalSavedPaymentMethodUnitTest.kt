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
            .put("label", "CREDIT UNION 1")
            .put("imageUrl", "https://example.com/bank.png")
            .put("lastDigits", "3357")
            .put("type", "BANK")
            .put("subtype", "PAY_LATER_US")

        val result = PayPalSavedPaymentMethod.fromJson(json)

        assertEquals("CREDIT UNION 1", result?.label)
        assertEquals("https://example.com/bank.png", result?.imageUrl)
        assertEquals("3357", result?.lastDigits)
        assertEquals(PayPalSavedPaymentMethodType.BANK, result?.type)
        assertEquals("PAY_LATER_US", result?.subtype)
    }

    @Test
    fun `fromJson sets type to null for an unrecognized type`() {
        val result = PayPalSavedPaymentMethod.fromJson(JSONObject().put("type", "SOMETHING_NEW"))

        assertNull(result?.type)
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
