package com.shanhe.framework.enums;

import com.shanhe.common.utils.bean.Dict;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 协议参数
 *
 * @author wjh
 * @since 2025/8/11
 */
@Getter
public enum ProtocolParamEnum {

    CHANNEL("{channel}", "串口号"),
    CHECK_SUM("{checkSum}", "校验码"),
    CHECK_CRC("{checkCrc16}", "crc校验码"),
    CHECK_LRC("{checkLrc16}", "lrc校验码"),
    CHECK_256("{check256}", "256校验码");

    private final String dictValue;

    private final String dictLabel;

    ProtocolParamEnum(String dictValue, String dictLabel) {
        this.dictValue = dictValue;
        this.dictLabel = dictLabel;
    }

    /**
     * 通过值查标签名
     */
    public static String findByValue(Object value) {
        for (ProtocolParamEnum dictEnum : ProtocolParamEnum.values()) {
            if (Objects.equals(dictEnum.getDictValue(), String.valueOf(value))) {
                return dictEnum.getDictLabel();
            }
        }
        return null;
    }

    /**
     * 转list
     */
    public static List<Dict> getDictList() {
        List<Dict> list = new ArrayList<>();
        for (ProtocolParamEnum dictEnum : ProtocolParamEnum.values()) {
            list.add(new Dict(dictEnum.getDictLabel(), dictEnum.getDictValue()));
        }
        return list;
    }
}
