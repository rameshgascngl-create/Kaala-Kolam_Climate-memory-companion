package edu.gascnagercoil.kaalakolam.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import edu.gascnagercoil.kaalakolam.ui.theme.PrototypeTheme

internal enum class CitizenSection(val route: String, val en: String, val ta: String, val icon: String) {
    TODAY(TODAY_ROUTE, "Today", "வானிலை", "cloud"),
    SAFETY(SAFETY_ROUTE, "Safety", "பாதுகாப்பு", "bolt"),
    CLIMATE(CLIMATE_ROUTE, "Climate", "காலநிலை", "globe"),
    LEARN(LEARNING_ROUTE, "Learn", "கற்றல்", "learn"),
    COMMUNITY(COMMUNITY_ROUTE, "Community", "சமூகம்", "pool");

    fun label(lang: String) = citizenText(lang, en, ta)
}

@Composable
internal fun CitizenNavigationBar(lang: String, currentRoute: String, onNavigate: (String) -> Unit) {
    val p = PrototypeTheme.palette
    val compact = LocalDensity.current.fontScale >= 1.5f || LocalConfiguration.current.screenWidthDp < 360
    val rows = if (compact) listOf(CitizenSection.entries.subList(0, 3), CitizenSection.entries.subList(3, 5))
        else listOf(CitizenSection.entries.toList())
    Column(
        modifier = Modifier.fillMaxWidth().background(p.ground2).navigationBarsPadding()
            .selectableGroup().padding(4.dp).testTag("citizen.navigation"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        rows.forEach { sections ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                sections.forEach { section ->
                    val selected = section.route == currentRoute
                    Column(
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            .background(if (selected) p.ground3 else p.ground2)
                            .selectable(selected, onClick = { onNavigate(section.route) }, role = Role.Tab)
                            .testTag("citizen.tab." + section.name).padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                    ) {
                        Icon(PrototypeIcons.get(section.icon), contentDescription = null,
                            tint = p.flour, modifier = Modifier.size(24.dp))
                        Text(section.label(lang), color = p.flour, style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
internal fun CitizenClimateScreen(lang: String, districtId: String) {
    val district = PilotDistrict.entries.firstOrNull { it.name == districtId }
    CitizenPage {
        item { CitizenHeading(citizenText(lang, "My climate", "நமது காலநிலை")) }
        item {
            CitizenCard {
                Text(district?.label(lang) ?: citizenText(lang, "No district selected", "மாவட்டம் தேர்ந்தெடுக்கப்படவில்லை"))
                Text(citizenText(lang, "Verified district climate data not yet available", "சரிபார்க்கப்பட்ட மாவட்டக் காலநிலைத் தரவுகள் இன்னும் கிடைக்கவில்லை"),
                    style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("citizen.climate.unavailable"))
                Text(citizenText(lang,
                    "Weather is what happens over hours or days. Climate describes the distribution of weather over decades. A climate normal is usually calculated over a 30-year reference period, such as 1991–2020.",
                    "வானிலை என்பது சில மணி நேரம் அல்லது நாட்களில் நிலவும் வளிமண்டல நிலை. காலநிலை என்பது பல பத்தாண்டுகளாக நிலவும் வானிலைப் பண்புகளின் தொகுப்பு. பொதுவாக 1991–2020 போன்ற 30 ஆண்டுக் குறிப்புக் காலத்தைப் பயன்படுத்திக் காலநிலை இயல்புநிலை கணக்கிடப்படுகிறது."))
                Text(citizenText(lang, "No local warming or rainfall trend can be inferred without suitable data.",
                    "பொருத்தமான தரவுகள் இல்லாமல் உள்ளூர் வெப்பநிலை அல்லது மழைப்பொழிவின் நீண்டகாலப் போக்கை அறிய முடியாது."))
            }
        }
        item {
            CitizenCard {
                Text(citizenText(lang, "Explore official climate information", "அதிகாரப்பூர்வக் காலநிலைத் தகவல்கள்"),
                    style = MaterialTheme.typography.titleMedium)
                OfficialLink(lang, "IMD", "https://mausam.imd.gov.in/")
                OfficialLink(lang, "IPCC", "https://www.ipcc.ch/report/ar6/syr/")
            }
        }
    }
}

@Composable
private fun CitizenModuleButton(label: String, route: String, onOpen: (String) -> Unit) {
    OutlinedButton(onClick = { onOpen(route) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        .testTag("citizen.module." + route)) { Text(label) }
}

@Composable
internal fun CitizenLearningScreen(lang: String, onOpen: (String) -> Unit) {
    CitizenPage {
        item { CitizenHeading(citizenText(lang, "Learn", "அறிந்துகொள்வோம்")) }
        item {
            CitizenCard {
                Text(citizenText(lang, "Explore weather, climate and the environment through the existing learning activities.",
                    "ஏற்கெனவே உள்ள கற்றல் செயல்பாடுகள் வழியாக வானிலை, காலநிலை, சுற்றுச்சூழலை அறிந்துகொள்ளுங்கள்."))
                CitizenModuleButton(citizenText(lang, "Climate lessons and activities", "காலநிலைப் பாடங்களும் செயல்பாடுகளும்"), "learn", onOpen)
                Text(citizenText(lang, "Test your understanding with a climate knowledge challenge. It is not a weather forecast.",
                    "காலநிலை அறிவுப் பயிற்சியின் மூலம் உங்கள் புரிதலைச் சோதியுங்கள். இது வானிலை முன்னறிவிப்பு அல்ல."))
                CitizenModuleButton(citizenText(lang, "Climate challenge", "காலநிலை அறிவுப் பயிற்சி"), "predict", onOpen)
            }
        }
    }
}

@Composable
internal fun CitizenCommunityScreen(lang: String, onOpen: (String) -> Unit) {
    CitizenPage {
        item { CitizenHeading(citizenText(lang, "Community", "நமது சமூகம்")) }
        item {
            CitizenCard {
                Text(citizenText(lang, "Record experiences, compare observations and discuss community decisions. Memories are not instrumental climate measurements.",
                    "அனுபவங்களைப் பதிவு செய்து, கவனிப்புகளை ஒப்பிட்டு, சமூக முடிவுகளை விவாதியுங்கள். நினைவுகள் கருவிகளால் அளவிடப்பட்ட காலநிலைத் தரவுகள் அல்ல."))
                CitizenModuleButton(citizenText(lang, "Elder interviews", "மூத்தோர் நேர்காணல்"), "elders", onOpen)
                CitizenModuleButton(citizenText(lang, "Class pooling", "வகுப்புத் தொகுப்பு"), "class", onOpen)
                Text(citizenText(lang, "The council is an educational simulation, not an official disaster risk assessment.",
                    "ஊர்சபை ஒரு கல்விசார் உருவகப் பயிற்சி. அது அதிகாரப்பூர்வப் பேரிடர் இடர் மதிப்பீடு அல்ல."))
                CitizenModuleButton(citizenText(lang, "Community planning simulation", "சமூகத் திட்டமிடல் பயிற்சி"), "council", onOpen)
                CitizenModuleButton(citizenText(lang, "Original learning home", "முந்தைய கற்றல் முகப்பு"), "home", onOpen)
            }
        }
    }
}
