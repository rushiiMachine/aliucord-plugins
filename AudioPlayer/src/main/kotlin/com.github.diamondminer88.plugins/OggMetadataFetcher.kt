package com.github.diamondminer88.plugins

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder

object OggMetadataFetcher {
    data class OggMetadata(
        val codec: String,
        val sampleRate: Int,
        val granulePos: Long,
        val duration: Double
    )

    @JvmStatic
    fun fetch(url: String): OggMetadata? {
        var sampleRate = -1
        var codec = ""
        val head = fetchBytes(url, 0, 8191) ?: return null

        // Opus
        for (i in 0 until head.size - 8) {
            if (head[i] == 'O'.code.toByte() && head[i+1] == 'p'.code.toByte() && head[i+2] == 'u'.code.toByte() &&
                head[i+3] == 's'.code.toByte() && head[i+4] == 'H'.code.toByte() && head[i+5] == 'e'.code.toByte() &&
                head[i+6] == 'a'.code.toByte() && head[i+7] == 'd'.code.toByte()
            ) {
                val offset = i + 12
                if (offset + 4 < head.size) {
                    val bb = ByteBuffer.wrap(head, offset, 4).order(ByteOrder.LITTLE_ENDIAN)
                    sampleRate = bb.int
                    codec = "Opus"
                }
                break
            }
        }
        
        // Vorbis
        if (sampleRate == -1) {
            for (i in 0 until head.size - 7) {
                if (head[i] == 0x01.toByte() && head[i+1] == 'v'.code.toByte() && head[i+2] == 'o'.code.toByte() &&
                    head[i+3] == 'r'.code.toByte() && head[i+4] == 'b'.code.toByte() && head[i+5] == 'i'.code.toByte() &&
                    head[i+6] == 's'.code.toByte()
                ) {
                    val offset = i + 12
                    if (offset + 4 < head.size) {
                        val bb = ByteBuffer.wrap(head, offset, 4).order(ByteOrder.LITTLE_ENDIAN)
                        sampleRate = bb.int
                        codec = "Vorbis"
                    }
                    break
                }
            }
        }

        val granulePos = fetchGranulePosition(url)

        return if (sampleRate > 0 && granulePos > 0) {
            OggMetadata(codec, sampleRate, granulePos, granulePos.toDouble() / sampleRate)
        } else {
            null
        }
    }

    fun fetchBytes(urlStr: String, start: Int, end: Int): ByteArray? {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("Range", "bytes=$start-$end")
        conn.connect()
        if (conn.responseCode != 206 && conn.responseCode != 200) return null
        val out = ByteArrayOutputStream()
        conn.inputStream.use { input ->
            val buf = ByteArray(4096)
            var n: Int
            while (input.read(buf).also { n = it } > 0) {
                out.write(buf, 0, n)
            }
        }
        return out.toByteArray()
    }

    fun fetchLastBytes(urlStr: String, numBytes: Int): ByteArray? {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "HEAD"
        conn.connect()
        val contentLength = conn.contentLength
        if (contentLength <= 0) return null
        val start = maxOf(0, contentLength - numBytes)
        return fetchBytes(urlStr, start, contentLength - 1)
    }

    fun fetchGranulePosition(fileUrl: String): Long {
        val tailBytes = 16384
        val tail = fetchLastBytes(fileUrl, tailBytes) ?: return -1

        var lastOggS = -1
        for (i in 0 until tail.size - 4) {
            if (tail[i] == 'O'.code.toByte() && tail[i+1] == 'g'.code.toByte() &&
                tail[i+2] == 'g'.code.toByte() && tail[i+3] == 'S'.code.toByte()) {
                lastOggS = i
            }
        }
        if (lastOggS == -1 || lastOggS + 14 > tail.size) return -1

        val bb = ByteBuffer.wrap(tail, lastOggS + 6, 8).order(ByteOrder.LITTLE_ENDIAN)
        return bb.long
    }
}