package com.shanhe.project.device.opt.vo;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;

/**
 * 蓄电池设置VO
 *
 * @author wjh
 * @since 2025/7/10
 */
@Data
public class BatterySetVO {
    /* 设备ID */
    public interface cmd{}
    /* 设备ID及包序号 */
    public interface cmd1{}
    /* 手动设置子模块ID */
    public interface cmd08{}
    /* 配置电池组 */
    public interface cmd09{}
    /* 自动设置子模块ID */
    public interface cmd18{}
    /* 内阻系数 */
    public interface cmd19{}
    /* 内阻基准值 */
    public interface cmd119{}
    /* 清鼓包值 */
    public interface cmd20{}
    /* 日期设置 */
    public interface cmd37{}
    /* 均衡设置 */
    public interface cmd38{}
    /* 清组数据 */
    public interface cmd78{}
    /* 蜂鸣器 */
    public interface cmd39{}
    /* 电池校正 */
    public interface cmd76{}

    /** 需要异步等待响应 */
    Boolean needDynResult = true;

    /** 设备ID */
    @NotNull(message = "设备ID不能为空", groups = {cmd08.class, cmd09.class, cmd18.class, cmd19.class, cmd1.class, cmd119.class, cmd20.class, cmd37.class, cmd38.class, cmd39.class, cmd78.class, cmd.class})
    private Long configId;
    /** 包序号 */
    @NotNull(message = "电池组不能为空", groups = {cmd08.class, cmd09.class, cmd18.class, cmd19.class, cmd1.class, cmd119.class, cmd20.class, cmd78.class, cmd76.class})
    private Integer packNum;

    /** 模块ID */
    @NotNull(message = "单体号不能为空", groups = {cmd08.class, cmd76.class})
    private Integer modelNum;
    /** 模块ID */
    @NotNull(message = "新单体号不能为空", groups = {cmd08.class})
    private Integer newModelNum;

    /** 内阻系数 */
    @NotNull(message = "内阻系数不能为空", groups = {cmd19.class})
    private Float resistance;

    /** 内阻基准值 */
    @NotNull(message = "内阻基准值不能为空", groups = {cmd119.class})
    @Max(value = 65535, message = "内阻基准值不能大于65535", groups = {cmd119.class})
    private Integer resistanceStandValue;

    /** 自动均衡 */
    @NotNull(message = "自动均衡不能为空", groups = {cmd38.class})
    private Integer autoBalanced;

    /** 手动均衡 */
    @NotNull(message = "手动均衡不能为空", groups = {cmd38.class})
    private Integer manualBalanced;

    /** 蜂鸣器状态 0 开 1 关闭 */
    @NotNull(message = "蜂鸣器状态", groups = {cmd39.class})
    private Integer buzzerStatus;


    /** 等级 */
    private Integer level;
    /** 参数号 */
    private String paramNum;
    /** 参数值 */
    private String paramValue;

    /** 蓄电池单体个数 */
    @NotNull(message = "单体个数不能为空", groups = {cmd09.class})
    private Integer batSinSize;
    /** 单体电池规格(1=1.2V 2=2V 3=2.4V 4=3.2V 5=3.9V 6=4.8V 7=6V 8=12V) */
    @NotNull(message = "单体电池规格不能为空", groups = {cmd09.class})
    private Integer batSinModel;
    /** 电池容量 */
    @NotNull(message = "电池容量不能为空", groups = {cmd09.class})
    private Double batCapacity;

    /** 日期 */
    @NotNull(message = "日期不能为空", groups = {cmd37.class})
    private String datetime;


    @NotNull(message = "数值类型", groups = {cmd76.class})
    private Integer dataType;
    @NotNull(message = "数值高低", groups = {cmd76.class})
    private Integer dataStatus;
    @NotNull(message = "数值", groups = {cmd76.class})
    private Integer dataInfo;
}
