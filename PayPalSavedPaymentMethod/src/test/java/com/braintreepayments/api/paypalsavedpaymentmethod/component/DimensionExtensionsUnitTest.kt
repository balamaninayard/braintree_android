package com.braintreepayments.api.paypalsavedpaymentmethod.component

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DimensionExtensionsUnitTest {

    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun `dpToPx converts a dp value using the given resources' density`() {
        val density = context.resources.displayMetrics.density

        val px = 16f.dpToPx(context.resources)

        assertEquals(16f * density, px, 0.001f)
    }

    @Test
    fun `zero dp converts to zero px`() {
        assertEquals(0f, 0f.dpToPx(context.resources), 0.001f)
    }
}
