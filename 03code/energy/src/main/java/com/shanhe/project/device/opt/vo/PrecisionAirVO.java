package com.shanhe.project.device.opt.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 精密空调参数
 *
 * @author wjh
 * @since 2025/7/3
 */
@Data
@Accessors(chain = true)
public class PrecisionAirVO implements Serializable {

    /**
     * 设备主键ID
     */
    @NotNull(message = "设备主键ID不能为空")
    private Long configId;
    /**
     * 控制指令 1:机组开关设定 2:机组模式设定 3:制冷温度设定值 4:制热温度设定值 5:温度报警上限 6:温度报警下限 7:内风机设定风速 8:首次上电压缩机延时时间
     */
    @NotNull(message = "控制指令不能为空")
    private Integer cmdType;
    /**
     * 设置值
     */
    @NotBlank(message = "指令参数错误")
    private String value;
}
