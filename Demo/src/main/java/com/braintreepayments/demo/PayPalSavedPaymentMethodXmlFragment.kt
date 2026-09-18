package com.braintreepayments.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView

class PayPalSavedPaymentMethodXmlFragment : PayPalSavedPaymentMethodComposeFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_paypal_saved_payment_method_xml, container, false).also {
        it.findViewById<ComposeView>(R.id.paypal_saved_payment_method_compose_host).setContent {
            PayPalSavedPaymentMethodComposeScreen()
        }
    }
}