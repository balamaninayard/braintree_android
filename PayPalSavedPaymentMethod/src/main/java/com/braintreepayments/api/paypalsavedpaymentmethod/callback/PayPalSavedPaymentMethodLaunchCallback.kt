package com.braintreepayments.api.paypalsavedpaymentmethod.callback

import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult

/**
 * Callback supplied to `PayPalSavedPaymentMethodView.initialize` for the entire lifetime of the
 * edit-FI flow started by the view's edit pencil, analogous to
 * [com.braintreepayments.api.uicomponents.PayPalLaunchCallback] +
 * `PayPalTokenizeCallback` for `PayPalButton`, merged into one listener since the view's public
 * API only wires a single callback at [initialize] time.
 *
 * A merchant that needs to survive a process kill mid-flow should store
 * [PayPalPendingRequest.Started.pendingRequestString] from [onSavedPaymentMethodLaunch] and resume
 * it via `PayPalSavedPaymentMethodView.handleReturnToApp`, whose outcome is then delivered to
 * [onSavedPaymentMethodResult].
 */
interface PayPalSavedPaymentMethodLaunchCallback {

    /**
     * @param payPalPendingRequest a request used to launch the edit-FI PayPal payment
     * authorization flow, or a [PayPalPendingRequest.Failure] if the launch could not start.
     */
    fun onSavedPaymentMethodLaunch(payPalPendingRequest: PayPalPendingRequest)

    /**
     * @param result the edit-FI flow's final outcome: a new funding instrument tokenized, the
     * buyer cancelled, or the flow failed.
     */
    fun onSavedPaymentMethodResult(result: PayPalResult)
}
