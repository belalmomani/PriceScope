package com.pricescope.app

import com.pricescope.app.core.Entitlements
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntitlementsTest {
    @Test
    fun freeUsersSeeAds() = assertTrue(Entitlements(Entitlements.TIER_FREE).adsEnabled)

    @Test
    fun paidUsersNeverSeeAds() {
        assertFalse(Entitlements(Entitlements.TIER_PLUS).adsEnabled)
        assertFalse(Entitlements(Entitlements.TIER_PRO).adsEnabled)
    }

    @Test
    fun defaultTierIsFree() = assertTrue(Entitlements().adsEnabled)
}
