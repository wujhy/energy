package com.shanhe.framework.enums;

import com.shanhe.common.utils.bean.Dict;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 操作类型枚举
 *
 * @author wjh
 * @since 2024/12/19
 */
@Getter
public enum OperationType {

    /** 其他 */
    OTHER(0, "其他"),
    /** 新增 */
    INSERT(1, "新增"),
    /** 修改 */
    UPDATE(2, "修改"),
    /** 删除 */
    DELETE(3, "删除"),
    /** 授权 */
    GRANT(4, "授权"),
    /** 导出 */
    EXPORT(5, "导出"),
    /** 导入 */
    IMPORT(6, "导入"),
    /** 强退 */
    FORCE(7, "强退"),
    /** 清空数据 */
    CLEAN(8, "清空数据");

    private final Integer dictValue;

    private final String dictLabel;

    OperationType(Integer dictValue, String dictLabel) {
        this.dictValue = dictValue;
        this.dictLabel = dictLabel;
    }

    /** 通过值查标签名 */
    public static String findByValue(Object value) {
        Integer dictValue;
        if (value instanceof String) {
            dictValue = Integer.valueOf((String) value);
        } else {
            dictValue = (Integer) value;
        }
        for (OperationType dictEnum : OperationType.values()) {
            if (Objects.equals(dictEnum.getDictValue(), dictValue)) {
                return dictEnum.getDictLabel();
            }
        }
        return null;
    }

    /** 转list */
    public static List<Dict> getDictList() {
        List<Dict> list = new ArrayList<>();
        for (OperationType dictEnum : OperationType.values()) {
            list.add(new Dict(dictEnum.getDictLabel(), dictEnum.getDictValue()));
        }
        return list;
    }
}
