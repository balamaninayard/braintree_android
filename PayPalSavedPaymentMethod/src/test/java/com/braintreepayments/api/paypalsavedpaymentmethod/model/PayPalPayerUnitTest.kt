package com.braintreepayments.api.paypalsavedpaymentmethod.model

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PayPalPayerUnitTest {

    @Test
    fun `fromJson parses email and editable`() {
        val result = PayPalPayer.fromJson(JSONObject().put("email", "buyer@example.com").put("editable", true))

        assertEquals("buyer@example.com", result?.email)
        assertEquals(true, result?.isEditable)
    }

    @Test
    fun `fromJson returns a null isEditable when the field is absent`() {
        val result = PayPalPayer.fromJson(JSONObject().put("email", "buyer@example.com"))

        assertNull(result?.isEditable)
    }

    @Test
    fun `fromJson returns a null isEditable when the field is null`() {
        val result = PayPalPayer.fromJson(JSONObject().put("editable", JSONObject.NULL))

        assertNull(result?.isEditable)
    }

    @Test
    fun `fromJson returns null for a null json object`() {
        assertNull(PayPalPayer.fromJson(null))
    }
}
