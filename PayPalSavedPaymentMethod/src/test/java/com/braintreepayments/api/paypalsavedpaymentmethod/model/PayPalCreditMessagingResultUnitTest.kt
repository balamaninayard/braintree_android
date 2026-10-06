package com.braintreepayments.api.paypalsavedpaymentmethod.model

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PayPalCreditMessagingResultUnitTest {

    @Test
    fun `fromJson parses all items and metadata`() {
        val json = JSONObject(
            """
            {
              "messages": [
                {
                  "preferred_message": {
                    "id": "message-1",
                    "type": "PLST_SQ",
                    "content": {
                      "main_items": [ { "type": "TEXT", "text": "Pay in 4" } ],
                      "disclaimer_items": [ { "type": "TEXT", "text": "See terms" } ],
                      "action_items": [ { "type": "LINK", "text": "Learn more", "click_url": "https://paypal.com" } ]
                    },
                    "analytics": { "impression_url": "https://paypal.com/impression" }
                  }
                }
              ]
            }
            """.trimIndent()
        )

        val result = PayPalCreditMessagingResult.fromJson(json)

        assertEquals("Pay in 4", result?.mainItems?.single()?.text)
        assertEquals("See terms", result?.disclaimerItems?.single()?.text)
        assertEquals("Learn more", result?.actionItems?.single()?.text)
        assertEquals("message-1", result?.messageId)
        assertEquals("PLST_SQ", result?.messageType)
        assertEquals("https://paypal.com/impression", result?.impressionUrl)
    }

    @Test
    fun `fromJson returns null when mainItems has no displayable text`() {
        assertNull(PayPalCreditMessagingResult.fromJson(messageWithMainItems("""[ { "type": "TEXT", "text": "" } ]""")))
    }

    @Test
    fun `fromJson treats an image block's alternativeText as displayable text`() {
        val json = messageWithMainItems("""[ { "type": "IMAGE", "alternative_text": "PayPal" } ]""")

        assertNotNull(PayPalCreditMessagingResult.fromJson(json))
    }

    @Test
    fun `fromJson drops malformed entries from content item arrays`() {
        val json = messageWithMainItems("""[ "not-an-object", { "type": "TEXT", "text": "Pay in 4" } ]""")

        assertEquals(1, PayPalCreditMessagingResult.fromJson(json)?.mainItems?.size)
    }

    @Test
    fun `fromJson returns null when preferred_message has no content`() {
        val json = JSONObject("""{ "messages": [ { "preferred_message": { "id": "message-1" } } ] }""")

        assertNull(PayPalCreditMessagingResult.fromJson(json))
    }

    @Test
    fun `fromJson returns null when there is no preferred_message`() {
        assertNull(PayPalCreditMessagingResult.fromJson(JSONObject("""{ "messages": [] }""")))
    }

    private fun messageWithMainItems(mainItems: String) = JSONObject(
        """{ "messages": [ { "preferred_message": { "content": { "main_items": $mainItems } } } ] }"""
    )
}
