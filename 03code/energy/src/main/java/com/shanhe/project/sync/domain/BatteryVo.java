package com.shanhe.project.sync.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 设备接口信息
 */
@Data
@Accessors(chain = true)
public class BatteryVo implements Serializable {
    /**
     * 蓄电池组ID
     */
    private Long batPackId;
    /**
     * 设备主键ID，蓄电池ID
     */
    private Long devId;

    /**
     * 蓄电池主编号，1,2,3,4
     */
    private Integer packNum;

    /**
     * 蓄电池单体个数
     */
    private Integer batSinSize;

    /**
     * 单体电池规格(1=1.2V 2=2V 3=2.4V 4=3.2V 5=3.9V 6=4.8V 7=6V 8=12V)
     */
    private Integer batSinModel;

    /**
     * 蓄电池品牌
     */
    private String batBrand;

    /**
     * 蓄电池型号规格
     */
    private String batModel;

    /**
     * 电池容量
     */
    private Double batCapacity;

    /**
     * 生成日期
     */
    private Date producedDate;

    /**
     * 是否启用0否1是
     */
    private Integer isEnabled;
    /**
     * 是否允许备电测试0是1否
     */
    private Integer isAllowPower;
    /**
     * 是否删除0否1是
     */
    private Integer isDelete;
    /**
     * 连接条是否显示 0 否 1 是
     */
    private Integer isShowConnect;

}
