package com.braintreepayments.api.paypalsavedpaymentmethod.styling

import android.content.Context
import androidx.annotation.ColorInt
import androidx.annotation.DimenRes
import androidx.annotation.FontRes
import androidx.core.content.ContextCompat
import com.braintreepayments.api.paypalsavedpaymentmethod.R

/**
 * Resolves a merchant-supplied [PayPalSavedPaymentMethodViewStyle] against the SDK defaults —
 * every property here is a concrete value the renderer can use directly, no more nulls to check.
 * SDK defaults live in `res/values/colors.xml` / `res/values/dimens.xml`
 * (`paypal_saved_payment_method_style_default_*`).
 *
 * Element-specific font sizes follow the documented fallback chain: the element's own value, then
 * [ComponentAppearance.baseFontSizeSp] only if the merchant explicitly set it, then that element's
 * own SDK default — never the resolved base size, since each element's default differs from the
 * generic base default.
 *
 * [heightDp] is the one exception: null here still means wrap-content, it is never defaulted to a
 * number.
 */
@Suppress("LongParameterList")
internal class PayPalSavedPaymentMethodStyleResolver(style: PayPalSavedPaymentMethodViewStyle, context: Context) {

    internal constructor(context: Context, style: PayPalSavedPaymentMethodViewStyle) : this(style, context)

    val showLogo: Boolean = style.showPayPalLogo
    val showPayPalLogo: Boolean = showLogo
    val showLabel: Boolean = style.showPayPalLabel
    val showCreditMessaging: Boolean = style.showPayPalCreditMessaging

    private val baseFontSizeSp: Float? = style.componentAppearance?.baseFontSizeSp

    @ColorInt
    val backgroundColor: Int = style.componentAppearance?.backgroundColor
        ?: context.colorInt(R.color.paypal_saved_payment_method_style_default_background)

    @ColorInt
    val textColor: Int = style.componentAppearance?.textColor
        ?: context.colorInt(R.color.paypal_saved_payment_method_style_default_text_color)

    @FontRes
    val fontResId: Int? = style.componentAppearance?.fontResId

    val heightDp: Float? = style.container?.heightDp
    val horizontalPaddingDp: Float = style.container?.horizontalPaddingDp
        ?: context.dimenDp(R.dimen.paypal_saved_payment_method_style_default_horizontal_padding)
    val verticalPaddingDp: Float = style.container?.verticalPaddingDp
        ?: context.dimenDp(R.dimen.paypal_saved_payment_method_style_default_vertical_padding)
    val cornerRadiusDp: Float = style.container?.cornerRadiusDp
        ?: context.dimenDp(R.dimen.paypal_saved_payment_method_style_default_corner_radius)

    @ColorInt
    val borderColor: Int = style.container?.borderColor
        ?: context.colorInt(R.color.paypal_saved_payment_method_style_default_border_color)
    val borderWidthDp: Float = style.container?.borderWidthDp
        ?: context.dimenDp(R.dimen.paypal_saved_payment_method_style_default_border_width)

    val logoWidthDp: Float = style.container?.logo?.widthDp
        ?: context.dimenDp(R.dimen.paypal_saved_payment_method_style_default_logo_width)

    val labelFontSizeSp: Float = style.container?.label?.fontSizeSp
        ?: baseFontSizeSp
        ?: context.dimenSp(R.dimen.paypal_saved_payment_method_style_default_label_font_size)
    val labelMarginStartDp: Float = style.container?.label?.marginStartDp
        ?: context.dimenDp(R.dimen.paypal_saved_payment_method_style_default_label_margin_start)

    val fundingInstrumentTextFontSizeSp: Float = style.container?.fundingInstrument?.textFontSizeSp
        ?: baseFontSizeSp
        ?: context.dimenSp(R.dimen.paypal_saved_payment_method_style_default_funding_instrument_text_font_size)
    val editIconSizeDp: Float = style.container?.fundingInstrument?.editIconSizeDp
        ?: context.dimenDp(R.dimen.paypal_saved_payment_method_style_default_edit_icon_size)
    val fundingInstrumentMarginStartDp: Float = style.container?.fundingInstrument?.marginStartDp
        ?: context.dimenDp(R.dimen.paypal_saved_payment_method_style_default_funding_instrument_margin_start)

    val creditMessagingFontSizeSp: Float = style.container?.creditMessaging?.fontSizeSp
        ?: baseFontSizeSp
        ?: context.dimenSp(R.dimen.paypal_saved_payment_method_style_default_credit_messaging_font_size)

    @ColorInt
    val creditMessagingLinkColor: Int? = style.container?.creditMessaging?.linkColor
}

@ColorInt
private fun Context.colorInt(colorRes: Int): Int = ContextCompat.getColor(this, colorRes)

private fun Context.dimenDp(@DimenRes dimenRes: Int): Float =
    resources.getDimension(dimenRes) / resources.displayMetrics.density

@Suppress("DEPRECATION")
private fun Context.dimenSp(@DimenRes dimenRes: Int): Float =
    resources.getDimension(dimenRes) / resources.displayMetrics.scaledDensity
