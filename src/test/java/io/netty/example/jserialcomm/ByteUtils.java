package io.netty.example.jserialcomm;

public class ByteUtils {


    /**
     * byte字节数组转换为Hex字符串
     */
    public static String parseByteToHexStr(byte[] buf) {
        // Create Hex String
        StringBuilder hexString = new StringBuilder();
        for (byte aBuf : buf) {
            String hex = Integer.toHexString(aBuf & 0xFF);
            if (hex.length() == 1) {
                hexString.append(0);
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Hex字符串转换为byte字节数组
     */
    public static byte[] parseHexStrToByte(String hexStr) {
        if (hexStr == null || hexStr.isEmpty()) {
            return new byte[]{};
        }

        // 检查字符串长度是否是偶数
        if (hexStr.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string length must be even.");
        }

        byte[] result = new byte[hexStr.length() / 2];
        for (int i = 0; i < hexStr.length() / 2; i++) {
            int value = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 2), 16);
            result[i] = (byte) value;
        }
        return result;
    }
}
