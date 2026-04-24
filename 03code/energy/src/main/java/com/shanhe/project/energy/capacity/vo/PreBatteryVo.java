package com.shanhe.project.energy.capacity.vo;

import lombok.Data;

import java.util.Date;

/**
 * 电池预测单体信息
 */
@Data
public class PreBatteryVo implements Cloneable {
    // 单体编号
    private int batNum;
    // 额定容量
    private Double acapacity;
    // 开始电压
    private Double startVoltage;
    // 截止电压
    private Double endVoltage;
    // 温度
    private Double temperature;
    // 内阻值
    private Integer resistance;
    // 预测容量
    private Double bcapacity;
    // 统计时间
    private Date staticTime;

    private static PreBatteryVo preBatteryInfo = new PreBatteryVo();

    /**
     * 复制克隆对象
     *
     * @return
     */
    public static PreBatteryVo getNewPreBatteryInfo() {
        try {
            return (PreBatteryVo) preBatteryInfo.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return new PreBatteryVo();
    }
}
