package edu.gascnagercoil.kaalakolam.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Pure Kotlin ports of the deterministic workflow rules in the shipped HTML.
 *
 * M2 deliberately contains no Compose, Android framework, storage or network
 * code. Later Predict/Elders/Class/Council screens consume these functions so
 * functional parity can be tested independently of rendering.
 */
object M2Domain {
    const val QUESTION_COUNT = 10
    const val COUNCIL_BUDGET = 12
    private const val SHARE_ALPHABET =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

    @Serializable
    data class InterviewRecord(
        val t: String,
        val i: String,
        val p: Int,
        val b: Int,
        val a: String,
    )

    @Serializable
    data class PlanRecord(
        val t: String,
        val i: String,
        val s: Int,
        val o: List<Int>,
    )

    sealed interface ShareRecord {
        data class Interview(val value: InterviewRecord) : ShareRecord
        data class Plan(val value: PlanRecord) : ShareRecord
    }

    data class ImportResult(
        val records: List<ShareRecord>,
        val added: Int,
        val duplicates: Int,
        val rejected: Int,
        val found: Int,
    )

    data class AggregateRow(
        val label: String,
        val counts: List<Int>,
        val n: Int,
        val mean: Double?,
        val agreement: Double?,
    )

    data class CouncilRisk(
        val hazard: Double,
        val risks: Map<String, Double>,
        val mean: Double,
    )

    data class PredictionSpec(
        val id: String,
        val min: Double,
        val max: Double,
        val step: Double,
        val initial: Double,
        val low: Double,
        val high: Double,
        val unit: String,
    )

    data class PredictionAnswer(
        val id: String,
        val guess: Double,
        val confidence: Int,
        val hit: Boolean,
    )

    data class CalibrationBucket(
        val confidence: Int,
        val n: Int,
        val correct: Int,
    )

    enum class CertainCalibration {
        OVERCONFIDENT,
        PERFECT_SO_FAR,
        KEEP_GOING,
    }

    private data class Group(val id: String, val exposure: Double, val vulnerability: Double)

    private data class Option(
        val id: String,
        val cost: Int,
        val groups: List<String>,
        val exposureReduction: Double = 0.0,
        val vulnerabilityReduction: Double = 0.0,
        val localHazardReduction: Double = 0.0,
        val globalMultiplierReduction: Double = 0.0,
    )

    private val json = Json {
        encodeDefaults = true
        explicitNulls = false
        ignoreUnknownKeys = false
    }

    private val shareRegex = Regex("""K[KP]1\.[A-Za-z0-9_-]+\.[0-9a-f]{6}""")

    private val groups = listOf(
        Group("fish", exposure = 8.0, vulnerability = 6.0),
        Group("farm", exposure = 6.0, vulnerability = 6.0),
        Group("old", exposure = 6.0, vulnerability = 8.0),
        Group("poor", exposure = 8.0, vulnerability = 8.0),
    )

    private val options = listOf(
        Option("warn", 2, listOf("fish", "farm"), vulnerabilityReduction = 1.2),
        Option("shelter", 3, listOf("old", "poor", "fish"), vulnerabilityReduction = 1.3),
        Option("retrofit", 3, listOf("poor", "old"), vulnerabilityReduction = 1.8),
        Option("drains", 2, listOf("farm", "poor"), exposureReduction = 1.0),
        Option(
            "mangrove",
            3,
            listOf("fish", "farm"),
            exposureReduction = 0.8,
            localHazardReduction = 0.08,
        ),
        Option("income", 2, listOf("poor", "farm", "old"), vulnerabilityReduction = 1.0),
        Option("clean", 2, emptyList(), globalMultiplierReduction = 0.03),
    )
    private val optionsById = options.associateBy { it.id }
    private val scenarioMultipliers = listOf(1.0, 1.15, 1.4)

