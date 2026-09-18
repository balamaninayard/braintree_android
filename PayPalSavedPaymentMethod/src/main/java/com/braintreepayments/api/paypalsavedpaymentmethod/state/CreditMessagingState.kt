package com.braintreepayments.api.paypalsavedpaymentmethod.state

import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalCreditMessagingContent as RawPayPalCreditMessagingContent

/**
 * The credit messaging row's own internal state, mutated by `PayPalSavedPaymentMethodView` in
 * response to its own credit-messaging fetch. This fetch is independent of the FI fetch backing
 * [FiClusterState] and must never block or delay the FI section's render.
 */
sealed class CreditMessagingState {

    /** The in-flight state -- rendered as a shimmer. */
    data object Loading : CreditMessagingState()

    /**
     * Content was fetched successfully and is ready to render (style-level enable/disable happens
     * before this state is ever entered).
     *
     * @property content the message/link content to render
     */
    data class Content(val content: CreditMessagingContent) : CreditMessagingState()

    /**
     * The row is hidden -- either the fetch returned an empty response, the fetch failed, or the
     * row is disabled via style. All three collapse to the same visual (no row rendered).
     */
    data object Hidden : CreditMessagingState()
}

/** Maps the raw fetch result into the credit-messaging row's render state. Shared by XML View and Compose. */
@OptIn(ExperimentalBetaApi::class)
internal fun RawPayPalCreditMessagingContent?.toCreditMessagingState(): CreditMessagingState {
    val result = this ?: return CreditMessagingState.Hidden
    if (result.message.isBlank()) return CreditMessagingState.Hidden
    return CreditMessagingState.Content(
        CreditMessagingContent(
            message = result.message,
            learnMoreText = result.learnMoreText,
            learnMoreUrl = result.learnMoreUrl
        )
    )
}
