package com.braintreepayments.api.paypalsavedpaymentmethod.state

/**
 * The rendered content for the credit messaging row (the "Pay in 4..." line), assembled from the
 * checkout API response's structured content blocks.
 *
 * @property message the assembled message copy (from `main_items` TEXT/TEXT_VARIABLE blocks)
 * @property learnMoreText the "Learn more" link's display text
 * @property learnMoreUrl the URL opened in the credit-messaging lander when the link is tapped
 */
data class CreditMessagingContent(
    val message: String,
    val learnMoreText: String,
    val learnMoreUrl: String
)
