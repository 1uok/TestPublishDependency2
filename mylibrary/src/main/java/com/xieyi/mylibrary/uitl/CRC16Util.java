package com.xieyi.mylibrary.uitl;


public class CRC16Util {


    /**
     * 将int转换成byte数组，高位在前，低位在后
     * 改变高低位顺序只需调换数组序号
     */
    private static byte[] intToBytes(int value) {
        byte[] src = new byte[2];
        src[1] = (byte) (value & 0xFF);
        src[0] = (byte) ((value >> 8) & 0xFF);
        return src;
    }

    public static int byteArrayToIntFour(byte[] b) {
        return b[3] & 0xFF | (b[2] & 0xFF) << 8 | (b[1] & 0xFF) << 16 | (b[0] & 0xFF) << 24;
    }

    public static int byteArrayToInt(byte[] b) {
        return b[1] & 0xFF |
                (b[0] & 0xFF) << 8;
    }

    /**
     * 将int转为高字节在前，低字节在后的byte数组
     */
    public static byte[] toHH(int n) {
        byte[] b = new byte[4];
        b[3] = (byte) (n & 0xff);
        b[2] = (byte) (n >> 8 & 0xff);
        b[1] = (byte) (n >> 16 & 0xff);
        b[0] = (byte) (n >> 24 & 0xff);
        return b;
    }

    public static byte[] toHH6(long n) {
        byte[] b = new byte[6];
        b[5] = (byte) (n & 0xff);
        b[4] = (byte) (n >> 8 & 0xff);
        b[3] = (byte) (n >> 16 & 0xff);
        b[2] = (byte) (n >> 24 & 0xff);
        b[1] = (byte) (n >> 32 & 0xff);
        b[0] = (byte) (n >> 40 & 0xff);
        return b;
    }

    public static byte[] toOneByte(int value) {
        byte[] src = new byte[1];
        src[0] = (byte) (value & 0xFF);
        return src;
    }

    public static byte[] CRC_XModem(byte[] bytes) {
        int crc = 0x00;
        int polynomial = 0x1021;
        for (int index = 0; index < bytes.length; index++) {
            byte b = bytes[index];
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b >> (7 - i) & 1) == 1);
                boolean c15 = ((crc >> 15 & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit) crc ^= polynomial;
            }
        }
        crc &= 0xffff;
        return intToBytes(crc);
    }

}
