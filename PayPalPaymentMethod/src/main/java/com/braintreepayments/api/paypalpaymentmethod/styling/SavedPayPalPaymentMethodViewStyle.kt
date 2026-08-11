package com.braintreepayments.api.paypalpaymentmethod.styling

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.FontRes

/**
 * Style contract for
 * [com.braintreepayments.api.paypalpaymentmethod.compose.SavedPayPalPaymentMethodView] — v1, happy
 * path.
 *
 * Plain Kotlin data classes only — no UI-toolkit types. Single source of truth across
 * implementations; unit conversion happens only inside each renderer's own drawing code, never
 * here.
 *
 * Scope: single-row layout, light theme only. Dark theme, responsive stacked layout, and logo
 * box-scaling are deliberately deferred.
 */
data class SavedPayPalPaymentMethodViewStyle(
    val showLogo: Boolean = true,
    val showLabel: Boolean = true,
    val showCreditMessaging: Boolean = true,
    val theme: ThemeStyle = ThemeStyle(),
    val container: ContainerStyle = ContainerStyle()
)

/**
 * Colors, fonts, and brand identity.
 *
 * @param linkColor when null, credit-messaging links fall back to bold+underline in
 * [textColorBase].
 */
data class ThemeStyle(
    @ColorInt val backgroundColor: Int = Color.WHITE,
    @ColorInt val textColorBase: Int = Color.parseColor("#222222"),
    val baseFontSizeSp: Float = 14f,
    @FontRes val fontResId: Int? = null,
    @ColorInt val linkColor: Int? = null
)

/**
 * The outer box (own shape) plus its four children's styles.
 *
 * @param heightDp when null, the container wraps content; never clamped.
 */
@Suppress("MagicNumber")
data class ContainerStyle(
    val heightDp: Float? = null,
    val horizontalPaddingDp: Float = 0f,
    val verticalPaddingDp: Float = 10f,
    val cornerRadiusDp: Float = 0f,
    @ColorInt val borderColor: Int = Color.TRANSPARENT,
    val borderWidthDp: Float = 0f,

    val logo: LogoStyle = LogoStyle(),
    val label: LabelStyle = LabelStyle(),
    val fiCluster: FiClusterStyle = FiClusterStyle(),
    val creditMessaging: CreditMessagingStyle = CreditMessagingStyle()
)

/**
 * @param widthDp Figma: "Payment Card Thumbnail", 48x30 (height follows source aspect ratio). Logo
 * is the first child, so its start offset comes from [ContainerStyle.horizontalPaddingDp], not its
 * own margin.
 */
@Suppress("MagicNumber")
data class LogoStyle(
    val widthDp: Float = 48f
)

/**
 * @param fontSizeSp Figma-sourced default (intentionally differs from web's 14px).
 * @param marginStartDp gap from Logo (Figma: "Pay with" / Marks Message V2, 12.727px).
 */
@Suppress("MagicNumber")
data class LabelStyle(
    val fontSizeSp: Float = 20f,
    val marginStartDp: Float = 13f
)

/**
 * @param iconWidthDp Figma: "Funding Icon", 27.87x20.72 (height follows source aspect ratio).
 * @param marginStartDp gap from Label; collapses toward Logo if Label is hidden.
 * @param backgroundColor Figma: Edit FI Chip background.
 * @param cornerRadiusDp Figma: Edit FI Chip corner radius.
 * @param horizontalPaddingDp Figma: Edit FI Chip / FI w edit, left+right padding.
 * @param verticalPaddingDp Figma: Edit FI Chip / FI w edit, top+bottom padding.
 */
@Suppress("MagicNumber")
data class FiClusterStyle(
    val textFontSizeSp: Float = 14f,
    val editIconSizeDp: Float = 16f,
    val iconWidthDp: Float = 28f,
    val marginStartDp: Float = 8f,
    @ColorInt val backgroundColor: Int = Color.parseColor("#F0F2F9"),
    val cornerRadiusDp: Float = 6f,
    val horizontalPaddingDp: Float = 8f,
    val verticalPaddingDp: Float = 4f
)

/**
 * `messageText` / `learnMoreText` / `learnMoreUrl` are deliberately absent — that's
 * compliance content from the API (see `CreditMessagingState`), never merchant-authored via
 * style.
 */
@Suppress("MagicNumber")
data class CreditMessagingStyle(
    val fontSizeSp: Float = 16f
)