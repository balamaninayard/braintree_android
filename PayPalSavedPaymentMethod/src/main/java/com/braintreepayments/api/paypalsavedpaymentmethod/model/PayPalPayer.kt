package com.braintreepayments.api.paypalsavedpaymentmethod.model

import com.braintreepayments.api.sharedutils.Json
import org.json.JSONObject

/**
 * The buyer's PayPal account.
 *
 * @property email The email address associated with the buyer's PayPal account.
 * @property isEditable Whether the buyer is allowed to change the funding instrument PayPal will charge.
 */
internal data class PayPalPayer(
    val email: String?,
    val isEditable: Boolean?
) {

    companion object {
        private const val EMAIL_KEY = "email"
        private const val EDITABLE_KEY = "editable"

        /**
         * Parses the `payer` field of a funding instrument details response. Returns `null` when PayPal returns no
         * payer.
         */
        fun fromJson(json: JSONObject?): PayPalPayer? {
            if (json == null) return null
            return PayPalPayer(
                email = Json.optString(json, EMAIL_KEY, null),
                isEditable = if (json.isNull(EDITABLE_KEY)) null else json.optBoolean(EDITABLE_KEY)
            )
        }
    }
}
