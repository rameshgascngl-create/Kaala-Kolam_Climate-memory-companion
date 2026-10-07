package edu.gascnagercoil.kaalakolam.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.gascnagercoil.kaalakolam.content.ClassPoolCopy
import edu.gascnagercoil.kaalakolam.content.ContentRepository
import edu.gascnagercoil.kaalakolam.content.ElderQuestionContent
import edu.gascnagercoil.kaalakolam.content.ElderSliceBContent
import edu.gascnagercoil.kaalakolam.content.ElderSliceCContent
import edu.gascnagercoil.kaalakolam.domain.AppState
import edu.gascnagercoil.kaalakolam.domain.ClassGroupBy
import edu.gascnagercoil.kaalakolam.domain.ClassPoolDomain
import edu.gascnagercoil.kaalakolam.domain.ElderMode
import edu.gascnagercoil.kaalakolam.domain.ElderSession
import edu.gascnagercoil.kaalakolam.domain.EldersDomain
import edu.gascnagercoil.kaalakolam.domain.Interview
import edu.gascnagercoil.kaalakolam.domain.InterviewAnswer
import edu.gascnagercoil.kaalakolam.domain.M2Domain
import edu.gascnagercoil.kaalakolam.domain.PredictionState
import edu.gascnagercoil.kaalakolam.text.TextSafety
import edu.gascnagercoil.kaalakolam.ui.theme.PrototypeTheme
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlinx.coroutines.delay

private fun wfText(lang: String, en: String, ta: String): String = if (lang == "ta") ta else en

private val pagePadding = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 28.dp)

@Composable
private fun WorkflowPage(content: @Composable ColumnScope.() -> Unit) {
    val p = PrototypeTheme.palette
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(p.ground),
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().then(pagePadding),
                content = content,
            )
        }
    }
}

@Composable
private fun WfH1(text: String) {
    Text(
        text = text,
        color = PrototypeTheme.palette.flour,
        style = MaterialTheme.typography.headlineLarge.copy(
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .fidelityTag("screen.heading"),
        onTextLayout = fidelityTextLayout(
            tag = "screen.heading",
            kind = "heading",
            text = text,
        ),
    )
}

@Composable
private fun WfH2(text: String) {
    Text(
        text = text,
        color = PrototypeTheme.palette.flour,
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun WfH3(text: String) {
    Text(
        text = text,
        color = PrototypeTheme.palette.flour,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun WfParagraph(
    text: String,
    muted: Boolean = false,
    bottom: Int = 13,
    tag: String? = null,
) {
    val tagged = if (tag == null) Modifier else Modifier.fidelityTag(tag)
    Text(
        text = text,
        color = if (muted) PrototypeTheme.palette.faint else PrototypeTheme.palette.flour,
        style = if (muted) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(bottom = bottom.dp).then(tagged),
        onTextLayout = if (tag == null) {
            {}
        } else {
            fidelityTextLayout(tag = tag, kind = "body", text = text)
        },
    )
}

@Composable
private fun WfCard(
    modifier: Modifier = Modifier,
    tag: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val p = PrototypeTheme.palette
    val tagged = if (tag == null) Modifier else Modifier.fidelityTag(tag)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .then(tagged)
            .background(p.ground2, RoundedCornerShape(14.dp))
            .border(1.dp, p.line, RoundedCornerShape(14.dp))
            .padding(16.dp),
        content = content,
    )
}

@Composable
private fun WfButton(
    text: String,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    selected: Boolean = false,
    enabled: Boolean = true,
    tag: String? = null,
    fidelityMetricTag: String? = null,
    onClick: () -> Unit = {},
) {
    val p = PrototypeTheme.palette
    val useFill = filled || selected
    val bg = if (useFill) p.turmeric.copy(alpha = if (enabled) 1f else .45f) else Color.Transparent
    val border = if (useFill) p.turmeric else p.line
    val fg = if (useFill) Color(0xFF17120A).copy(alpha = if (enabled) 1f else .45f) else p.flour
    val effectiveTag = tag ?: "button.${text.hashCode()}"
    val effectiveMetricTag = fidelityMetricTag ?: effectiveTag
    FidelityTouchTarget(
        tag = effectiveTag,
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .fidelityTag(
                    tag = effectiveTag,
                    metricTag = effectiveMetricTag,
                )
                .heightIn(min = 46.dp)
                .background(bg, RoundedCornerShape(12.dp))
                .border(1.dp, border, RoundedCornerShape(12.dp))
                .padding(horizontal = 18.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = fg,
                fontWeight = if (useFill) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                onTextLayout = fidelityTextLayout(
                    tag = "$effectiveMetricTag.label",
                    kind = "button",
                    text = text,
                ),
            )
        }
    }
}

@Composable
private fun WfIconButton(
    icon: String,
    text: String,
    modifier: Modifier = Modifier,
    tag: String? = null,
    onClick: () -> Unit = {},
) {
    val p = PrototypeTheme.palette
    val effectiveTag = tag ?: "icon-button.${text.hashCode()}"
    FidelityTouchTarget(
        tag = effectiveTag,
        modifier = modifier,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 46.dp)
                .border(1.dp, p.line, RoundedCornerShape(12.dp))
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = PrototypeIcons.get(icon),
                contentDescription = text,
                tint = p.flour,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text,
                color = p.flour,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                onTextLayout = fidelityTextLayout(
                    tag = "$effectiveTag.label",
                    kind = "button",
                    text = text,
                ),
            )
        }
    }
}

@Composable
private fun WfSegments(
    labels: List<String>,
    selected: Int,
    tagPrefix: String = "segment",
    onSelect: (Int) -> Unit = {},
) {
    val p = PrototypeTheme.palette
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val active = index == selected
            FidelityTouchTarget(
                tag = "$tagPrefix.$index",
                modifier = Modifier.weight(1f),
                onClick = { onSelect(index) },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 46.dp)
                        .background(
                            if (active) p.turmeric else Color.Transparent,
                            RoundedCornerShape(10.dp),
                        )
                        .border(
                            1.dp,
                            if (active) p.turmeric else p.line,
                            RoundedCornerShape(10.dp),
                        )
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        color = if (active) Color(0xFF17120A) else p.flour,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 20.sp,
                        ),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun WfNote(text: String) {
    val p = PrototypeTheme.palette
    Text(
        text = text,
        color = p.flour,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .background(p.ground3, RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp))
            .border(
                BorderStroke(0.dp, Color.Transparent),
                RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp),
            )
            .padding(start = 16.dp, top = 8.dp, end = 12.dp, bottom = 8.dp),
    )
}

@Composable
private fun WfFooter(lang: String) {
    val p = PrototypeTheme.palette
    Spacer(Modifier.height(14.dp))
    Box(Modifier.fillMaxWidth().height(1.dp).background(p.line))
    Spacer(Modifier.height(12.dp))
    Text(
        "Department of Zoology, GASC, Nagercoil / Created by R. Ramesh",
        color = p.faint,
        fontSize = 12.8.sp,
        lineHeight = 19.84.sp,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        wfText(
            lang,
            "Tamil text is a draft awaiting language review.",
            "தமிழ் உரை வரைவு நிலையில் உள்ளது; மொழிப் பிழைதிருத்தம் தேவை.",
        ),
        color = p.faint,
        fontSize = 12.8.sp,
        lineHeight = 19.84.sp,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        "Figures and thresholds quoted here (IPCC AR6, IMD, MoES 2020, NOAA, WMO and UNEP) and the emergency numbers should be checked against current sources before release. The council is a toy model for thinking, not a forecast. Testimony is evidence about experience, not a measurement.",
        color = p.faint,
        fontSize = 12.8.sp,
        lineHeight = 19.84.sp,
    )
}

@Composable
private fun WfInput(
    value: String,
    placeholder: String,
    minHeight: Int = 46,
    onValueChange: (String) -> Unit = {},
    accessibilityLabel: String? = null,
) {
    val p = PrototypeTheme.palette
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = p.flour),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight.dp)
            .semantics { accessibilityLabel?.let { contentDescription = it } }
            .background(p.ground, RoundedCornerShape(10.dp))
            .border(1.dp, p.line, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.TopStart) {
                if (value.isEmpty()) {
                    Text(placeholder, color = p.faint, style = MaterialTheme.typography.bodyLarge)
                }
                inner()
            }
        },
    )
}

@Composable
private fun WfCoin(text: String) {
    val p = PrototypeTheme.palette
    Box(
        modifier = Modifier
            .heightIn(min = 28.dp)
            .widthIn(min = 28.dp)
            .background(p.turmeric, RoundedCornerShape(99.dp))
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color(0xFF17120A), fontWeight = FontWeight.Bold, fontSize = 13.6.sp)
    }
}

@Composable
fun LearnPrototypeScreen(
    lang: String,
    initialMode: String = "home",
) {
    var mode by remember(initialMode) { mutableStateOf(initialMode) }
    when (mode) {
        "topic", "deep" -> LearnTopicPrototype(lang, deep = mode == "deep", onBack = { mode = "home" })
        "game" -> LearnGamePrototype(lang, onBack = { mode = "home" })
        "words" -> LearnWordsPrototype(lang, onBack = { mode = "home" })
        else -> LearnHomePrototype(lang, onTopic = { mode = "topic" }, onGame = { mode = "game" }, onWords = { mode = "words" })
    }
}

