package com.braintreepayments.api.paypalsavedpaymentmethod.component

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import com.braintreepayments.api.paypalsavedpaymentmethod.state.CreditMessagingContent
import com.braintreepayments.api.paypalsavedpaymentmethod.state.CreditMessagingState
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodStyleResolver

private const val SHIMMER_MAX_ALPHA = 1f
private const val SHIMMER_MIN_ALPHA = 0.4f
private const val SHIMMER_DURATION_MS = 600L

/**
 * The Pay Later / credit messaging row. Renders one of three states:
 * [CreditMessagingState.Loading] (shimmer), [CreditMessagingState.Content] (message + "Learn
 * more" tap target), or [CreditMessagingState.Hidden].
 *
 * Internal to the module -- not part of `PayPalSavedPaymentMethodView`'s public API -- so it
 * consumes [PayPalSavedPaymentMethodStyleResolver] directly rather than the raw nullable style
 * contract.
 *
 * This view owns one piece of interaction directly: tapping "Learn more" opens
 * [CreditMessagingLander], since that's pure UI (an in-app webview bottom sheet), not a
 * merchant-facing outcome -- unlike the edit-FI flow, nothing here needs to reach the merchant's
 * callback surface.
 */
internal class CreditMessagingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val shimmerView: View
    private val textView: TextView

    private var linkColor: Int? = null
    private var shimmerAnimator: ValueAnimator? = null
    private val lander by lazy { CreditMessagingLander(context) }

    init {
        LayoutInflater.from(context).inflate(R.layout.credit_messaging_view, this, true)

        shimmerView = findViewById(R.id.paypal_saved_payment_method_credit_messaging_shimmer)
        textView = findViewById(R.id.paypal_saved_payment_method_credit_messaging_text)
        textView.movementMethod = LinkMovementMethod.getInstance()
        // ClickableSpan already gives per-word feedback; a full-line highlight box looks wrong
        // for a single-line compliance message.
        textView.highlightColor = android.graphics.Color.TRANSPARENT

        setState(CreditMessagingState.Hidden)
    }

    /** Applies resolved style values to the row. Safe to call before or after [setState]. */
    fun applyStyle(resolvedStyle: PayPalSavedPaymentMethodStyleResolver) {
        textView.setTextColor(resolvedStyle.textColor)
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, resolvedStyle.creditMessagingFontSizeSp)
        resolvedStyle.fontResId?.let { fontResId ->
            runCatching { ResourcesCompat.getFont(context, fontResId) }
                .getOrNull()
                ?.let { textView.typeface = it }
        }
        linkColor = resolvedStyle.creditMessagingLinkColor
    }

    /**
     * Renders [state]. `PayPalSavedPaymentMethodView` calls this in response to its own
     * credit-messaging fetch; this view never triggers that fetch itself.
     */
    fun setState(state: CreditMessagingState) {
        when (state) {
            is CreditMessagingState.Loading -> renderLoading()
            is CreditMessagingState.Content -> renderContent(state.content)
            is CreditMessagingState.Hidden -> renderHidden()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopShimmer()
    }

    private fun renderLoading() {
        isVisible = true
        shimmerView.isVisible = true
        textView.isVisible = false
        startShimmer()
    }

    private fun renderContent(content: CreditMessagingContent) {
        isVisible = true
        shimmerView.isVisible = false
        stopShimmer()
        textView.isVisible = true
        textView.text = buildMessageSpan(content)
    }

    private fun renderHidden() {
        isVisible = false
        stopShimmer()
    }

    private fun buildMessageSpan(content: CreditMessagingContent): CharSequence {
        val builder = SpannableStringBuilder()
        builder.append(content.message)
        builder.append(" ")

        val learnMoreStart = builder.length
        builder.append(content.learnMoreText)
        val learnMoreEnd = builder.length

        val resolvedLinkColor = linkColor
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                lander.load(content.learnMoreUrl)
            }

            override fun updateDrawState(ds: android.text.TextPaint) {
                super.updateDrawState(ds)
                if (resolvedLinkColor != null) {
                    ds.color = resolvedLinkColor
                    ds.isUnderlineText = false
                } else {
                    // null linkColor -> bold + underlined in textColor, per CreditMessagingStyle's
                    // KDoc.
                    ds.color = textView.currentTextColor
                    ds.isUnderlineText = true
                }
            }
        }
        builder.setSpan(clickableSpan, learnMoreStart, learnMoreEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        if (resolvedLinkColor == null) {
            builder.setSpan(
                StyleSpan(android.graphics.Typeface.BOLD),
                learnMoreStart,
                learnMoreEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.setSpan(UnderlineSpan(), learnMoreStart, learnMoreEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return builder
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
}
