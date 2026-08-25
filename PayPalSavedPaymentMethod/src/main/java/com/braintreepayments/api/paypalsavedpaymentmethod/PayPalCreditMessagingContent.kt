package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.ExperimentalBetaApi
import org.json.JSONArray
import org.json.JSONObject

/**
 * Pay Later / Credit presentment messaging for the edit-FI row, flattened and ready to render.
 * Assembled by [fromJson] using [PayPalCreditMessagingUtils] so both the classic View and Compose
 * UI consume the same shape.
 *
 * Note: **This module is in beta. It's public API may change or be removed in future releases.**
 *
 * @property message Copy to render, built from `content.main_items` and `content.disclaimer_items`.
 * Image blocks (e.g. the PayPal logo) contribute their `alternative_text` instead of being dropped.
 * @property learnMoreText Display text for the "Learn more" action, or empty if none was returned.
 * @property learnMoreUrl URL to open when "Learn more" is tapped, or empty if none was returned.
 */
@ExperimentalBetaApi
data class PayPalCreditMessagingContent(
    val message: String,
    val learnMoreText: String,
    val learnMoreUrl: String
) {

    internal companion object {
        private const val MESSAGES_KEY = "messages"
        private const val PREFERRED_MESSAGE_KEY = "preferred_message"
        private const val CONTENT_KEY = "content"
        private const val MAIN_ITEMS_KEY = "main_items"
        private const val DISCLAIMER_ITEMS_KEY = "disclaimer_items"
        private const val ACTION_ITEMS_KEY = "action_items"
        private const val TYPE_KEY = "type"
        private const val TEXT_KEY = "text"
        private const val ALTERNATIVE_TEXT_KEY = "alternative_text"
        private const val CLICK_URL_KEY = "click_url"

        /**
         * Parses a `/v2/credit/fetch-presentment-messages` response, or null if there's no
         * `preferred_message` to render - callers should hide the messaging row; the FI card
         * still renders.
         */
        @OptIn(ExperimentalBetaApi::class)
        fun fromJson(response: JSONObject): PayPalCreditMessagingContent? =
            response
                .optJSONArray(MESSAGES_KEY)
                ?.optJSONObject(0)
                ?.optJSONObject(PREFERRED_MESSAGE_KEY)
                ?.optJSONObject(CONTENT_KEY)
                ?.let { content ->
                    val contentBlocks = CreditMessagingContentBlocks(
                        mainItems = content.optJSONArray(MAIN_ITEMS_KEY).toContentItems(),
                        disclaimerItems = content.optJSONArray(DISCLAIMER_ITEMS_KEY).toContentItems(),
                        actionItems = content.optJSONArray(ACTION_ITEMS_KEY).toActionItems()
                    )
                    PayPalCreditMessagingContent(
                        message = PayPalCreditMessagingUtils.message(contentBlocks),
                        learnMoreText = PayPalCreditMessagingUtils.learnMoreText(contentBlocks),
                        learnMoreUrl = PayPalCreditMessagingUtils.learnMoreUrl(contentBlocks)
                    )
                }

        // Pure extraction - every field is copied as-is with no decisions made;
        // PayPalCreditMessagingUtils decides how each item is used.
        private fun JSONArray?.toContentItems(): List<ContentItem> {
            if (this == null) return emptyList()
            return (0 until length()).map { index ->
                val item = getJSONObject(index)
                ContentItem(
                    type = item.optString(TYPE_KEY),
                    text = item.optString(TEXT_KEY),
                    alternativeText = item.optString(ALTERNATIVE_TEXT_KEY)
                )
            }
        }

        private fun JSONArray?.toActionItems(): List<ActionItem> {
            if (this == null) return emptyList()
            return (0 until length()).map { index ->
                val item = getJSONObject(index)
                ActionItem(
                    type = item.optString(TYPE_KEY),
                    text = item.optString(TEXT_KEY),
                    url = item.optString(CLICK_URL_KEY)
                )
            }
        }
    }
}
