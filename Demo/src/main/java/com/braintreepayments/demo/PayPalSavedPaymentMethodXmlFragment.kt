package com.braintreepayments.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalPaymentUserAction
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypalsavedpaymentmethod.callback.PayPalSavedPaymentMethodLaunchCallback
import com.braintreepayments.api.paypalsavedpaymentmethod.component.PayPalSavedPaymentMethodView
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.ComponentAppearance
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.ContainerStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalLabelStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalLogoStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodViewStyle
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.core.net.toUri
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs

class PayPalSavedPaymentMethodXmlFragment : BaseFragment() {

    private val args: PayPalSavedPaymentMethodXmlFragmentArgs by navArgs()
    private lateinit var clientTokenInput: EditText
    private lateinit var amountInput: EditText
    private lateinit var flowSpinner: Spinner
    private lateinit var appSwitchToggle: SwitchMaterial
    private lateinit var savedPaymentMethodView: PayPalSavedPaymentMethodView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_paypal_saved_payment_method_xml, container, false)
        clientTokenInput = view.findViewById<EditText>(R.id.paypal_saved_payment_method_xml_client_token)
            .apply { setText(args.clientToken) }
        amountInput = view.findViewById<EditText>(R.id.paypal_saved_payment_method_xml_amount)
            .apply { setText(args.amount) }
        flowSpinner = view.findViewById<Spinner>(R.id.paypal_saved_payment_method_xml_flow)
            .apply { setSelection(if (args.payNow) FLOW_PAY_NOW else FLOW_CONTINUE) }
        appSwitchToggle = view.findViewById<SwitchMaterial>(R.id.paypal_saved_payment_method_xml_app_switch)
            .apply { isChecked = args.enableAppSwitch }
        savedPaymentMethodView = view.findViewById(R.id.paypal_saved_payment_method_xml_view)

        view.findViewById<Button>(R.id.paypal_saved_payment_method_xml_load)
            .setOnClickListener { reload() }
        view.findViewById<Button>(R.id.paypal_saved_payment_method_xml_style)
            .setOnClickListener { showStyleBottomSheet() }

        if (args.clientToken.isBlank()) {
            savedPaymentMethodView.visibility = View.GONE
        } else {
            initializeComponent()
        }
        return view
    }

    private fun showStyleBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheet = layoutInflater.inflate(
            R.layout.bottom_sheet_paypal_saved_payment_method_xml_style,
            null
        )
        val showLogo = sheet.findViewById<SwitchMaterial>(R.id.paypal_saved_payment_method_xml_style_show_logo)
        val showLabel = sheet.findViewById<SwitchMaterial>(R.id.paypal_saved_payment_method_xml_style_show_label)
        val showCredit = sheet.findViewById<SwitchMaterial>(R.id.paypal_saved_payment_method_xml_style_show_credit)
        val cornerRadius = sheet.findViewById<EditText>(R.id.paypal_saved_payment_method_xml_style_corner_radius)
        val borderWidth = sheet.findViewById<EditText>(R.id.paypal_saved_payment_method_xml_style_border_width)
        val height = sheet.findViewById<EditText>(R.id.paypal_saved_payment_method_xml_style_height)
        val horizontalPadding = sheet.findViewById<EditText>(R.id.paypal_saved_payment_method_xml_style_horizontal_padding)
        val verticalPadding = sheet.findViewById<EditText>(R.id.paypal_saved_payment_method_xml_style_vertical_padding)
        val baseFontSize = sheet.findViewById<EditText>(R.id.paypal_saved_payment_method_xml_style_base_font_size)
        val logoWidth = sheet.findViewById<EditText>(R.id.paypal_saved_payment_method_xml_style_logo_width)
        val labelFontSize = sheet.findViewById<EditText>(R.id.paypal_saved_payment_method_xml_style_label_font_size)
        val labelMarginStart = sheet.findViewById<EditText>(R.id.paypal_saved_payment_method_xml_style_label_margin_start)

        sheet.findViewById<Button>(R.id.paypal_saved_payment_method_xml_style_apply)
            .setOnClickListener {
                savedPaymentMethodView.setStyle(
                    PayPalSavedPaymentMethodViewStyle(
                        showPayPalLogo = showLogo.isChecked,
                        showPayPalLabel = showLabel.isChecked,
                        showPayPalCreditMessaging = showCredit.isChecked,
                        componentAppearance = ComponentAppearance(
                            baseFontSizeSp = baseFontSize.floatOrNull()
                        ),
                        container = ContainerStyle(
                            heightDp = height.floatOrNull(),
                            horizontalPaddingDp = horizontalPadding.floatOrNull(),
                            verticalPaddingDp = verticalPadding.floatOrNull(),
                            cornerRadiusDp = cornerRadius.floatOrNull(),
                            borderWidthDp = borderWidth.floatOrNull(),
                            logo = PayPalLogoStyle(widthDp = logoWidth.floatOrNull()),
                            label = PayPalLabelStyle(
                                fontSizeSp = labelFontSize.floatOrNull(),
                                marginStartDp = labelMarginStart.floatOrNull()
                            )
                        )
                    )
                )
                dialog.dismiss()
            }
        dialog.setContentView(sheet)
        dialog.show()
    }

    @OptIn(ExperimentalBetaApi::class)
    private fun initializeComponent() {
        savedPaymentMethodView.initialize(
            activityResultCaller = this,
            authorization = args.clientToken,
            appLinkReturnUrl = APP_LINK_RETURN_URL.toUri(),
            payPalRequest = buildPayPalRequest(),
            callback = launchCallback,
            deepLinkFallbackUrlScheme = DEEP_LINK_FALLBACK_URL_SCHEME
        )
    }

    override fun onResume() {
        super.onResume()
        val pendingRequest = PendingRequestStore.getInstance()
            .getPayPalPendingRequest(requireContext()) ?: return
        savedPaymentMethodView.handleReturnToApp(pendingRequest, requireActivity().intent)
        PendingRequestStore.getInstance().clearPayPalPendingRequest(requireContext())
        requireActivity().intent.data = null
    }

    @OptIn(ExperimentalBetaApi::class)
    private fun buildPayPalRequest(): PayPalCheckoutRequest =
        PayPalRequestFactory.createPayPalCheckoutRequest(
            requireContext(), args.amount, null, null, null, false, null, false, false, false
        ).apply {
            currencyCode = CURRENCY_CODE
            enablePayPalAppSwitch = args.enableAppSwitch
            userAction = if (args.payNow) {
                PayPalPaymentUserAction.USER_ACTION_COMMIT
            } else {
                PayPalPaymentUserAction.USER_ACTION_DEFAULT
            }
        }

    private fun reload() {
        val token = clientTokenInput.text.toString().trim()
        if (token.isBlank()) {
            showDialog(getString(R.string.paypal_saved_payment_method_missing_token))
            return
        }
        val action = PayPalSavedPaymentMethodXmlFragmentDirections
            .actionPayPalSavedPaymentMethodXmlFragmentSelf()
            .setAuthString(authStringArg)
            .setClientToken(token)
            .setAmount(amountInput.text.toString().trim())
            .setPayNow(flowSpinner.selectedItemPosition == FLOW_PAY_NOW)
            .setEnableAppSwitch(appSwitchToggle.isChecked)
        findNavController().navigate(action)
    }

    private val launchCallback = object : PayPalSavedPaymentMethodLaunchCallback {
        override fun onSavedPaymentMethodLaunch(payPalPendingRequest: PayPalPendingRequest) {
            when (payPalPendingRequest) {
                is PayPalPendingRequest.Started -> PendingRequestStore.getInstance()
                    .putPayPalPendingRequest(requireContext(), payPalPendingRequest)
                is PayPalPendingRequest.Failure -> handleError(payPalPendingRequest.error)
            }
        }

        override fun onSavedPaymentMethodResult(result: PayPalResult) {
            when (result) {
                is PayPalResult.Success -> onPaymentMethodNonceCreated(result.nonce)
                is PayPalResult.Cancel -> handleError(Exception("User did not complete payment flow"))
                is PayPalResult.Failure -> handleError(result.error)
            }
        }
    }
}

private fun EditText.floatOrNull(): Float? = text.toString().trim().toFloatOrNull()

private const val FLOW_CONTINUE = 0
private const val FLOW_PAY_NOW = 1
private const val CURRENCY_CODE = "USD"
private const val APP_LINK_RETURN_URL =
    "https://mobile-sdk-demo-site-838cead5d3ab.herokuapp.com/braintree-payments"
private const val DEEP_LINK_FALLBACK_URL_SCHEME = "com.braintreepayments.demo.braintree"