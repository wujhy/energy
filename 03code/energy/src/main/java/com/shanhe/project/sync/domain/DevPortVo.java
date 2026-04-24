package com.shanhe.project.sync.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 同步串口VO
 */
@Data
@Accessors(chain = true)
public class DevPortVo implements Serializable {
    /** 串口ID */
    private Long portId;

    /** 设备主键ID */
    private Long devId;

    /** 串口编号 */
    private Integer parentSn;

    /** 模块地址，端口下的自设备序号 */
    private Integer sonSn;

    /** 串口占用设备分类,枚举值信息 */
    private String classId;

    /** 串口类型0无效1RS485,2RS232 */
    private Integer portType;

    /** 波特率 */
    private Integer baudRate;

    /** 数据位 */
    private Integer bitData;

    /** 停止位  ：0：1位,1：1.5位,2：2位（1 字节） */
    private Integer bitStop;

    /** 奇偶校验：0：None,1：Odd,2：Even,3：Mark,4：Space（1字节） */
    private Integer parityCheck;

    /** 数据轮询间隔时间(单位ms) */
    private Integer pollTime;
    /** 默认值 */
    private String value;
    /** 默认值描述 */
    private String valueDesc;
}
