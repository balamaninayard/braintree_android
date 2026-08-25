package com.braintreepayments.api.paypalsavedpaymentmethod

import android.content.Context
import android.net.Uri
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.ClientToken
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.core.GraphQLConstants
import com.braintreepayments.api.core.MerchantRepository
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.paypal.PayPalPaymentAuthCallback
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalTokenizeCallback
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Entry point for the saved/editable PayPal payment method feature — implements the fetch/refetch-FI
 * GraphQL calls directly against [BraintreeClient], and starts the edit-FI PayPal payment auth flow
 * via [PayPalClient].
 */
internal class PayPalSavedPaymentMethodClient(
    private val braintreeClient: BraintreeClient,
    private val payPalClient: PayPalClient,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    private val merchantRepository: MerchantRepository = MerchantRepository.instance
) {

    /**
     * Initializes a new [PayPalSavedPaymentMethodClient] instance
     *
     * @param context          an Android Context
     * @param authorization    a Tokenization Key or Client Token used to authenticate
     * @param appLinkReturnUrl A [Uri] containing the Android App Link website associated with
     * your application to be used to return to your app from the PayPal payment flows.
     * @param deepLinkFallbackUrlScheme A return url scheme that will be used as a deep link fallback when returning to
     * your app via App Link is not available (buyer unchecks the "Open supported links" setting).
     */
    constructor(
        context: Context,
        authorization: String,
        appLinkReturnUrl: Uri,
        deepLinkFallbackUrlScheme: String? = null
    ) : this(
        braintreeClient = BraintreeClient(
            context = context,
            authorization = authorization,
            deepLinkFallbackUrlScheme = deepLinkFallbackUrlScheme,
            appLinkReturnUri = appLinkReturnUrl
        ),
        payPalClient = PayPalClient(
            context = context,
            authorization = authorization,
            appLinkReturnUrl = appLinkReturnUrl,
            deepLinkFallbackUrlScheme = deepLinkFallbackUrlScheme
        )
    )

    /**
     * Starts the PayPal payment auth flow for the edit-FI checkout using the provided
     * [payPalRequest]. Routes through [PayPalClient.createPaymentAuthRequestForEditFi] so
     * `edit_billing_agreement_jwt` is included, distinguishing this from a normal PayPal checkout.
     *
     * @param context       Android Context
     * @param payPalRequest a [PayPalCheckoutRequest] used to customize the request.
     * @param callback      [PayPalPaymentAuthCallback]
     */
    @ExperimentalBetaApi
    fun createPaymentAuthRequest(
        context: Context,
        payPalRequest: PayPalCheckoutRequest,
        callback: PayPalPaymentAuthCallback
    ) = payPalClient.createPaymentAuthRequestForEditFi(context, payPalRequest, callback)

    /**
     * Tokenizes the result of the edit-FI PayPal payment auth flow, after the app returns from
     * the browser switch.
     *
     * @param paymentAuthResult a successful [PayPalPaymentAuthResult.Success] from
     * `PayPalLauncher.handleReturnToApp`.
     * @param callback          [PayPalTokenizeCallback]
     */
    @ExperimentalBetaApi
    fun tokenize(
        paymentAuthResult: PayPalPaymentAuthResult.Success,
        callback: PayPalTokenizeCallback
    ) = payPalClient.tokenize(paymentAuthResult, callback)

    /**
     * `suspend` variant of [tokenize] - call from a coroutine to receive the result directly as
     * the return value.
     *
     * @param paymentAuthResult a successful [PayPalPaymentAuthResult.Success] from
     * `PayPalLauncher.handleReturnToApp`.
     * @return [PayPalResult]
     */
    @ExperimentalBetaApi
    suspend fun tokenize(paymentAuthResult: PayPalPaymentAuthResult.Success): PayPalResult =
        payPalClient.tokenize(paymentAuthResult)

    /**
     * Fetches the sticky (default) vaulted funding instrument for display.
     *
     * Callback-based variant: the result is delivered asynchronously to [callback]. Use the
     * `suspend` [fetchFI] overload when calling from a coroutine.
     *
     * The `paymentMethodIdJwt` identifying the vaulted funding instrument is read from the
     * client token the SDK was initialized with. If it is missing or blank the [callback]
     * receives a [PayPalSavedPaymentMethodSummaryResult.Failure] with a
     * [PayPalSavedPaymentMethodSummaryException].
     *
     * @param merchantAccountId the merchant account to fetch the funding instrument for; when
     * null, Atmosphere defaults to the merchant's default account.
     * @param callback [PayPalSavedPaymentMethodSummaryCallback] invoked with the result
     */
    @ExperimentalBetaApi
    fun fetchFI(
        merchantAccountId: String? = null,
        callback: PayPalSavedPaymentMethodSummaryCallback
    ) {
        coroutineScope.launch {
            callback.onPayPalSavedPaymentMethodSummaryResult(fetchFI(merchantAccountId))
        }
    }

    /**
     * Fetches the sticky (default) vaulted funding instrument for display.
     *
     * `suspend` variant: call from a coroutine to receive the result directly as the return value.
     * Use the [fetchFI] overload that takes a [PayPalSavedPaymentMethodSummaryCallback] outside a
     * coroutine.
     *
     * The `paymentMethodIdJwt` identifying the vaulted funding instrument is read from the
     * client token the SDK was initialized with. If it is missing or blank a
     * [PayPalSavedPaymentMethodSummaryResult.Failure] with a
     * [PayPalSavedPaymentMethodSummaryException] is returned.
     *
     * @param merchantAccountId the merchant account to fetch the funding instrument for; when
     * null, Atmosphere defaults to the merchant's default account.
     * @return [PayPalSavedPaymentMethodSummaryResult]
     */
    @ExperimentalBetaApi
    suspend fun fetchFI(
        merchantAccountId: String? = null
    ): PayPalSavedPaymentMethodSummaryResult {
        val paymentMethodIdJwt = (merchantRepository.authorization as? ClientToken)?.paymentMethodIdJwt
        if (paymentMethodIdJwt.isNullOrBlank()) {
            return PayPalSavedPaymentMethodSummaryResult.Failure(
                PayPalSavedPaymentMethodSummaryException(
                    errorClass = null,
                    message = PayPalSavedPaymentMethodSummaryException.MISSING_PAYMENT_METHOD_ID_JWT,
                )
            )
        }
        return getPaymentMethod(
            GetPayPalSavedPaymentMethodGraphQLBody.stickyFi(paymentMethodIdJwt, merchantAccountId)
        )
    }

    /**
     * Refreshes the vaulted funding instrument after an edit, keyed by the approved-checkout order
     * id.
     *
     * Callback-based variant: the result is delivered asynchronously to [callback]. Use the
     * `suspend` [refetchFI] overload when calling from a coroutine.
     *
     * @param orderId  the approved-checkout order id
     * @param merchantAccountId the merchant account to fetch the funding instrument for; when
     * null, Atmosphere defaults to the merchant's default account.
     * @param callback [PayPalSavedPaymentMethodSummaryCallback] invoked with the result
     */
    @ExperimentalBetaApi
    fun refetchFI(
        orderId: String,
        merchantAccountId: String? = null,
        callback: PayPalSavedPaymentMethodSummaryCallback
    ) {
        coroutineScope.launch {
            callback.onPayPalSavedPaymentMethodSummaryResult(refetchFI(orderId, merchantAccountId))
        }
    }

    /**
     * Refreshes the vaulted funding instrument after an edit, keyed by the approved-checkout order
     * id.
     *
     * `suspend` variant: call from a coroutine to receive the result directly as the return value.
     * Use the [refetchFI] overload that takes a [PayPalSavedPaymentMethodSummaryCallback] outside a
     * coroutine.
     *
     * @param orderId the approved-checkout order id
     * @param merchantAccountId the merchant account to fetch the funding instrument for; when
     * null, Atmosphere defaults to the merchant's default account.
     * @return [PayPalSavedPaymentMethodSummaryResult]
     */
    @ExperimentalBetaApi
    suspend fun refetchFI(
        orderId: String,
        merchantAccountId: String? = null
    ): PayPalSavedPaymentMethodSummaryResult =
        getPaymentMethod(
            GetPayPalSavedPaymentMethodGraphQLBody.fromApprovedCheckout(orderId, merchantAccountId)
        )

    @OptIn(ExperimentalBetaApi::class)
    @Suppress("TooGenericExceptionCaught")
    private suspend fun getPaymentMethod(body: JSONObject): PayPalSavedPaymentMethodSummaryResult =
        try {
            val response = JSONObject(braintreeClient.sendGraphQLPOST(body))
            val errors = response.optJSONArray(GraphQLConstants.Keys.ERRORS)
            if (errors != null && errors.length() > 0) {
                throw PayPalSavedPaymentMethodSummaryException.fromGraphQLResponse(response)
            }
            PayPalSavedPaymentMethodSummaryResult.Success(PayPalSavedPaymentMethodSummary.fromJson(response))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            PayPalSavedPaymentMethodSummaryResult.Failure(e)
        }

    /**
     * Fetches PayPal Pay Later / Credit presentment messaging for the edit-FI row.
     *
     * @param amount   the order amount, e.g. "55.00"
     * @param currency ISO currency code, e.g. "USD"; when null, falls back to the merchant's
     * configured PayPal currency. If neither is available, returns null.
     * @return [PayPalCreditMessagingContent], or null if the fetch fails or returns no
     * `preferred_message` - callers should hide the messaging row; the FI card still renders.
     */
    @ExperimentalBetaApi
    @Suppress("TooGenericExceptionCaught")
    suspend fun fetchCreditPresentmentMessages(
        amount: String,
        currency: String? = null
    ): PayPalCreditMessagingContent? = try {
        val configuration = braintreeClient.getConfiguration()
        val currencyCode = currency ?: configuration.payPalCurrencyIsoCode
        if (currencyCode == null) {
            null
        } else {
            val request = PayPalCreditMessagingRequest.forAmount(currencyCode = currencyCode, value = amount)
            val responseBody = braintreeClient.sendPOST(
                url = PayPalCreditMessagingUrlAssembler.assembleURL(configuration.environment),
                data = request.build().toString()
            )
            PayPalCreditMessagingContent.fromJson(JSONObject(responseBody))
        }
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        null
    }
}
