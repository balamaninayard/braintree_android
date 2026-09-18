package com.braintreepayments.api.paypalsavedpaymentmethod.component

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.ComponentAppearance
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.ContainerStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.CreditMessagingStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.FundingInstrumentStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalLabelStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalLogoStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodViewStyle

/** Parses [attrs] into a [PayPalSavedPaymentMethodViewStyle]; unset attrs resolve to SDK defaults. */
internal fun styleFromAttrs(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
): PayPalSavedPaymentMethodViewStyle {
    val typedArray = context.obtainStyledAttributes(
        attrs,
        R.styleable.PayPalSavedPaymentMethodView,
        defStyleAttr,
        0
    )
    return try {
        val density = context.resources.displayMetrics.density
        val scaledDensity = context.resources.displayMetrics.scaledDensity

        PayPalSavedPaymentMethodViewStyle(
            showPayPalLogo = typedArray.getBoolean(R.styleable.PayPalSavedPaymentMethodView_showPayPalLogo, true),
            showPayPalLabel = typedArray.getBoolean(R.styleable.PayPalSavedPaymentMethodView_showPayPalLabel, true),
            showPayPalCreditMessaging = typedArray.getBoolean(
                R.styleable.PayPalSavedPaymentMethodView_showPayPalCreditMessaging,
                true
            ),
            componentAppearance = parseComponentAppearance(typedArray, scaledDensity),
            container = parseContainerStyle(typedArray, density, scaledDensity)
        )
    } finally {
        typedArray.recycle()
    }
}

private fun parseComponentAppearance(typedArray: TypedArray, scaledDensity: Float): ComponentAppearance =
    ComponentAppearance(
        backgroundColor = typedArray.colorOrNull(R.styleable.PayPalSavedPaymentMethodView_componentBackgroundColor),
        textColor = typedArray.colorOrNull(R.styleable.PayPalSavedPaymentMethodView_componentTextColor),
        baseFontSizeSp = typedArray.dimensionOrNull(
            R.styleable.PayPalSavedPaymentMethodView_componentBaseFontSizeSp,
            scaledDensity
        ),
        fontResId = typedArray.resourceIdOrNull(R.styleable.PayPalSavedPaymentMethodView_componentFontResId)
    )

private fun parseContainerStyle(typedArray: TypedArray, density: Float, scaledDensity: Float): ContainerStyle =
    ContainerStyle(
        heightDp = typedArray.dimensionOrNull(R.styleable.PayPalSavedPaymentMethodView_containerHeightDp, density),
        horizontalPaddingDp = typedArray.dimensionOrNull(
            R.styleable.PayPalSavedPaymentMethodView_containerHorizontalPaddingDp,
            density
        ),
        verticalPaddingDp = typedArray.dimensionOrNull(
            R.styleable.PayPalSavedPaymentMethodView_containerVerticalPaddingDp,
            density
        ),
        cornerRadiusDp = typedArray.dimensionOrNull(
            R.styleable.PayPalSavedPaymentMethodView_containerCornerRadiusDp,
            density
        ),
        borderColor = typedArray.colorOrNull(R.styleable.PayPalSavedPaymentMethodView_containerBorderColor),
        borderWidthDp = typedArray.dimensionOrNull(
            R.styleable.PayPalSavedPaymentMethodView_containerBorderWidthDp,
            density
        ),
        logo = parseLogoStyle(typedArray, density),
        label = parseLabelStyle(typedArray, density, scaledDensity),
        fundingInstrument = parseFundingInstrumentStyle(typedArray, density, scaledDensity),
        creditMessaging = parseCreditMessagingStyle(typedArray, scaledDensity)
    )

private fun parseLogoStyle(typedArray: TypedArray, density: Float): PayPalLogoStyle =
    PayPalLogoStyle(
        widthDp = typedArray.dimensionOrNull(R.styleable.PayPalSavedPaymentMethodView_payPalLogoWidthDp, density)
    )

private fun parseLabelStyle(typedArray: TypedArray, density: Float, scaledDensity: Float): PayPalLabelStyle =
    PayPalLabelStyle(
        fontSizeSp = typedArray.dimensionOrNull(
            R.styleable.PayPalSavedPaymentMethodView_payPalLabelFontSizeSp,
            scaledDensity
        ),
        marginStartDp = typedArray.dimensionOrNull(
            R.styleable.PayPalSavedPaymentMethodView_payPalLabelMarginStartDp,
            density
        )
    )

private fun parseFundingInstrumentStyle(
    typedArray: TypedArray,
    density: Float,
    scaledDensity: Float
): FundingInstrumentStyle =
    FundingInstrumentStyle(
        textFontSizeSp = typedArray.dimensionOrNull(
            R.styleable.PayPalSavedPaymentMethodView_fundingInstrumentTextFontSizeSp,
            scaledDensity
        ),
        editIconSizeDp = typedArray.dimensionOrNull(
            R.styleable.PayPalSavedPaymentMethodView_fundingInstrumentEditIconSizeDp,
            density
        ),
        marginStartDp = typedArray.dimensionOrNull(
            R.styleable.PayPalSavedPaymentMethodView_fundingInstrumentMarginStartDp,
            density
        )
    )

private fun parseCreditMessagingStyle(typedArray: TypedArray, scaledDensity: Float): CreditMessagingStyle =
    CreditMessagingStyle(
        fontSizeSp = typedArray.dimensionOrNull(
            R.styleable.PayPalSavedPaymentMethodView_creditMessagingFontSizeSp,
            scaledDensity
        ),
        linkColor = typedArray.colorOrNull(R.styleable.PayPalSavedPaymentMethodView_creditMessagingLinkColor)
    )

private fun TypedArray.colorOrNull(index: Int): Int? =
    if (hasValue(index)) getColor(index, 0) else null

private fun TypedArray.resourceIdOrNull(index: Int): Int? =
    if (hasValue(index)) getResourceId(index, 0) else null

private fun TypedArray.dimensionOrNull(index: Int, density: Float): Float? =
    if (hasValue(index)) getDimension(index, 0f) / density else null
