package com.braintreepayments.api.paypalsavedpaymentmethod.component

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypal.PayPalAccountNonce
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPaymentAuthCallback
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalTokenizeCallback
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodClient
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodSummary
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodSummaryResult
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethod
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import com.braintreepayments.api.paypalsavedpaymentmethod.callback.PayPalSavedPaymentMethodLaunchCallback
import com.braintreepayments.api.paypalsavedpaymentmethod.state.FiClusterState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.Runs
import io.mockk.coVerify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalBetaApi::class)
@RunWith(RobolectricTestRunner::class)
class PayPalSavedPaymentMethodViewBehaviorUnitTest {

    private val activity: Activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
    private val context: Context = activity
    private val view = PayPalSavedPaymentMethodView(context).also { activity.setContentView(it) }

    private val client = mockk<PayPalSavedPaymentMethodClient>()
    private val launcher = mockk<PayPalLauncher>()
    private val fakeCallback = FakeLaunchCallback()

    private val fiSection get() = view.findViewById<FiSection>(R.id.paypal_saved_payment_method_fi_section)
    private val creditMessagingView
        get() = view.findViewById<CreditMessagingView>(R.id.paypal_saved_payment_method_credit_messaging)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        setPrivateField(view, "payPalSavedPaymentMethodClient", client)
        setPrivateField(view, "payPalLauncher", launcher)
        setPrivateField(view, "callback", fakeCallback)
        setPrivateField(
            view,
            "payPalRequest",
            PayPalCheckoutRequest(amount = "10.00", hasUserLocationConsent = true)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -- handleReturnToApp branch dispatch --

    @Test
    fun `handleReturnToApp NoResult reports Cancel and restores the last FI state`() {
        setPrivateField(view, "lastFiClusterState", FiClusterState.NoNetworkLoad)
        every { launcher.handleReturnToApp(any(), any()) } returns PayPalPaymentAuthResult.NoResult

        view.handleReturnToApp(PayPalPendingRequest.Started("pending-string"), Intent())

        assertEquals(PayPalResult.Cancel, fakeCallback.lastResult)
        assertTrue(fiSection.visibility != android.view.View.VISIBLE)
    }

    @Test
    fun `handleReturnToApp Failure reports the same error`() {
        val error = Exception("browser switch failed")
        val failureResult = mockk<PayPalPaymentAuthResult.Failure>()
        every { failureResult.error } returns error
        every { launcher.handleReturnToApp(any(), any()) } returns failureResult

        view.handleReturnToApp(PayPalPendingRequest.Started("pending-string"), Intent())

        val result = fakeCallback.lastResult as? PayPalResult.Failure
        assertEquals(error, result?.error)
    }

    @Test
    fun `handleReturnToApp Success delegates to the client's tokenize call`() {
        val successResult = mockk<PayPalPaymentAuthResult.Success>()
        every { launcher.handleReturnToApp(any(), any()) } returns successResult
        every { client.tokenize(any(), any()) } just Runs

        view.handleReturnToApp(PayPalPendingRequest.Started("pending-string"), Intent())

        verify { client.tokenize(successResult, any()) }
    }

    @Test
    fun `Success tokenize result refetches FI keyed by the order id and reports success`() {
        val successAuthResult = mockk<PayPalPaymentAuthResult.Success>()
        every { launcher.handleReturnToApp(any(), any()) } returns successAuthResult

        val nonce = mockk<PayPalAccountNonce>()
        every { nonce.paymentId } returns "order-123"
        val tokenizeCallbackSlot = io.mockk.slot<PayPalTokenizeCallback>()
        every { client.tokenize(successAuthResult, capture(tokenizeCallbackSlot)) } answers {
            tokenizeCallbackSlot.captured.onPayPalResult(PayPalResult.Success(nonce))
        }

        val summary = PayPalSavedPaymentMethodSummary(
            paypalPayer = null,
            paypalSavedPaymentMethods = listOf(paymentMethod("4242"))
        )
        coEvery { client.refetchFI(orderId = "order-123") } returns
            PayPalSavedPaymentMethodSummaryResult.Success(summary)

        view.handleReturnToApp(PayPalPendingRequest.Started("pending-string"), Intent())

        assertTrue(fakeCallback.lastResult is PayPalResult.Success)
        assertEquals("••4242", fiSection.findViewById<android.widget.TextView>(
            com.braintreepayments.api.paypalsavedpaymentmethod.R.id.paypal_saved_payment_method_fi_text
        ).text.toString())
    }

    @Test
    fun `tokenize Cancel after browser Success reports Cancel, restores last FI, and does not refetch`() {
        setPrivateField(view, "lastFiClusterState", FiClusterState.NoNetworkLoad)

        val successAuthResult = mockk<PayPalPaymentAuthResult.Success>()
        every { launcher.handleReturnToApp(any(), any()) } returns successAuthResult
        val tokenizeCallbackSlot = io.mockk.slot<PayPalTokenizeCallback>()
        every { client.tokenize(successAuthResult, capture(tokenizeCallbackSlot)) } answers {
            tokenizeCallbackSlot.captured.onPayPalResult(PayPalResult.Cancel)
        }

        view.handleReturnToApp(PayPalPendingRequest.Started("pending-string"), Intent())

        assertEquals(PayPalResult.Cancel, fakeCallback.lastResult)
        assertTrue(fiSection.visibility != android.view.View.VISIBLE)
        coVerify(exactly = 0) { client.refetchFI(any(), any()) }
    }

    @Test
    fun `tokenize Failure after browser Success reports the error, restores last FI, and does not refetch`() {
        setPrivateField(view, "lastFiClusterState", FiClusterState.NoNetworkLoad)

        val error = Exception("tokenize failed")
        val successAuthResult = mockk<PayPalPaymentAuthResult.Success>()
        every { launcher.handleReturnToApp(any(), any()) } returns successAuthResult
        val tokenizeCallbackSlot = io.mockk.slot<PayPalTokenizeCallback>()
        every { client.tokenize(successAuthResult, capture(tokenizeCallbackSlot)) } answers {
            tokenizeCallbackSlot.captured.onPayPalResult(PayPalResult.Failure(error))
        }

        view.handleReturnToApp(PayPalPendingRequest.Started("pending-string"), Intent())

        val result = fakeCallback.lastResult as? PayPalResult.Failure
        assertEquals(error, result?.error)
        assertTrue(fiSection.visibility != android.view.View.VISIBLE)
        coVerify(exactly = 0) { client.refetchFI(any(), any()) }
    }

    // -- fetchFI / fetchCreditPresentmentMessages wiring --

    @Test
    fun `fetchFI success renders the fetched funding instrument`() {
        coEvery { client.fetchFI(any()) } returns PayPalSavedPaymentMethodSummaryResult.Success(
            PayPalSavedPaymentMethodSummary(
                paypalPayer = null,
                paypalSavedPaymentMethods = listOf(paymentMethod("9999"))
            )
        )
        coEvery { client.fetchCreditPresentmentMessages(any(), any()) } returns null

        view.initializeForTest()

        assertEquals(
            "••9999",
            fiSection.findViewById<android.widget.TextView>(
                com.braintreepayments.api.paypalsavedpaymentmethod.R.id.paypal_saved_payment_method_fi_text
            ).text.toString()
        )
    }

    @Test
    fun `fetchFI failure hides the FI section`() {
        coEvery { client.fetchFI(any()) } returns PayPalSavedPaymentMethodSummaryResult.Failure(Exception("network"))
        coEvery { client.fetchCreditPresentmentMessages(any(), any()) } returns null

        view.initializeForTest()

        assertFalse(fiSection.visibility == android.view.View.VISIBLE)
    }

    @Test
    fun `fetchCreditMessage is skipped and hidden when showPayPalCreditMessaging is false`() {
        setPrivateField(
            view,
            "style",
            com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodViewStyle(
                showPayPalCreditMessaging = false
            )
        )
        coEvery { client.fetchFI(any()) } returns PayPalSavedPaymentMethodSummaryResult.Failure(Exception("n/a"))

        view.initializeForTest()

        io.mockk.coVerify(exactly = 0) { client.fetchCreditPresentmentMessages(any(), any()) }
    }

    // -- full-screen loader --

    @Test
    fun `edit pencil tap shows a full-screen loader, hidden again on auth request failure`() {
        val error = Exception("could not create auth request")
        val failureRequest = mockk<PayPalPaymentAuthRequest.Failure>()
        every { failureRequest.error } returns error
        val callbackSlot = io.mockk.slot<PayPalPaymentAuthCallback>()
        every { client.createPaymentAuthRequest(any(), any(), capture(callbackSlot)) } answers {
            callbackSlot.captured.onPayPalPaymentAuthRequest(failureRequest)
        }

        fiSection.findViewById<android.view.View>(
            com.braintreepayments.api.paypalsavedpaymentmethod.R.id.paypal_saved_payment_method_fi_edit_icon
        ).performClick()

        assertTrue((fakeCallback.lastLaunch as? PayPalPendingRequest.Failure)?.error === error)
        assertNull(getPrivateField(view, "fullScreenLoaderOverlay"))
    }

    @Test
    fun `edit pencil tap launches the PayPal flow and reports the pending request`() {
        val readyToLaunch = mockk<PayPalPaymentAuthRequest.ReadyToLaunch>()
        val callbackSlot = io.mockk.slot<PayPalPaymentAuthCallback>()
        every { client.createPaymentAuthRequest(any(), any(), capture(callbackSlot)) } answers {
            callbackSlot.captured.onPayPalPaymentAuthRequest(readyToLaunch)
        }
        every { launcher.launch(any(), readyToLaunch) } returns PayPalPendingRequest.Started("pending-string")

        fiSection.findViewById<android.view.View>(
            com.braintreepayments.api.paypalsavedpaymentmethod.R.id.paypal_saved_payment_method_fi_edit_icon
        ).performClick()

        assertEquals("pending-string", (fakeCallback.lastLaunch as? PayPalPendingRequest.Started)?.pendingRequestString)
        assertNotNull(getPrivateField(view, "fullScreenLoaderOverlay"))
    }

    private fun PayPalSavedPaymentMethodView.initializeForTest() {
        val method = this::class.java.getDeclaredMethod("startFetches")
        method.isAccessible = true
        method.invoke(this)
    }

    private fun setPrivateField(target: Any, name: String, value: Any?) {
        val field = target::class.java.getDeclaredField(name)
        field.isAccessible = true
        field.set(target, value)
    }

    private fun getPrivateField(target: Any, name: String): Any? {
        val field = target::class.java.getDeclaredField(name)
        field.isAccessible = true
        return field.get(target)
    }

    private fun paymentMethod(lastDigits: String) = PayPalSavedPaymentMethod(
        label = "label",
        imageUrl = "https://example.com/icon.png",
        lastDigits = lastDigits,
        type = "CARD",
        subtype = null
    )

    private class FakeLaunchCallback : PayPalSavedPaymentMethodLaunchCallback {
        var lastLaunch: PayPalPendingRequest? = null
        var lastResult: PayPalResult? = null

        override fun onSavedPaymentMethodLaunch(payPalPendingRequest: PayPalPendingRequest) {
            lastLaunch = payPalPendingRequest
        }

        override fun onSavedPaymentMethodResult(result: PayPalResult) {
            lastResult = result
        }
    }
}
