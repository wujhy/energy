package com.shanhe.framework.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

import java.util.Objects;

/**
 * tcp命令编号
 *
 * @author wjh
 * @since 2025/3/17
 */
@Getter
public enum TcpCidEnum {

    _80("80", "心跳包"),
    _88("88", "注册包"),
    _50("50", "设置系统数据上报时间"),
    _D0("D0", "响应设置系统数据上报时间"),
    _51("51", "设置配置参数"),
    _D1("D1", "响应设置配置参数"),
    _D2("D2", "响应设置串口存储指令包"),
    _D3("D3", "串口存储指令包应答"),
    _54("54", "串口读指令"),
    _D4("D4", "响应串口读指令"),
    _55("55", "读取模拟量"),
    _D5("D5", "响应读取模拟量"),
    _56("56", "设置输出模拟量"),
    _D6("D6", "响应设置输出模拟量"),
    _57("57", "读取开关量"),
    _D7("D7", "响应读取开关量"),
    _58("58", "设置输出开关量"),
    _D8("D8", "响应设置输出开关量"),
    _76("76", "校正模拟量数据"),
    _F6("F6", "响应校正模拟量数据"),
    _DA("DA", "响应删除全部存储指令"),
    _5B("5B", "删除单条存储指令"),
    _DB("DB", "响应删除单条存储指令"),
    _5D("5D", "读取全部存储指令"),
    _DD("DD", "响应读取全部存储指令"),
    _37("37", "修改日期时间"),
    _E7("E7", "响应修改日期时间"),
    _5E("5E", "设置设备IP地址"),
    _DE("DE", "响应设置设备IP地址"),
    _5F("5F", "设置云服务器IP地址"),
    _DF("DF", "响应设置云服务器IP地址"),
    _0C("0C", "设置模块ID"),
    _8C("8C", "响应设置模块ID"),
    _60("60", "读取配置参数"),
    _B0("B0", "响应读取配置参数"),
    _61("61", "读取设备IP地址"),
    _B1("B1", "响应读取设备IP地址"),
    _62("62", "读取云服务器IP地址"),
    _B2("B2", "响应读取云服务器IP地址"),
    _63("63", "读取系统数据上报时间"),
    _B3("B3", "响应读取系统数据上报时间"),
    _error("error", "错误处理");

    private final String dictValue;

    private final String dictLabel;

    TcpCidEnum(String dictValue, String dictLabel) {
        this.dictValue = dictValue;
        this.dictLabel = dictLabel;
    }

    public static TcpCidEnum find(String dictValue) {
        if (StrUtil.isBlank(dictValue)) {
            return _error;
        }
        for (TcpCidEnum tcpCidEnum : TcpCidEnum.values()) {
            if (Objects.equals(tcpCidEnum.getDictValue(), dictValue)) {
                return tcpCidEnum;
            }
        }
        return _error;
    }

}
