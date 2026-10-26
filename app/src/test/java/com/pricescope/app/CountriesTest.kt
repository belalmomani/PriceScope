package com.pricescope.app

import com.pricescope.app.core.Countries
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CountriesTest {
    @Test
    fun listsAllIsoCountriesNotJustOne() {
        val all = Countries.all(Locale.ENGLISH)
        assertTrue("expected the full ISO country list", all.size > 200)
        assertTrue(all.map { it.code }.containsAll(listOf("JO", "US", "FR", "CN", "IN", "ES")))
    }

    @Test
    fun countryCurrencyComesFromSystemData() {
        assertEquals("JOD", Countries.currencyOf("JO"))
        assertEquals("USD", Countries.currencyOf("US"))
        assertNull(Countries.currencyOf(null))
    }

    @Test
    fun currencyListHasNoPseudoCurrencies() {
        val codes = Countries.currencies(Locale.ENGLISH).map { it.code }
        assertTrue("USD" in codes && "JOD" in codes)
        assertTrue("XXX" !in codes)
    }
}
