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
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalPaymentUserAction
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypalsavedpaymentmethod.callback.PayPalSavedPaymentMethodLaunchCallback
import com.braintreepayments.api.paypalsavedpaymentmethod.component.PayPalSavedPaymentMethodView
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.ComponentAppearance
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.ContainerStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.CreditMessagingStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.FundingInstrumentStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalLabelStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalLogoStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodViewStyle
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.switchmaterial.SwitchMaterial

/**
 * Demo screen for the View/Edit FI component, wired to the real
 * [PayPalSavedPaymentMethodView] and its client.
 *
 * Three inputs drive it: a client token, the order amount, and the checkout flow. The token must
 * carry an unexpired `paymentMethodIdJwt` — `PayPalSavedPaymentMethodClient.fetchFI()` reads that
 * field off the token and fails without it, leaving only the PayPal brand mark on screen. Demo's
 * standard authorization does not include it, which is why it is entered by hand here.
 *
 * The amount feeds both the edit-FI checkout request and the Pay Later credit-messaging fetch, so
 * changing it changes the messaging copy.
 */
class PayPalSavedPaymentMethodFragment : BaseFragment() {

    private val args: PayPalSavedPaymentMethodFragmentArgs by navArgs()

    private lateinit var clientTokenInput: EditText
    private lateinit var amountInput: EditText
    private lateinit var flowSpinner: Spinner
    private lateinit var savedPaymentMethodView: PayPalSavedPaymentMethodView
    private lateinit var nonceSection: View
    private lateinit var nonceText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_paypal_saved_payment_method, container, false)

        clientTokenInput = view.findViewById<EditText>(R.id.paypal_saved_payment_method_client_token_input)
            .apply { setText(args.clientToken) }
        amountInput = view.findViewById<EditText>(R.id.paypal_saved_payment_method_amount_input)
            .apply { setText(args.amount) }
        flowSpinner = view.findViewById<Spinner>(R.id.paypal_saved_payment_method_flow_spinner)
            .apply { setSelection(if (args.payNow) FLOW_PAY_NOW_POSITION else FLOW_CONTINUE_POSITION) }
        savedPaymentMethodView = view.findViewById(R.id.paypal_saved_payment_method_view)
        nonceSection = view.findViewById(R.id.paypal_saved_payment_method_nonce_section)
        nonceText = view.findViewById(R.id.paypal_saved_payment_method_nonce_text)

        view.findViewById<Button>(R.id.paypal_saved_payment_method_load_button)
            .setOnClickListener { reload() }
        view.findViewById<Button>(R.id.paypal_saved_payment_method_style_button)
            .setOnClickListener { showStyleBottomSheet() }
        view.findViewById<Button>(R.id.paypal_saved_payment_method_nonce_clear_button)
            .setOnClickListener { nonceSection.visibility = View.GONE }

        if (args.clientToken.isBlank()) {
            // First entry / no token yet: keep the component hidden until "Load component" is tapped.
            savedPaymentMethodView.visibility = View.GONE
        } else {
            savedPaymentMethodView.visibility = View.VISIBLE
            initializeComponent()
        }
        return view
    }

    /**
     * Must run before the fragment is STARTED: `initialize()` builds a `PayPalLauncher`, which
     * registers an activity result launcher. Re-running it from a button tap would throw, so
     * [reload] re-creates the destination instead.
     */
    private fun initializeComponent() {
        // Apply before initialize() so the first fetch honors showPayPalCreditMessaging.
        pendingStyle?.let { savedPaymentMethodView.setStyle(it) }
        savedPaymentMethodView.initialize(
            activityResultCaller = this,
            authorization = args.clientToken.ifBlank { authStringArg },
            appLinkReturnUrl = APP_LINK_RETURN_URL.toUri(),
            payPalRequest = buildPayPalRequest(),
            callback = launchCallback,
            deepLinkFallbackUrlScheme = DEEP_LINK_FALLBACK_URL_SCHEME
        )
    }

    /** Bottom sheet to configure the component's style; applying re-loads the component. */
    private fun showStyleBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheet = layoutInflater.inflate(
            R.layout.bottom_sheet_paypal_saved_payment_method_style, null
        )

        val showLogoSwitch =
            sheet.findViewById<SwitchMaterial>(R.id.paypal_saved_payment_method_style_show_logo_switch)
        val showLabelSwitch =
            sheet.findViewById<SwitchMaterial>(R.id.paypal_saved_payment_method_style_show_label_switch)
        val showCreditMessagingSwitch =
            sheet.findViewById<SwitchMaterial>(R.id.paypal_saved_payment_method_style_show_credit_messaging_switch)
        val cornerRadiusInput =
            sheet.findViewById<EditText>(R.id.paypal_saved_payment_method_style_corner_radius_input)
        val borderWidthInput =
            sheet.findViewById<EditText>(R.id.paypal_saved_payment_method_style_border_width_input)
        val heightInput =
            sheet.findViewById<EditText>(R.id.paypal_saved_payment_method_style_height_input)
        val horizontalPaddingInput =
            sheet.findViewById<EditText>(R.id.paypal_saved_payment_method_style_horizontal_padding_input)
        val verticalPaddingInput =
            sheet.findViewById<EditText>(R.id.paypal_saved_payment_method_style_vertical_padding_input)
        val baseFontSizeInput =
            sheet.findViewById<EditText>(R.id.paypal_saved_payment_method_style_base_font_size_input)
        val logoWidthInput =
            sheet.findViewById<EditText>(R.id.paypal_saved_payment_method_style_logo_width_input)
        val labelFontSizeInput =
            sheet.findViewById<EditText>(R.id.paypal_saved_payment_method_style_label_font_size_input)
        val labelMarginStartInput =
            sheet.findViewById<EditText>(R.id.paypal_saved_payment_method_style_label_margin_start_input)
        val fiTextFontSizeInput =
            sheet.findViewById<EditText>(R.id.paypal_saved_payment_method_style_fi_text_font_size_input)
        val editIconSizeInput =
            sheet.findViewById<EditText>(R.id.paypal_saved_payment_method_style_edit_icon_size_input)
        val fiMarginStartInput =
            sheet.findViewById<EditText>(R.id.paypal_saved_payment_method_style_fi_margin_start_input)
        val creditMessagingFontSizeInput =
            sheet.findViewById<EditText>(R.id.paypal_saved_payment_method_style_credit_messaging_font_size_input)

        val backgroundColorRow = colorRow(
            sheet,
            R.id.paypal_saved_payment_method_style_background_color_preview,
            R.id.paypal_saved_payment_method_style_background_color_pick_button,
            R.id.paypal_saved_payment_method_style_background_color_default_checkbox,
            pendingStyle?.componentAppearance?.backgroundColor
        )
        val textColorRow = colorRow(
            sheet,
            R.id.paypal_saved_payment_method_style_text_color_preview,
            R.id.paypal_saved_payment_method_style_text_color_pick_button,
            R.id.paypal_saved_payment_method_style_text_color_default_checkbox,
            pendingStyle?.componentAppearance?.textColor
        )
        val borderColorRow = colorRow(
            sheet,
            R.id.paypal_saved_payment_method_style_border_color_preview,
            R.id.paypal_saved_payment_method_style_border_color_pick_button,
            R.id.paypal_saved_payment_method_style_border_color_default_checkbox,
            pendingStyle?.container?.borderColor
        )
        val creditMessagingLinkColorRow = colorRow(
            sheet,
            R.id.paypal_saved_payment_method_style_credit_messaging_link_color_preview,
            R.id.paypal_saved_payment_method_style_credit_messaging_link_color_pick_button,
            R.id.paypal_saved_payment_method_style_credit_messaging_link_color_default_checkbox,
            pendingStyle?.container?.creditMessaging?.linkColor
        )

        pendingStyle?.let { style ->
            showLogoSwitch.isChecked = style.showPayPalLogo
            showLabelSwitch.isChecked = style.showPayPalLabel
            showCreditMessagingSwitch.isChecked = style.showPayPalCreditMessaging
            style.container?.cornerRadiusDp?.let { cornerRadiusInput.setText(it.toString()) }
            style.container?.borderWidthDp?.let { borderWidthInput.setText(it.toString()) }
            style.container?.heightDp?.let { heightInput.setText(it.toString()) }
            style.container?.horizontalPaddingDp?.let { horizontalPaddingInput.setText(it.toString()) }
            style.container?.verticalPaddingDp?.let { verticalPaddingInput.setText(it.toString()) }
            style.componentAppearance?.baseFontSizeSp?.let { baseFontSizeInput.setText(it.toString()) }
            style.container?.logo?.widthDp?.let { logoWidthInput.setText(it.toString()) }
            style.container?.label?.fontSizeSp?.let { labelFontSizeInput.setText(it.toString()) }
            style.container?.label?.marginStartDp?.let { labelMarginStartInput.setText(it.toString()) }
            style.container?.fundingInstrument?.textFontSizeSp?.let { fiTextFontSizeInput.setText(it.toString()) }
            style.container?.fundingInstrument?.editIconSizeDp?.let { editIconSizeInput.setText(it.toString()) }
            style.container?.fundingInstrument?.marginStartDp?.let { fiMarginStartInput.setText(it.toString()) }
            style.container?.creditMessaging?.fontSizeSp?.let { creditMessagingFontSizeInput.setText(it.toString()) }
        }

        sheet.findViewById<Button>(R.id.paypal_saved_payment_method_style_apply_button)
            .setOnClickListener {
                pendingStyle = PayPalSavedPaymentMethodViewStyle(
                    showPayPalLogo = showLogoSwitch.isChecked,
                    showPayPalLabel = showLabelSwitch.isChecked,
                    showPayPalCreditMessaging = showCreditMessagingSwitch.isChecked,
                    componentAppearance = ComponentAppearance(
                        backgroundColor = backgroundColorRow.color,
                        textColor = textColorRow.color,
                        baseFontSizeSp = baseFontSizeInput.floatOrNull()
                    ),
                    container = ContainerStyle(
                        heightDp = heightInput.floatOrNull(),
                        horizontalPaddingDp = horizontalPaddingInput.floatOrNull(),
                        verticalPaddingDp = verticalPaddingInput.floatOrNull(),
                        cornerRadiusDp = cornerRadiusInput.floatOrNull(),
                        borderColor = borderColorRow.color,
                        borderWidthDp = borderWidthInput.floatOrNull(),
                        logo = PayPalLogoStyle(widthDp = logoWidthInput.floatOrNull()),
                        label = PayPalLabelStyle(
                            fontSizeSp = labelFontSizeInput.floatOrNull(),
                            marginStartDp = labelMarginStartInput.floatOrNull()
                        ),
                        fundingInstrument = FundingInstrumentStyle(
                            textFontSizeSp = fiTextFontSizeInput.floatOrNull(),
                            editIconSizeDp = editIconSizeInput.floatOrNull(),
                            marginStartDp = fiMarginStartInput.floatOrNull()
                        ),
                        creditMessaging = CreditMessagingStyle(
                            fontSizeSp = creditMessagingFontSizeInput.floatOrNull(),
                            linkColor = creditMessagingLinkColorRow.color
                        )
                    )
                )
                dialog.dismiss()
                reload()
            }

        dialog.setContentView(sheet)
        dialog.show()
    }

    private fun EditText.floatOrNull(): Float? = text.toString().trim().toFloatOrNull()

    /** Wires a color preview + pick button + "use default" checkbox; exposes the chosen color. */
    private fun colorRow(
        sheet: View,
        previewId: Int,
        pickButtonId: Int,
        defaultCheckBoxId: Int,
        initialColor: Int?
    ): ColorSelection {
        val preview = sheet.findViewById<View>(previewId)
        val pickButton = sheet.findViewById<Button>(pickButtonId)
        val defaultCheckBox = sheet.findViewById<CheckBox>(defaultCheckBoxId)
        val selection = ColorSelection(initialColor)

        fun render() = preview.setBackgroundColor(selection.color ?: Color.LTGRAY)
        render()
        defaultCheckBox.isChecked = selection.color == null
        defaultCheckBox.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                selection.color = null
                render()
            }
        }
        pickButton.setOnClickListener {
            showColorPicker(selection.color) { picked ->
                selection.color = picked
                defaultCheckBox.isChecked = false
                render()
            }
        }
        return selection
    }

    /** RGB slider + hex color picker; [onPicked] fires with the chosen color on confirm. */
    private fun showColorPicker(initialColor: Int?, onPicked: (Int) -> Unit) {
        val pickerView = layoutInflater.inflate(R.layout.dialog_color_picker, null)
        val preview = pickerView.findViewById<View>(R.id.color_picker_preview)
        val hexInput = pickerView.findViewById<EditText>(R.id.color_picker_hex)
        val redBar = pickerView.findViewById<SeekBar>(R.id.color_picker_red)
        val greenBar = pickerView.findViewById<SeekBar>(R.id.color_picker_green)
        val blueBar = pickerView.findViewById<SeekBar>(R.id.color_picker_blue)

        fun currentColor() = Color.rgb(redBar.progress, greenBar.progress, blueBar.progress)
        fun hexOf(color: Int) = String.format("#%06X", 0xFFFFFF and color)

        val start = initialColor ?: Color.GRAY
        redBar.progress = Color.red(start)
        greenBar.progress = Color.green(start)
        blueBar.progress = Color.blue(start)
        hexInput.setText(hexOf(start))
        preview.setBackgroundColor(start)

        // Guards against the slider <-> hex updates re-triggering each other.
        var syncing = false

        val changeListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val color = currentColor()
                preview.setBackgroundColor(color)
                syncing = true
                hexInput.setText(hexOf(color))
                syncing = false
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        }
        redBar.setOnSeekBarChangeListener(changeListener)
        greenBar.setOnSeekBarChangeListener(changeListener)
        blueBar.setOnSeekBarChangeListener(changeListener)

        hexInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (syncing) return
                val color = runCatching { Color.parseColor(s.toString().trim()) }.getOrNull() ?: return
                redBar.progress = Color.red(color)
                greenBar.progress = Color.green(color)
                blueBar.progress = Color.blue(color)
                preview.setBackgroundColor(color)
            }
        })

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.paypal_saved_payment_method_style_color_picker_title)
            .setView(pickerView)
            .setPositiveButton(android.R.string.ok) { _, _ -> onPicked(currentColor()) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    @OptIn(ExperimentalBetaApi::class)
    private fun buildPayPalRequest(): PayPalCheckoutRequest =
        PayPalRequestFactory.createPayPalCheckoutRequest(
            /* context = */ requireContext(),
            /* amount = */ args.amount,
            /* buyerEmailAddress = */ null,
            /* buyerPhoneCountryCode = */ null,
            /* buyerPhoneNationalNumber = */ null,
            /* isContactInformationEnabled = */ false,
            /* shopperInsightsSessionId = */ null,
            /* offerPayLater = */ false,
            /* offerCredit = */ false,
            /* isAmountBreakdownEnabled = */ false
        ).apply {
            currencyCode = CURRENCY_CODE
            // "Continue" shows a final confirmation back here; "Pay Now" commits on the PayPal page.
            userAction = if (args.payNow) {
                PayPalPaymentUserAction.USER_ACTION_COMMIT
            } else {
                PayPalPaymentUserAction.USER_ACTION_DEFAULT
            }
        }

    override fun onResume() {
        super.onResume()
        val pendingRequest =
            PendingRequestStore.getInstance().getPayPalPendingRequest(requireContext()) ?: return
        savedPaymentMethodView.handleReturnToApp(pendingRequest, requireActivity().intent)
        PendingRequestStore.getInstance().clearPayPalPendingRequest(requireContext())
        requireActivity().intent.data = null
    }

    /** Re-creates this destination so the component initializes with the entered values. */
    private fun reload() {
        val enteredClientToken = clientTokenInput.text.toString().trim()
        if (enteredClientToken.isBlank()) {
            showDialog(getString(R.string.paypal_saved_payment_method_missing_token))
            return
        }
        val action = PayPalSavedPaymentMethodFragmentDirections.actionPayPalSavedPaymentMethodFragmentSelf()
        action.authString = authStringArg
        action.clientToken = enteredClientToken
        action.amount = amountInput.text.toString().trim()
        action.payNow = flowSpinner.selectedItemPosition == FLOW_PAY_NOW_POSITION
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
                is PayPalResult.Success -> showNonce(result)
                is PayPalResult.Cancel -> handleError(Exception("User did not complete payment flow"))
                is PayPalResult.Failure -> handleError(result.error)
            }
        }
    }

    /**
     * Shown inline rather than navigating to `DisplayNonceFragment`, so the funding instrument
     * refresh that follows a successful edit stays on screen.
     */
    private fun showNonce(result: PayPalResult.Success) {
        onPaymentMethodNonceCreated(result.nonce)
        nonceText.text = getString(
            R.string.paypal_saved_payment_method_nonce_placeholder,
            result.nonce.string
        )
        nonceSection.visibility = View.VISIBLE
    }

    companion object {
        // Persists the last-applied style across the self-navigation triggered by reload().
        private var pendingStyle: PayPalSavedPaymentMethodViewStyle? = null
    }
}

private const val FLOW_CONTINUE_POSITION = 0
private const val FLOW_PAY_NOW_POSITION = 1
private const val CURRENCY_CODE = "USD"
private const val APP_LINK_RETURN_URL =
    "https://mobile-sdk-demo-site-838cead5d3ab.herokuapp.com/braintree-payments"
private const val DEEP_LINK_FALLBACK_URL_SCHEME = "com.braintreepayments.demo.braintree"

/** Mutable holder for a color-picker row's current selection (null means SDK default). */
private class ColorSelection(var color: Int?)
