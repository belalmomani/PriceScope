package com.pricescope.app.core

/**
 * What the current user may use. Today every user is "free" because accounts and billing
 * are not connected yet. Later this is loaded from the server (my_entitlements()), never
 * from a locally editable value, so a paid tier can never be granted by the device.
 */
data class Entitlements(val tier: String = TIER_FREE) {
    val adsEnabled: Boolean get() = tier == TIER_FREE

    companion object {
        const val TIER_FREE = "free"
        const val TIER_PLUS = "plus"
        const val TIER_PRO = "pro"
    }
}
