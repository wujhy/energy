package com.shanhe.project.energy.stat.vo;

import lombok.Data;

// 修改为public类，以便其他包可以访问
@Data
public class EvaluationFactors {
    // 名称
    private String name;
    // 值
    private String value;
    // 是否报警 0 报警
    private Integer isAlarm;

    public EvaluationFactors() {
    }
    public EvaluationFactors(String name, String value, Integer isAlarm) {
        this.name = name;
        this.value = value;
        this.isAlarm = isAlarm;
    }
}
