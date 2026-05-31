package org.zero.qes

object QesNative {
    init {
        System.loadLibrary("qes_core")
    }

    external fun selfTest(): String

    external fun encryptText(
        plaintext: String,
        password: String,
        seed1: String,
        seed2: String,
        seed3: String,
        seed4: String,
        glyph: String,
        value: Int,
        vector: String,
        phase: Long,
        amplitude: Int,
    ): String

    external fun decryptText(
        packageText: String,
        password: String,
        seed1: String,
        seed2: String,
        seed3: String,
        seed4: String,
        glyph: String,
        value: Int,
        vector: String,
        phase: Long,
        amplitude: Int,
    ): String

    external fun encryptBytes(
        data: ByteArray,
        password: String,
        seed1: String,
        seed2: String,
        seed3: String,
        seed4: String,
        glyph: String,
        value: Int,
        vector: String,
        phase: Long,
        amplitude: Int,
    ): ByteArray

    external fun decryptBytes(
        packageBytes: ByteArray,
        password: String,
        seed1: String,
        seed2: String,
        seed3: String,
        seed4: String,
        glyph: String,
        value: Int,
        vector: String,
        phase: Long,
        amplitude: Int,
    ): ByteArray
}