    val predictionSpecs = listOf(
        PredictionSpec("ocean", 0.0, 100.0, 1.0, 50.0, 85.0, 95.0, "%"),
        PredictionSpec("co2", 250.0, 600.0, 5.0, 350.0, 420.0, 440.0, " ppm"),
        PredictionSpec("warm", 0.0, 4.0, 0.1, 0.5, 1.0, 1.6, " °C"),
        PredictionSpec("nogh", -50.0, 30.0, 1.0, 0.0, -25.0, -12.0, " °C"),
        PredictionSpec("vapour", 0.0, 25.0, 1.0, 3.0, 6.0, 8.0, "%"),
        PredictionSpec("sea", 0.0, 100.0, 1.0, 5.0, 15.0, 25.0, " cm"),
        PredictionSpec("india", 0.0, 3.0, 0.1, 0.3, 0.5, 0.9, " °C"),
        PredictionSpec("nat", -0.5, 1.5, 0.1, 0.5, -0.2, 0.2, " °C"),
    )

    fun interviewCode(record: InterviewRecord): String {
        require(validInterview(record))
        return makeCode("K", json.encodeToString(record))
    }

    fun planCode(record: PlanRecord): String {
        require(validPlan(record))
        return makeCode("P", json.encodeToString(record))
    }

    fun decodeShareCode(code: String): ShareRecord? {
        val parts = code.trim().split('.')
        if (parts.size != 3) return null
        val prefix = parts[0]
        if (prefix != "KK1" && prefix != "KP1") return null
        val payload = parts[1]
        if (fnv6(payload) != parts[2]) return null
        val decoded = runCatching { base64UrlDecode(payload).decodeToString() }.getOrNull() ?: return null
        return when (prefix) {
            "KK1" -> runCatching { json.decodeFromString<InterviewRecord>(decoded) }
                .getOrNull()
                ?.takeIf(::validInterview)
                ?.let(ShareRecord::Interview)
            "KP1" -> runCatching { json.decodeFromString<PlanRecord>(decoded) }
                .getOrNull()
                ?.takeIf(::validPlan)
                ?.let(ShareRecord::Plan)
            else -> null
        }
    }

    fun importCodes(text: String, existingIds: Set<String> = emptySet()): ImportResult {
        val matches = shareRegex.findAll(text).map { it.value }.toList()
        val ids = existingIds.toMutableSet()
        val records = mutableListOf<ShareRecord>()
        var duplicates = 0
        var rejected = 0
        for (code in matches) {
            val record = decodeShareCode(code)
            if (record == null) {
                rejected += 1
                continue
            }
            val id = when (record) {
                is ShareRecord.Interview -> record.value.i
                is ShareRecord.Plan -> record.value.i
            }
            if (!ids.add(id)) {
                duplicates += 1
                continue
            }
            records += record
        }
        return ImportResult(
            records = records,
            added = records.size,
            duplicates = duplicates,
            rejected = rejected,
            found = matches.size,
        )
    }

    fun interviewRecord(
        id: String,
        place: Int,
        birthDecade: Int,
        answers: List<Int?>,
    ): InterviewRecord {
        require(answers.size == QUESTION_COUNT)
        val encoded = answers.joinToString(separator = "") { rating ->
            if (rating == null) {
                "0"
            } else {
                require(rating in -2..2)
                (rating + 3).toString()
            }
        }
        return InterviewRecord("I", id, place, birthDecade, encoded)
    }

    fun sampleRecords(): List<InterviewRecord> {
        val random = Mulberry32(20260)
        val means = doubleArrayOf(1.3, 1.0, -0.9, 0.8, -0.6, 0.7, -0.5, -1.6, -1.0, 0.4)
        val noise = doubleArrayOf(2.4, 2.4, 3.0, 2.6, 2.8, 2.6, 3.0, 1.2, 2.6, 3.0)
        val places = intArrayOf(0, 0, 0, 1, 1, 1, 2, 3, 4, 4)
        val decades = intArrayOf(0, 1, 1, 2, 2, 3, 3, 4)
        return List(28) { index ->
            val place = places[floor(random.next() * places.size).toInt()]
            val decade = decades[floor(random.next() * decades.size).toInt()]
            val amplitude = if (decade <= 1) 1.2 else 1.0
            val answer = buildString {
                repeat(QUESTION_COUNT) { questionIndex ->
                    if (questionIndex == 5 && place != 0 && random.next() < 0.8) {
                        append('0')
                    } else if (random.next() < 0.06) {
                        append('0')
                    } else {
                        val raw = means[questionIndex] * amplitude +
                            (random.next() - 0.5) * noise[questionIndex]
                        val value = floor(raw + 0.5).toInt().coerceIn(-2, 2)
                        append(value + 3)
                    }
                }
            }
            InterviewRecord(
                t = "I",
                i = "smp" + index.toString().padStart(3, '0'),
                p = place,
                b = decade,
                a = answer,
            )
        }
    }

