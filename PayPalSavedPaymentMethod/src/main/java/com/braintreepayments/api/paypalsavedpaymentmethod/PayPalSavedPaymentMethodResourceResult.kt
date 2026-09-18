package com.braintreepayments.api.paypalsavedpaymentmethod

/**
 * The result of calling `create_payment_resource` to start the edit-FI flow.
 *
 * Phase 1 note: this shape is a minimal placeholder sized to what
 * [PayPalSavedPaymentMethodClient]'s mock implementation needs to exercise the edit-tap ->
 * loading -> re-render path. The real Phase 2 contract (redirect parsing, app-switch eligibility
 * params) is not yet finalized and should be revisited before Phase 2 work begins.
 *
 * @property redirectUrl the URL the buyer should be redirected to in order to complete the edit
 * @property redirectType how [redirectUrl] should be launched (e.g. PayPal app switch vs. in-app
 * browser)
 */
data class PayPalSavedPaymentMethodResourceResult(
    val redirectUrl: String,
    val redirectType: String
)
