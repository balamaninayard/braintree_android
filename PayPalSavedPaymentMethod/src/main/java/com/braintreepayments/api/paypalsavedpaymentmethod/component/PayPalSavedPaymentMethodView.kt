package com.braintreepayments.api.paypalsavedpaymentmethod.component

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCaller
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodClient
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import com.braintreepayments.api.paypalsavedpaymentmethod.callback.PayPalSavedPaymentMethodLaunchCallback
import com.braintreepayments.api.paypalsavedpaymentmethod.state.CreditMessagingState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.FiClusterState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.toCreditMessagingState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.toFiClusterState
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodStyleResolver
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodViewStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Root component: shows a returning buyer's saved PayPal funding instrument (View FI), lets them
 * change it via the edit pencil (Edit FI), and shows Pay Later credit messaging.
 *
 * TODO: full documentation pass once the design is finalized.
 */
@Suppress("TooManyFunctions")
class PayPalSavedPaymentMethodView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val payPalLogoView: ImageView
    private val payPalLabelView: TextView
    private val fiSection: FiSection
    private val creditMessagingView: CreditMessagingView
    private val containerBackground = GradientDrawable()
    private var style: PayPalSavedPaymentMethodViewStyle

    /** Constructed in [initialize]. */
    private lateinit var payPalSavedPaymentMethodClient: PayPalSavedPaymentMethodClient

    /** Constructed in [initialize], mirrors `PayPalButton`'s [PayPalLauncher] ownership. */
    private lateinit var payPalLauncher: PayPalLauncher

    private var callback: PayPalSavedPaymentMethodLaunchCallback? = null

    private var payPalRequest: PayPalCheckoutRequest? = null
    private var lastFiClusterState: FiClusterState = FiClusterState.Loading

    private var viewScope: CoroutineScope? = null
    private var fiFetchJob: Job? = null
    private var creditMessagingFetchJob: Job? = null
    private var editFlowJob: Job? = null

    private var fullScreenLoaderOverlay: View? = null

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.saved_paypal_payment_method_view, this, true)

        payPalLogoView = findViewById(R.id.paypal_saved_payment_method_paypal_logo)
        payPalLabelView = findViewById(R.id.paypal_saved_payment_method_paypal_label)
        fiSection = findViewById(R.id.paypal_saved_payment_method_fi_section)
        creditMessagingView = findViewById(R.id.paypal_saved_payment_method_credit_messaging)

        fiSection.setOnEditClickListener { startEditFlow() }

        style = styleFromAttrs(context, attrs, defStyleAttr)
        setupBackground()
        applyStyle(style)
    }

    /** Wires the edit-FI request/callback and starts the FI (+ credit-messaging) fetch. */
    fun initialize(
        activityResultCaller: ActivityResultCaller,
        authorization: String,
        appLinkReturnUrl: Uri,
        payPalRequest: PayPalCheckoutRequest,
        callback: PayPalSavedPaymentMethodLaunchCallback,
        deepLinkFallbackUrlScheme: String? = null
    ) {
        payPalLauncher = PayPalLauncher(activityResultCaller)
        payPalSavedPaymentMethodClient = PayPalSavedPaymentMethodClient(
            context,
            authorization,
            appLinkReturnUrl,
            deepLinkFallbackUrlScheme
        )
        this.payPalRequest = payPalRequest
        this.callback = callback
        startFetches()
    }

    /** Fully replaces any style parsed from XML attrs. */
    fun setStyle(style: PayPalSavedPaymentMethodViewStyle) {
        this.style = style
        applyStyle(style)
    }

    /**
     * Handles the return from the PayPal auth flow browser switch. Call from `onResume`/`onNewIntent`.
     *
     * @param pendingRequest the [PayPalPendingRequest.Started] delivered to
     * [PayPalSavedPaymentMethodLaunchCallback.onSavedPaymentMethodLaunch]. The merchant is responsible
     * for storing this across a process death and passing it back in here, matching `PayPalButton`.
     */
    @OptIn(ExperimentalBetaApi::class)
    fun handleReturnToApp(pendingRequest: PayPalPendingRequest.Started, intent: Intent) {
        val authResult = payPalLauncher.handleReturnToApp(pendingRequest, intent)
        when (authResult) {
            is PayPalPaymentAuthResult.Success ->
                payPalSavedPaymentMethodClient.tokenize(authResult, ::onTokenizeResult)
            is PayPalPaymentAuthResult.NoResult -> onEditFlowResult(PayPalResult.Cancel)
            is PayPalPaymentAuthResult.Failure -> onEditFlowResult(PayPalResult.Failure(authResult.error))
        }
    }

    private fun onTokenizeResult(result: PayPalResult) {
        when (result) {
            is PayPalResult.Success -> onEditFlowSuccess(result)
            else -> onEditFlowResult(result)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewScope?.cancel()
        viewScope = null
        fiFetchJob = null
        creditMessagingFetchJob = null
        editFlowJob = null
        hideFullScreenLoader()
    }

    private fun scope(): CoroutineScope =
        viewScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also { viewScope = it }

    /** Triggers this view's own FI + credit-messaging fetches. */
    private fun startFetches() {
        fetchFI()
        fetchCreditMessage()
    }

    @OptIn(ExperimentalBetaApi::class)
    private fun fetchFI() {
        fiSection.setState(FiClusterState.Loading)
        fiFetchJob = scope().launch {
            val state = payPalSavedPaymentMethodClient.fetchFI().toFiClusterState()
            lastFiClusterState = state
            fiSection.setState(state)
        }
    }

    @OptIn(ExperimentalBetaApi::class)
    private fun fetchCreditMessage() {
        if (!style.showPayPalCreditMessaging) {
            creditMessagingView.setState(CreditMessagingState.Hidden)
            return
        }
        creditMessagingView.setState(CreditMessagingState.Loading)
        creditMessagingFetchJob = scope().launch {
            val request = payPalRequest
            val state = payPalSavedPaymentMethodClient
                .fetchCreditPresentmentMessages(
                    amount = request?.amount.orEmpty(),
                    currency = request?.currencyCode
                )
                .toCreditMessagingState()
            creditMessagingView.setState(state)
        }
    }

    /** Edit pencil tap -> auth request -> browser/app switch. Outcome arrives via [handleReturnToApp]. */
    @OptIn(ExperimentalBetaApi::class)
    private fun startEditFlow() {
        val request = payPalRequest
            ?: throw NullPointerException("PayPalSavedPaymentMethodView must be initialized first")
        val activity = findActivity() ?: run {
            callback?.onSavedPaymentMethodResult(
                PayPalResult.Failure(NullPointerException("Activity is null"))
            )
            return
        }
        showFullScreenLoader()

        payPalSavedPaymentMethodClient.createPaymentAuthRequest(context, request) { paymentAuthRequest ->
            when (paymentAuthRequest) {
                is PayPalPaymentAuthRequest.ReadyToLaunch -> launchEditFlow(activity, paymentAuthRequest)
                is PayPalPaymentAuthRequest.Failure -> onAuthRequestFailure(paymentAuthRequest.error)
            }
        }
    }

    private fun launchEditFlow(
        activity: ComponentActivity,
        paymentAuthRequest: PayPalPaymentAuthRequest.ReadyToLaunch
    ) {
        when (val pendingRequest = payPalLauncher.launch(activity, paymentAuthRequest)) {
            is PayPalPendingRequest.Started -> callback?.onSavedPaymentMethodLaunch(pendingRequest)
            is PayPalPendingRequest.Failure -> onAuthRequestFailure(pendingRequest.error)
        }
    }

    /**
     * Auth-request creation or launch failed before ever reaching the browser switch -- surfaced
     * via the launch callback (matching `PayPalButton`), not the final result callback.
     */
    private fun onAuthRequestFailure(error: Exception) {
        fiSection.setState(lastFiClusterState)
        hideFullScreenLoader()
        callback?.onSavedPaymentMethodLaunch(PayPalPendingRequest.Failure(error))
    }

    /** Refetches the FI keyed by the just-approved order id, then reports success to the merchant. */
    @OptIn(ExperimentalBetaApi::class)
    private fun onEditFlowSuccess(result: PayPalResult.Success) {
        editFlowJob = scope().launch {
            hideFullScreenLoader()
            callback?.onSavedPaymentMethodResult(result)
            result.nonce.paymentId?.let { orderId ->
                val state = payPalSavedPaymentMethodClient.refetchFI(orderId = orderId).toFiClusterState()
                lastFiClusterState = state
                fiSection.setState(state)
            }
        }
    }

    /** Cancel/failure outcome of the edit-FI flow: restore the last known FI state and notify the merchant. */
    @OptIn(ExperimentalBetaApi::class)
    private fun onEditFlowResult(result: PayPalResult) {
        editFlowJob = scope().launch {
            fiSection.setState(lastFiClusterState)
            hideFullScreenLoader()
            callback?.onSavedPaymentMethodResult(result)
        }
    }

    /** Dims/blocks the entire host screen while edit-flow async work is in flight. */
    private fun showFullScreenLoader() {
        if (fullScreenLoaderOverlay != null) return
        val decorView = findActivity()?.window?.decorView as? ViewGroup ?: return

        val overlay = FrameLayout(context).apply {
            setBackgroundColor(ContextCompat.getColor(context, R.color.paypal_saved_payment_method_loader_scrim_color))
            isClickable = true
            isFocusable = true
        }
        val spinner = ProgressBar(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        }
        overlay.addView(spinner)

        decorView.addView(overlay, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        fullScreenLoaderOverlay = overlay
    }

    private fun hideFullScreenLoader() {
        val overlay = fullScreenLoaderOverlay ?: return
        (overlay.parent as? ViewGroup)?.removeView(overlay)
        fullScreenLoaderOverlay = null
    }

    private fun findActivity(): ComponentActivity? {
        var current: Context = context
        while (current is ContextWrapper) {
            if (current is ComponentActivity) return current
            current = current.baseContext
        }
        return null
    }

    private fun setupBackground() {
        containerBackground.shape = GradientDrawable.RECTANGLE
        background = containerBackground
    }

    private fun applyStyle(style: PayPalSavedPaymentMethodViewStyle) {
        val resolvedStyle = PayPalSavedPaymentMethodStyleResolver(context, style)

        payPalLogoView.isVisible = resolvedStyle.showPayPalLogo
        payPalLabelView.isVisible = resolvedStyle.showLabel

        // LogoStyle's contract fixes the logo's bounding box at 1:1 (width == height). The actual
        // ic_paypal_brand_logo asset is a non-square 48x30 baked-in shape, so it's drawn with
        // FIT_CENTER inside that square footprint -- undistorted, letterboxed -- rather than
        // stretched to fill it.
        val logoSizePx = resolvedStyle.logoWidthDp.dpToPx(resources).toInt()
        payPalLogoView.layoutParams = payPalLogoView.layoutParams.apply {
            width = logoSizePx
            height = logoSizePx
        }
        payPalLogoView.requestLayout()

        payPalLabelView.setTextColor(resolvedStyle.textColor)
        payPalLabelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, resolvedStyle.labelFontSizeSp)
        resolvedStyle.fontResId?.let { fontResId ->
            runCatching { ResourcesCompat.getFont(context, fontResId) }
                .getOrNull()
                ?.let { payPalLabelView.typeface = it }
        }
        (payPalLabelView.layoutParams as? LinearLayout.LayoutParams)?.marginStart =
            if (resolvedStyle.showPayPalLogo) resolvedStyle.labelMarginStartDp.dpToPx(resources).toInt() else 0

        (fiSection.layoutParams as? LinearLayout.LayoutParams)?.marginStart =
            resolvedStyle.fundingInstrumentMarginStartDp.dpToPx(resources).toInt()
        fiSection.applyStyle(resolvedStyle)

        creditMessagingView.applyStyle(resolvedStyle)
        (creditMessagingView.layoutParams as? LinearLayout.LayoutParams)?.marginStart =
            creditMessagingAnchorMarginPx(resolvedStyle)

        containerBackground.setColor(resolvedStyle.backgroundColor)
        containerBackground.cornerRadius = resolvedStyle.cornerRadiusDp.dpToPx(resources)
        containerBackground.setStroke(resolvedStyle.borderWidthDp.dpToPx(resources).toInt(), resolvedStyle.borderColor)
        resolvedStyle.heightDp?.let {
            layoutParams = layoutParams.apply { height = it.dpToPx(resources).toInt() }
        }
        setPadding(
            resolvedStyle.horizontalPaddingDp.dpToPx(resources).toInt(),
            resolvedStyle.verticalPaddingDp.dpToPx(resources).toInt(),
            resolvedStyle.horizontalPaddingDp.dpToPx(resources).toInt(),
            resolvedStyle.verticalPaddingDp.dpToPx(resources).toInt()
        )
    }

    /** Anchors under the label's start position; falls back to FiSection when logo+label hidden. */
    private fun creditMessagingAnchorMarginPx(resolvedStyle: PayPalSavedPaymentMethodStyleResolver): Int {
        if (!resolvedStyle.showPayPalLogo && !resolvedStyle.showLabel) return 0
        val logoWidth = if (resolvedStyle.showPayPalLogo) resolvedStyle.logoWidthDp else 0f
        return (logoWidth + resolvedStyle.labelMarginStartDp).dpToPx(resources).toInt()
    }
}
