package edu.gascnagercoil.kaalakolam.text

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.intl.LocaleList

object LocaleText {
    private val englishTokens = Regex(
        """(?<![A-Za-z0-9])(?:IPCC|AR6|SR1\.5|IMD|MoES|NOAA|WMO|UNEP|ENSO|ITCZ|SPI|SPEI|ONI|CO₂|ppm)(?![A-Za-z0-9])""",
    )
    private val englishLocale = LocaleList("en")

    fun annotateTamil(text: String): AnnotatedString = buildAnnotatedString {
        append(text)
        englishTokens.findAll(text).forEach { match ->
            addStyle(
                SpanStyle(localeList = englishLocale),
                start = match.range.first,
                end = match.range.last + 1,
            )
        }
    }
}
