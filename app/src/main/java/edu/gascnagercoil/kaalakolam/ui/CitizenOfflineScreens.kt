package edu.gascnagercoil.kaalakolam.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import edu.gascnagercoil.kaalakolam.ui.theme.PrototypeTheme

private fun citizenText(lang: String, en: String, ta: String) = if (lang == "ta") ta else en

// Local UI identifiers only, not official meteorological district codes.
internal enum class PilotDistrict(val en: String, val ta: String) {
    KANNIYAKUMARI("Kanniyakumari", "கன்னியாகுமரி"),
    TIRUNELVELI("Tirunelveli", "திருநெல்வேலி"),
    TENKASI("Tenkasi", "தென்காசி"),
    THOOTHUKUDI("Thoothukudi", "தூத்துக்குடி");
    fun label(lang: String) = citizenText(lang, en, ta)
}

private data class SafetyGuide(
    val en: String, val ta: String,
    val actionEn: String, val actionTa: String,
    val authority: String, val url: String,
)

private val guides = listOf(
    SafetyGuide("Floods", "வெள்ளம்",
        "Stay out of floodwater, on foot or in a vehicle. Move to higher ground by a safe route when directed.",
        "வெள்ளநீரில் நடந்தோ வாகனத்திலோ செல்லாதீர்கள். அதிகாரிகள் அறிவுறுத்தினால் பாதுகாப்பான வழியில் மேடான இடத்திற்குச் செல்லுங்கள்.",
        "MHA", "https://dgfscdhg.gov.in/floods"),
    SafetyGuide("Extreme heat", "கடும் வெப்பம்",
        "Drink water regularly unless medically restricted. Rest in a cooler place and avoid strenuous activity during the hottest hours.",
        "மருத்துவக் காரணத்தால் நீர் அருந்தக் கட்டுப்பாடு இல்லாவிட்டால் அடிக்கடி தண்ணீர் குடியுங்கள். குளிர்ச்சியான இடத்தில் ஓய்வெடுங்கள். வெப்பம் மிகுந்த நேரத்தில் கடினமான வேலைகளைத் தவிருங்கள்.",
        "MHA", "https://dgfscdhg.gov.in/heat-wave"),
    SafetyGuide("Thunder and lightning", "இடி, மின்னல்",
        "When thunder is audible, seek a substantial building. Wait at least 30 minutes after the last thunder before returning outdoors.",
        "இடி கேட்டவுடன் உறுதியான கட்டடத்திற்குள் செல்லுங்கள். கடைசியாக இடி கேட்டதிலிருந்து குறைந்தது 30 நிமிடங்கள் கழித்தே வெளியே செல்லுங்கள்.",
        "SDMA", "https://sdma.maharashtra.gov.in/en/dos-and-donts/"),
    SafetyGuide("Cyclones", "புயல்",
        "Keep essential supplies ready. Follow official evacuation directions. A temporary lull does not mean the cyclone has ended.",
        "அத்தியாவசியப் பொருள்களைத் தயாராக வைத்திருங்கள். வெளியேறுமாறு அதிகாரிகள் அறிவுறுத்தினால் பின்பற்றுங்கள். காற்று சிறிது நேரம் ஓய்வது புயல் முடிந்துவிட்டதைக் குறிக்காது.",
        "NDMA", "https://sachet.ndma.gov.in/DosDont"),
    SafetyGuide("Coastal waters", "கடலோரப் பகுதிகள்",
        "Check marine and fishing advisories before leaving shore. Calm-looking local weather is not proof of safe sea conditions.",
        "கடலுக்குச் செல்லுமுன் கடல் நிலை, மீன்பிடி தொடர்பான அதிகாரப்பூர்வ அறிவுறுத்தல்களைப் பாருங்கள். கரையில் வானிலை அமைதியாக இருப்பதால் கடல் பாதுகாப்பாக உள்ளது என்று கருதாதீர்கள்.",
        "INCOIS", "https://incois.gov.in/site/services/hwa.jsp"),
)

