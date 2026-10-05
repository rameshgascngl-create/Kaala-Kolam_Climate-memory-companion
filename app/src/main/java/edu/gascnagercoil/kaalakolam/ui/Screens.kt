package edu.gascnagercoil.kaalakolam.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.gascnagercoil.kaalakolam.R
import edu.gascnagercoil.kaalakolam.content.GapManifest
import edu.gascnagercoil.kaalakolam.domain.AppState
import edu.gascnagercoil.kaalakolam.text.TextSafety
import java.util.Locale
import edu.gascnagercoil.kaalakolam.ui.theme.PrototypeTheme
import edu.gascnagercoil.kaalakolam.ui.theme.PrototypeTokens
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private fun localized(lang: String, en: String, ta: String): String =
    if (lang == "ta") ta else en

@Composable
fun HomeScreen(
    appState: AppState,
    onNavigate: (String) -> Unit,
) {
    val lang = appState.language
    val p = PrototypeTheme.palette
    val done = listOf(
        appState.memoryFlags["interview"] == true,
        appState.memoryFlags["crosscheck"] == true,
        appState.memoryFlags["pool"] == true,
        appState.memoryFlags["council"] == true,
        appState.memoryFlags["predict"] == true,
    )
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(p.ground),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(
                modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val homeHeading = localized(
                    lang,
                    "A climate app that starts with people, not theory",
                    "கோட்பாட்டில் அல்ல, மக்களிடமிருந்து தொடங்கும் காலநிலைப் பயன்பாடு",
                )
                Text(
                    text = homeHeading,
                    color = p.flour,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier
                        .widthIn(max = 285.dp)
                        .fidelityTag("screen.heading"),
                    onTextLayout = fidelityTextLayout(
                        tag = "screen.heading",
                        kind = "heading",
                        text = homeHeading,
                    ),
                )
                MemoryStripeHero(lang)
                val homeBody = localized(
                    lang,
                    "From a child of six to a grandmother of ninety, everyone can learn how weather and climate work here. Your grandmother has watched this place change for seventy years and instruments have watched too. You will learn the science, interview an elder, cross-check their memories against the evidence, pool what your class found, then run a village council where every choice has a cost.",
                    "ஆறு வயதுச் சிறுவன் முதல் தொண்ணூறு வயதுப் பாட்டி வரை எல்லோரும் இங்கு வானிலையும் காலநிலையும் எப்படிச் செயல்படுகின்றன என்று கற்கலாம். உங்கள் பாட்டி இந்த ஊர் மாறுவதை எழுபது ஆண்டுகளாகப் பார்த்திருக்கிறார்; கருவிகளும் பதிவு செய்திருக்கின்றன. அறிவியலைக் கற்று, ஒரு மூத்தவரிடம் நேர்காணல் நடத்தி, அவர் நினைவுகளைச் சான்றுகளுடன் ஒப்பிட்டு, உங்கள் வகுப்பின் கண்டுபிடிப்புகளை ஒன்றாக்கி, ஒவ்வொரு தேர்வுக்கும் விலை உள்ள ஊர்சபையை நடத்துவீர்கள்.",
                )
                Text(
                    text = homeBody,
                    color = p.flour,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fidelityTag("screen.body.primary"),
                    onTextLayout = fidelityTextLayout(
                        tag = "screen.body.primary",
                        kind = "body",
                        text = homeBody,
                    ),
                )
                PrototypeButton(
                    text = localized(lang, "Start an interview", "நேர்காணலைத் தொடங்கு"),
                    filled = true,
                    tag = "screen.primary-button",
                    onClick = { onNavigate("elders") },
                )
            }
        }

        item {
            PrototypeCard(tag = "screen.first-card") {
                Text(
                    localized(lang, "Choose your way in", "உங்கள் வழியைத் தேர்ந்தெடுங்கள்"),
                    color = p.flour,
                    style = MaterialTheme.typography.headlineMedium,
                )
                EntryOption(
                    icon = "sun",
                    title = localized(lang, "I am a child", "நான் ஒரு சிறுவன் அல்லது சிறுமி"),
                    body = localized(
                        lang,
                        "Short, simple answers with pictures and things to try",
                        "படங்களுடன் குறுகிய, எளிய விளக்கங்கள்; செய்து பார்க்க செயல்கள்",
                    ),
                    onClick = { onNavigate("learn") },
                )
                EntryOption(
                    icon = "globe",
                    title = localized(lang, "I want the full picture", "முழுமையாக அறிய விரும்புகிறேன்"),
                    body = localized(
                        lang,
                        "Clear explanations of weather, climate and change, with a deep-dive layer",
                        "வானிலை, காலநிலை, மாற்றம் பற்றிய தெளிவான விளக்கங்கள்; ஆழ்ந்த நிலையும் உண்டு",
                    ),
                    onClick = { onNavigate("learn") },
                )
                EntryOption(
                    icon = "bolt",
                    title = localized(lang, "Something is happening now", "இப்போது ஒரு இடர் நடக்கிறது"),
                    body = localized(
                        lang,
                        "Safety steps for heat, floods, cyclones, lightning and the sea",
                        "வெப்பம், வெள்ளம், புயல், மின்னல், கடலுக்கான பாதுகாப்பு வழிகள்",
                    ),
                    onClick = { onNavigate("learn") },
                )
                Text(
                    text = localized(
                        lang,
                        "Topics you understand: " + appState.learnedTopicIds.size + " / 24. Tap “Aa” at the top for larger text.",
                        "நீங்கள் புரிந்துகொண்ட தலைப்புகள்: " + appState.learnedTopicIds.size + " / 24. பெரிய எழுத்துக்கு மேலே “Aa”-ஐத் தொடுங்கள்.",
                    ),
                    color = p.faint,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }

        item {
            PrototypeCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    KolamProgress(
                        done = done,
                        lang = lang,
                        modifier = Modifier.size(154.dp),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            localized(lang, "Your kolam", "உங்கள் கோலம்"),
                            color = p.flour,
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text(
                            localized(
                                lang,
                                "A kolam is drawn by hand at dawn and washed away by evening. Yours fills in as you take part. Complete all five petals to close the border.",
                                "கோலம் காலையில் வரையப்பட்டு மாலைக்குள் அழியும். நீங்கள் பங்கேற்கும்போது உங்கள் கோலம் நிறைவடையும். ஐந்து இதழ்களையும் முடித்தால் வெளிவளையம் இணையும்.",
                            ),
                            color = p.flour,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        val petals = listOf(
                            Pair("Interview an elder", "மூத்தவரிடம் நேர்காணல்"),
                            Pair("Cross-check the evidence", "சான்றுடன் ஒப்பிடு"),
                            Pair("Pool the class", "வகுப்பைத் தொகு"),
                            Pair("Hold the council", "ஊர்சபை நடத்து"),
                            Pair("Predict and calibrate", "கணித்து அளவிடு"),
                        )
                        petals.forEachIndexed { index, pair ->
                            Text(
                                text = (if (done[index]) "● " else "○ ") +
                                    localized(lang, pair.first, pair.second),
                                color = if (done[index]) p.turmeric else p.faint,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }

        val cards = listOf(
            HomeRouteCard(
                route = "elders",
                titleEn = "Interview an elder",
                titleTa = "மூத்தவரிடம் நேர்காணல்",
                bodyEn = "Record how rain, heat, water and wildlife changed in one lifetime, then test each memory against the evidence.",
                bodyTa = "மழை, வெப்பம், நீர், உயிரினங்கள் ஒரு வாழ்நாளில் எப்படி மாறின என்பதைப் பதிவு செய்து, ஒவ்வொரு நினைவையும் சான்றுடன் சோதியுங்கள்.",
            ),
            HomeRouteCard(
                route = "class",
                titleEn = "Pool the class",
                titleTa = "வகுப்பைத் தொகு",
                bodyEn = "Combine everyone’s codes to see where memories agree. Agreement is interesting, but it is not proof.",
                bodyTa = "அனைவரின் குறியீடுகளையும் இணைத்து, நினைவுகள் எங்கே ஒத்துப்போகின்றன என்று பாருங்கள். ஒத்துப்போவது சுவாரசியம்; ஆனால் அதுவே சான்று அல்ல.",
            ),
            HomeRouteCard(
                route = "council",
                titleEn = "Hold the village council",
                titleTa = "ஊர்சபை நடத்துங்கள்",
                bodyEn = "Spend a limited budget. Notice who is protected and who is left out.",
                bodyTa = "குறைந்த நிதியைச் செலவிடுங்கள். யார் பாதுகாக்கப்படுகிறார்கள், யார் விடுபடுகிறார்கள் என்று கவனியுங்கள்.",
            ),
            HomeRouteCard(
                route = "predict",
                titleEn = "Predict and calibrate",
                titleTa = "கணித்து அளவிடு",
                bodyEn = "Guess key numbers and say how sure you are. Learn whether your confidence deserves your trust.",
                bodyTa = "முக்கிய எண்களை ஊகித்து, எவ்வளவு உறுதி என்று கூறுங்கள். உங்கள் நம்பிக்கை எவ்வளவு சரியானது என்று அறியுங்கள்.",
            ),
        )
        items(cards, key = { it.route }) { card ->
            PrototypeCard {
                Text(
                    localized(lang, card.titleEn, card.titleTa),
                    color = p.flour,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    localized(lang, card.bodyEn, card.bodyTa),
                    color = p.flour,
                    style = MaterialTheme.typography.bodyLarge,
                )
                PrototypeButton(
                    text = localized(lang, card.titleEn, card.titleTa),
                    filled = false,
                    onClick = { onNavigate(card.route) },
                )
            }
        }

        item {
            PrototypeCard {
                Text(
                    localized(lang, "Why memory alone is not enough", "நினைவு மட்டும் ஏன் போதாது?"),
                    color = p.flour,
                    style = MaterialTheme.typography.headlineMedium,
                )
                BaselineChart(lang)
                Text(
                    localized(
                        lang,
                        "Each generation takes the world it grew up in as normal, so slow change slips past us. Instruments do not forget, but they cannot tell us what a change meant to a family. We need both, and we need to know which is which.",
                        "ஒவ்வொரு தலைமுறையும் தான் வளர்ந்த உலகையே இயல்பு எனக் கருதும்; அதனால் மெதுவான மாற்றம் நம் கவனத்தைத் தப்பிவிடும். கருவிகள் மறப்பதில்லை; ஆனால் ஒரு மாற்றம் ஒரு குடும்பத்துக்கு என்ன பொருள் என்று அவற்றால் சொல்ல முடியாது. இரண்டும் தேவை; எது எது என்று பிரித்தறியவும் வேண்டும்.",
                    ),
                    color = p.flour,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        item {
            PrototypeNote(
                text = localized(
                    lang,
                    "Nothing leaves this device unless you copy a code and share it yourself. No names are stored. Give your elder a nickname.",
                    "நீங்களே ஒரு குறியீட்டை நகலெடுத்துப் பகிர்ந்தாலன்றி எதுவும் இந்தச் சாதனத்தை விட்டு வெளியே செல்லாது. பெயர்கள் சேமிக்கப்படுவதில்லை; உங்கள் மூத்தவருக்கு ஒரு செல்லப்பெயர் வையுங்கள்.",
                ),
            )
        }

        item { PrototypeFooter(lang) }
    }
}

private data class HomeRouteCard(
    val route: String,
    val titleEn: String,
    val titleTa: String,
    val bodyEn: String,
    val bodyTa: String,
)

@Composable
private fun PrototypeCard(
    tag: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val p = PrototypeTheme.palette
    val tagged = if (tag == null) Modifier else Modifier.fidelityTag(tag)
    Column(
        modifier = tagged
            .fillMaxWidth()
            .background(p.ground2, RoundedCornerShape(14.dp))
            .border(1.dp, p.line, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun EntryOption(
    icon: String,
    title: String,
    body: String,
    onClick: () -> Unit,
) {
    val p = PrototypeTheme.palette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .background(p.ground2, RoundedCornerShape(12.dp))
            .border(1.dp, p.line, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = PrototypeIcons.get(icon),
                contentDescription = title,
                tint = p.turmeric,
                modifier = Modifier.size(30.dp),
            )
            Text(title, color = p.flour, fontWeight = FontWeight.Bold)
        }
        Text(body, color = p.faint, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PrototypeButton(
    text: String,
    filled: Boolean,
    tag: String? = null,
    onClick: () -> Unit,
) {
    val p = PrototypeTheme.palette
    val background = if (filled) p.turmeric else Color.Transparent
    val foreground = if (filled) Color(0xFF17120A) else p.flour
    val effectiveTag = tag ?: "button.${text.hashCode()}"
    FidelityTouchTarget(
        tag = effectiveTag,
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = 46.dp)
                .background(background, RoundedCornerShape(12.dp))
                .border(1.dp, if (filled) p.turmeric else p.line, RoundedCornerShape(12.dp))
                .padding(horizontal = 18.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text,
                color = foreground,
                fontWeight = if (filled) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
private fun PrototypeNote(
    text: String,
    bad: Boolean = false,
) {
    val p = PrototypeTheme.palette
    val line = if (bad) p.vermilion else p.turmeric
    Text(
        text = text,
        color = p.flour,
        modifier = Modifier
            .fillMaxWidth()
            .background(p.ground3, RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp))
            .drawBehind {
                drawRect(
                    color = line,
                    topLeft = Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height),
                )
            }
            .padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 8.dp),
    )
}

@Composable
private fun PrototypeFooter(lang: String) {
    val p = PrototypeTheme.palette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = p.line,
                    start = Offset.Zero,
                    end = Offset(size.width, 0f),
                    strokeWidth = 1f,
                )
            }
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "Department of Zoology, GASC, Nagercoil / Created by R. Ramesh",
            color = p.faint,
            fontSize = 12.8.sp,
        )
        Text(
            localized(
                lang,
                "Tamil text is a draft awaiting language review.",
                "தமிழ் உரை வரைவு நிலையில் உள்ளது; மொழிப் பிழைதிருத்தம் தேவை.",
            ),
            color = p.faint,
            fontSize = 12.8.sp,
        )
    }
}

@Composable
private fun MemoryStripeHero(lang: String) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .clip(RoundedCornerShape(10.dp))
            .semantics {
                contentDescription = localized(
                    lang,
                    "Memory stripes showing climate change over time",
                    "காலப்போக்கில் காலநிலை மாற்றத்தைக் காட்டும் நினைவுக் கோடுகள்",
                )
            },
    ) {
        val random = Mulberry32(7)
        val colours = PrototypeTokens.ScaleColours
        val count = 56
        val cell = size.width / count
        repeat(count) { index ->
            val value =
                -1.7 + 3.4 * (index.toDouble() / (count - 1)) + (random.nextDouble() - 0.5) * 1.1
            drawRect(
                color = scaleColour(value, colours),
                topLeft = Offset(index * cell, 0f),
                size = androidx.compose.ui.geometry.Size(cell * 1.02f, size.height),
            )
        }
    }
}

private fun scaleColour(value: Double, colours: List<Color>): Color {
    val shifted = value.coerceIn(-2.0, 2.0) + 2.0
    val index = min(3, floor(shifted).toInt())
    val fraction = shifted - index
    val a = colours[index]
    val b = colours[index + 1]
    fun channel(start: Float, end: Float): Int =
        (start * 255 + (end * 255 - start * 255) * fraction)
            .roundToInt()
            .coerceIn(0, 255)
    return Color(
        red = channel(a.red, b.red),
        green = channel(a.green, b.green),
        blue = channel(a.blue, b.blue),
        alpha = 255,
    )
}

private class Mulberry32(seed: Int) {
    private var a = seed

    fun nextDouble(): Double {
        a += 0x6D2B79F5.toInt()
        var t = a
        t = (t xor (t ushr 15)) * (1 or t)
        t += ((t xor (t ushr 7)) * (61 or t)) xor t
        val out = t xor (t ushr 14)
        return out.toUInt().toLong().toDouble() / 4294967296.0
    }
}

@Composable
private fun KolamProgress(
    done: List<Boolean>,
    lang: String,
    modifier: Modifier = Modifier,
) {
    val p = PrototypeTheme.palette
    Canvas(
        modifier = modifier.semantics {
            contentDescription = localized(
                lang,
                "Kolam learning progress: " + done.count { it } + " of 5 petals complete",
                "கோலக் கற்றல் முன்னேற்றம்: 5 இதழ்களில் " + done.count { it } + " நிறைவு",
            )
        },
    ) {
        val scale = min(size.width, size.height) / 224f
        val center = Offset(size.width / 2f, size.height / 2f)
        val dash = PathEffect.dashPathEffect(floatArrayOf(3f * scale, 6f * scale))
        repeat(5) { index ->
            val rect = Rect(
                left = center.x - 17f * scale,
                top = center.y + (-94f) * scale,
                right = center.x + 17f * scale,
                bottom = center.y + (-14f) * scale,
            )
            withTransform({
                rotate(index * 72f, center)
            }) {
                if (done[index]) {
                    drawOval(
                        color = PrototypeTokens.BrandTurmeric.copy(alpha = 0.18f),
                        topLeft = rect.topLeft,
                        size = rect.size,
                    )
                }
                drawOval(
                    color = if (done[index]) p.flour else p.faint,
                    topLeft = rect.topLeft,
                    size = rect.size,
                    style = Stroke(
                        width = (if (done[index]) 2.4f else 1.4f) * scale,
                        pathEffect = if (done[index]) null else dash,
                    ),
                )
            }
        }

        drawCircle(p.flour, 3.4f * scale, center)
        val outer = mutableListOf<Pair<Offset, Offset>>()
        repeat(5) { index ->
            val a = Math.toRadians((index * 72 - 90).toDouble())
            val b = Math.toRadians((index * 72 - 54).toDouble())
            val c = Math.toRadians((index * 72 - 18).toDouble())
            val inner = Offset(
                center.x + (22 * cos(a)).toFloat() * scale,
                center.y + (22 * sin(a)).toFloat() * scale,
            )
            val edge = Offset(
                center.x + (94 * cos(b)).toFloat() * scale,
                center.y + (94 * sin(b)).toFloat() * scale,
            )
            val control = Offset(
                center.x + (116 * cos(c)).toFloat() * scale,
                center.y + (116 * sin(c)).toFloat() * scale,
            )
            drawCircle(if (done[index]) p.flour else p.faint, 2.6f * scale, inner)
            drawCircle(p.faint, 2.4f * scale, edge)
            outer += Pair(edge, control)
        }
        if (done.all { it }) {
            val lace = Path().apply {
                moveTo(outer[0].first.x, outer[0].first.y)
                repeat(5) { index ->
                    val next = outer[(index + 1) % 5].first
                    val control = outer[index].second
                    quadraticTo(control.x, control.y, next.x, next.y)
                }
            }
            drawPath(
                lace,
                p.turmeric,
                style = Stroke(width = 2f * scale, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
private fun BaselineChart(lang: String) {
    val p = PrototypeTheme.palette
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(168.dp)
            .semantics {
                contentDescription = localized(
                    lang,
                    "Chart showing three generations treating different climate levels as normal",
                    "மூன்று தலைமுறைகள் வெவ்வேறு காலநிலை நிலைகளை இயல்பாகக் கருதுவதைக் காட்டும் வரைபடம்",
                )
            },
    ) {
        val random = Mulberry32(11)
        val points = (0..60).map { index ->
            val x = 10.0 + index * (300.0 / 60.0)
            val y = 60.0 + index * 0.9 + (random.nextDouble() - 0.5) * 14.0
            Offset(
                (x / 320.0 * size.width).toFloat(),
                (y / 190.0 * size.height).toFloat(),
            )
        }
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { point -> lineTo(point.x, point.y) }
        }
        drawPath(path, p.flour2, style = Stroke(width = 1.6f))
        repeat(3) { group ->
            val segment = points.subList(group * 20, group * 20 + 21)
            val average = segment.map { it.y }.average().toFloat()
            drawLine(
                color = p.turmeric,
                start = Offset(segment.first().x, average),
                end = Offset(segment.last().x, average),
                strokeWidth = 4f,
                cap = StrokeCap.Round,
            )
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            localized(lang, "Grandparents’ normal", "தாத்தா பாட்டியின் இயல்பு"),
            color = p.faint,
            fontSize = 10.sp,
        )
        Text(
            localized(lang, "Parents’ normal", "பெற்றோரின் இயல்பு"),
            color = p.faint,
            fontSize = 10.sp,
        )
        Text(
            localized(lang, "Your normal", "உங்கள் இயல்பு"),
            color = p.faint,
            fontSize = 10.sp,
        )
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    val p = PrototypeTheme.palette
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(p.ground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, color = p.flour, style = MaterialTheme.typography.headlineLarge)
        PrototypeNote(
            "PLACEHOLDER — visual parity and full workflow have not been implemented for this tab.",
            bad = true,
        )
    }
}

@Composable
fun GapListScreen(
    title: String,
    manifest: GapManifest?,
    kind: String,
) {
    val p = PrototypeTheme.palette
    if (manifest == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(p.ground),
            contentAlignment = Alignment.Center,
        ) {
            Text("…", color = p.turmeric, style = MaterialTheme.typography.headlineLarge)
        }
        return
    }

    val gaps = manifest.gaps.filter { it.kind == kind }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(p.ground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(title, color = p.flour, style = MaterialTheme.typography.headlineLarge)
            Text(
                text = stringResource(R.string.content_review_note),
                color = p.faint,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        items(gaps, key = { it.logicalPath }) { gap ->
            PrototypeCard {
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(99.dp),
                    border = BorderStroke(1.dp, p.line),
                ) {
                    Text(
                        text = manifest.uiLabel,
                        color = p.faint,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Text(gap.en, color = p.flour, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun AboutScreen(
    appState: AppState,
    onBackup: (AppState) -> String,
    onValidateBackup: (String) -> AppState?,
    onRestore: (AppState) -> Unit,
    onReset: () -> Unit,
) {
    val context = LocalContext.current
    val p = PrototypeTheme.palette
    var backupCode by remember { mutableStateOf<String?>(null) }
    var restoreEntryOpen by remember { mutableStateOf(false) }
    var restoreText by remember { mutableStateOf(TextFieldValue("")) }
    var invalidRestore by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<AppState?>(null) }
    var resetOpen by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(p.ground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                stringResource(R.string.about),
                color = p.flour,
                style = MaterialTheme.typography.headlineLarge,
            )
        }
        item { AboutSection(R.string.about_purpose_title, R.string.about_purpose) }
        item { AboutSection(R.string.about_storage_title, R.string.about_storage) }
        item {
            PrototypeCard {
                Text(
                    stringResource(R.string.about_version),
                    color = p.flour,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(stringResource(R.string.about_credit), color = p.flour)
                Text(stringResource(R.string.contact_placeholder), color = p.vermilion)
            }
        }
        item { AboutSection(R.string.about_sources_title, R.string.about_sources) }
        item { AboutSection(R.string.about_limits_title, R.string.about_limits) }
        item {
            PrototypeButton(
                text = stringResource(R.string.backup_data),
                filled = true,
                onClick = { backupCode = onBackup(appState) },
            )
        }
        backupCode?.let { code ->
            item {
                PrototypeCard {
                    Text(stringResource(R.string.backup_explanation), color = p.flour)
                    SelectionContainer {
                        Text(code, color = p.flour2, style = MaterialTheme.typography.bodySmall)
                    }
                    PrototypeButton(
                        text = stringResource(R.string.copy),
                        filled = false,
                        onClick = {
                            val clipboard =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText("Kaala Kolam backup", code),
                            )
                        },
                    )
                }
            }
        }
        item {
            PrototypeButton(
                text = stringResource(R.string.restore_data),
                filled = false,
                onClick = {
                    restoreText = TextFieldValue("")
                    invalidRestore = false
                    restoreEntryOpen = true
                },
            )
        }
        item {
            PrototypeButton(
                text = stringResource(R.string.reset_data),
                filled = false,
                onClick = { resetOpen = true },
            )
        }
        item { PrototypeFooter(appState.language) }
    }

    if (restoreEntryOpen) {
        AlertDialog(
            onDismissRequest = { restoreEntryOpen = false },
            title = { Text(stringResource(R.string.restore_data)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = restoreText,
                        onValueChange = {
                            restoreText = TextSafety.limitTextFieldValue(it, 65_536, Locale.ROOT)
                            invalidRestore = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (invalidRestore) {
                        Text(stringResource(R.string.restore_invalid), color = p.vermilion)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val decoded = onValidateBackup(restoreText.text)
                        if (decoded == null) {
                            invalidRestore = true
                        } else {
                            restoreEntryOpen = false
                            pendingRestore = decoded
                        }
                    },
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { restoreEntryOpen = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    pendingRestore?.let { validated ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text(stringResource(R.string.restore_confirm_title)) },
            text = { Text(stringResource(R.string.restore_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRestore = null
                        onRestore(validated)
                    },
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestore = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (resetOpen) {
        AlertDialog(
            onDismissRequest = { resetOpen = false },
            title = { Text(stringResource(R.string.reset_confirm_title)) },
            text = { Text(stringResource(R.string.reset_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        resetOpen = false
                        onReset()
                    },
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { resetOpen = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun AboutSection(
    title: Int,
    body: Int,
) {
    val p = PrototypeTheme.palette
    PrototypeCard {
        Text(stringResource(title), color = p.flour, style = MaterialTheme.typography.titleMedium)
        Text(stringResource(body), color = p.flour)
    }
}
