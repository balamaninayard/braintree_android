package com.braintreepayments.demo

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
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
import com.google.android.material.bottomsheet.BottomSheetBehavior
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
        val fiTextSize = sheet.findViewById<EditText>(R.id.paypal_saved_payment_method_xml_style_fi_text_size)
        val editIconSize = sheet.findViewById<EditText>(R.id.paypal_saved_payment_method_xml_style_edit_icon_size)
        val fiMarginStart = sheet.findViewById<EditText>(R.id.paypal_saved_payment_method_xml_style_fi_margin_start)
        val creditFontSize = sheet.findViewById<EditText>(R.id.paypal_saved_payment_method_xml_style_credit_font_size)

        val borderColor = colorRow(sheet, R.id.paypal_saved_payment_method_xml_style_border_color_preview, R.id.paypal_saved_payment_method_xml_style_border_color_pick, R.id.paypal_saved_payment_method_xml_style_border_color_default)
        val backgroundColor = colorRow(sheet, R.id.paypal_saved_payment_method_xml_style_background_color_preview, R.id.paypal_saved_payment_method_xml_style_background_color_pick, R.id.paypal_saved_payment_method_xml_style_background_color_default)
        val textColor = colorRow(sheet, R.id.paypal_saved_payment_method_xml_style_text_color_preview, R.id.paypal_saved_payment_method_xml_style_text_color_pick, R.id.paypal_saved_payment_method_xml_style_text_color_default)
        val linkColor = colorRow(sheet, R.id.paypal_saved_payment_method_xml_style_link_color_preview, R.id.paypal_saved_payment_method_xml_style_link_color_pick, R.id.paypal_saved_payment_method_xml_style_link_color_default)

        sheet.findViewById<View>(R.id.paypal_saved_payment_method_xml_style_close)
            .setOnClickListener { dialog.dismiss() }

        sheet.findViewById<Button>(R.id.paypal_saved_payment_method_xml_style_apply)
            .setOnClickListener {
                savedPaymentMethodView.setStyle(
                    PayPalSavedPaymentMethodViewStyle(
                        showPayPalLogo = showLogo.isChecked,
                        showPayPalLabel = showLabel.isChecked,
                        showPayPalCreditMessaging = showCredit.isChecked,
                        componentAppearance = ComponentAppearance(
                            backgroundColor = backgroundColor.color,
                            textColor = textColor.color,
                            baseFontSizeSp = baseFontSize.floatOrNull()
                        ),
                        container = ContainerStyle(
                            heightDp = height.floatOrNull(),
                            horizontalPaddingDp = horizontalPadding.floatOrNull(),
                            verticalPaddingDp = verticalPadding.floatOrNull(),
                            cornerRadiusDp = cornerRadius.floatOrNull(),
                            borderColor = borderColor.color,
                            borderWidthDp = borderWidth.floatOrNull(),
                            logo = PayPalLogoStyle(widthDp = logoWidth.floatOrNull()),
                            label = PayPalLabelStyle(
                                fontSizeSp = labelFontSize.floatOrNull(),
                                marginStartDp = labelMarginStart.floatOrNull()
                            ),
                            fundingInstrument = com.braintreepayments.api.paypalsavedpaymentmethod.styling.FundingInstrumentStyle(
                                textFontSizeSp = fiTextSize.floatOrNull(),
                                editIconSizeDp = editIconSize.floatOrNull(),
                                marginStartDp = fiMarginStart.floatOrNull()
                            ),
                            creditMessaging = com.braintreepayments.api.paypalsavedpaymentmethod.styling.CreditMessagingStyle(
                                fontSizeSp = creditFontSize.floatOrNull(),
                                linkColor = linkColor.color
                            )
                        )
                    )
                )
                dialog.dismiss()
            }
        dialog.setContentView(sheet)
        dialog.setOnShowListener {
            val bottomSheet = sheet.parent as? View ?: return@setOnShowListener
            bottomSheet.layoutParams = bottomSheet.layoutParams.apply {
                this.height = ViewGroup.LayoutParams.MATCH_PARENT
            }
            BottomSheetBehavior.from(bottomSheet).apply {
                state = BottomSheetBehavior.STATE_EXPANDED
                skipCollapsed = true
                isDraggable = false
            }
        }
        dialog.show()
    }

    private fun colorRow(sheet: View, previewId: Int, pickId: Int, defaultId: Int): ColorSelection {
        val selection = ColorSelection(null)
        val preview = sheet.findViewById<View>(previewId)
        val defaultCheck = sheet.findViewById<CheckBox>(defaultId)
        fun render() { preview.setBackgroundColor(selection.color ?: Color.LTGRAY) }
        render()
        defaultCheck.isChecked = true
        defaultCheck.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                selection.color = null
                render()
            }
        }
        sheet.findViewById<Button>(pickId).setOnClickListener {
            showColorPicker(selection.color) { color ->
                selection.color = color
                defaultCheck.isChecked = false
                render()
            }
        }
        return selection
    }

    private fun showColorPicker(initialColor: Int?, onPicked: (Int) -> Unit) {
        val picker = layoutInflater.inflate(R.layout.dialog_color_picker, null)
        val preview = picker.findViewById<View>(R.id.color_picker_preview)
        val hex = picker.findViewById<EditText>(R.id.color_picker_hex)
        val red = picker.findViewById<SeekBar>(R.id.color_picker_red)
        val green = picker.findViewById<SeekBar>(R.id.color_picker_green)
        val blue = picker.findViewById<SeekBar>(R.id.color_picker_blue)
        val start = initialColor ?: Color.GRAY
        fun currentColor() = Color.rgb(red.progress, green.progress, blue.progress)
        fun hexOf(color: Int) = String.format("#%06X", 0xFFFFFF and color)
        red.progress = Color.red(start)
        green.progress = Color.green(start)
        blue.progress = Color.blue(start)
        hex.setText(hexOf(start))
        preview.setBackgroundColor(start)
        var syncing = false
        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val color = currentColor()
                preview.setBackgroundColor(color)
                syncing = true
                hex.setText(hexOf(color))
                syncing = false
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        }
        red.setOnSeekBarChangeListener(listener)
        green.setOnSeekBarChangeListener(listener)
        blue.setOnSeekBarChangeListener(listener)
        hex.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (syncing) return
                runCatching { Color.parseColor(s.toString().trim()) }.getOrNull()?.let { color ->
                    red.progress = Color.red(color)
                    green.progress = Color.green(color)
                    blue.progress = Color.blue(color)
                    preview.setBackgroundColor(color)
                }
            }
        })
        AlertDialog.Builder(requireContext())
            .setTitle("Pick a color")
            .setView(picker)
            .setPositiveButton(android.R.string.ok) { _, _ -> onPicked(currentColor()) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
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

private class ColorSelection(var color: Int?)

private const val FLOW_CONTINUE = 0
private const val FLOW_PAY_NOW = 1
private const val CURRENCY_CODE = "USD"
private const val APP_LINK_RETURN_URL =
    "https://mobile-sdk-demo-site-838cead5d3ab.herokuapp.com/braintree-payments"
private const val DEEP_LINK_FALLBACK_URL_SCHEME = "com.braintreepayments.demo.braintree"