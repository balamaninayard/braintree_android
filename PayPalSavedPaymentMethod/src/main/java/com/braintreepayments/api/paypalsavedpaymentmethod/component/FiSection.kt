package com.braintreepayments.api.paypalsavedpaymentmethod.component

import android.content.Context
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import com.braintreepayments.api.paypalsavedpaymentmethod.state.FiClusterState
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodStyleResolver

// Pill corner radius/padding are fixed SDK constants, not merchant-configurable -- the current
// PayPalSavedPaymentMethodViewStyle contract (see FundingInstrumentStyle) intentionally has no
// cornerRadiusDp/paddingDp fields for this cluster. The pill background color comes from
// R.color.paypal_saved_payment_method_pill_background_color so it reacts to the system light/dark theme.
private const val PILL_CORNER_RADIUS_DP = 999f
private const val PILL_PADDING_DP = 4f
private const val SHIMMER_MAX_ALPHA = 1f
private const val SHIMMER_MIN_ALPHA = 0.4f
private const val SHIMMER_DURATION_MS = 600L

/**
 * The funding-instrument pill: card-art icon, masked FI text (or fallback email), and an edit
 * pencil. Renders all states of [FiClusterState].
 *
 * Internal to the module -- not part of `PayPalSavedPaymentMethodView`'s public API -- so it
 * consumes [PayPalSavedPaymentMethodStyleResolver] directly rather than the raw nullable style
 * contract.
 *
 * TODO: this renders a fallback glyph for the funding-instrument icon; loading the real
 * card-art image is being provided separately.
 */
internal class FiSection @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val iconView: ImageView
    private val textView: TextView
    private val editIconView: ImageView
    private val shimmerView: View
    private val pillBackground = GradientDrawable()

    private var onEditClickListener: (() -> Unit)? = null
    private var shimmerAnimator: ValueAnimator? = null

    init {
        orientation = HORIZONTAL
        LayoutInflater.from(context).inflate(R.layout.fi_section, this, true)

        iconView = findViewById(R.id.paypal_saved_payment_method_fi_icon)
        textView = findViewById(R.id.paypal_saved_payment_method_fi_text)
        editIconView = findViewById(R.id.paypal_saved_payment_method_fi_edit_icon)
        shimmerView = findViewById(R.id.paypal_saved_payment_method_fi_shimmer)

        background = pillBackground
        editIconView.setOnClickListener { onEditClickListener?.invoke() }

        pillBackground.shape = GradientDrawable.RECTANGLE
        pillBackground.setColor(
            ContextCompat.getColor(context, R.color.paypal_saved_payment_method_pill_background_color)
        )
        pillBackground.cornerRadius = PILL_CORNER_RADIUS_DP.dpToPx(resources)
        val padding = PILL_PADDING_DP.dpToPx(resources).toInt()
        setPadding(padding, padding, padding, padding)

        renderLoading()
    }

    /** Applies resolved style values to the pill. Safe to call before or after [setState]. */
    fun applyStyle(resolvedStyle: PayPalSavedPaymentMethodStyleResolver) {
        textView.setTextColor(resolvedStyle.textColor)
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, resolvedStyle.fundingInstrumentTextFontSizeSp)
        resolvedStyle.fontResId?.let { fontResId ->
            runCatching { ResourcesCompat.getFont(context, fontResId) }
                .getOrNull()
                ?.let { textView.typeface = it }
        }

        val editIconSize = resolvedStyle.editIconSizeDp.dpToPx(resources).toInt()
        editIconView.layoutParams = editIconView.layoutParams.apply {
            width = editIconSize
            height = editIconSize
        }
        editIconView.requestLayout()
    }

    /** Registers a listener for taps on the edit pencil. */
    fun setOnEditClickListener(listener: () -> Unit) {
        onEditClickListener = listener
    }

    /**
     * Renders [state]. `PayPalSavedPaymentMethodView` calls this in response to its own FI fetch;
     * this view never triggers that fetch itself.
     */
    fun setState(state: FiClusterState) {
        iconView.setImageDrawable(null)

        when (state) {
            is FiClusterState.Loading -> renderLoading()
            is FiClusterState.Available -> renderAvailable(state)
            is FiClusterState.NoFiLoad -> renderNoFiLoad(state)
            is FiClusterState.NoNetworkLoad -> {
                stopShimmer()
                isVisible = false
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopShimmer()
    }

    private fun renderLoading() {
        isVisible = true
        iconView.isVisible = false
        editIconView.isVisible = false
        shimmerView.isVisible = true
        textView.isVisible = false
        textView.text = null
        startShimmer()
    }

    @OptIn(ExperimentalBetaApi::class)
    private fun renderAvailable(state: FiClusterState.Available) {
        isVisible = true
        stopShimmer()
        shimmerView.isVisible = false
        textView.isVisible = true
        editIconView.isVisible = true
        iconView.isVisible = true

        val method = state.paymentMethod
        textView.text = method.lastDigits
            ?.takeIf { it.isNotBlank() }
            ?.let {
                resources.getString(
                    R.string.paypal_saved_payment_method_label_funding_instrument_card_masked_number,
                    it
                )
            }
            ?: method.label
        iconView.setImageResource(fallbackIconRes(method.type))
    }

    private fun renderNoFiLoad(state: FiClusterState.NoFiLoad) {
        isVisible = true
        stopShimmer()
        shimmerView.isVisible = false
        textView.isVisible = true
        editIconView.isVisible = true
        iconView.isVisible = false
        textView.text = state.email
    }

    private fun startShimmer() {
        if (shimmerAnimator != null) return
        shimmerAnimator = ObjectAnimator.ofFloat(shimmerView, "alpha", SHIMMER_MAX_ALPHA, SHIMMER_MIN_ALPHA).apply {
            duration = SHIMMER_DURATION_MS
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
        }
    }

    private fun stopShimmer() {
        shimmerAnimator?.cancel()
        shimmerAnimator = null
        shimmerView.alpha = 1f
    }

    private fun fallbackIconRes(type: String): Int = if (type.equals("BANK", ignoreCase = true)) {
        R.drawable.ic_fi_bank_placeholder
    } else {
        R.drawable.ic_fi_card_placeholder
    }
}