@Composable
private fun LearnHomePrototype(
    lang: String,
    onTopic: () -> Unit,
    onGame: () -> Unit,
    onWords: () -> Unit,
) {
    var level by remember { mutableStateOf(1) }
    WorkflowPage {
        WfH1(wfText(lang, "Learn", "கற்றுக்கொள்ளுங்கள்"))
        WfParagraph(
            wfText(
                lang,
                "Weather and climate explained at three levels, for children, for everyone, and for those who want the detail. Every topic has an animated clip and can be read aloud.",
                "வானிலையும் காலநிலையும் மூன்று நிலைகளில் விளக்கப்படுகின்றன: சிறுவர்களுக்கு, அனைவருக்கும், விவரம் விரும்புவோருக்கு. ஒவ்வொரு தலைப்புக்கும் அனிமேஷன் காட்சி உண்டு; குரலிலும் வாசிக்கச் செய்யலாம்.",
            ),
            tag = "screen.body.primary",
        )
        WfSegments(
            labels = listOf(
                wfText(lang, "Child", "சிறுவர்"),
                wfText(lang, "Everyone", "அனைவருக்கும்"),
                wfText(lang, "Deep dive", "ஆழ்ந்து அறிய"),
            ),
            selected = level,
            onSelect = { level = it },
        )
        WfCard(tag = "screen.first-card") {
            WfH2(wfText(lang, "Happening now? Safety steps", "இப்போது நடக்கிறதா? பாதுகாப்பு வழிகள்"))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WfIconButton("heat", wfText(lang, "Heat", "வெப்பம்"), Modifier.weight(1f))
                WfIconButton("flood", wfText(lang, "Flood", "வெள்ளம்"), Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WfIconButton("cyclone", wfText(lang, "Cyclone", "புயல்"), Modifier.weight(1f))
                WfIconButton("bolt", wfText(lang, "Lightning", "மின்னல்"), Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            WfIconButton("sea", wfText(lang, "Sea and tsunami", "கடல், சுனாமி"), Modifier.widthIn(max = 190.dp))
            Spacer(Modifier.height(10.dp))
            WfParagraph(
                wfText(
                    lang,
                    "Emergency 112 (all India) · Ambulance 108 · Tamil Nadu State control room 1070 · District control room 1077. Check the numbers for your own district.",
                    "அவசர உதவி 112 (இந்தியா முழுவதும்) · ஆம்புலன்ஸ் 108 · தமிழ்நாடு மாநிலக் கட்டுப்பாட்டு அறை 1070 · மாவட்டக் கட்டுப்பாட்டு அறை 1077. உங்கள் மாவட்டத்தின் எண்களைச் சரிபார்த்துக்கொள்ளுங்கள்.",
                ),
                muted = true,
                bottom = 0,
            )
        }
        Row(Modifier.padding(vertical = 10.dp)) {
            WfButton(
                wfText(lang, "Tamil–English word list", "தமிழ்–ஆங்கிலச் சொற்பட்டி"),
                tag = "screen.primary-button",
                onClick = onWords,
            )
        }
        WfCard {
            WfH3(wfText(lang, "Topics", "தலைப்புகள்"))
            WfInput("", wfText(lang, "Search: for example rain, cyclone, heat", "தேடுங்கள்: எ.கா. மழை, புயல், வெப்பம்"))
            Spacer(Modifier.height(10.dp))
            WfButton(
                wfText(lang, "Sun, seasons and why places differ", "சூரியன், பருவங்கள், இடங்கள் ஏன் வேறுபடுகின்றன"),
                modifier = Modifier.fillMaxWidth(),
                onClick = onTopic,
            )
            Spacer(Modifier.height(10.dp))
            WfButton(
                wfText(lang, "Weather or climate?", "வானிலையா? காலநிலையா?"),
                modifier = Modifier.fillMaxWidth(),
                filled = true,
                onClick = onGame,
            )
        }
    }
}

@Composable
private fun LearnTopicPrototype(lang: String, deep: Boolean, onBack: () -> Unit) {
    var level by remember(deep) { mutableStateOf(if (deep) 2 else 1) }
    WorkflowPage {
        WfButton(wfText(lang, "← All topics", "← அனைத்துத் தலைப்புகள்"), onClick = onBack)
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = PrototypeIcons.get("sun"),
                contentDescription = wfText(lang, "Sun", "சூரியன்"),
                tint = PrototypeTheme.palette.turmeric,
                modifier = Modifier.size(44.dp),
            )
            val topicHeading = wfText(lang, "Sun, seasons and why places differ", "சூரியன், பருவங்கள், இடங்கள் ஏன் வேறுபடுகின்றன")
            val topicHeadingWidth = if (
                lang == "ta" && LocalConfiguration.current.screenWidthDp >= 360
            ) {
                Modifier.widthIn(max = 268.dp)
            } else {
                Modifier.weight(1f)
            }
            Text(
                topicHeading,
                color = PrototypeTheme.palette.flour,
                style = MaterialTheme.typography.headlineLarge.copy(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                ),
                modifier = topicHeadingWidth.fidelityTag("screen.heading"),
                onTextLayout = fidelityTextLayout(
                    tag = "screen.heading",
                    kind = "heading",
                    text = topicHeading,
                ),
            )
        }
        WfSegments(
            listOf(
                wfText(lang, "Child", "சிறுவர்"),
                wfText(lang, "Everyone", "அனைவருக்கும்"),
                wfText(lang, "Deep dive", "ஆழ்ந்து அறிய"),
            ),
            selected = level,
            onSelect = { level = it },
        )
        Spacer(Modifier.height(14.dp))
        WfCard {
            WfH2(wfText(lang, "Watch the animation", "அனிமேஷனைப் பாருங்கள்"))
            WfParagraph(
                wfText(
                    lang,
                    "Watch for: Where Earth is on its path, and which way its axis leans.",
                    "கவனியுங்கள்: பூமி அதன் பாதையில் எங்கே இருக்கிறது, அதன் அச்சு எந்தப் பக்கம் சாய்ந்துள்ளது என்று பாருங்கள்.",
                ),
                bottom = 8,
                tag = "screen.body.primary",
            )
            AnimationPreview(lang)
        }
        val text = when (level) {
            0 -> wfText(
                lang,
                "The Sun warms the Earth, but not evenly. Places where sunlight falls straight down get hotter, like a torch pointed straight at a wall.",
                "சூரியன் பூமியை வெப்பப்படுத்துகிறது, ஆனால் எல்லா இடங்களிலும் சமமாக இல்லை. சூரிய ஒளி நேராக விழும் இடங்கள் அதிகம் சூடாகின்றன; சுவரில் நேராக அடிக்கும் டார்ச் வெளிச்சம் போல.",
            )
            2 -> "The energy received per unit area depends on the angle of the Sun’s rays. Earth’s axial tilt of about 23.4° drives the seasons. The orbit is slightly elliptical, but distance from the Sun is not the cause of seasons. Land and ocean respond differently because water has a very high heat capacity."
            else -> wfText(
                lang,
                "Earth is tilted. As it circles the Sun, different places face the Sun more directly at different times of year, and that creates the seasons. Near the equator sunlight is strong all year, so the main seasonal change is rain, not temperature. Farther from the equator, summers and winters differ strongly. India’s weather department describes four seasons: winter, pre-monsoon summer, the south-west monsoon, and the post-monsoon months.",
                "பூமி சாய்வாக உள்ளது. சூரியனைச் சுற்றும்போது ஆண்டின் வெவ்வேறு காலங்களில் வெவ்வேறு இடங்களில் சூரிய ஒளி நேராகவோ சாய்வாகவோ விழுகிறது; இதனால் பருவங்கள் உண்டாகின்றன. நிலநடுக்கோட்டுக்கு அருகில் ஆண்டு முழுவதும் சூரிய ஒளி வலுவாக இருப்பதால் முக்கிய பருவ மாற்றம் மழையே, வெப்பநிலை அல்ல. தொலைவில் உள்ள இடங்களில் கோடையும் குளிர்காலமும் மிகவும் வேறுபடும். இந்திய வானிலைத் துறை குளிர்காலம், கோடைக்காலம், தென்மேற்குப் பருவமழை, பருவமழைக்குப் பிந்தைய மாதங்கள் என நான்கு பருவங்களை வகுக்கிறது.",
            )
        }
        WfParagraph(text)
    }
}

