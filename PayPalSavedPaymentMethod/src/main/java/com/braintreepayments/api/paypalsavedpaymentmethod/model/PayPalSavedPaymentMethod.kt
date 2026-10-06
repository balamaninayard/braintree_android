package com.braintreepayments.api.paypalsavedpaymentmethod.model

import com.braintreepayments.api.sharedutils.Json
import org.json.JSONObject

/**
 * A funding instrument PayPal can charge on the buyer's behalf.
 *
 * @property type The kind of funding instrument, or `null` when PayPal returns a type this SDK version does not
 * recognize.
 * @property label The display name of the funding instrument, for example `"Visa"` or `"CREDIT UNION 1"`.
 * @property imageUrl The card art or bank glyph to render alongside the label.
 * @property lastDigits The last digits of the funding instrument's account number.
 * @property subtype A further qualifier on [type], returned for [PayPalSavedPaymentMethodType.PAYPAL_CREDIT]
 * instruments.
 */
internal data class PayPalSavedPaymentMethod(
    val type: PayPalSavedPaymentMethodType?,
    val label: String?,
    val imageUrl: String?,
    val lastDigits: String?,
    val subtype: String?
) {

    companion object {
        private const val TYPE_KEY = "type"
        private const val LABEL_KEY = "label"
        private const val IMAGE_URL_KEY = "imageUrl"
        private const val LAST_DIGITS_KEY = "lastDigits"
        private const val SUBTYPE_KEY = "subtype"

        /**
         * Parses a single entry of the `paymentMethods` array. Returns `null` when the entry is not an object.
         */
        fun fromJson(json: JSONObject?): PayPalSavedPaymentMethod? {
            if (json == null) return null
            return PayPalSavedPaymentMethod(
                type = PayPalSavedPaymentMethodType.fromRawValue(Json.optString(json, TYPE_KEY, null)),
                label = Json.optString(json, LABEL_KEY, null),
                imageUrl = Json.optString(json, IMAGE_URL_KEY, null),
                lastDigits = Json.optString(json, LAST_DIGITS_KEY, null),
                subtype = Json.optString(json, SUBTYPE_KEY, null)
            )
        }
    }
}
