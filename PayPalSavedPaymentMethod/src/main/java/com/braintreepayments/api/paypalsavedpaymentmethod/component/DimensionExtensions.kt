package com.braintreepayments.api.paypalsavedpaymentmethod.component

import android.content.res.Resources
import android.util.TypedValue

/** Converts a dp value to pixels using [resources]' current display metrics. */
internal fun Float.dpToPx(resources: Resources): Float =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, resources.displayMetrics)
