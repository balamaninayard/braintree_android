package com.braintreepayments.api.paypalsavedpaymentmethod.component

import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.UiThreadTestRule
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.ContainerStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalLogoStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodViewStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs on a real device/emulator (as opposed to the Robolectric-backed
 * `PayPalSavedPaymentMethodViewUnitTest`) to catch anything Robolectric's shadow layer can mask --
 * e.g. real resource linking and a real measure/layout pass with the device's actual density.
 *
 * `@UiThreadTest` is required: constructing the view starts `FiSection`'s loading shimmer
 * (`ObjectAnimator`), which throws `AndroidRuntimeException: Animators may only be run on Looper
 * threads` if built off the main thread -- the default thread for instrumented test methods.
 */
@RunWith(AndroidJUnit4::class)
class PayPalSavedPaymentMethodViewInstrumentedTest {

    @get:Rule
    val uiThreadTestRule = UiThreadTestRule()

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun buildAndLayout(view: PayPalSavedPaymentMethodView) {
        val widthSpec = android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.AT_MOST)
        val heightSpec = android.view.View.MeasureSpec.makeMeasureSpec(1920, android.view.View.MeasureSpec.AT_MOST)
        view.measure(widthSpec, heightSpec)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
    }

    @Test
    @UiThreadTest
    fun defaultConstruction_inflatesWithoutCrashingAndShowsLogoAndLabel() {
        val view = PayPalSavedPaymentMethodView(context)
        buildAndLayout(view)

        val logo = view.findViewById<android.view.View>(
            com.braintreepayments.api.paypalsavedpaymentmethod.R.id.paypal_saved_payment_method_paypal_logo
        )
        val label = view.findViewById<android.view.View>(
            com.braintreepayments.api.paypalsavedpaymentmethod.R.id.paypal_saved_payment_method_paypal_label
        )

        assertEquals(android.view.View.VISIBLE, logo.visibility)
        assertEquals(android.view.View.VISIBLE, label.visibility)
    }

    @Test
    @UiThreadTest
    fun setStyle_appliesRealPixelLogoWidthMatchingDeviceDensity() {
        val view = PayPalSavedPaymentMethodView(context)
        view.setStyle(
            PayPalSavedPaymentMethodViewStyle(container = ContainerStyle(logo = PayPalLogoStyle(widthDp = 32f)))
        )
        buildAndLayout(view)

        val logo = view.findViewById<android.view.View>(
            com.braintreepayments.api.paypalsavedpaymentmethod.R.id.paypal_saved_payment_method_paypal_logo
        )
        val density = context.resources.displayMetrics.density
        val expectedPx = (32f * density).toInt()

        assertEquals(expectedPx, logo.layoutParams.width)
        assertEquals(expectedPx, logo.measuredWidth)
    }

    @Test
    @UiThreadTest
    fun setStyle_calledTwiceFullyReplacesThePreviousStyleRatherThanMerging() {
        val view = PayPalSavedPaymentMethodView(context)
        view.setStyle(PayPalSavedPaymentMethodViewStyle(showPayPalLogo = false, showPayPalLabel = false))
        view.setStyle(PayPalSavedPaymentMethodViewStyle())
        buildAndLayout(view)

        val logo = view.findViewById<android.view.View>(
            com.braintreepayments.api.paypalsavedpaymentmethod.R.id.paypal_saved_payment_method_paypal_logo
        )
        val label = view.findViewById<android.view.View>(
            com.braintreepayments.api.paypalsavedpaymentmethod.R.id.paypal_saved_payment_method_paypal_label
        )

        assertEquals(android.view.View.VISIBLE, logo.visibility)
        assertEquals(android.view.View.VISIBLE, label.visibility)
    }

    @Test
    @UiThreadTest
    fun setStyle_withCustomBackgroundColor_appliesToARealGradientDrawable() {
        val view = PayPalSavedPaymentMethodView(context)
        view.setStyle(
            PayPalSavedPaymentMethodViewStyle(
                componentAppearance = com.braintreepayments.api.paypalsavedpaymentmethod.styling.ComponentAppearance(
                    backgroundColor = android.graphics.Color.BLUE
                )
            )
        )
        buildAndLayout(view)

        val background = view.background as android.graphics.drawable.GradientDrawable
        assertNotEquals(0, background.color?.defaultColor)
        assertEquals(android.graphics.Color.BLUE, background.color?.defaultColor)
    }
}
