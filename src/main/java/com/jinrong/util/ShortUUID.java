package com.jinrong.util;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.UUID;

public class ShortUUID {
    private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    public static String compressToBase62() {
        UUID uuid = UUID.randomUUID();
        byte[] bytes = ByteBuffer.allocate(16)
                .putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits())
                .array();
        BigInteger number = new BigInteger(1, bytes);
        StringBuilder result = new StringBuilder();
        while (number.compareTo(BigInteger.ZERO) > 0) {
            BigInteger[] divmod = number.divideAndRemainder(BigInteger.valueOf(62));
            result.insert(0, BASE62.charAt(divmod[1].intValue()));
            number = divmod[0];
        }
        return result.toString();
    }
}