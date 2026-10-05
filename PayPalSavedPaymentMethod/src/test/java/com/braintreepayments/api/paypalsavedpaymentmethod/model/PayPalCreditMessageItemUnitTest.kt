package com.braintreepayments.api.paypalsavedpaymentmethod.model

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PayPalCreditMessageItemUnitTest {

    @Test
    fun `fromJson parses a text block`() {
        val json = JSONObject()
            .put("type", "TEXT")
            .put("text", "4 interest-free payments of \$13.75 with ")
            .put("name", "periodic_payment_count")
            .put("embeddable", true)

        val item = PayPalCreditMessageItem.fromJson(json)

        assertEquals(PayPalCreditMessageItemType.TEXT, item?.type)
        assertEquals("4 interest-free payments of \$13.75 with ", item?.text)
        assertEquals("periodic_payment_count", item?.name)
        assertEquals(true, item?.isEmbeddable)
    }

    @Test
    fun `fromJson parses an image block`() {
        val json = JSONObject()
            .put("type", "IMAGE")
            .put("alternative_text", "PayPal")
            .put("click_url", "https://paypal.com/learn-more")
            .put("source_url", "https://paypal.com/logo.png")

        val item = PayPalCreditMessageItem.fromJson(json)

        assertEquals(PayPalCreditMessageItemType.IMAGE, item?.type)
        assertEquals("PayPal", item?.alternativeText)
        assertEquals("https://paypal.com/learn-more", item?.clickUrl)
        assertEquals("https://paypal.com/logo.png", item?.sourceUrl)
    }

    @Test
    fun `fromJson returns a null isEmbeddable when the field is absent`() {
        val item = PayPalCreditMessageItem.fromJson(JSONObject().put("type", "LINK"))

        assertNull(item?.isEmbeddable)
    }

    @Test
    fun `fromJson sets type to null for an unrecognized type`() {
        val item = PayPalCreditMessageItem.fromJson(JSONObject().put("type", "TEXT_VARIABLE"))

        assertNull(item?.type)
    }

    @Test
    fun `fromJson returns null for a null json object`() {
        assertNull(PayPalCreditMessageItem.fromJson(null))
    }
}
