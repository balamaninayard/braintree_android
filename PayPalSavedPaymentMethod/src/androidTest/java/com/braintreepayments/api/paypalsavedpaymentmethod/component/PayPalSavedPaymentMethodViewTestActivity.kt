package com.braintreepayments.api.paypalsavedpaymentmethod.component

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity

class PayPalSavedPaymentMethodViewTestActivity : AppCompatActivity() {

    lateinit var savedPaymentMethodView: PayPalSavedPaymentMethodView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedPaymentMethodView = PayPalSavedPaymentMethodView(this)
        setContentView(
            savedPaymentMethodView,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
    }
}