@Composable
private fun CitizenPage(content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(PrototypeTheme.palette.ground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun CitizenCard(content: @Composable ColumnScope.() -> Unit) {
    val palette = PrototypeTheme.palette
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = palette.ground2, contentColor = palette.flour),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun CitizenHeading(text: String) {
    Text(text, color = PrototypeTheme.palette.flour, style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.semantics { heading() })
}

@Composable
private fun OfficialLink(lang: String, authority: String, url: String) {
    val handler = LocalUriHandler.current
    var failed by rememberSaveable(url) { mutableStateOf(false) }
    OutlinedButton(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        onClick = { failed = runCatching { handler.openUri(url) }.isFailure },
    ) {
        Text(citizenText(lang, "Open official source in browser", "அதிகாரப்பூர்வத் தளத்தைத் திறக்கவும்") + " (" + authority + ")")
    }
    if (failed) {
        Text(citizenText(lang, "Could not open the browser. Use the official website directly.",
            "இணைய உலாவியைத் திறக்க முடியவில்லை. அதிகாரப்பூர்வ இணையதளத்தை நேரடியாகப் பயன்படுத்துங்கள்."))
    }
}

@Composable
internal fun CitizenTodayScreen(lang: String, onSafety: () -> Unit) {
    var districtId by rememberSaveable { mutableStateOf("") }
    val chosen = PilotDistrict.entries.firstOrNull { it.name == districtId }
    CitizenPage {
        item { CitizenHeading(citizenText(lang, "Today: weather and warnings", "இன்றைய வானிலை")) }
        item {
            CitizenCard {
                Text(citizenText(lang, "Live information unavailable", "நிகழ்நேரத் தகவல் கிடைக்கவில்லை"),
                    style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("citizen.weather.unavailable"))
                Text(citizenText(lang,
                    "This offline build has no live weather connection. No observations, forecasts or warnings have been received. This does not mean conditions are safe.",
                    "இந்தப் பதிப்பு இணையமின்றி இயங்குகிறது. வானிலை அளவீடுகள், முன்னறிவிப்புகள், எச்சரிக்கைகள் எதுவும் பெறப்படவில்லை. இதனால் ஆபத்து இல்லை என்று பொருளல்ல."))
                Text(citizenText(lang, "Last official update: none", "கடைசி அதிகாரப்பூர்வத் தகவல்: பெறப்படவில்லை"))
            }
        }
        item {
            CitizenCard {
                Text(citizenText(lang, "Choose a pilot district", "மாவட்டத்தைத் தேர்ந்தெடுக்கவும்"),
                    style = MaterialTheme.typography.titleMedium)
                Text(chosen?.label(lang) ?: citizenText(lang, "No district selected", "மாவட்டம் தேர்ந்தெடுக்கப்படவில்லை"),
                    modifier = Modifier.testTag("citizen.district.selected"))
                PilotDistrict.entries.forEach { district ->
                    OutlinedButton(
                        onClick = { districtId = district.name },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("citizen.district." + district.name),
                    ) { Text(district.label(lang)) }
                }
                Text(citizenText(lang, "Selection is for this pilot screen only; no location is sent or forecast fetched.",
                    "இந்தத் தேர்வு இத்திரைக்கு மட்டுமே. இருப்பிடத் தகவல் அனுப்பப்படுவதில்லை; முன்னறிவிப்பும் பெறப்படுவதில்லை."))
            }
        }
        item {
            CitizenCard {
                Text(citizenText(lang, "Official information", "அதிகாரப்பூர்வத் தகவல்"),
                    style = MaterialTheme.typography.titleMedium)
                Text(citizenText(lang, "These links open separate websites and require a connection. Check the current bulletin for your exact area.",
                    "இவை இணைய உலாவியில் திறக்கப்படும். இணைய இணைப்பு தேவை. உங்கள் பகுதிக்கான தற்போதைய அறிவிப்பைப் பாருங்கள்."))
                OfficialLink(lang, "IMD", "https://mausam.imd.gov.in/chennai/")
                OfficialLink(lang, "NDMA", "https://sachet.ndma.gov.in/")
                OfficialLink(lang, "INCOIS", "https://incois.gov.in/site/services/hwa.jsp")
            }
        }
        item {
            Button(onClick = onSafety, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("citizen.open.safety")) {
                Text(citizenText(lang, "Offline safety guidance", "பாதுகாப்பு வழிமுறைகள்"))
            }
        }
    }
}

@Composable
internal fun CitizenSafetyScreen(lang: String, onToday: () -> Unit) {
    CitizenPage {
        item { CitizenHeading(citizenText(lang, "Safety", "பாதுகாப்பு")) }
        item {
            CitizenCard {
                Text(citizenText(lang, "Current warning status unknown", "தற்போதைய எச்சரிக்கை நிலை தெரியவில்லை"),
                    style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("citizen.warning.unknown"))
                Text(citizenText(lang, "General precautions below are not live alerts or an all-clear. Follow current instructions from local authorities.",
                    "கீழே உள்ளவை பொதுவான முன்னெச்சரிக்கைகள் மட்டுமே. நிகழ்நேர எச்சரிக்கைகளோ, ஆபத்து நீங்கியதற்கான அறிவிப்போ அல்ல. உள்ளூர் அதிகாரிகளின் தற்போதைய அறிவுறுத்தல்களைப் பின்பற்றுங்கள்."))
                Button(onClick = onToday, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("citizen.open.today")) {
                    Text(citizenText(lang, "Weather and official sources", "வானிலை, அதிகாரப்பூர்வத் தகவல்"))
                }
            }
        }
        guides.forEach { guide ->
            item {
                CitizenCard {
                    Text(citizenText(lang, guide.en, guide.ta), style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() })
                    Text(citizenText(lang, guide.actionEn, guide.actionTa))
                    OfficialLink(lang, guide.authority, guide.url)
                }
            }
        }
    }
}
