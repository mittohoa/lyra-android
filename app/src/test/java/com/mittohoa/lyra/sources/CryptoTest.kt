package com.mittohoa.lyra.sources

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Kiem bang VECTOR CHUAN, khong phai bang chinh ket qua cua minh.
 *
 * Ba phep nay khong the "gan dung": sai mot bit la Zing tra ve "Incorect
 * signature" va file .lrc cua NCT ra rac. Doi chieu voi dap an cong bo cua RFC
 * va cua tai lieu RC4 thi moi biet chac la dung.
 */
class CryptoTest {

    @Test
    fun `SHA-256 chuoi rong khop dap an chuan`() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            sha256Hex("")
        )
    }

    @Test
    fun `SHA-256 chuoi abc khop dap an chuan`() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            sha256Hex("abc")
        )
    }

    @Test
    fun `HMAC-SHA512 khop vector cua RFC 4231`() {
        // RFC 4231, truong hop 2
        assertEquals(
            "164b7a7bfcf819e2e395fbe73b56e0a387bd64222e831fd610270cd7ea2505549758bf75c05a994a" +
                "6d034f65f8f0e6fdcaeab1a34d4a6b4b636e070a38bce737",
            hmacSha512Hex("what do ya want for nothing?", "Jefe")
        )
    }

    @Test
    fun `RC4 khop vector chuan`() {
        // Vector kinh dien: khoa "Key", ban ro "Plaintext"
        val out = rc4("Key".toByteArray(), "Plaintext".toByteArray())
        assertEquals("bbf316e8d940af0ad3", out.toHex())
    }

    @Test
    fun `RC4 giai ma la chinh no`() {
        val key = "keyDecryptLyric".toByteArray()
        val plain = "[00:12.34]Dòng lời tiếng Việt có dấu".toByteArray()
        assertArrayEquals(plain, rc4(key, rc4(key, plain)))
    }

    @Test
    fun `doi hex qua lai khong mat gi`() {
        val bytes = byteArrayOf(0, 1, 15, 16, -1, -128, 127)
        assertArrayEquals(bytes, bytes.toHex().hexToBytesOrNull())
    }

    @Test
    fun `hex hong thi tra ve null chu khong no`() {
        assertNull("abc".hexToBytesOrNull())      // le so ky tu
        assertNull("zz".hexToBytesOrNull())       // khong phai hex
        assertNull("".hexToBytesOrNull())         // rong
    }
}