@Composable
private fun AnimationPreview(lang: String) {
    val p = PrototypeTheme.palette
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Color(0xFF0B1633), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cy = size.height * .52f
            drawCircle(Color(0xFFB78A18), radius = size.height * .18f, center = Offset(size.width * .15f, cy))
            repeat(8) { index ->
                val a = Math.toRadians((index * 45).toDouble())
                val r1 = size.height * .23f
                val r2 = size.height * .28f
                drawLine(
                    Color(0xFFB78A18),
                    Offset(size.width * .15f + kotlin.math.cos(a).toFloat() * r1, cy + kotlin.math.sin(a).toFloat() * r1),
                    Offset(size.width * .15f + kotlin.math.cos(a).toFloat() * r2, cy + kotlin.math.sin(a).toFloat() * r2),
                    strokeWidth = 2.dp.toPx(),
                )
            }
            drawCircle(Color(0xFF174B76), radius = size.height * .13f, center = Offset(size.width * .76f, cy))
            drawCircle(Color(0xFF4D7C3C), radius = size.height * .055f, center = Offset(size.width * .74f, cy - size.height * .02f))
            repeat(6) { index ->
                drawCircle(
                    Color(0xFF9A7A2A),
                    radius = 2.dp.toPx(),
                    center = Offset(size.width * (.28f + index * .065f), cy),
                )
            }
        }
        Icon(
            imageVector = PrototypeIcons.get("play"),
            contentDescription = wfText(lang, "Play animation", "அனிமேஷனை இயக்கு"),
            tint = Color.White,
            modifier = Modifier.size(84.dp),
        )
        Text(
            wfText(lang, "Watch", "பார்க்க"),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp),
        )
    }
}

@Composable
private fun LearnGamePrototype(lang: String, onBack: () -> Unit) {
    val p = PrototypeTheme.palette
    WorkflowPage {
        WfButton(wfText(lang, "← Learn", "← கற்க"), onClick = onBack)
        Spacer(Modifier.height(12.dp))
        WfH1(wfText(lang, "Weather or climate?", "வானிலையா? காலநிலையா?"))
        Text("1 / 10", color = p.faint, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(6.dp).background(p.ground3, RoundedCornerShape(99.dp))) {
            Box(Modifier.fillMaxWidth(.1f).height(6.dp).background(p.turmeric, RoundedCornerShape(99.dp)))
        }
        Spacer(Modifier.height(14.dp))
        val gamePrompt = wfText(lang, "It rained heavily in my town last night.", "நேற்றிரவு என் ஊரில் கனமழை பெய்தது.")
        Text(
            gamePrompt,
            color = p.flour,
            fontFamily = MaterialTheme.typography.headlineMedium.fontFamily,
            fontSize = 20.8.sp,
            lineHeight = 28.08.sp,
            modifier = Modifier.padding(bottom = 13.dp).fidelityTag("screen.body.primary"),
            onTextLayout = fidelityTextLayout(
                tag = "screen.body.primary",
                kind = "body",
                text = gamePrompt,
            ),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WfButton(wfText(lang, "Weather", "வானிலை"), Modifier.weight(1f).heightIn(min = 64.dp))
            WfButton(wfText(lang, "Climate", "காலநிலை"), Modifier.weight(1f).heightIn(min = 64.dp))
        }
    }
}

private data class WordRow(val en: String, val ta: String, val definition: String)

private val wordRows = listOf(
    WordRow("weather", "வானிலை", "ஓர் இடத்தில் இன்றோ இந்த வாரமோ உள்ள வெப்பம், மழை, காற்று நிலை."),
    WordRow("climate", "காலநிலை", "ஓர் இடத்தின் வானிலை பல ஆண்டுகளில் (பொதுவாக 30 ஆண்டுகள்) எப்படி இருக்கும் என்பதன் சராசரி."),
    WordRow("climate change", "காலநிலை மாற்றம்", "காலநிலையின் சராசரி நிலை பல பத்தாண்டுகளில் மாறுவது."),
    WordRow("global warming", "புவி வெப்பமாதல்", "பூமியின் சராசரி வெப்பநிலை உயர்வது."),
)