    fun aggregateAll(records: List<InterviewRecord>, questionIndex: Int): List<AggregateRow> =
        aggregate(records, questionIndex, listOf(0 to "all")) { 0 }

    fun aggregateByPlace(records: List<InterviewRecord>, questionIndex: Int): List<AggregateRow> =
        aggregate(records, questionIndex, (0..4).map { it to ("p" + it) }) { it.p }

    fun aggregate(
        records: List<InterviewRecord>,
        questionIndex: Int,
        keys: List<Pair<Int, String>>,
        keyOf: (InterviewRecord) -> Int,
    ): List<AggregateRow> {
        require(questionIndex in 0 until QUESTION_COUNT)
        return keys.map { (key, label) ->
            val counts = MutableList(5) { 0 }
            var n = 0
            var sum = 0.0
            var squareSum = 0.0
            records.forEach { record ->
                if (keyOf(record) != key) return@forEach
                val char = record.a[questionIndex]
                if (char == '0') return@forEach
                val value = char.digitToInt() - 3
                counts[value + 2] += 1
                n += 1
                sum += value
                squareSum += value * value
            }
            val mean = if (n == 0) null else sum / n
            val standardDeviation = if (n > 1 && mean != null) {
                sqrt(max(0.0, squareSum / n - mean * mean))
            } else {
                null
            }
            AggregateRow(
                label = label,
                counts = counts,
                n = n,
                mean = mean,
                agreement = standardDeviation?.let { 1.0 - it / 2.0 },
            )
        }
    }

    fun councilSpent(picks: List<String>): Int =
        picks.sumOf { optionsById[it]?.cost ?: 0 }

    fun councilUncovered(picks: List<String>): List<String> {
        val covered = buildSet {
            picks.forEach { id ->
                optionsById[id]?.groups?.forEach { groupId -> add(groupId) }
            }
        }
        return groups.map { it.id }.filterNot(covered::contains)
    }

    fun councilRisk(picks: List<String>, scenario: Int): CouncilRisk {
        require(scenario in scenarioMultipliers.indices)
        var globalMultiplier = scenarioMultipliers[scenario]
        var localHazardReduction = 0.0
        val working = groups.associate { it.id to doubleArrayOf(it.exposure, it.vulnerability) }.toMutableMap()

        picks.forEach { id ->
            val option = optionsById[id] ?: return@forEach
            option.groups.forEach { groupId ->
                val values = working.getValue(groupId)
                values[0] = max(1.0, values[0] - option.exposureReduction)
                values[1] = max(1.0, values[1] - option.vulnerabilityReduction)
            }
            localHazardReduction += option.localHazardReduction
            globalMultiplier -= option.globalMultiplierReduction
        }

        val hazard = min(10.0, 5.0 * globalMultiplier * (1.0 - min(localHazardReduction, 0.3)))
        val risks = linkedMapOf<String, Double>()
        var total = 0.0
        groups.forEach { group ->
            val values = working.getValue(group.id)
            val risk = hazard * values[0] * values[1] / 10.0
            risks[group.id] = risk
            total += risk
        }
        return CouncilRisk(hazard = hazard, risks = risks, mean = total / groups.size)
    }

    fun predictionHit(spec: PredictionSpec, guess: Double): Boolean =
        guess >= spec.low && guess <= spec.high

