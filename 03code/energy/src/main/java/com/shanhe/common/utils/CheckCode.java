package com.shanhe.common.utils;

/**
 * 校验码
 *
 * @author wjh
 * @since 2025/7/31
 */
public class CheckCode {

    /**
     * 计算校验码 - 所有字节的模256的和
     */
    public static String check256(String tempStr) {
        if (tempStr == null || tempStr.isEmpty()) {
            return "";
        }
        // 去除空格
        tempStr = tempStr.replaceAll(" ", "");
        byte[] data = new byte[tempStr.length() / 2];

        for (int i = 0; i < tempStr.length() / 2; i++) {
            String itemStr = tempStr.substring(i * 2, i * 2 + 2);
            // 使用16进制解析
            data[i] = (byte) Integer.parseInt(itemStr, 16);
        }

        int count = 0;
        for (byte datum : data) {
            // 使用 & 0xFF 确保按无符号字节处理
            count += datum & 0xFF;
        }
        // 模256运算，等同于与0xFF进行按位与操作
        int checksum = count & 0xFF;
        // 转换为16进制字符串，确保是两位数
        return String.format("%02X", checksum);
    }
}
