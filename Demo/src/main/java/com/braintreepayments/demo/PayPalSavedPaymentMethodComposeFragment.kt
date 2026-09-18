package com.braintreepayments.demo

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.core.net.toUri
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalPaymentUserAction
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalTokenizeCallback
import com.braintreepayments.api.paypalsavedpaymentmethod.compose.PayPalSavedPaymentMethodView
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.ComponentAppearance
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.ContainerStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.CreditMessagingStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.FundingInstrumentStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalLabelStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalLogoStyle
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodViewStyle

open class PayPalSavedPaymentMethodComposeFragment : BaseFragment() {

    private val args: PayPalSavedPaymentMethodComposeFragmentArgs by navArgs()
    private val nonceState = mutableStateOf<String?>(null)

    open override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PayPalSavedPaymentMethodComposeScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalBetaApi::class)
    @Composable
    protected fun PayPalSavedPaymentMethodComposeScreen() {
        var clientToken by rememberSaveable(args.clientToken) { mutableStateOf(args.clientToken) }
        var amount by rememberSaveable(args.amount) { mutableStateOf(args.amount) }
        val flowOptions = listOf("Continue", "Pay Now")
        var flowSelection by rememberSaveable(args.payNow) { mutableStateOf(if (args.payNow) 1 else 0) }
        var flowMenuExpanded by rememberSaveable { mutableStateOf(false) }
        val payNow = flowSelection == 1
        var enableAppSwitch by rememberSaveable(args.enableAppSwitch) { mutableStateOf(args.enableAppSwitch) }
        var showLogo by rememberSaveable { mutableStateOf(true) }
        var showLabel by rememberSaveable { mutableStateOf(true) }
        var showCreditMessaging by rememberSaveable { mutableStateOf(true) }
        var showStyleSheet by rememberSaveable { mutableStateOf(false) }
        val nonce by nonceState

        var cornerRadius by rememberSaveable { mutableStateOf("") }
        var borderWidth by rememberSaveable { mutableStateOf("") }
        var height by rememberSaveable { mutableStateOf("") }
        var horizontalPadding by rememberSaveable { mutableStateOf("") }
        var verticalPadding by rememberSaveable { mutableStateOf("") }
        var baseFontSize by rememberSaveable { mutableStateOf("") }
        var logoWidth by rememberSaveable { mutableStateOf("") }
        var labelFontSize by rememberSaveable { mutableStateOf("") }
        var labelMarginStart by rememberSaveable { mutableStateOf("") }
        var fiTextFontSize by rememberSaveable { mutableStateOf("") }
        var editIconSize by rememberSaveable { mutableStateOf("") }
        var fiMarginStart by rememberSaveable { mutableStateOf("") }
        var creditMessagingFontSize by rememberSaveable { mutableStateOf("") }
        var backgroundColor by remember { mutableStateOf<Int?>(null) }
        var textColor by remember { mutableStateOf<Int?>(null) }
        var borderColor by remember { mutableStateOf<Int?>(null) }
        var creditMessagingLinkColor by remember { mutableStateOf<Int?>(null) }

        val style = remember(
            showLogo,
            showLabel,
            showCreditMessaging,
            cornerRadius,
            borderWidth,
            height,
            horizontalPadding,
            verticalPadding,
            baseFontSize,
            logoWidth,
            labelFontSize,
            labelMarginStart,
            fiTextFontSize,
            editIconSize,
            fiMarginStart,
            creditMessagingFontSize,
            backgroundColor,
            textColor,
            borderColor,
            creditMessagingLinkColor
        ) {
            PayPalSavedPaymentMethodViewStyle(
                showPayPalLogo = showLogo,
                showPayPalLabel = showLabel,
                showPayPalCreditMessaging = showCreditMessaging,
                componentAppearance = ComponentAppearance(
                    backgroundColor = backgroundColor,
                    textColor = textColor,
                    baseFontSizeSp = baseFontSize.toFloatOrNull()
                ),
                container = ContainerStyle(
                    heightDp = height.toFloatOrNull(),
                    horizontalPaddingDp = horizontalPadding.toFloatOrNull(),
                    verticalPaddingDp = verticalPadding.toFloatOrNull(),
                    cornerRadiusDp = cornerRadius.toFloatOrNull(),
                    borderColor = borderColor,
                    borderWidthDp = borderWidth.toFloatOrNull(),
                    logo = PayPalLogoStyle(widthDp = logoWidth.toFloatOrNull()),
                    label = PayPalLabelStyle(
                        fontSizeSp = labelFontSize.toFloatOrNull(),
                        marginStartDp = labelMarginStart.toFloatOrNull()
                    ),
                    fundingInstrument = FundingInstrumentStyle(
                        textFontSizeSp = fiTextFontSize.toFloatOrNull(),
                        editIconSizeDp = editIconSize.toFloatOrNull(),
                        marginStartDp = fiMarginStart.toFloatOrNull()
                    ),
                    creditMessaging = CreditMessagingStyle(
                        fontSizeSp = creditMessagingFontSize.toFloatOrNull(),
                        linkColor = creditMessagingLinkColor
                    )
                )
            )
        }

        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "PayPalSavedPaymentMethodView",
                style = MaterialTheme.typography.titleMedium
            )

            Text(text = "Client Token")
            OutlinedTextField(
                value = clientToken,
                onValueChange = { clientToken = it },
                label = { Text("Paste a client token with paymentMethodIdJwt") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 4
            )

            Text(text = "Amount")
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Flow")
                ExposedDropdownMenuBox(
                    expanded = flowMenuExpanded,
                    onExpandedChange = { flowMenuExpanded = !flowMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = flowOptions[flowSelection],
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = flowMenuExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(0.55f)
                    )

                    DropdownMenu(
                        expanded = flowMenuExpanded,
                        onDismissRequest = { flowMenuExpanded = false }
                    ) {
                        flowOptions.forEachIndexed { index, option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    flowSelection = index
                                    flowMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable PayPal App Switch")
                Switch(checked = enableAppSwitch, onCheckedChange = { enableAppSwitch = it })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (clientToken.isBlank()) {
                            showDialog(getString(R.string.paypal_saved_payment_method_missing_token))
                            return@Button
                        }
                        findNavController().navigate(
                            R.id.action_payPalSavedPaymentMethodComposeFragment_self,
                            bundleOf(
                                "authString" to authStringArg,
                                "clientToken" to clientToken.trim(),
                                "amount" to amount.trim(),
                                "payNow" to payNow,
                                "enableAppSwitch" to enableAppSwitch
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Load component")
                }

                Button(
                    onClick = { showStyleSheet = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Style")
                }
            }

            if (args.clientToken.isNotBlank()) {
                PayPalSavedPaymentMethodView(
                    payPalCheckoutRequest = buildPayPalRequest(
                        amount = args.amount,
                        payNow = args.payNow,
                        enableAppSwitch = args.enableAppSwitch
                    ),
                    authorization = args.clientToken,
                    appLinkReturnUrl = APP_LINK_RETURN_URL.toUri(),
                    deepLinkFallbackUrlScheme = DEEP_LINK_FALLBACK_URL_SCHEME,
                    style = style,
                    paypalTokenizeCallback = paypalTokenizeCallback
                )
            }

            nonce?.let { nonceValue ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "Nonce")
                    OutlinedTextField(
                        value = nonceValue,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        singleLine = false
                    )
                    Button(onClick = { nonceState.value = null }) {
                        Text("Clear")
                    }
                }
            }
        }

        if (showStyleSheet) {
            ModalBottomSheet(
                sheetState = sheetState,
                onDismissRequest = { showStyleSheet = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Configure component style",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Button(onClick = { showStyleSheet = false }) {
                            Text("Close")
                        }
                    }

                    ToggleRow("Show PayPal logo", showLogo) { showLogo = it }
                    ToggleRow("Show PayPal label", showLabel) { showLabel = it }
                    ToggleRow("Show credit messaging", showCreditMessaging) { showCreditMessaging = it }

                    NumberField("Corner radius (dp)", cornerRadius) { cornerRadius = it }
                    NumberField("Border width (dp)", borderWidth) { borderWidth = it }
                    ColorField("Border color", borderColor) { borderColor = it }
                    NumberField("Height (dp)", height) { height = it }
                    NumberField("Horizontal padding (dp)", horizontalPadding) { horizontalPadding = it }
                    NumberField("Vertical padding (dp)", verticalPadding) { verticalPadding = it }
                    ColorField("Background color", backgroundColor) { backgroundColor = it }
                    ColorField("Text color", textColor) { textColor = it }
                    NumberField("Base font size (sp)", baseFontSize) { baseFontSize = it }
                    NumberField("Logo width (dp)", logoWidth) { logoWidth = it }
                    NumberField("Label font size (sp)", labelFontSize) { labelFontSize = it }
                    NumberField("Label margin start (dp)", labelMarginStart) { labelMarginStart = it }
                    NumberField("Funding instrument text size (sp)", fiTextFontSize) { fiTextFontSize = it }
                    NumberField("Edit icon size (dp)", editIconSize) { editIconSize = it }
                    NumberField("Funding instrument margin start (dp)", fiMarginStart) { fiMarginStart = it }
                    NumberField("Credit messaging font size (sp)", creditMessagingFontSize) { creditMessagingFontSize = it }
                    ColorField("Credit messaging link color", creditMessagingLinkColor) {
                        creditMessagingLinkColor = it
                    }

                    Button(
                        onClick = {
                            showStyleSheet = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Apply & reload")
                    }
                }
            }
        }
    }

    @Composable
    private fun ToggleRow(label: String, checked: Boolean, onCheckedChanged: (Boolean) -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label)
            Switch(checked = checked, onCheckedChange = onCheckedChanged)
        }
    }

    @Composable
    private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label)
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("0.0") }
            )
        }
    }

    @Composable
    private fun ColorField(label: String, color: Int?, onColorChange: (Int?) -> Unit) {
        var showPicker by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(androidx.compose.ui.graphics.Color(color ?: Color.DKGRAY))
                )
                Button(onClick = { showPicker = true }) {
                    Text("Pick color")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = color == null,
                        onCheckedChange = { if (it) onColorChange(null) }
                    )
                    Text("Use default")
                }
            }
        }

        if (showPicker) {
            ColorPickerDialog(
                initialColor = color ?: Color.GRAY,
                onDismiss = { showPicker = false },
                onConfirm = {
                    onColorChange(it)
                    showPicker = false
                }
            )
        }
    }

    @Composable
    private fun ColorPickerDialog(
        initialColor: Int,
        onDismiss: () -> Unit,
        onConfirm: (Int) -> Unit
    ) {
        var red by remember { mutableStateOf(Color.red(initialColor).toFloat()) }
        var green by remember { mutableStateOf(Color.green(initialColor).toFloat()) }
        var blue by remember { mutableStateOf(Color.blue(initialColor).toFloat()) }
        var hex by remember { mutableStateOf(hexOf(initialColor)) }
        var syncingHex by remember { mutableStateOf(false) }
        val currentColor = Color.rgb(red.toInt(), green.toInt(), blue.toInt())

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Pick color") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(androidx.compose.ui.graphics.Color(currentColor))
                    )
                    OutlinedTextField(
                        value = hex,
                        onValueChange = { value ->
                            hex = value
                            runCatching { Color.parseColor(value.trim()) }.getOrNull()?.let {
                                red = Color.red(it).toFloat()
                                green = Color.green(it).toFloat()
                                blue = Color.blue(it).toFloat()
                            }
                        },
                        label = { Text("Hex color") },
                        singleLine = true
                    )
                    ColorSlider("Red", red) {
                        red = it
                        if (!syncingHex) {
                            syncingHex = true
                            hex = hexOf(Color.rgb(red.toInt(), green.toInt(), blue.toInt()))
                            syncingHex = false
                        }
                    }
                    ColorSlider("Green", green) {
                        green = it
                        if (!syncingHex) {
                            syncingHex = true
                            hex = hexOf(Color.rgb(red.toInt(), green.toInt(), blue.toInt()))
                            syncingHex = false
                        }
                    }
                    ColorSlider("Blue", blue) {
                        blue = it
                        if (!syncingHex) {
                            syncingHex = true
                            hex = hexOf(Color.rgb(red.toInt(), green.toInt(), blue.toInt()))
                            syncingHex = false
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { onConfirm(currentColor) }) { Text("OK") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        )
    }

    @Composable
    private fun ColorSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
        Column {
            Text("$label: ${value.toInt()}")
            Slider(value = value, onValueChange = onValueChange, valueRange = 0f..255f)
        }
    }

    private fun hexOf(color: Int): String = String.format("#%06X", 0xFFFFFF and color)

    @OptIn(ExperimentalBetaApi::class)
    private fun buildPayPalRequest(
        amount: String,
        payNow: Boolean,
        enableAppSwitch: Boolean,
    ): PayPalCheckoutRequest =
        PayPalRequestFactory.createPayPalCheckoutRequest(
            requireContext(),
            amount,
            null,
            null,
            null,
            false,
            null,
            false,
            false,
            false
        ).apply {
            currencyCode = CURRENCY_CODE
            enablePayPalAppSwitch = enableAppSwitch
            userAction = if (payNow) {
                PayPalPaymentUserAction.USER_ACTION_COMMIT
            } else {
                PayPalPaymentUserAction.USER_ACTION_DEFAULT
            }
        }

    private val paypalTokenizeCallback = PayPalTokenizeCallback { result ->
        when (result) {
            is PayPalResult.Success -> {
                onPaymentMethodNonceCreated(result.nonce)
                nonceState.value = result.nonce.string
            }
            is PayPalResult.Cancel -> handleError(Exception("User did not complete payment flow"))
            is PayPalResult.Failure -> handleError(result.error)
        }
    }

    companion object {
        private const val CURRENCY_CODE = "USD"
        private const val APP_LINK_RETURN_URL =
            "https://mobile-sdk-demo-site-838cead5d3ab.herokuapp.com/braintree-payments"
        private const val DEEP_LINK_FALLBACK_URL_SCHEME = "com.braintreepayments.demo.braintree"
    }
}
