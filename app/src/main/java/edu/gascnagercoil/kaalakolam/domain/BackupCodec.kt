package edu.gascnagercoil.kaalakolam.domain

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object BackupCodec {
    private const val PREFIX = "KB1"
    private const val MAX_CODE_CHARS = 65_536
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

    private val json = Json {
        encodeDefaults = true
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    fun encode(state: AppState): String {
        val payload = json.encodeToString(state.repair()).encodeToByteArray()
        val encoded = base64UrlEncode(payload)
        val checksum = fnv1a32(payload).toUInt().toString(16).padStart(8, '0')
        return "$PREFIX.$encoded.$checksum"
    }

    fun decode(code: String): Result<AppState> = runCatching {
        val trimmed = code.trim()
        require(trimmed.length in 1..MAX_CODE_CHARS) { "Backup code length is invalid." }
        val parts = trimmed.split('.')
        require(parts.size == 3 && parts[0] == PREFIX) { "Backup code prefix is invalid." }
        val payload = base64UrlDecode(parts[1])
        val supplied = parts[2].lowercase()
        require(supplied.matches(Regex("[0-9a-f]{8}"))) { "Backup checksum is invalid." }
        val actual = fnv1a32(payload).toUInt().toString(16).padStart(8, '0')
        require(actual == supplied) { "Backup checksum does not match." }
        json.decodeFromString<AppState>(payload.decodeToString()).repair()
    }

    internal fun fnv1a32(bytes: ByteArray): Int {
        var hash = 0x811c9dc5.toInt()
        for (byte in bytes) {
            hash = hash xor (byte.toInt() and 0xff)
            hash *= 0x01000193
        }
        return hash
    }

    private fun base64UrlEncode(input: ByteArray): String {
        if (input.isEmpty()) return ""
        val out = StringBuilder((input.size * 4 + 2) / 3)
        var index = 0
        while (index < input.size) {
            val b0 = input[index].toInt() and 0xff
            val b1 = if (index + 1 < input.size) input[index + 1].toInt() and 0xff else -1
            val b2 = if (index + 2 < input.size) input[index + 2].toInt() and 0xff else -1

            out.append(ALPHABET[b0 ushr 2])
            out.append(ALPHABET[((b0 and 0x03) shl 4) or (if (b1 >= 0) b1 ushr 4 else 0)])
            if (b1 >= 0) {
                out.append(ALPHABET[((b1 and 0x0f) shl 2) or (if (b2 >= 0) b2 ushr 6 else 0)])
            }
            if (b2 >= 0) {
                out.append(ALPHABET[b2 and 0x3f])
            }
            index += 3
        }
        return out.toString()
    }

    private fun base64UrlDecode(input: String): ByteArray {
        require(input.all { it in ALPHABET }) { "Backup payload contains invalid characters." }
        require(input.length % 4 != 1) { "Backup payload length is invalid." }
        if (input.isEmpty()) return byteArrayOf()

        val output = ByteArray((input.length * 6) / 8)
        var buffer = 0
        var bits = 0
        var outIndex = 0
        for (char in input) {
            val value = ALPHABET.indexOf(char)
            require(value >= 0) { "Backup payload contains invalid characters." }
            buffer = (buffer shl 6) or value
            bits += 6
            if (bits >= 8) {
                bits -= 8
                output[outIndex++] = ((buffer ushr bits) and 0xff).toByte()
            }
        }
        return output.copyOf(outIndex)
    }
}
