package com.shanhe.project.sync.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 设备接口信息
 */
@Data
@Accessors(chain = true)
public class DeviceVo implements Serializable {
    /**
     * 设备主键ID
     */
    private Long devId;

    /**
     * 设备编号
     */
    private String devCode;

    /**
     * 设备名称
     */
    private String devName;

    /**
     * 设备IMEI(设备ID)
     */
    private String deviceId;

    /**
     * 设备连接主端口地址
     */
    private Integer parentSn;

    /**
     * 设备连接子端口地址
     */
    private Integer sonSn;

    /**
     * 设备分类,枚举值信息
     */
    private String classId;
    /**
     * 子设备类型
     */
    private String subClassId;
    /** 设备类型编码 */
    private String typeCode;
    /**
     * 设备类型ID
     */
    private Long typeId;

    /**
     * 所属主机ID，0是自身
     */
    private Long hostDevId;

    /**
     * 设备MAC地址
     */
    private String macAddr;

    /**
     * 软件版本号
     */
    private String softNum;

    /**
     * 设备状态0禁用，1启用
     */
    private Integer status;

    /**
     * 流媒体流地址
     */
    private String mediaAddr;

    /**
     * 生产厂家
     */
    private String manufacturer;

    /**
     * 规格型号
     */
    private String model;

    /**
     * 是否删除0否1是
     */
    private Integer isDelete;

    /**
     * 使用时间
     */
    private Date useTime;
    /**
     * 数据上报间隔
     */
    private Integer spaceTime;
    /**
     * 端口类型
     */
    private Integer portType;

    /**
     * 波特率
     */
    private Integer baudRate;

    /**
     * 数据位
     */
    private Integer bitData;

    /**
     * 停止位
     */
    private Integer bitStop;

    /**
     * 校验位
     */
    private Integer parityCheck;

    /**
     * 轮询间隔
     */
    private Integer pollTime;
    /**
     * 蓄电池组
     */
    private List<BatteryVo> childDev;
}
