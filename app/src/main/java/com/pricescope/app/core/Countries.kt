package com.pricescope.app.core

import java.text.Collator
import java.util.Currency
import java.util.Locale

data class CountryItem(val code: String, val name: String, val currencyCode: String?)

data class CurrencyItem(val code: String, val name: String)

/** Countries and currencies come from system ISO data, never from a hand-written list. */
object Countries {
    fun all(locale: Locale = Locale.getDefault()): List<CountryItem> {
        val collator = Collator.getInstance(locale)
        return Locale.getISOCountries()
            .mapNotNull { code ->
                val name = Locale("", code).getDisplayCountry(locale)
                if (name.isBlank()) null
                else CountryItem(code, name, currencyOf(code))
            }
            .sortedWith { a, b -> collator.compare(a.name, b.name) }
    }

    fun nameOf(code: String?, locale: Locale = Locale.getDefault()): String? =
        code?.let { Locale("", it).getDisplayCountry(locale).ifBlank { null } }

    fun currencyOf(countryCode: String?): String? =
        countryCode?.let { runCatching { Currency.getInstance(Locale("", it)).currencyCode }.getOrNull() }

    fun currencies(locale: Locale = Locale.getDefault()): List<CurrencyItem> {
        val collator = Collator.getInstance(locale)
        return Currency.getAvailableCurrencies()
            .filter { it.defaultFractionDigits >= 0 }
            .map { CurrencyItem(it.currencyCode, it.getDisplayName(locale)) }
            .sortedWith { a, b -> collator.compare(a.name, b.name) }
    }

    fun currencyName(code: String?, locale: Locale = Locale.getDefault()): String? =
        code?.let { runCatching { Currency.getInstance(it).getDisplayName(locale) }.getOrNull() }
}
