package com.shanhe.project.sync.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 设备采集指令
 */
@Data
@Accessors(chain = true)
public class AlarmOrderVo implements Serializable {
    /**
     * 指令模版ID
     */
    private Long temCmdId;

    /** 设备主键ID */
    private Long devId;

    /**
     * 协议类型1自定义协议2modbus协议
     */
    private Integer protocolType;

    /**
     * 指令类型52设置串口存储指令包，54串口读指令，57读取开关量
     */
    private String cmdType;

    /**
     * 指令名称不能为空
     */
    private String cmdName;

    /**
     * 校验算法 0 无校验 1 CRC16 2 杉和累加和算法
     */
    private Integer checksumAlgorithm;

    /**
     * 指令编号,系统指定2位直接标记码，在解析时用于标记指令数据
     */
    private String enCode;

    /**
     * 设备协议内容，只存放设备的指令，不包含动环机的协议
     */
    private String cmdContent;

    /**
     * 状态0不启用1启用
     */
    private Integer status;

    /**
     * 解析指令
     */
    private List<AlarmRuleVo> listRule;
}
