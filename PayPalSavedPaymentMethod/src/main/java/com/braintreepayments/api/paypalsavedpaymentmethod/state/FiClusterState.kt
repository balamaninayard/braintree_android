package com.braintreepayments.api.paypalsavedpaymentmethod.state

import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethod
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodSummaryResult

/**
 * The FI section's own internal state, mutated by `PayPalSavedPaymentMethodView` in response to
 * its own FI fetch. Never constructed or supplied by the merchant.
 *
 * [Loading] is a transient state shown while the fetch is in flight; it is distinct from the
 * content states below, which describe how the fetch's *result* renders.
 */
internal sealed class FiClusterState {

    /** The initial/in-flight state, shown until the FI fetch resolves. */
    data object Loading : FiClusterState()

    /**
     * A funding instrument is available to display.
     *
     * Whether [paymentMethod]'s [PayPalSavedPaymentMethod.imageUrl] actually loads is not part of
     * this state -- it can't be known at fetch time, since it depends on a separate, later image
     * load. `FiSection` attempts the load itself when rendering this state and falls back to a
     * generic bank/card glyph on failure; that fallback is a rendering-layer outcome, not a
     * distinct state the fetch layer produces.
     *
     * @property paymentMethod the funding instrument to render
     */
    data class Available @OptIn(ExperimentalBetaApi::class) constructor(
        val paymentMethod: PayPalSavedPaymentMethod
    ) : FiClusterState()

    /**
     * No funding instrument is available; falls back to showing the buyer's email only.
     * The edit pencil is still shown in this state.
     *
     * @property email the buyer's PayPal account email address
     */
    data class NoFiLoad(val email: String) : FiClusterState()

    /**
     * The FI fetch failed outright (e.g. no network). The entire FI section is hidden; only the
     * brand mark (logo/label) stays visible.
     */
    object NoNetworkLoad : FiClusterState()
}

/** Maps the raw fetch result into the FI section's render state. Shared by XML View and Compose. */
@OptIn(ExperimentalBetaApi::class)
internal fun PayPalSavedPaymentMethodSummaryResult.toFiClusterState(): FiClusterState {
    val summary = (this as? PayPalSavedPaymentMethodSummaryResult.Success)?.paymentMethodSummary
        ?: return FiClusterState.NoNetworkLoad
    val method = summary.paypalSavedPaymentMethods.firstOrNull()
    val payer = summary.paypalPayer
    return when {
        method != null -> FiClusterState.Available(method)
        payer != null -> FiClusterState.NoFiLoad(payer.email)
        else -> FiClusterState.NoNetworkLoad
    }
}
