package com.shanhe.common.utils.uuid;

import cn.hutool.core.lang.Snowflake;

import java.util.Random;

/**
 * ID生成器工具类
 *
 * @author wjh
 * @since 2024/12/19
 */
public class IdUtils {

    private static final Snowflake SNOWFLAKE = new Snowflake(0L, 0L);

    public static long getSnowflakeId() {
        return SNOWFLAKE.nextId();
    }

    /**
     * 基于雪花ID生成随机十位整数
     */
    public static String genImei() {
        // 获取雪花算法生成的id
        long snowflakeId = getSnowflakeId();
        // 按位运算得到时间戳（右移22位移出 机器码和序列号）
        long timestamp = (snowflakeId >> 22) & 0x1FFFFFFFFFFL;
        // 机器码
        long machine = (snowflakeId >> 12) & 0xFFL;
        // 时间戳和序列号拼接结果
        long result = timestamp * 10L + machine;
        // 随机数
        result += new Random().nextInt(10);

        // 返回10位整数,位数不足则补0
        return String.format("%s%08d", "18", result % 100000000L);
    }

    /**
     * 获取随机UUID
     * 
     * @return 随机UUID
     */
    public static String randomUuid() {
        return UUID.randomUuid().toString();
    }

    /**
     * 获取随机UUID
     *
     * @return 随机UUID
     */
    public static Long longId() {
        UUID uuId = UUID.randomUuid();
        return Math.abs(uuId.getMostSignificantBits());
    }

    /**
     * 简化的UUID，去掉了横线
     * 
     * @return 简化的UUID，去掉了横线
     */
    public static String simpleUuid() {
        return UUID.randomUuid().toString(true);
    }

    /**
     * 获取随机UUID，使用性能更好的ThreadLocalRandom生成UUID
     * 
     * @return 随机UUID
     */
    public static String fastUuid() {
        return UUID.fastUuid().toString();
    }

    /**
     * 简化的UUID，去掉了横线，使用性能更好的ThreadLocalRandom生成UUID
     * 
     * @return 简化的UUID，去掉了横线
     */
    public static String fastSimpleUuid() {
        return UUID.fastUuid().toString(true);
    }
}