    fun calibrationBuckets(answers: List<PredictionAnswer>): List<CalibrationBucket> =
        listOf(3, 2, 1).map { confidence ->
            val matching = answers.filter { it.confidence == confidence }
            CalibrationBucket(
                confidence = confidence,
                n = matching.size,
                correct = matching.count { it.hit },
            )
        }

    fun certainCalibration(answers: List<PredictionAnswer>): CertainCalibration {
        val certain = calibrationBuckets(answers).first { it.confidence == 3 }
        return when {
            certain.n >= 2 && certain.correct.toDouble() / certain.n < 0.6 ->
                CertainCalibration.OVERCONFIDENT
            certain.n >= 2 && certain.correct == certain.n ->
                CertainCalibration.PERFECT_SO_FAR
            else -> CertainCalibration.KEEP_GOING
        }
    }

    private fun validInterview(record: InterviewRecord): Boolean =
        record.t == "I" &&
            record.i.length <= 12 &&
            record.p in 0..4 &&
            record.b in 0..5 &&
            record.a.length == QUESTION_COUNT &&
            record.a.all { it in '0'..'5' }

    private fun validPlan(record: PlanRecord): Boolean =
        record.t == "P" &&
            record.i.length <= 12 &&
            record.s in 0..2 &&
            record.o.size <= options.size &&
            record.o.all { it in options.indices }

    private fun makeCode(kind: String, jsonPayload: String): String {
        val payload = base64UrlEncode(jsonPayload.encodeToByteArray())
        return "K" + kind + "1." + payload + "." + fnv6(payload)
    }

    internal fun fnv6(text: String): String {
        var hash = 2166136261u
        text.forEach { char ->
            hash = hash xor char.code.toUInt()
            hash *= 16777619u
        }
        val padded = hash.toString(16).padStart(8, '0')
        return buildString(6) {
            repeat(6) { index -> append(padded[index]) }
        }
    }

    private fun base64UrlEncode(input: ByteArray): String {
        if (input.isEmpty()) return ""
        val output = StringBuilder((input.size * 4 + 2) / 3)
        var index = 0
        while (index < input.size) {
            val b0 = input[index].toInt() and 0xff
            val b1 = if (index + 1 < input.size) input[index + 1].toInt() and 0xff else -1
            val b2 = if (index + 2 < input.size) input[index + 2].toInt() and 0xff else -1
            output.append(SHARE_ALPHABET[b0 ushr 2])
            output.append(
                SHARE_ALPHABET[
                    ((b0 and 0x03) shl 4) or (if (b1 >= 0) b1 ushr 4 else 0),
                ],
            )
            if (b1 >= 0) {
                output.append(
                    SHARE_ALPHABET[
                        ((b1 and 0x0f) shl 2) or (if (b2 >= 0) b2 ushr 6 else 0),
                    ],
                )
            }
            if (b2 >= 0) output.append(SHARE_ALPHABET[b2 and 0x3f])
            index += 3
        }
        return output.toString()
    }

    private fun base64UrlDecode(input: String): ByteArray {
        if (input.any { it !in SHARE_ALPHABET } || input.length % 4 == 1) {
            throw IllegalArgumentException("Invalid share payload")
        }
        if (input.isEmpty()) return byteArrayOf()
        val output = ByteArray((input.length * 6) / 8)
        var buffer = 0
        var bits = 0
        var outputIndex = 0
        input.forEach { char ->
            val value = SHARE_ALPHABET.indexOf(char)
            buffer = (buffer shl 6) or value
            bits += 6
            if (bits >= 8) {
                bits -= 8
                output[outputIndex++] = ((buffer ushr bits) and 0xff).toByte()
            }
        }
        return output.copyOf(outputIndex)
    }

    private class Mulberry32(seed: Int) {
        private var state: Int = seed

        fun next(): Double {
            state += 0x6D2B79F5.toInt()
            var value = (state xor (state ushr 15)) * (1 or state)
            value = (value + ((value xor (value ushr 7)) * (61 or value))) xor value
            val unsigned = (value xor (value ushr 14)).toUInt().toLong()
            return unsigned / 4294967296.0
        }
    }
}