@Composable
private fun LearnWordsPrototype(lang: String, onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    WorkflowPage {
        WfButton(wfText(lang, "← All topics", "← அனைத்துத் தலைப்புகள்"), onClick = onBack)
        Spacer(Modifier.height(8.dp))
        WfH1(wfText(lang, "Tamil–English word list", "தமிழ்–ஆங்கிலச் சொற்பட்டி"))
        WfParagraph(
            wfText(
                lang,
                "The Tamil terms used in this app. A ⚠ mark means the term is a draft: please check it with a Tamil language expert or the Tamil Nadu textbook glossary before printing.",
                "இந்தச் செயலியில் பயன்படுத்தப்படும் தமிழ்ச் சொற்கள். ⚠ குறியிட்ட சொல் வரைவு நிலையில் உள்ளது: அச்சிடும் முன் தமிழ் மொழி வல்லுநரிடம் அல்லது தமிழ்நாடுப் பாடநூல் கலைச்சொல் பட்டியலுடன் சரிபாருங்கள்.",
            ),
            tag = "screen.body.primary",
        )
        WfInput(query, wfText(lang, "Search a word", "சொல்லைத் தேடுங்கள்"), onValueChange = { query = it })
        Spacer(Modifier.height(10.dp))
        wordRows.filter {
            query.isBlank() || (it.en + " " + it.ta + " " + it.definition).contains(query, ignoreCase = true)
        }.forEach { row ->
            val p = PrototypeTheme.palette
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(p.ground2, RoundedCornerShape(14.dp))
                    .border(1.dp, p.line, RoundedCornerShape(14.dp))
                    .padding(12.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text(row.ta, color = p.flour, fontWeight = FontWeight.Bold, fontSize = 17.6.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(row.en, color = p.faint, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(4.dp))
                Text(row.definition, color = p.flour, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun ElderSelect(
    label: String,
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val p = PrototypeTheme.palette
    val safeIndex = selected.coerceIn(0, options.lastIndex)
    Text(
        text = label,
        color = p.flour,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(bottom = 6.dp),
    )
    Box(modifier = Modifier.fillMaxWidth()) {
        WfButton(
            text = options[safeIndex],
            modifier = Modifier.fillMaxWidth(),
            onClick = { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(p.ground2),
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            color = p.flour,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
    }
}

@Composable
fun ElderPrototypeScreen(
    lang: String,
    appState: AppState = AppState(language = lang),
    onSessionChange: (ElderSession) -> Unit = {},
    onCreateInterview: (String, Int, Int, String) -> Unit = { _, _, _, _ -> },
    onDeleteInterview: (String) -> Unit = {},
    onAnswerChange: (String, String, InterviewAnswer) -> Unit = { _, _, _ -> },
    onAdvance: (String, String, InterviewAnswer?, ElderSession) -> Unit = { _, _, _, _ -> },
    onReactionChange: (String, String) -> Unit = { _, _ -> },
    initialDeletePendingId: String? = null,
) {
    val context = LocalContext.current
    val asset = remember(context) {
        ContentRepository(context).loadElders()
    }
    val content = asset.sliceB
    val activeInterview = appState.elderSession.activeId?.let { activeId ->
        appState.interviews.firstOrNull { it.id == activeId }
    }

    if (appState.elderSession.mode == ElderMode.SETUP) {
        ElderSetupScreen(
            lang = lang,
            content = content,
            atLimit = appState.interviews.size >= EldersDomain.MAX_INTERVIEWS,
            onBack = { onSessionChange(ElderSession()) },
            onCreateInterview = onCreateInterview,
        )
        return
    }

    if (appState.elderSession.mode == ElderMode.RESULT && activeInterview != null) {
        ElderResultScreen(
            lang = lang,
            interview = activeInterview,
            questions = asset.elderQuestions,
            content = content,
            onReactionChange = { reaction -> onReactionChange(activeInterview.id, reaction) },
            onAllInterviews = { onSessionChange(ElderSession()) },
            onNewInterview = { onSessionChange(ElderSession(mode = ElderMode.SETUP)) },
        )
        return
    }

    if (appState.elderSession.mode == ElderMode.ASK && activeInterview != null) {
        val question = asset.elderQuestions[appState.elderSession.questionIndex]
        ElderAskScreen(
            lang = lang,
            interview = activeInterview,
            question = question,
            questionIndex = appState.elderSession.questionIndex,
            questionCount = asset.elderQuestions.size,
            content = asset.sliceC,
            onAnswerChange = { questionId, answer ->
                onAnswerChange(activeInterview.id, questionId, answer)
            },
            onBack = {
                val index = appState.elderSession.questionIndex
                if (index > 0) {
                    onSessionChange(appState.elderSession.copy(questionIndex = index - 1))
                } else {
                    onSessionChange(ElderSession())
                }
            },
            onForward = { answer ->
                val index = appState.elderSession.questionIndex
                val last = index == asset.elderQuestions.lastIndex
                val nextSession = if (last) {
                    appState.elderSession.copy(
                        mode = ElderMode.RESULT,
                        questionIndex = index,
                    )
                } else {
                    appState.elderSession.copy(questionIndex = index + 1)
                }
                onAdvance(activeInterview.id, question.id, answer, nextSession)
            },
        )
        return
    }

    var deletePendingId by remember(initialDeletePendingId) { mutableStateOf(initialDeletePendingId) }
    LaunchedEffect(deletePendingId) {
        val armedId = deletePendingId ?: return@LaunchedEffect
        delay(3_000)
        if (deletePendingId == armedId) {
            deletePendingId = null
        }
    }

    val atLimit = appState.interviews.size >= EldersDomain.MAX_INTERVIEWS
    WorkflowPage {
        WfH1(content.listTitle.text(lang))
        WfParagraph(
            content.intro.text(lang),
            tag = "screen.body.primary",
        )
        WfButton(
            text = content.start.text(lang),
            filled = true,
            enabled = !atLimit,
            tag = "elder-start",
            fidelityMetricTag = "screen.primary-button",
            onClick = { onSessionChange(ElderSession(mode = ElderMode.SETUP)) },
        )
        if (atLimit) {
            WfNote(content.limitMessage.text(lang))
        }

        if (appState.interviews.isEmpty()) {
            WfNote(content.empty.text(lang))
        } else {
            Spacer(Modifier.height(12.dp))
            appState.interviews.asReversed().forEach { interview ->
                val place = content.places
                    .getOrElse(interview.place) { content.places.first() }
                    .text(lang)
                val decade = content.decades
                    .getOrElse(interview.birthDecade) { content.decades.first() }
                val metadata = buildString {
                    append(place)
                    append(" · ")
                    append(content.bornPrefix.text(lang))
                    append(decade)
                    append(" · ")
                    append(EldersDomain.answeredCount(interview))
                    append("/")
                    append(EldersDomain.questionIds.size)
                }
                WfCard {
                    WfH3(interview.nickname)
                    WfParagraph(
                        text = metadata,
                        muted = true,
                        bottom = 8,
                    )
                    val armed = deletePendingId == interview.id
                    WfButton(
                        text = if (armed) {
                            content.deleteConfirm.text(lang)
                        } else {
                            content.delete.text(lang)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (armed) {
                                deletePendingId = null
                                onDeleteInterview(interview.id)
                            } else {
                                deletePendingId = interview.id
                            }
                        },
                    )
                }
            }
        }
        WfFooter(lang)
    }
}

@Composable
private fun ElderResultScreen(
    lang: String,
    interview: Interview,
    questions: List<ElderQuestionContent>,
    content: ElderSliceBContent,
    onReactionChange: (String) -> Unit,
    onAllInterviews: () -> Unit,
    onNewInterview: () -> Unit,
) {
    val p = PrototypeTheme.palette
    val clipboard = LocalClipboardManager.current
    val answered = questions.mapNotNull { question ->
        interview.answers[question.id]?.takeIf { it.answered }?.let { question to it }
    }
    val rankedCandidates = answered
        .filter { (_, answer) -> answer.rating != null && answer.rating != 0 }
        .sortedByDescending { (_, answer) -> kotlin.math.abs(requireNotNull(answer.rating)) }
    val ranked = rankedCandidates.subList(0, minOf(3, rankedCandidates.size))
    val comparable = answered.filter { (question, answer) ->
        question.exp != 0 && answer.rating != null && answer.rating != 0
    }
    val matching = comparable.count { (question, answer) ->
        Integer.signum(requireNotNull(answer.rating)) == Integer.signum(question.exp)
    }
    val shareCode = if (EldersDomain.answeredCount(interview) >= 3) {
        EldersDomain.exportShareCode(interview)
    } else null

    WorkflowPage {
        WfH1(interview.nickname)
        WfParagraph(
            text = content.places.getOrElse(interview.place) { content.places.first() }.text(lang) +
                " · " + content.bornPrefix.text(lang) +
                content.decades.getOrElse(interview.birthDecade) { content.decades.first() },
            muted = true,
            tag = "elder-result-meta",
        )
        WfCard(tag = "elder-result-stripes") {
            WfH2(wfText(lang, "Memory stripes", "நினைவுக் கோடுகள்"))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                questions.forEach { question ->
                    val answer = interview.answers[question.id]
                    val colour = when (answer?.rating) {
                        -2 -> wfHexColor("#1f6f8b")
                        -1 -> wfHexColor("#6bb3c4")
                        0 -> wfHexColor("#d8d2c4")
                        1 -> wfHexColor("#eba46d")
                        2 -> wfHexColor("#c8452c")
                        else -> p.ground3
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .background(colour, RoundedCornerShape(5.dp))
                            .semantics { contentDescription = question.title.text(lang) },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            WfParagraph(
                wfText(
                    lang,
                    "Each stripe is one thing your elder said about change. Cross-check it against the evidence below.",
                    "ஒவ்வொரு கோடும் மாற்றம் பற்றி உங்கள் மூத்தவர் சொன்ன ஒரு விஷயம். கீழே உள்ள சான்றுகளுடன் அதை ஒப்பிடுங்கள்.",
                ),
                bottom = 0,
            )
        }
        if (ranked.isNotEmpty()) {
            WfParagraph(
                text = wfText(lang, "Biggest changes they noticed: ", "அவர் கவனித்த மிகப் பெரிய மாற்றங்கள்: ") +
                    ranked.joinToString("; ") { (question, answer) ->
                        question.title.text(lang) + " (" + elderRatingText(lang, answer.rating) + ")"
                    } + ".",
                tag = "elder-result-biggest",
            )
        }
        if (comparable.size >= 3) {
            WfNote(
                wfText(
                    lang,
                    "On $matching of ${comparable.size} comparable questions, the memory points in the same direction as the instrument record.",
                    "ஒப்பிடக்கூடிய ${comparable.size} கேள்விகளில் $matching-இல், நினைவு கருவிப் பதிவுகளின் திசையிலேயே உள்ளது.",
                ),
            )
        }
        WfH2(wfText(lang, "Cross-check each memory", "நினைவை சான்றுடன் ஒப்பிடு"))
        answered.forEach { (question, answer) ->
            WfCard(tag = "elder-result-evidence.${question.id}") {
                WfH3(question.title.text(lang))
                WfParagraph(
                    wfText(lang, "Your elder: ", "உங்கள் மூத்தவர்: ") + elderRatingText(lang, answer.rating),
                    muted = true,
                )
                if (answer.story.isNotBlank()) WfParagraph("“${answer.story}”")
                WfParagraph(
                    wfText(lang, "Verdict: ", "முடிவு: ") + elderVerdictText(lang, question.verdict),
                    muted = true,
                )
                WfH3(wfText(lang, "What instruments and studies say", "கருவிகளும் ஆய்வுகளும் சொல்வது"))
                WfParagraph(question.sci.text(lang))
                WfH3(wfText(lang, "What else could explain it", "வேறு என்ன காரணமாக இருக்கலாம்"))
                WfParagraph(question.conf.text(lang))
                WfH3(wfText(lang, "How you can check", "நீங்களே சோதிக்க"))
                WfParagraph(question.check.text(lang), bottom = 0)
            }
        }
        WfCard(tag = "elder-result-reaction") {
            WfH2(wfText(lang, "Take it back to your elder", "மூத்தவரிடம் திரும்பிச் சொல்லுங்கள்"))
            WfParagraph(
                wfText(
                    lang,
                    "Tell your elder one thing the evidence matched or challenged, then record their reply.",
                    "சான்றுகள் பொருந்திய அல்லது சவால் செய்த ஒன்றை மூத்தவரிடம் சொல்லி, அவர் பதிலைப் பதிவு செய்யுங்கள்.",
                ),
            )
            WfInput(
                value = interview.reaction,
                placeholder = wfText(lang, "What they said", "அவர் சொன்னது"),
                minHeight = 84,
                onValueChange = onReactionChange,
            )
        }
        if (shareCode != null) {
            WfCard(tag = "elder-result-share") {
                WfH2(wfText(lang, "Share with your class", "வகுப்புடன் பகிர"))
                WfParagraph(
                    wfText(
                        lang,
                        "This code contains only place type, birth decade and answers. It contains no nickname, stories, reaction, notes or reflection.",
                        "இந்தக் குறியீட்டில் இட வகை, பிறந்த பத்தாண்டு, பதில்கள் மட்டுமே உள்ளன. செல்லப்பெயர், கதைகள், பதில், குறிப்புகள் அல்லது பிரதிபலிப்பு இல்லை.",
                    ),
                )
                Text(
                    text = shareCode,
                    color = p.flour,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fidelityTag("elder-share-code")
                        .background(p.ground, RoundedCornerShape(10.dp))
                        .border(1.dp, p.line, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                )
                Spacer(Modifier.height(8.dp))
                WfButton(
                    text = wfText(lang, "Copy", "நகலெடு"),
                    filled = true,
                    tag = "elder-copy-code",
                    onClick = { clipboard.setText(AnnotatedString(shareCode)) },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WfButton(
                text = wfText(lang, "All interviews", "அனைத்து நேர்காணல்கள்"),
                modifier = Modifier.weight(1f),
                tag = "elder-result-all",
                onClick = onAllInterviews,
            )
            WfButton(
                text = wfText(lang, "New interview", "புதிய நேர்காணல்"),
                modifier = Modifier.weight(1f),
                tag = "elder-result-new",
                onClick = onNewInterview,
            )
        }
        WfFooter(lang)
    }
}

private fun elderRatingText(lang: String, rating: Int?): String = when (rating) {
    null -> wfText(lang, "Cannot say", "சொல்ல இயலாது")
    -2 -> wfText(lang, "Much less", "மிகக் குறைவு")
    -1 -> wfText(lang, "Less", "குறைவு")
    0 -> wfText(lang, "Same", "அதே அளவு")
    1 -> wfText(lang, "More", "அதிகம்")
    else -> wfText(lang, "Much more", "மிக அதிகம்")
}

private fun elderVerdictText(lang: String, verdict: String): String = when (verdict) {
    "consistent" -> wfText(lang, "Consistent", "ஒத்துப்போகிறது")
    "mixed" -> wfText(lang, "Mixed", "கலப்பு")
    "confounded" -> wfText(lang, "Several causes", "பல காரணங்கள்")
    else -> verdict
}

@Composable
private fun ElderAskScreen(
    lang: String,
    interview: Interview,
    question: ElderQuestionContent,
    questionIndex: Int,
    questionCount: Int,
    content: ElderSliceCContent,
    onAnswerChange: (String, InterviewAnswer) -> Unit,
    onBack: () -> Unit,
    onForward: (InterviewAnswer?) -> Unit,
) {
    val p = PrototypeTheme.palette
    val answer = interview.answers[question.id]
    val isAnswered = answer?.answered == true
    val rating = if (isAnswered) answer?.rating else null
    val last = questionIndex == questionCount - 1
    val progress = (questionIndex + 1).toFloat() / questionCount.toFloat()

    WorkflowPage {
        WfParagraph(
            text = interview.nickname + " · " + (questionIndex + 1) + " / " + questionCount,
            muted = true,
            bottom = 4,
            tag = "elder-progress-label",
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(p.ground3, RoundedCornerShape(99.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(6.dp)
                    .background(p.turmeric, RoundedCornerShape(99.dp)),
            )
        }
        Spacer(Modifier.height(14.dp))
        WfH2(question.title.text(lang))
        WfParagraph(
            text = content.askAloud.text(lang),
            muted = true,
            bottom = 4,
        )
        Text(
            text = question.ask.text(lang),
            color = p.flour,
            fontFamily = MaterialTheme.typography.headlineMedium.fontFamily,
            fontSize = 20.8.sp,
            lineHeight = 28.08.sp,
            modifier = Modifier
                .padding(bottom = 12.dp)
                .fidelityTag("elder-question"),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = content.answerGroupLabel.text(lang) },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            (-2..2).forEachIndexed { index, value ->
                ElderScaleButton(
                    tag = "elder-answer.$value",
                    glyph = content.scaleGlyphs[index],
                    label = content.ratingLabels[index].text(lang),
                    background = wfHexColor(content.scaleColours[index]),
                    foreground = wfHexColor(content.scaleTextColours[index]),
                    selected = isAnswered && rating == value,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onAnswerChange(
                            question.id,
                            (answer ?: InterviewAnswer()).copy(
                                answered = true,
                                rating = value,
                            ),
                        )
                    },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        ElderCannotSayButton(
            text = content.cannotSay.text(lang),
            selected = isAnswered && rating == null,
            onClick = {
                onAnswerChange(
                    question.id,
                    (answer ?: InterviewAnswer()).copy(
                        answered = true,
                        rating = null,
                    ),
                )
            },
        )

        if (isAnswered && rating != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = content.memoryClarity.text(lang),
                color = p.flour,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            WfSegments(
                labels = content.confidenceLabels.map { it.text(lang) },
                selected = (answer?.confidence ?: 0) - 1,
                tagPrefix = "elder-confidence",
                onSelect = { selected ->
                    onAnswerChange(
                        question.id,
                        requireNotNull(answer).copy(confidence = selected + 1),
                    )
                },
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = content.storyLabel.text(lang),
                color = p.flour,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            ElderStoryField(
                lang = lang,
                interviewId = interview.id,
                questionId = question.id,
                persistedStory = answer?.story.orEmpty(),
                accessibilityLabel = content.storyLabel.text(lang),
                onStoryChange = { story ->
                    onAnswerChange(
                        question.id,
                        requireNotNull(answer).copy(story = story),
                    )
                },
            )
        }

        Row(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            WfButton(
                text = content.back.text(lang),
                modifier = Modifier.weight(1f),
                tag = "elder-back",
                onClick = onBack,
            )
            val forwardText = if (last) {
                content.finish.text(lang)
            } else if (isAnswered) {
                content.next.text(lang)
            } else {
                content.skip.text(lang)
            }
            val forwardEnabled = !(last && !isAnswered && EldersDomain.answeredCount(interview) < 3)
            WfButton(
                text = forwardText,
                modifier = Modifier.weight(1f),
                filled = true,
                enabled = forwardEnabled,
                tag = "elder-forward",
                onClick = { onForward(answer?.takeIf { it.answered }) },
            )
        }
    }
}

@Composable
private fun ElderScaleButton(
    tag: String,
    glyph: String,
    label: String,
    background: Color,
    foreground: Color,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val p = PrototypeTheme.palette
    FidelityTouchTarget(
        tag = tag,
        modifier = modifier,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .background(background, RoundedCornerShape(12.dp))
                .border(
                    width = if (selected) 2.dp else 2.dp,
                    color = if (selected) p.flour else Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 2.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = glyph,
                color = foreground,
                fontWeight = FontWeight.Bold,
                fontSize = 17.6.sp,
            )
            Text(
                text = label,
                color = foreground,
                fontSize = 10.56.sp,
                lineHeight = 11.62.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ElderCannotSayButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val p = PrototypeTheme.palette
    FidelityTouchTarget(
        tag = "elder-cannot-say",
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 46.dp)
                .border(
                    1.dp,
                    if (selected) p.turmeric else p.line,
                    RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 18.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = text, color = p.flour, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ElderStoryField(
    lang: String,
    interviewId: String,
    questionId: String,
    persistedStory: String,
    accessibilityLabel: String,
    onStoryChange: (String) -> Unit,
) {
    val p = PrototypeTheme.palette
    val locale = if (lang == "ta") Locale("ta", "IN") else Locale.ENGLISH
    var value by rememberSaveable(
        interviewId,
        questionId,
        stateSaver = TextFieldValue.Saver,
    ) {
        mutableStateOf(TextFieldValue(EldersDomain.graphemePrefix(persistedStory, locale = locale)))
    }

    BasicTextField(
        value = value,
        onValueChange = { proposed ->
            val limited = TextSafety.limitTextFieldValue(
                proposed,
                EldersDomain.MAX_STORY_GRAPHEMES,
                locale,
            )
            value = limited
            onStoryChange(limited.text)
        },
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = p.flour),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .fidelityTag("elder-story")
            .semantics { contentDescription = accessibilityLabel }
            .background(p.ground, RoundedCornerShape(10.dp))
            .border(1.dp, p.line, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

private fun wfHexColor(hex: String): Color {
    val raw = hex.removePrefix("#")
    val expanded = if (raw.length == 3) {
        buildString {
            raw.forEach { digit ->
                append(digit)
                append(digit)
            }
        }
    } else {
        raw
    }
    return Color(0xFF000000L or expanded.toLong(16))
}

@Composable
private fun ElderSetupScreen(
    lang: String,
    content: ElderSliceBContent,
    atLimit: Boolean,
    onBack: () -> Unit,
    onCreateInterview: (String, Int, Int, String) -> Unit,
) {
    var nickname by remember { mutableStateOf("") }
    var decade by remember { mutableStateOf(1) }
    var place by remember { mutableStateOf(0) }

    WorkflowPage {
        WfH1(content.newTitle.text(lang))
        WfCard(tag = "screen.first-card") {
            Text(
                text = content.nicknameLabel.text(lang),
                color = PrototypeTheme.palette.flour,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            WfInput(
                value = nickname,
                placeholder = content.nicknamePlaceholder.text(lang),
                minHeight = 48,
                onValueChange = { value ->
                    if (value.length <= EldersDomain.MAX_NICKNAME_CHARS) {
                        nickname = value
                    }
                },
                accessibilityLabel = content.nicknameLabel.text(lang),
            )
            Spacer(Modifier.height(12.dp))
            ElderSelect(
                label = content.decadeLabel.text(lang),
                options = content.decades,
                selected = decade,
                onSelect = { decade = it },
            )
            Spacer(Modifier.height(12.dp))
            ElderSelect(
                label = content.placeLabel.text(lang),
                options = content.places.map { it.text(lang) },
                selected = place,
                onSelect = { place = it },
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WfButton(
                    text = content.start.text(lang),
                    modifier = Modifier.weight(1f),
                    filled = true,
                    enabled = !atLimit,
                    tag = "elder-setup-start",
                    onClick = {
                        onCreateInterview(
                            nickname,
                            decade,
                            place,
                            content.defaultNickname.text(lang),
                        )
                    },
                )
                WfButton(
                    text = content.back.text(lang),
                    modifier = Modifier.weight(1f),
                    tag = "elder-setup-back",
                    onClick = onBack,
                )
            }
        }
        if (atLimit) {
            WfNote(content.limitMessage.text(lang))
        }
        WfNote(content.consent.text(lang))
    }
}

@Composable
fun ClassPoolPrototypeScreen(
    lang: String,
    appState: AppState = AppState(language = lang),
    onImportCodes: (String) -> Unit = {},
    onSampleChange: (Boolean) -> Unit = {},
    onGroupByChange: (ClassGroupBy) -> Unit = {},
    onPoolUsed: () -> Unit = {},
) {
    val context = LocalContext.current
    val elders = remember(context) { ContentRepository(context).loadElders() }
    val copy = ClassPoolCopy
    val clipboard = LocalClipboardManager.current
    var code by rememberSaveable { mutableStateOf("") }
    var lastFound by rememberSaveable { mutableStateOf(-1) }
    var lastAdded by rememberSaveable { mutableStateOf(0) }
    var lastDuplicates by rememberSaveable { mutableStateOf(0) }
    var lastRejected by rememberSaveable { mutableStateOf(0) }

    val records = ClassPoolDomain.allInterviewRecords(appState)
    val plans = ClassPoolDomain.allPlanRecords(appState)
    val mine = appState.interviews.filter { EldersDomain.answeredCount(it) >= 3 }
    val groupBy = appState.classPool.groupBy

    WorkflowPage {
        WfH1(copy.title.text(lang))
        WfParagraph(copy.intro.text(lang), tag = "screen.body.primary")
        WfCard(tag = "screen.first-card") {
            WfH2(copy.addCodes.text(lang))
            WfInput(
                value = code,
                placeholder = copy.pastePlaceholder.text(lang),
                minHeight = 64,
                onValueChange = { code = it },
                accessibilityLabel = copy.pastePlaceholder.text(lang),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WfButton(
                    text = copy.add.text(lang),
                    filled = true,
                    tag = "screen.primary-button",
                    onClick = {
                        val summary = ClassPoolDomain.importCodes(code, appState.classPool).summary
                        lastFound = summary.found
                        lastAdded = summary.added
                        lastDuplicates = summary.duplicates
                        lastRejected = summary.rejected
                        if (summary.found > 0) {
                            onImportCodes(code)
                            code = ""
                        }
                    },
                )
                WfButton(
                    text = if (appState.classPool.sample) {
                        copy.removeSample.text(lang)
                    } else {
                        copy.trySample.text(lang)
                    },
                    onClick = { onSampleChange(!appState.classPool.sample) },
                )
            }
            if (lastFound >= 0) {
                Spacer(Modifier.height(8.dp))
                val statusText = if (lastFound == 0) {
                    if (lang == "ta") {
                        "செல்லுபடியாகும் குறியீடு கிடைக்கவில்லை."
                    } else {
                        "No valid codes found. Each code starts with KK1. or KP1."
                    }
                } else if (lang == "ta") {
                    "சேர்க்கப்பட்டது " + lastAdded + ", மீண்டும் வந்தவை " + lastDuplicates +
                        ", செல்லாதவை " + lastRejected + "."
                } else {
                    "Added " + lastAdded + ", duplicates " + lastDuplicates +
                        ", rejected " + lastRejected + "."
                }
                Text(
                    text = statusText,
                    color = PrototypeTheme.palette.faint,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .semantics { liveRegion = LiveRegionMode.Polite }
                        .fidelityTag("class-import-status"),
                )
            }
            if (appState.classPool.sample) {
                Spacer(Modifier.height(8.dp))
                ClassWarningNote(copy.sampleWarning.text(lang))
            }
        }

        if (records.isEmpty() && plans.isEmpty()) {
            WfNote(copy.noPooled.text(lang))
        } else {
            if (records.isNotEmpty()) {
                WfCard(tag = "class-memory-summary") {
                    WfH2(
                        if (lang == "ta") {
                            "நினைவுகள்: " + records.size + " நேர்காணல்கள்"
                        } else {
                            "Memories: " + records.size + " interviews"
                        },
                    )
                    Text(
                        text = copy.splitBy.text(lang),
                        color = PrototypeTheme.palette.flour,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                    WfSegments(
                        labels = copy.groupLabels.map { it.text(lang) },
                        selected = groupBy.ordinal,
                        tagPrefix = "class-group",
                        onSelect = { index ->
                            onGroupByChange(ClassGroupBy.entries[index])
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                    ClassLegend(lang)
                }

                val strongest = ClassPoolDomain.strongestSharedMemories(records)
                if (strongest.isNotEmpty()) {
                    WfCard(tag = "class-strongest") {
                        WfH2(copy.strongest.text(lang))
                        strongest.forEach { shared ->
                            val question = elders.elderQuestions[shared.questionIndex]
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "• " + question.title.text(lang),
                                    color = PrototypeTheme.palette.flour,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = classMean(shared.row.mean) + " · n=" + shared.row.n,
                                    color = PrototypeTheme.palette.faint,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            ClassVerdictChip(question.verdict, lang)
                        }
                        if (strongest.any { elders.elderQuestions[it.questionIndex].verdict == "confounded" }) {
                            Spacer(Modifier.height(8.dp))
                            ClassWarningNote(copy.agreementWarning.text(lang))
                        }
                    }
                }

                elders.elderQuestions.forEachIndexed { questionIndex, question ->
                    val rows = ClassPoolDomain.aggregate(records, questionIndex, groupBy)
                        .filter { it.n > 0 }
                    WfCard(tag = "class-question." + question.id) {
                        WfH3(question.title.text(lang))
                        ClassVerdictChip(question.verdict, lang)
                        Spacer(Modifier.height(8.dp))
                        if (rows.isEmpty()) {
                            WfParagraph(copy.noAnswers.text(lang), muted = true, bottom = 0)
                        } else {
                            rows.forEachIndexed { rowIndex, row ->
                                ClassDistributionRow(
                                    lang = lang,
                                    label = classGroupLabel(
                                        lang = lang,
                                        groupBy = groupBy,
                                        rowLabel = row.label,
                                        elders = elders,
                                    ),
                                    row = row,
                                )
                            }
                            if (rows.any { it.n < 8 }) {
                                WfParagraph(
                                    text = copy.smallGroupWarning.text(lang),
                                    muted = true,
                                    bottom = 0,
                                )
                            }
                        }
                    }
                }
            }

            if (plans.isNotEmpty()) {
                val summary = ClassPoolDomain.planSummary(plans)
                WfCard(tag = "class-plan-summary") {
                    WfH2(copy.councilPlans.text(lang) + ": " + plans.size)
                    ClassPoolCopy.planOptions.forEachIndexed { index, option ->
                        val count = summary.optionCounts[index]
                        val percent = ((count.toDouble() / plans.size.toDouble()) * 100.0 + 0.5).toInt()
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = option.title.text(lang),
                                color = PrototypeTheme.palette.flour,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = percent.toString() + "%",
                                color = PrototypeTheme.palette.faint,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    WfParagraph(
                        text = if (lang == "ta") {
                            summary.plansLeavingSomeoneUnprotected.toString() + " / " + plans.size +
                                " திட்டங்கள் குறைந்தது ஒரு குழுவுக்கு எந்தப் பாதுகாப்பும் தரவில்லை."
                        } else {
                            summary.plansLeavingSomeoneUnprotected.toString() + " of " + plans.size +
                                " plans left at least one group with no protection at all."
                        },
                    )
                    WfParagraph(copy.planCompare.text(lang), bottom = 0)
                }
            }

            if (mine.isNotEmpty()) {
                WfCard(tag = "class-my-codes") {
                    WfH2(copy.myCodes.text(lang))
                    mine.forEach { interview ->
                        val shareCode = EldersDomain.exportShareCode(interview)
                        Text(
                            text = interview.nickname,
                            color = PrototypeTheme.palette.faint,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                        )
                        Text(
                            text = shareCode,
                            color = PrototypeTheme.palette.flour,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = interview.nickname }
                                .background(PrototypeTheme.palette.ground, RoundedCornerShape(10.dp))
                                .border(1.dp, PrototypeTheme.palette.line, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                        )
                        Spacer(Modifier.height(6.dp))
                        WfButton(
                            text = copy.copy.text(lang),
                            tag = "class-copy." + interview.id,
                            onClick = {
                                clipboard.setText(AnnotatedString(shareCode))
                                onPoolUsed()
                            },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
        WfFooter(lang)
    }
}

private fun classMean(mean: Double?): String {
    if (mean == null) return "—"
    val value = BigDecimal.valueOf(mean)
        .setScale(1, RoundingMode.HALF_UP)
        .toPlainString()
    return if (mean > 0.0) "+" + value else value
}

private fun classGroupLabel(
    lang: String,
    groupBy: ClassGroupBy,
    rowLabel: String,
    elders: edu.gascnagercoil.kaalakolam.content.EldersAsset,
): String = when (groupBy) {
    ClassGroupBy.ALL -> ClassPoolCopy.groupLabels[0].text(lang)
    ClassGroupBy.PLACE -> {
        val index = rowLabel.removePrefix("p").toIntOrNull() ?: 0
        elders.sliceB.places.getOrElse(index) { elders.sliceB.places.first() }.text(lang)
    }
    ClassGroupBy.DECADE -> {
        val index = rowLabel.removePrefix("d").toIntOrNull() ?: 0
        elders.sliceB.decades.getOrElse(index) { elders.sliceB.decades.first() }
    }
}

@Composable
private fun ClassLegend(lang: String) {
    val colours = listOf("#1f6f8b", "#6bb3c4", "#d8d2c4", "#eba46d", "#c8452c")
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(ClassPoolCopy.less.text(lang), color = PrototypeTheme.palette.faint, fontSize = 11.5.sp)
        colours.forEach { colour ->
            Box(
                modifier = Modifier
                    .size(15.dp)
                    .background(wfHexColor(colour), RoundedCornerShape(3.dp)),
            )
        }
        Text(ClassPoolCopy.more.text(lang), color = PrototypeTheme.palette.faint, fontSize = 11.5.sp)
    }
    Text(
        text = ClassPoolCopy.colourMeaning.text(lang),
        color = PrototypeTheme.palette.faint,
        fontSize = 11.5.sp,
        lineHeight = 16.sp,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun ClassVerdictChip(verdict: String, lang: String) {
    val text = ClassPoolCopy.verdicts[verdict]?.text(lang) ?: verdict
    val border = if (verdict == "confounded") PrototypeTheme.palette.vermilion else PrototypeTheme.palette.line
    Text(
        text = text,
        color = PrototypeTheme.palette.flour,
        fontSize = 11.2.sp,
        lineHeight = 15.sp,
        modifier = Modifier
            .border(1.dp, border, RoundedCornerShape(99.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun ClassWarningNote(text: String) {
    val p = PrototypeTheme.palette
    Text(
        text = text,
        color = p.flour,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .background(p.vermilion.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
            .border(1.dp, p.vermilion, RoundedCornerShape(10.dp))
            .padding(10.dp),
    )
}

@Composable
private fun ClassDistributionRow(
    lang: String,
    label: String,
    row: M2Domain.AggregateRow,
) {
    val p = PrototypeTheme.palette
    val glyphs = listOf("−−", "−", "=", "+", "++")
    val colours = listOf("#1f6f8b", "#6bb3c4", "#d8d2c4", "#eba46d", "#c8452c")
    val description = ClassPoolCopy.distributionPrefix.text(lang) +
        glyphs.indices.joinToString(", ") { index -> glyphs[index] + " " + row.counts[index] }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            text = label,
            color = p.flour,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.widthIn(min = 76.dp, max = 116.dp),
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .height(18.dp)
                .semantics { contentDescription = description }
                .background(p.ground3, RoundedCornerShape(5.dp))
                .border(1.dp, p.line, RoundedCornerShape(5.dp)),
        ) {
            row.counts.forEachIndexed { index, count ->
                if (count > 0) {
                    Box(
                        modifier = Modifier
                            .weight(count.toFloat())
                            .fillMaxSize()
                            .background(wfHexColor(colours[index])),
                    )
                }
            }
        }
        Text(
            text = classMean(row.mean) + " · n=" + row.n,
            color = p.faint,
            fontSize = 11.2.sp,
            modifier = Modifier.widthIn(min = 60.dp),
        )
    }
}

@Composable
fun CouncilPrototypeScreen(lang: String) {
    var scenario by remember { mutableStateOf(0) }
    var role by remember { mutableStateOf<String?>(null) }
    WorkflowPage {
        WfH1(wfText(lang, "The Kadalur council", "கடலூர் ஊர்சபை"))
        WfParagraph(
            wfText(
                lang,
                "Kadalur is an invented village on a low coast, with four groups of families. A storm season is coming. You have a budget of 12 coins and cannot buy everything. Hold the meeting in a group of three to five, with each person taking a role, and agree on one plan.",
                "கடலூர் தாழ்வான கடற்கரையில் உள்ள ஒரு கற்பனை கிராமம்; நான்கு குழுக் குடும்பங்கள் உள்ளன. புயல் பருவம் வருகிறது. உங்களிடம் 12 நாணயங்கள் உள்ளன; எல்லாவற்றையும் வாங்க முடியாது. மூன்று முதல் ஐந்து பேர் குழுவாக, ஒவ்வொருவரும் ஒரு பாத்திரம் ஏற்று, ஒரு திட்டத்தில் உடன்படுங்கள்.",
            ),
            tag = "screen.body.primary",
        )
        WfCard(tag = "screen.first-card") {
            WfH2(wfText(lang, "Your role", "உங்கள் பாத்திரம்"))
            role?.let {
                WfNote(it)
            }
            WfButton(
                wfText(lang, "Deal me a role", "ஒரு பாத்திரம் வழங்கு"),
                tag = "screen.primary-button",
                onClick = {
                    role = wfText(
                        lang,
                        "School teacher — the school is also the only shelter.",
                        "பள்ளி ஆசிரியர் — பள்ளியே ஒரே பாதுகாப்பு மையம்.",
                    )
                },
            )
        }
        WfCard {
            WfH2(wfText(lang, "Which climate are you planning for?", "எந்தக் காலநிலைக்குத் திட்டமிடுகிறீர்கள்?"))
            val labels = listOf(
                wfText(lang, "Today’s climate", "இன்றைய காலநிலை"),
                wfText(lang, "2050, strong emission cuts", "2050, வலுவான உமிழ்வுக் குறைப்பு"),
                wfText(lang, "2050, high emissions", "2050, அதிக உமிழ்வு"),
            )
            labels.forEachIndexed { index, label ->
                WfButton(
                    label,
                    modifier = Modifier.fillMaxWidth(),
                    selected = scenario == index,
                    onClick = { scenario = index },
                )
                if (index != labels.lastIndex) Spacer(Modifier.height(6.dp))
            }
            Spacer(Modifier.height(8.dp))
            WfParagraph(
                wfText(
                    lang,
                    "The multipliers on storm hazard are illustrative, chosen to show how the plan changes. Real changes differ by hazard and place.",
                    "புயல் அபாயத்தின் பெருக்கிகள் விளக்கத்துக்காக எடுக்கப்பட்டவை. உண்மையான மாற்றம் அபாயத்துக்கும் இடத்துக்கும் ஏற்ப வேறுபடும்.",
                ),
                muted = true,
                bottom = 0,
            )
        }
        WfCard {
            WfH2(wfText(lang, "Budget: 12 / 12", "நிதி: 12 / 12"))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(12) {
                    Box(
                        Modifier.weight(1f).height(14.dp)
                            .background(PrototypeTheme.palette.ground3, RoundedCornerShape(4.dp))
                            .border(1.dp, PrototypeTheme.palette.line, RoundedCornerShape(4.dp)),
                    )
                }
            }
        }
    }
}

private data class PredictionCopy(
    val id: String,
    val questionEn: String,
    val questionTa: String,
    val truth: String,
    val why: String,
)

private val predictionCopyById = listOf(
    PredictionCopy(
        "ocean",
        "Of the extra heat trapped since the 1970s, what percentage went into the oceans?",
        "1970-களுக்குப் பிறகு சேர்ந்த கூடுதல் வெப்பத்தில் எத்தனை சதவீதம் பெருங்கடல்களுக்குச் சென்றது?",
        "About 91% (IPCC AR6).",
        "The ocean is the planet’s heat store. That is why seas rise as water expands, and why marine heatwaves matter.",
    ),
    PredictionCopy(
        "co2",
        "Before industry, CO₂ was about 280 ppm. What is it today?",
        "தொழிற்புரட்சிக்கு முன் CO₂ சுமார் 280 ppm. இன்று எவ்வளவு?",
        "About 430 ppm, rising by roughly 2–3 ppm a year. Check NOAA’s live figure.",
        "The rise is far faster than natural changes recorded in ice cores, and its carbon signature points to fossil fuels.",
    ),
    PredictionCopy(
        "warm",
        "How much has global average surface temperature risen since 1850–1900?",
        "1850–1900-க்குப் பிறகு உலக சராசரி மேற்பரப்பு வெப்பநிலை எவ்வளவு உயர்ந்துள்ளது?",
        "About 1.1 °C for 2011–2020 (IPCC AR6). Recent single years have been higher.",
        "A small change in a global average moves the whole distribution of weather, including the hot tail.",
    ),
    PredictionCopy(
        "nogh",
        "Without any natural greenhouse effect, Earth’s average surface temperature would be about… (today’s is about +15 °C)",
        "இயற்கையான பசுமைக்குடில் விளைவே இல்லாவிட்டால் பூமியின் சராசரி மேற்பரப்பு வெப்பநிலை சுமார்… (இன்றைய அளவு சுமார் +15 °C)",
        "Roughly −18 °C (a standard textbook estimate).",
        "The greenhouse effect is natural and essential. Climate change is about strengthening it.",
    ),
    PredictionCopy(
        "vapour",
        "For each 1 °C of warming, air can hold about how much more water vapour?",
        "ஒவ்வொரு 1 °C வெப்ப உயர்வுக்கும் காற்று சுமார் எவ்வளவு கூடுதல் நீராவியைத் தாங்கும்?",
        "About 7% (Clausius–Clapeyron relation).",
        "This is one reason heavy downpours intensify in a warmer world.",
    ),
    PredictionCopy(
        "sea",
        "Global average sea-level rise between 1901 and 2018?",
        "1901 முதல் 2018 வரை உலக சராசரிக் கடல்மட்ட உயர்வு?",
        "About 20 cm (IPCC AR6).",
        "It sounds small, yet it raises storm-surge reach and floods low coasts far more often.",
    ),
    PredictionCopy(
        "india",
        "How much has India’s average surface temperature risen since 1901?",
        "1901-க்குப் பிறகு இந்தியாவின் சராசரி மேற்பரப்பு வெப்பநிலை எவ்வளவு உயர்ந்துள்ளது?",
        "About 0.7 °C for 1901–2018 (MoES 2020 assessment).",
        "Regional change differs from the global figure, so local records still matter.",
    ),
    PredictionCopy(
        "nat",
        "How much of the warming since 1850–1900 is due to natural factors such as the Sun and volcanoes?",
        "1850–1900-க்குப் பிறகான வெப்ப உயர்வில் சூரியன், எரிமலை போன்ற இயற்கைக் காரணிகளால் எவ்வளவு?",
        "Close to zero, about −0.1 to +0.1 °C (IPCC AR6).",
        "Natural factors push climate around, but they do not explain the sustained rise.",
    ),
).associateBy { it.id }

@Composable
fun PredictPrototypeScreen(
    lang: String,
    answers: Map<String, PredictionState> = emptyMap(),
    onAnswer: (String, PredictionState) -> Unit = { _, _ -> },
    onReset: () -> Unit = {},
) {
    val specs = M2Domain.predictionSpecs
    val answered = specs.mapNotNull { spec -> answers[spec.id]?.let { spec to it } }
    val next = M2Domain.nextPredictionSpec(answers.keys)
    var guess by remember(next?.id) { mutableStateOf(next?.initial ?: 0.0) }
    var confidence by remember(next?.id) { mutableStateOf(0) }

    WorkflowPage {
        WfH1(wfText(lang, "Predict and calibrate", "கணித்து அளவிடு"))
        WfParagraph(
            wfText(
                lang,
                "Move the slider to your best guess, then say how sure you are. The point is not to score high. It is to find out whether feeling certain and being right go together for you.",
                "உங்கள் சிறந்த ஊகத்துக்கு ஸ்லைடரை நகர்த்தி, எவ்வளவு உறுதி என்று சொல்லுங்கள். அதிக மதிப்பெண் பெறுவது நோக்கமல்ல; உறுதியாக உணர்வதும் சரியாக இருப்பதும் உங்களுக்கு ஒன்றாக வருகின்றனவா என்று அறிவதே நோக்கம்.",
            ),
            tag = "screen.body.primary",
        )

        answered.forEachIndexed { index, (spec, answer) ->
            val copy = predictionCopyById.getValue(spec.id)
            WfCard(tag = if (index == 0) "screen.first-card" else null) {
                WfH3(wfText(lang, copy.questionEn, copy.questionTa))
                val inRange = answer.hit
                Text(
                    wfText(
                        lang,
                        if (inRange) "In range" else "Outside range",
                        if (inRange) "சரி" else "தவறு",
                    ),
                    color = if (inRange) PrototypeTheme.palette.ink else PrototypeTheme.palette.flour,
                    fontSize = 12.48.sp,
                    modifier = Modifier
                        .background(
                            if (inRange) PrototypeTheme.palette.sea else PrototypeTheme.palette.vermilion,
                            RoundedCornerShape(99.dp),
                        )
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                )
                Spacer(Modifier.height(8.dp))
                WfParagraph(
                    wfText(lang, "You said ", "உங்கள் ஊகம் ") +
                        M2Domain.formatPredictionValue(spec, answer.guess) +
                        " · " +
                        when (answer.confidence) {
                            3 -> wfText(lang, "Certain", "முழு உறுதி")
                            2 -> wfText(lang, "Fairly sure", "ஓரளவு உறுதி")
                            else -> wfText(lang, "Guessing", "ஊகம் மட்டும்")
                        },
                    bottom = 8,
                )
                Text(
                    copy.truth,
                    color = PrototypeTheme.palette.flour,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                WfParagraph(copy.why, muted = true, bottom = 0)
            }
        }

        if (next != null) {
            val copy = predictionCopyById.getValue(next.id)
            WfCard(tag = if (answered.isEmpty()) "screen.first-card" else null) {
                Text(
                    (specs.indexOf(next) + 1).toString() + " / " + specs.size,
                    color = PrototypeTheme.palette.faint,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(4.dp))
                WfH2(wfText(lang, copy.questionEn, copy.questionTa))
                Text(
                    M2Domain.formatPredictionValue(next, guess),
                    color = PrototypeTheme.palette.flour,
                    fontFamily = MaterialTheme.typography.headlineLarge.fontFamily,
                    fontSize = 35.2.sp,
                    lineHeight = 35.2.sp,
                )
                Spacer(Modifier.height(8.dp))
                val sliderSteps = (
                    kotlin.math.round((next.max - next.min) / next.step).toInt() - 1
                ).coerceAtLeast(0)
                Slider(
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    value = guess.toFloat(),
                    onValueChange = { raw ->
                        guess = M2Domain.snapPredictionGuess(next, raw.toDouble())
                    },
                    valueRange = next.min.toFloat()..next.max.toFloat(),
                    steps = sliderSteps,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    wfText(lang, "How sure are you?", "எவ்வளவு உறுதி?"),
                    color = PrototypeTheme.palette.flour,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                WfSegments(
                    listOf(
                        wfText(lang, "Guessing", "ஊகம் மட்டும்"),
                        wfText(lang, "Fairly sure", "ஓரளவு உறுதி"),
                        wfText(lang, "Certain", "முழு உறுதி"),
                    ),
                    selected = confidence - 1,
                    onSelect = { confidence = it + 1 },
                )
                Spacer(Modifier.height(14.dp))
                WfButton(
                    wfText(lang, "Lock in and reveal", "பூட்டி விடையைப் பார்"),
                    tag = "screen.primary-button",
                    filled = true,
                    enabled = confidence in 1..3,
                    onClick = {
                        val snapped = M2Domain.snapPredictionGuess(next, guess)
                        onAnswer(
                            next.id,
                            PredictionState(
                                guess = snapped,
                                confidence = confidence,
                                hit = M2Domain.predictionHit(next, snapped),
                            ),
                        )
                    },
                )
            }
        }

        if (answered.size >= 3) {
            val domainAnswers = answered.map { (spec, answer) ->
                M2Domain.PredictionAnswer(
                    id = spec.id,
                    guess = answer.guess,
                    confidence = answer.confidence,
                    hit = answer.hit,
                )
            }
            val buckets = M2Domain.calibrationBuckets(domainAnswers)
            WfCard {
                WfH2(wfText(lang, "Your calibration", "உங்கள் அளவீடு"))
                buckets.forEach { bucket ->
                    val label = when (bucket.confidence) {
                        3 -> wfText(lang, "Certain", "முழு உறுதி")
                        2 -> wfText(lang, "Fairly sure", "ஓரளவு")
                        else -> wfText(lang, "Guessing", "ஊகம்")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            label,
                            color = PrototypeTheme.palette.flour,
                            modifier = Modifier.widthIn(min = 78.dp),
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .background(PrototypeTheme.palette.ground3, RoundedCornerShape(99.dp)),
                        ) {
                            if (bucket.n > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(bucket.correct.toFloat() / bucket.n)
                                        .height(8.dp)
                                        .background(PrototypeTheme.palette.sea, RoundedCornerShape(99.dp)),
                                )
                            }
                        }
                        Text(
                            bucket.correct.toString() + " / " + bucket.n,
                            color = PrototypeTheme.palette.faint,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                val calibrationMessage = when (M2Domain.certainCalibration(domainAnswers)) {
                    M2Domain.CertainCalibration.OVERCONFIDENT -> wfText(
                        lang,
                        "Your “certain” answers were right less than 60% of the time. That is overconfidence, and nearly everyone shows it. Confidence is a feeling, not a measurement.",
                        "உங்கள் “முழு உறுதி” பதில்களில் 60%-க்கும் குறைவே சரி. இது அதீத நம்பிக்கை; கிட்டத்தட்ட அனைவருக்கும் இது உண்டு. நம்பிக்கை ஒரு உணர்வு; அளவீடு அல்ல.",
                    )
                    M2Domain.CertainCalibration.PERFECT_SO_FAR -> wfText(
                        lang,
                        "Every answer you were certain about held up. With so few questions, treat that as a good sign, not a settled fact.",
                        "நீங்கள் முழு உறுதி என்ற ஒவ்வொரு பதிலும் சரியாக இருந்தது. இவ்வளவு குறைந்த கேள்விகளில் இதை நல்ல அறிகுறியாக மட்டும் கொள்ளுங்கள்.",
                    )
                    M2Domain.CertainCalibration.KEEP_GOING -> wfText(
                        lang,
                        "Keep going: calibration needs a few confident answers to judge. Try committing to “certain” when you truly are.",
                        "தொடருங்கள்: அளவீட்டை மதிப்பிட சில உறுதியான பதில்கள் தேவை. உண்மையிலேயே உறுதியாக இருக்கும்போது “முழு உறுதி” எனத் தேர்ந்தெடுங்கள்.",
                    )
                }
                WfParagraph(calibrationMessage, bottom = 0)
            }
        }

        if (next == null) {
            Row(Modifier.padding(vertical = 4.dp)) {
                WfButton(
                    wfText(lang, "Try again", "மீண்டும் முயல்"),
                    onClick = onReset,
                )
            }
        }
        WfFooter(lang)
    }
}

@Composable
private fun PrototypeRange(fraction: Float) {
    val p = PrototypeTheme.palette
    Canvas(Modifier.fillMaxWidth().height(36.dp)) {
        val y = size.height / 2f
        val start = 4.dp.toPx()
        val end = size.width - 4.dp.toPx()
        val x = start + (end - start) * fraction.coerceIn(0f, 1f)
        drawLine(p.line, Offset(start, y), Offset(end, y), strokeWidth = 2.dp.toPx())
        drawLine(p.turmeric, Offset(start, y), Offset(x, y), strokeWidth = 6.dp.toPx())
        drawCircle(p.turmeric, radius = 7.dp.toPx(), center = Offset(x, y))
    }
}
