package org.zero.qes;

public final class QesNative {
    static {
        System.loadLibrary("qes_quantum_encryption_system");
    }

    private QesNative() {}

    public static native String selfTest();

    public static native String encryptText(
            String plaintext,
            String password,
            String seed1,
            String seed2,
            String seed3,
            String seed4,
            String glyph,
            int value,
            String vector,
            long phase,
            int amplitude
    );

    public static native String decryptText(
            String packageText,
            String password,
            String seed1,
            String seed2,
            String seed3,
            String seed4,
            String glyph,
            int value,
            String vector,
            long phase,
            int amplitude
    );

    public static native byte[] encryptBytes(
            byte[] data,
            String password,
            String seed1,
            String seed2,
            String seed3,
            String seed4,
            String glyph,
            int value,
            String vector,
            long phase,
            int amplitude
    );

    public static native byte[] decryptBytes(
            byte[] packageBytes,
            String password,
            String seed1,
            String seed2,
            String seed3,
            String seed4,
            String glyph,
            int value,
            String vector,
            long phase,
            int amplitude
    );
    public static native byte[] encryptStreamBlock(
            byte[] data,
            String password,
            String seed1,
            String seed2,
            String seed3,
            String seed4,
            String glyph,
            int value,
            String vector,
            long phase,
            int amplitude,
            String streamMode,
            long blockIndex
    );

    public static native byte[] decryptStreamBlock(
            byte[] packageBytes,
            String password,
            String seed1,
            String seed2,
            String seed3,
            String seed4,
            String glyph,
            int value,
            String vector,
            long phase,
            int amplitude,
            String streamMode,
            long blockIndex
    );


}
