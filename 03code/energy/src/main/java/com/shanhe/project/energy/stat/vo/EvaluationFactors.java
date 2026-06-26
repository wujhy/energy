package com.shanhe.project.energy.stat.vo;

import lombok.Data;

/**
 * 评估因素
 *
 * @author wjh
 * @since 2026-05-25
 */
@Data
public class EvaluationFactors {
    /** 名称 */
    private String name;
    /** 值 */
    private String value;
    /** 是否报警：0-报警 */
    private Integer isAlarm;

    public EvaluationFactors() {
    }
    public EvaluationFactors(String name, String value, Integer isAlarm) {
        this.name = name;
        this.value = value;
        this.isAlarm = isAlarm;
    }
}
