package com.shanhe.project.device.opt.vo;

import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotNull;

/**
 * 开关控制VO
 *
 * @author wjh
 * @since 2025/7/10
 */
@Data
public class SwitchVO {
    /**
     * 设备ID
     */
    @NotNull(message = "设备ID不能为空")
    private Long configId;
    /**
     * 开关,1打开0闭合
     */
    @NotNull(message = "开关值不能为空")
    @Range(min = 0, max = 1, message = "开关值只能为0或1")
    private Integer paramValue;
}
