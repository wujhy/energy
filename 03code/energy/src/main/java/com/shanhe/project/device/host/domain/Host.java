package com.shanhe.project.device.host.domain;

import lombok.Data;
import com.shanhe.framework.aspectj.lang.annotation.Excel;
import com.shanhe.framework.web.domain.BaseEntity;
import lombok.EqualsAndHashCode;

/**
 * 主机对象 dev_host
 * 
 * @author wjh
 * @since 2024-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Host extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主机id */
    private Long hostId;

    /** 主机名 */
    @Excel(name = "主机名")
    private String name;
    /** 主机账号 */
    private String account;
    /** 主机密码 */
    private String password;

    /** 主机状态 0-未连接，1-已连接，2-连接异常 */
    private Integer status;

    /** 图片 */
    @Excel(name = "图片")
    private String picture;

    /** 联系地址 */
    @Excel(name = "联系地址")
    private String address;

    /** 经度 */
    @Excel(name = "经度")
    private String lng;

    /** 纬度 */
    @Excel(name = "纬度")
    private String lat;

    /** 联系人 */
    @Excel(name = "联系人")
    private String contacts;

    /** 联系电话 */
    @Excel(name = "联系电话")
    private String phone;

    /** 本地IP地址 */
    @Excel(name = "本地IP地址")
    private String ip;
    /** IP网卡 */
    private String ipAddr;

    /** 子网IP地址 */
    @Excel(name = "子网IP地址")
    private String subIp;

    /** 网关IP地址 */
    @Excel(name = "网关IP地址")
    private String netIp;

    /** 本地端口号 */
    @Excel(name = "本地端口号")
    private Integer port;

    /** 设备IP地址 */
    @Excel(name = "设备IP地址")
    private String deviceIp;

    /** 设备端口号 */
    @Excel(name = "设备端口号")
    private Integer devicePort;

    /** 是否需要上报，0：是，1：否 */
    private Integer needReport;

    /** 服务器IP地址 */
    @Excel(name = "服务器IP地址")
    private String reportIp;

    /** 服务器端口号 */
    @Excel(name = "服务器端口号")
    private Integer reportPort;

    /** 主机类型 */
    @Excel(name = "主机产品类型")
    private String type;

    /** 主机设备类型 */
    @Excel(name = "主机设备类型")
    private Integer deviceType;

    /** 子接口总数目 */
    @Excel(name = "子接口总数目")
    private String num;

    @Excel(name = "MAC地址")
    private String mac;

    @Excel(name = "设备IMEI")
    private String imei;

    @Excel(name = "设备型号")
    private String model;

    @Excel(name = "设备版本号")
    private String version;
    private String softVersion;

    /** 数据上报间隔时间 */
    @Excel(name = "数据上报间隔时间，秒")
    private Integer spaceTime;
    /** 设备数据上报间隔时间 秒*/
    private Integer deviceSpaceTime;
    /** 设备数据存储间隔时间 分钟*/
    private Integer storageTime;
    /** 设备数据删除时间 天*/
    private Integer cleanLogDays;

    /** 组态配置 */
    private String configData;

    private String extend1;
    private String extend2;
    private String extend3;
    private String extend4;
    /** 创建时间 */
    private String createTimeStr;
}
