package com.shanhe.framework.enums;

import lombok.Getter;

import java.util.Objects;

/**
 * 内阻测试状态枚举
 *
 * @author wjh
 * @since 2026-05-26
 */
@Getter
public enum ResistanceTestStatusEnum {

    NOT_TESTING("0", "不在内阻测试"),
    TESTING("6", "正在内阻测试"),
    NORMAL_END("7", "内阻测试正常结束"),
    ABNORMAL_END("8", "内阻测试异常结束");

    private final String code;
    private final String label;

    ResistanceTestStatusEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static ResistanceTestStatusEnum find(String code) {
        for (ResistanceTestStatusEnum status : values()) {
            if (Objects.equals(status.code, code)) {
                return status;
            }
        }
        return null;
    }

    public static boolean isCode(String code, ResistanceTestStatusEnum expected) {
        return Objects.equals(code, expected.code);
    }
}
