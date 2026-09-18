package com.braintreepayments.api.paypalsavedpaymentmethod.component

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethod
import com.braintreepayments.api.paypalsavedpaymentmethod.state.FiClusterState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises `PayPalSavedPaymentMethodView` hosted in a real, live `Activity` (as opposed to the
 * plain-`Context` construction in `PayPalSavedPaymentMethodViewInstrumentedTest`), covering the
 * paths that specifically depend on a real Activity/decorView being present:
 * `findActivity()`/`showFullScreenLoader()`'s real overlay attachment, and FI-section state
 * changes rendering correctly once actually laid out on-screen.
 *
 * Does not exercise `createPaymentAuthRequest()`/`PayPalLauncher.launch()` -- both
 * `PayPalSavedPaymentMethodClient` and `PayPalLauncher` are `final` classes with `internal`
 * constructors, so there is no fake/interface seam to substitute a test double, and this repo has
 * no `mockk-android` (or any on-device mocking) precedent in any module. The full-screen loader
 * show/hide is invoked directly via reflection instead of by tapping the edit pencil, to verify
 * its real Activity-attachment behavior without going through the client.
 */
@OptIn(ExperimentalBetaApi::class)
@RunWith(AndroidJUnit4::class)
class PayPalSavedPaymentMethodViewIntegrationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private fun launchActivity(): ActivityScenario<PayPalSavedPaymentMethodViewTestActivity> {
        val intent = Intent(
            instrumentation.targetContext,
            PayPalSavedPaymentMethodViewTestActivity::class.java
        )
        return ActivityScenario.launch(intent)
    }

    private fun waitForMain() = instrumentation.waitForIdleSync()

    private fun invokePrivate(target: Any, name: String) {
        val method = target::class.java.getDeclaredMethod(name)
        method.isAccessible = true
        method.invoke(target)
    }

    private fun overlay(view: PayPalSavedPaymentMethodView): View? {
        val field = view::class.java.getDeclaredField("fullScreenLoaderOverlay")
        field.isAccessible = true
        return field.get(view) as? View
    }

    private fun paymentMethod(lastDigits: String) = PayPalSavedPaymentMethod(
        label = "label",
        imageUrl = "https://example.com/icon.png",
        lastDigits = lastDigits,
        type = "CARD",
        subtype = null
    )

    @Test
    fun showFullScreenLoader_attachesARealOverlayToTheActivityDecorView() {
        launchActivity().use { scenario ->
            scenario.onActivity { activity ->
                val decorView = activity.window.decorView as ViewGroup
                val childCountBefore = decorView.childCount

                invokePrivate(activity.savedPaymentMethodView, "showFullScreenLoader")

                val loader = overlay(activity.savedPaymentMethodView)
                assertTrue(loader != null)
                assertEquals(childCountBefore + 1, decorView.childCount)
                assertEquals(loader, decorView.getChildAt(decorView.childCount - 1))

                invokePrivate(activity.savedPaymentMethodView, "hideFullScreenLoader")

                assertNull(overlay(activity.savedPaymentMethodView))
                assertEquals(childCountBefore, decorView.childCount)
            }
        }
    }

    @Test
    fun showFullScreenLoader_calledTwiceDoesNotAttachASecondOverlay() {
        launchActivity().use { scenario ->
            scenario.onActivity { activity ->
                val decorView = activity.window.decorView as ViewGroup
                val childCountBefore = decorView.childCount

                invokePrivate(activity.savedPaymentMethodView, "showFullScreenLoader")
                invokePrivate(activity.savedPaymentMethodView, "showFullScreenLoader")

                assertEquals(childCountBefore + 1, decorView.childCount)

                // Torn down here rather than left for the scenario to close with the overlay
                // still attached to a live decorView -- doing so crashes the test process with an
                // NPE in ViewRootImpl.dispatchDetachedFromWindow during Activity destroy.
                invokePrivate(activity.savedPaymentMethodView, "hideFullScreenLoader")
            }
        }
    }

    @Test
    fun fiSection_setState_Available_rendersFundingInstrumentOnARealHostedActivity() {
        launchActivity().use { scenario ->
            scenario.onActivity { activity ->
                val fiSection = activity.savedPaymentMethodView.findViewById<FiSection>(
                    com.braintreepayments.api.paypalsavedpaymentmethod.R.id.paypal_saved_payment_method_fi_section
                )

                fiSection.setState(FiClusterState.Available(paymentMethod("4242")))
            }
            waitForMain()
            scenario.onActivity { activity ->
                val fiSection = activity.savedPaymentMethodView.findViewById<FiSection>(
                    com.braintreepayments.api.paypalsavedpaymentmethod.R.id.paypal_saved_payment_method_fi_section
                )
                val text = fiSection.findViewById<android.widget.TextView>(
                    com.braintreepayments.api.paypalsavedpaymentmethod.R.id.paypal_saved_payment_method_fi_text
                )
                val editIcon = fiSection.findViewById<View>(
                    com.braintreepayments.api.paypalsavedpaymentmethod.R.id.paypal_saved_payment_method_fi_edit_icon
                )

                assertEquals(View.VISIBLE, fiSection.visibility)
                assertEquals("••4242", text.text.toString())
                assertEquals(View.VISIBLE, editIcon.visibility)
            }
        }
    }

    @Test
    fun fiSection_setState_NoNetworkLoad_hidesTheWholeSectionOnARealHostedActivity() {
        launchActivity().use { scenario ->
            scenario.onActivity { activity ->
                val fiSection = activity.savedPaymentMethodView.findViewById<FiSection>(
                    com.braintreepayments.api.paypalsavedpaymentmethod.R.id.paypal_saved_payment_method_fi_section
                )

                fiSection.setState(FiClusterState.Available(paymentMethod("4242")))
            }
            waitForMain()
            scenario.onActivity { activity ->
                val fiSection = activity.savedPaymentMethodView.findViewById<FiSection>(
                    com.braintreepayments.api.paypalsavedpaymentmethod.R.id.paypal_saved_payment_method_fi_section
                )

                fiSection.setState(FiClusterState.NoNetworkLoad)
            }
            waitForMain()
            scenario.onActivity { activity ->
                val fiSection = activity.savedPaymentMethodView.findViewById<FiSection>(
                    com.braintreepayments.api.paypalsavedpaymentmethod.R.id.paypal_saved_payment_method_fi_section
                )

                assertFalse(fiSection.visibility == View.VISIBLE)
            }
        }
    }

    @Test
    fun view_survivesConfigurationChange_withoutCrashingAndRestoresDefaultChrome() {
        launchActivity().use { scenario ->
            scenario.onActivity { activity ->
                val fiSection = activity.savedPaymentMethodView.findViewById<FiSection>(
                    com.braintreepayments.api.paypalsavedpaymentmethod.R.id.paypal_saved_payment_method_fi_section
                )
                fiSection.setState(FiClusterState.Available(paymentMethod("4242")))
            }
            waitForMain()

            scenario.recreate()
            waitForMain()

            scenario.onActivity { activity ->
                val logo = activity.savedPaymentMethodView.findViewById<View>(
                    com.braintreepayments.api.paypalsavedpaymentmethod.R.id.paypal_saved_payment_method_paypal_logo
                )
                assertEquals(View.VISIBLE, logo.visibility)
            }
        }
    }
}
