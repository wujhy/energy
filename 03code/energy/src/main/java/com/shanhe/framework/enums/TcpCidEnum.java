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
    _D1("D1", "响应设置配置参数"),
    _D2("D2", "响应设置串口存储指令包"),
    _D3("D3", "串口存储指令包应答"),
    _54("54", "串口读指令"),
    _D4("D4", "响应串口读指令"),
    _D5("D5", "响应读取模拟量"),
    _D6("D6", "响应设置输出模拟量"),
    _D7("D7", "响应读取开关量"),
    _58("58", "设置输出开关量"),
    _D8("D8", "响应设置输出开关量"),
    _DA("DA", "响应删除全部存储指令"),
    _DB("DB", "响应删除单条存储指令"),
    _DD("DD", "响应读取全部存储指令"),
    _B0("B0", "响应读取配置参数"),
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
