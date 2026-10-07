package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * A single main/disclaimer content block, extracted from JSON with no decisions made yet -
 * [PayPalCreditMessagingUtils] decides whether [text] or [alternativeText] renders.
 */
internal data class ContentItem(
    val type: String,
    val text: String,
    val alternativeText: String
)

/**
 * A single action item, extracted from JSON with no decisions made yet -
 * [PayPalCreditMessagingUtils] decides which action is the "Learn more" link.
 */
internal data class ActionItem(
    val type: String,
    val text: String,
    val url: String
)

/**
 * The raw, un-decided content blocks parsed from a single `/v2/credit/fetch-presentment-messages`
 * response - assembled by [PayPalCreditMessagingContent.fromJson] and handed to
 * [PayPalCreditMessagingUtils] as one unit instead of three separate lists.
 */
internal data class CreditMessagingContentBlocks(
    val mainItems: List<ContentItem>,
    val disclaimerItems: List<ContentItem>,
    val actionItems: List<ActionItem>
)

/**
 * Business logic backing [PayPalCreditMessagingContent] - message concatenation (including the
 * IMAGE/alternative_text substitution) and learn-more link selection. Shared by both the classic
 * View and Compose UI so the rules live in one place instead of being duplicated per UI surface.
 * [PayPalCreditMessagingContent.fromJson] owns all JSON parsing, extracting each item into a
 * [ContentItem] / [ActionItem] before calling into these functions.
 */
@ExperimentalBetaApi
internal object PayPalCreditMessagingUtils {

    private const val IMAGE_TYPE = "IMAGE"
    private const val LINK_TYPE = "LINK"

    // main items already carry their own spacing (e.g. "...mit " + logo + "."), so they're joined
    // with no separator. disclaimer items are a separate, optional sentence (e.g. "Nur mit dt.
    // PayPal Konto."), so when present they're appended after a space.
    fun message(content: CreditMessagingContentBlocks): String {
        val main = content.mainItems.displayText()
        val disclaimer = content.disclaimerItems.displayText()
        return if (disclaimer.isEmpty()) main else "$main $disclaimer"
    }

    fun learnMoreText(content: CreditMessagingContentBlocks): String =
        firstLink(content.actionItems)?.text.orEmpty()

    fun learnMoreUrl(content: CreditMessagingContentBlocks): String =
        firstLink(content.actionItems)?.url.orEmpty()

    // IMAGE blocks (e.g. the PayPal logo) render as their alternativeText instead of being
    // dropped, so the message still reads naturally without a second inline logo - the FI row
    // already shows one.
    private fun List<ContentItem>.displayText(): String =
        joinToString("") { if (it.type == IMAGE_TYPE) it.alternativeText else it.text }

    private fun firstLink(actionItems: List<ActionItem>): ActionItem? =
        actionItems.firstOrNull { it.type == LINK_TYPE }
}
