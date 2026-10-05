package com.braintreepayments.api.paypalsavedpaymentmethod.model

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PayPalSavedPaymentMethodSummaryUnitTest {

    @Test
    fun `fromJson parses an instrument response`() {
        val json = JSONObject(
            """
            {
              "payer": null,
              "paymentMethods": [
                {
                  "label": "CREDIT UNION 1",
                  "imageUrl": "https://example.com/bank.png",
                  "lastDigits": "3357",
                  "type": "BANK",
                  "subtype": null
                }
              ]
            }
            """.trimIndent()
        )

        val summary = PayPalSavedPaymentMethodSummary.fromJson(json)

        assertEquals(1, summary?.paymentMethods?.size)
        assertEquals("CREDIT UNION 1", summary?.paymentMethods?.first()?.label)
        assertNull(summary?.payer)
    }

    @Test
    fun `fromJson parses a display only payer response`() {
        val json = JSONObject(
            """{ "payer": { "email": "buyer@example.com", "editable": false }, "paymentMethods": [] }"""
        )

        val summary = PayPalSavedPaymentMethodSummary.fromJson(json)

        assertTrue(summary?.paymentMethods?.isEmpty() == true)
        assertEquals("buyer@example.com", summary?.payer?.email)
        assertEquals(false, summary?.payer?.isEditable)
    }

    @Test
    fun `fromJson returns an empty summary when there is nothing to display`() {
        val summary = PayPalSavedPaymentMethodSummary.fromJson(JSONObject())

        assertTrue(summary?.paymentMethods?.isEmpty() == true)
        assertNull(summary?.payer)
    }

    @Test
    fun `fromJson drops malformed entries from the paymentMethods array`() {
        val json = JSONObject("""{ "paymentMethods": [ "not-an-object", { "type": "CARD", "label": "Visa" } ] }""")

        val summary = PayPalSavedPaymentMethodSummary.fromJson(json)

        assertEquals(1, summary?.paymentMethods?.size)
        assertEquals("Visa", summary?.paymentMethods?.first()?.label)
    }

    @Test
    fun `fromJson returns null for a null json object`() {
        assertNull(PayPalSavedPaymentMethodSummary.fromJson(null))
    }
}
