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
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.gascnagercoil.kaalakolam.domain.M2Domain
import edu.gascnagercoil.kaalakolam.domain.PredictionState
import edu.gascnagercoil.kaalakolam.ui.theme.PrototypeTheme

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
    onClick: () -> Unit = {},
) {
    val p = PrototypeTheme.palette
    val useFill = filled || selected
    val bg = if (useFill) p.turmeric.copy(alpha = if (enabled) 1f else .45f) else Color.Transparent
    val border = if (useFill) p.turmeric else p.line
    val fg = if (useFill) Color(0xFF17120A).copy(alpha = if (enabled) 1f else .45f) else p.flour
    val effectiveTag = tag ?: "button.${text.hashCode()}"
    FidelityTouchTarget(
        tag = effectiveTag,
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .fidelityTag(effectiveTag)
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
                    tag = "$effectiveTag.label",
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
                tag = "segment.$index",
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
) {
    val p = PrototypeTheme.palette
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = p.flour),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight.dp)
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
                Modifier.widthIn(max = 234.dp)
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
fun ElderPrototypeScreen(lang: String) {
    WorkflowPage {
        WfH1(wfText(lang, "Elder interviews", "மூத்தோர் நேர்காணல்"))
        WfParagraph(
            wfText(
                lang,
                "Sit with someone who has lived in your place for decades. Ask permission first. Listen more than you talk. You will record how things changed, how sure they are, and a story if they offer one.",
                "பல பத்தாண்டுகள் உங்கள் ஊரில் வாழ்ந்தவருடன் அமருங்கள். முதலில் அனுமதி கேளுங்கள். பேசுவதைவிட அதிகம் கேளுங்கள். மாற்றங்கள், அவர் எவ்வளவு உறுதியாகச் சொல்கிறார், அவர் விரும்பினால் ஒரு கதை ஆகியவற்றைப் பதிவு செய்வீர்கள்.",
            ),
            tag = "screen.body.primary",
        )
        WfButton(wfText(lang, "Start an interview", "நேர்காணலைத் தொடங்கு"), filled = true, tag = "screen.primary-button")
        WfNote(wfText(lang, "No interviews yet. Try asking your grandparent first.", "இன்னும் நேர்காணல்கள் இல்லை. முதலில் உங்கள் தாத்தா அல்லது பாட்டியிடம் கேட்டுப் பாருங்கள்."))
        WfFooter(lang)
    }
}

@Composable
fun ClassPoolPrototypeScreen(lang: String) {
    var code by remember { mutableStateOf("") }
    var sample by remember { mutableStateOf(false) }
    WorkflowPage {
        WfH1(wfText(lang, "Class pool", "வகுப்புத் தொகுப்பு"))
        WfParagraph(
            wfText(
                lang,
                "Everyone shares a code. One person pastes all the codes here. No server is needed: send codes by WhatsApp, Bluetooth or on paper.",
                "ஒவ்வொருவரும் ஒரு குறியீட்டைப் பகிர்கிறார்கள். ஒருவர் அனைத்தையும் இங்கே ஒட்டுகிறார். சேவையகம் தேவையில்லை: வாட்ஸ்அப், புளூடூத் அல்லது காகிதத்தில் குறியீடுகளை அனுப்பலாம்.",
            ),
            tag = "screen.body.primary",
        )
        WfCard(tag = "screen.first-card") {
            WfH2(wfText(lang, "Add codes", "குறியீடுகளைச் சேர்"))
            WfInput(
                code,
                wfText(lang, "Paste codes here (each starts with KK1. or KP1.)", "குறியீடுகளை இங்கே ஒட்டுங்கள் (ஒவ்வொன்றும் KK1. அல்லது KP1. எனத் தொடங்கும்)"),
                minHeight = 64,
                onValueChange = { code = it },
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WfButton(wfText(lang, "Add", "சேர்"), filled = true, tag = "screen.primary-button")
                WfButton(
                    if (sample) wfText(lang, "Remove sample data", "மாதிரித் தரவை நீக்கு")
                    else wfText(lang, "Try sample data (synthetic)", "மாதிரித் தரவைப் பார் (செயற்கை)"),
                    onClick = { sample = !sample },
                )
            }
        }
        WfNote(wfText(lang, "No pooled interviews yet. Finish an interview and copy its code, or try the sample data.", "தொகுக்கப்பட்ட நேர்காணல்கள் இல்லை. ஒரு நேர்காணலை முடித்து அதன் குறியீட்டை நகலெடுங்கள், அல்லது மாதிரித் தரவை முயலுங்கள்."))
        WfFooter(lang)
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
