package com.shanhe.project.device.opt.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 指令测试
 *
 * @author wjh
 * @since 2025/7/3
 */
@Data
@Accessors(chain = true)
public class CmdDebugVO implements Serializable {

    /**
     * 设备主键ID
     */
    private Long configId;
    /**
     * 指令
     */
    @NotNull(message = "指令不能为空")
    @Size(min = 15, max = 300, message = "指令长度不符合")
    private String cmd;
    /**
     * 指令类型 0：完整指令，直接执行，1、info指令，封装
     */
    private Integer cmdType;
    /**
     * 是否回调
     */
    private Integer callback;
}
