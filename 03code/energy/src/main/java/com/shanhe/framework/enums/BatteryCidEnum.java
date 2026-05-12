package com.shanhe.framework.enums;

import lombok.Getter;

import java.util.Objects;

/**
 * tcp命令编号
 *
 * @author wjh
 * @since 2025/3/17
 */
@Getter
public enum BatteryCidEnum {

    _82("82", "蓄电池实时数据"),

    _03("03", "设置电池组报警参数"),
    _83("83", "设置电池组报警参数响应"),

    _05("05", "设置系统状态响应"),
    _85("85", "设置系统状态响应-应答"),

    _86("86", "上传电池组配置信息"),
    _87("87", "上传电池组报警状态"),

    _08("08", "手动设置子模块ID"),
    _88("88", "手动设置子模块ID-应答"),

    _09("09", "配置电池组"),
    _89("89", "配置电池组响应-应答"),

    _0A("0A", "单个告警参数屏蔽"),
    _8A("8A", "响应单个告警参数屏蔽-应答"),

    _8B("8B", "上传电池组参数"),

    _0E("0E", "设备型号及软件版本号"),
    _8E("8E", "上传设备型号及软件版本号-应答"),

    _8F("8F", "响应连接条电阻测试-应答"),
    _8D("8D", "上传设备故障类告警状态"),


    _E0("E0", "设置蓄电池工作模式-应答"),
    _E1("E1", "响应浮充管理配置-应答"),
    _E2("E2", "响应内阻测试配置-应答"),
    _E3("E3", "响应连接条电阻测试配置-应答"),
    _E4("E4", "响应核容测试配置-应答"),

    _35("35", "响应备电时长测试配置"),
    _E5("E5", "响应备电时长测试配置-应答"),

    _36("36", "响应单个电池内阻测试"),
    _E6("E6", "响应单个电池内阻测试-应答"),

    _E7("E7", "响应修改日期时间-应答"),

    _38("38", "均衡设置"),
    _E8("E8", "均衡设置-应答"),

    _3B("3B", "电池组工作模式"),
    _EB("EB", "电池组工作模式-应答"),

    _E9("E9", "响应获取屏蔽报警参数-应答"),

    _18("18", "自动设置子模块ID"),
    _A8("A8", "自动设置子模块ID-应答"),

    _19("19", "设置内阻系数"),
    _99("99", "设置内阻系数-应答"),

    _20("20", "清鼓包数据"),
    _2A("2A", "清鼓包数据-应答"),

    _75("75", "恢复出厂设置"),
    _F5("F5", "恢复出厂设置-应答"),

    _78("78", "清组数据"),
    _F8("F8", "清组数据-应答"),

    _79("79", "清主机数据"),
    _F9("F9", "清主机数据-应答"),

    _76("76", "校正电池组数据"),
    _F6("F6", "校正电池组数据-应答"),

    _999("999", "冗余应答");

    private final String dictValue;

    private final String dictLabel;

    BatteryCidEnum(String dictValue, String dictLabel) {
        this.dictValue = dictValue;
        this.dictLabel = dictLabel;
    }

    public static BatteryCidEnum find(String dictValue) {
        for (BatteryCidEnum tcpCidEnum : BatteryCidEnum.values()) {
            if (Objects.equals(tcpCidEnum.getDictValue(), dictValue)) {
                return tcpCidEnum;
            }
        }
        return _999;
    }

    public static String getLabel(String dictValue) {
        for (BatteryCidEnum tcpCidEnum : BatteryCidEnum.values()) {
            if (Objects.equals(tcpCidEnum.getDictValue(), dictValue)) {
                return tcpCidEnum.getDictLabel();
            }
        }
        return "";
    }
}
