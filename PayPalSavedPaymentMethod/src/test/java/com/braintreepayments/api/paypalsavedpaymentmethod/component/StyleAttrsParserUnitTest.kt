package com.braintreepayments.api.paypalsavedpaymentmethod.component

import android.content.Context
import android.graphics.Color
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class StyleAttrsParserUnitTest {

    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun `no attrs set resolves to a style with everything unset`() {
        val attrs = Robolectric.buildAttributeSet().build()

        val style = styleFromAttrs(context, attrs, 0)

        assertEquals(true, style.showPayPalLogo)
        assertEquals(true, style.showPayPalLabel)
        assertEquals(true, style.showPayPalCreditMessaging)
        assertNull(style.componentAppearance?.backgroundColor)
        assertNull(style.container?.horizontalPaddingDp)
    }

    @Test
    fun `boolean attrs are parsed`() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.showPayPalLogo, "false")
            .addAttribute(R.attr.showPayPalLabel, "false")
            .addAttribute(R.attr.showPayPalCreditMessaging, "false")
            .build()

        val style = styleFromAttrs(context, attrs, 0)

        assertEquals(false, style.showPayPalLogo)
        assertEquals(false, style.showPayPalLabel)
        assertEquals(false, style.showPayPalCreditMessaging)
    }

    @Test
    fun `componentAppearance attrs are parsed into matching fields`() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.componentBackgroundColor, "#0000FF")
            .addAttribute(R.attr.componentTextColor, "#FF0000")
            .addAttribute(R.attr.componentBaseFontSizeSp, "18sp")
            .build()

        val appearance = styleFromAttrs(context, attrs, 0).componentAppearance

        assertEquals(Color.BLUE, appearance?.backgroundColor)
        assertEquals(Color.RED, appearance?.textColor)
        assertEquals(18f, appearance?.baseFontSizeSp)
    }

    @Test
    fun `containerStyle dimension and color attrs are parsed into matching fields`() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.containerHeightDp, "48dp")
            .addAttribute(R.attr.containerHorizontalPaddingDp, "16dp")
            .addAttribute(R.attr.containerVerticalPaddingDp, "12dp")
            .addAttribute(R.attr.containerCornerRadiusDp, "8dp")
            .addAttribute(R.attr.containerBorderColor, "#00FF00")
            .addAttribute(R.attr.containerBorderWidthDp, "1dp")
            .build()

        val container = styleFromAttrs(context, attrs, 0).container

        assertEquals(48f, container?.heightDp)
        assertEquals(16f, container?.horizontalPaddingDp)
        assertEquals(12f, container?.verticalPaddingDp)
        assertEquals(8f, container?.cornerRadiusDp)
        assertEquals(Color.GREEN, container?.borderColor)
        assertEquals(1f, container?.borderWidthDp)
    }

    @Test
    fun `logo, label, fundingInstrument, and creditMessaging attrs nest under container`() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.payPalLogoWidthDp, "32dp")
            .addAttribute(R.attr.payPalLabelFontSizeSp, "18sp")
            .addAttribute(R.attr.payPalLabelMarginStartDp, "4dp")
            .addAttribute(R.attr.fundingInstrumentTextFontSizeSp, "13sp")
            .addAttribute(R.attr.fundingInstrumentEditIconSizeDp, "20dp")
            .addAttribute(R.attr.fundingInstrumentMarginStartDp, "10dp")
            .addAttribute(R.attr.creditMessagingFontSizeSp, "15sp")
            .addAttribute(R.attr.creditMessagingLinkColor, "#FF00FF")
            .build()

        val container = styleFromAttrs(context, attrs, 0).container

        assertEquals(32f, container?.logo?.widthDp)
        assertEquals(18f, container?.label?.fontSizeSp)
        assertEquals(4f, container?.label?.marginStartDp)
        assertEquals(13f, container?.fundingInstrument?.textFontSizeSp)
        assertEquals(20f, container?.fundingInstrument?.editIconSizeDp)
        assertEquals(10f, container?.fundingInstrument?.marginStartDp)
        assertEquals(15f, container?.creditMessaging?.fontSizeSp)
        assertEquals(Color.MAGENTA, container?.creditMessaging?.linkColor)
    }
}
