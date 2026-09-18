package com.braintreepayments.api.paypalsavedpaymentmethod.state

import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalCreditMessagingContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalBetaApi::class)
class CreditMessagingStateUnitTest {

    @Test
    fun `null result maps to Hidden`() {
        val result: PayPalCreditMessagingContent? = null

        assertTrue(result.toCreditMessagingState() is CreditMessagingState.Hidden)
    }

    @Test
    fun `result with blank message maps to Hidden`() {
        val result = creditMessagingContent(message = "  ")

        assertTrue(result.toCreditMessagingState() is CreditMessagingState.Hidden)
    }

    @Test
    fun `result with non-blank message maps to Content with message and learn-more fields`() {
        val result = creditMessagingContent(
            message = "Pay in 4 of \$25",
            learnMoreText = "Learn more",
            learnMoreUrl = "https://paypal.com/pay-later"
        )

        val state = result.toCreditMessagingState()

        assertTrue(state is CreditMessagingState.Content)
        val content = (state as CreditMessagingState.Content).content
        assertEquals("Pay in 4 of \$25", content.message)
        assertEquals("Learn more", content.learnMoreText)
        assertEquals("https://paypal.com/pay-later", content.learnMoreUrl)
    }

    @Test
    fun `empty learnMoreText and learnMoreUrl pass through unchanged`() {
        val result = creditMessagingContent(
            message = "Pay in 4",
            learnMoreText = "",
            learnMoreUrl = ""
        )

        val content = (result.toCreditMessagingState() as CreditMessagingState.Content).content

        assertEquals("", content.learnMoreText)
        assertEquals("", content.learnMoreUrl)
    }

    private fun creditMessagingContent(
        message: String,
        learnMoreText: String = "Learn more",
        learnMoreUrl: String = "https://paypal.com"
    ) = PayPalCreditMessagingContent(
        message = message,
        learnMoreText = learnMoreText,
        learnMoreUrl = learnMoreUrl
    )
}
