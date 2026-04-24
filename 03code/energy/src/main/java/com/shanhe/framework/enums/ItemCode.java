package com.shanhe.framework.enums;

import lombok.Getter;

/**
 * 电池告警参数
 *
 * @author wjh
 * @since 2025/4/15
 */
@Getter
public enum ItemCode {
    /** 单体电压过充告警 */
    DTDYGC("dtdygc", "01", 0, 2),
    /** 单体电压过放告警 */
    DTDYGF("dtdygf", "03", 2, 4),
    /** 单体浮充电压过高告警 */
    DTFCDYG("dtfcdyg", "05", 4, 6),
    /** 单体浮充电压过低告警 */
    DTFCDYD("dtfcdyd", "07", 6, 8),
    /** 单体电压不均告警 */
    DTDYBJ("dtdybj", "09", 8, 10),
    /** 单体电压极差告警 */
    DTDYJC("dtdyjc", "11", 10, 12),
    /** 组电压过充告警 */
    ZDYGC("zdygc", "13", 12, 14),
    /** 组电压过放告警 */
    ZDYGF("zdygf", "15", 14, 16),
    /** 总体浮充电压过高告警 */
    ZFCDYGG("zfcdygg", "17", 16, 18),
    /** 总体浮充电压过低告警 */
    ZFCDYGD("zfcdygd", "19", 18, 20),
    /** 电流过充告警 */
    ZCGDLGJ("zcgdlgj", "21", 20, 22),
    /** 环境高温告警 */
    ZWDG("zwdg", "25", 24, 26),
    /** 环境低温告警 */
    ZWDD("zwdd", "27", 26, 28),
    /** 电池高温告警 */
    DTDCWDG("dtdcwdg", "31", 28, 30),
    /** 电池低温告警 */
    DTDCWDD("dtdcwdd", "33", 30, 32),
    /** 电池温度不均告警 */
    DTDCWDBJ("dtdcwdbj", "35", 32, 34),
    /** 内阻过大告警系数 */
    DTNZGD("dtnzgd", "37", 34, 36),
    /** 内阻不均告警系数 */
    DTNZBJ("dtnzbj", "39", 36, 38),
    /** 内阻过小告警系数 */
    DTNZGX("dtnzgx", "41", 38, 40),
    /** 连接条告警 */
    DTLJTGJ("dtljtgj", "45", 40, 42),
    /** SOC低告警 */
    ZSOCDGJ("zsocdgj", "49", 42, 44),
    /** SOH低告警 */
    ZSOHDGJ("zsohdgj", "51", 44, 46),
    /** 鼓包告警 */
    DTGB("dtgb", "51", 44, 46),
    /** 单体电池开路异常 */
    DTDCKL("dtdckl", "55", 48, 49),
    /** 单体通信异常 */
    DTTXZT("dttxzt", "56", 49, 50),
    /** 漏液告警 */
    DTLYGJ("dtlygj", "57", 50, 51),
    /** 电池温度传感器故障 */
    DTWDCGQGZ("dtwdcgqgz", "58", 51, 52),
    /** 组压通讯状态 */
    TXZT("txzt", "59", 52, 53),
    /** 环境温度传感器1故障 */
    ZWDCGQ1GZ("zwdcgq1gz", "5A", 53, 54),
    /** 环境温度传感器2故障 */
    ZWDCGQ2GZ("zwdcgq2gz", "5B", 54, 55),
    /** 网络故障 */
    ZWLGZ("zwlgz", "5C", 55, 56),
    /** 停电告警 */
    ZTDGJ("ztdgj", "5D", 56, 57),
    /** 蓄电池脱离母线 */
    ZTLMXGJ("ztlmxgj", "5E", 57, 58);

    private final String code;
    private final String paramsNumber;
    private final int start;
    private final int end;

    ItemCode(String code, String paramsNumber, int start, int end) {
        this.code = code;
        this.paramsNumber = paramsNumber;
        this.start = start;
        this.end = end;
    }

    public static ItemCode fromCode(String code) {
        for (ItemCode item : values()) {
            if (item.getCode().equals(code)) {
                return item;
            }
        }
        return null;
    }

    public static String getParamsNumber(String itemCode) {
        for (ItemCode ic : ItemCode.values()) {
            if (ic.code.equals(itemCode)) {
                return ic.paramsNumber;
            }
        }
        return null;
    }

}
