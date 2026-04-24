package com.shanhe.project.device.config.domain;

import com.google.common.collect.Lists;
import com.shanhe.framework.aspectj.lang.annotation.Excel;
import com.shanhe.framework.web.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 设备蓄电池扩展
 *
 * @author wjh
 * @since 2025/2/20
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BatteryPack extends BaseEntity {
    private static final long serialVersionUID = 1L;
    public interface add{}
    public interface update{}


    /**
     * 蓄电池组ID
     */
    @NotNull(message = "设备ID不能为空", groups = {update.class})
    private Long packId;

    /**
     * 设备主键ID，蓄电池ID
     */
    @Excel(name = "设备主键ID，蓄电池ID")
    @NotNull(message = "设备ID不能为空", groups = {add.class, update.class})
    private Long configId;

    /**
     * 蓄电池主编号，1,2,3,4
     */
    @Excel(name = "蓄电池主编号，1,2,3,4")
    @NotNull(message = "电池组编号不能为空", groups = {add.class, update.class})
    private Integer packNum;

    /**
     * 蓄电池单体个数
     */
    @Excel(name = "蓄电池单体个数")
    @NotNull(message = "蓄电池单体个数不能为空", groups = {add.class, update.class})
    private Integer batSinSize;

    /**
     * 单体电池规格(1=1.2V 2=2V 3=2.4V 4=3.2V 5=3.9V 6=4.8V 7=6V 8=12V)
     */
    @Excel(name = "单体电池规格(1=1.2V 2=2V 3=2.4V 4=3.2V 5=3.9V 6=4.8V 7=6V 8=12V)")
    @NotNull(message = "单体电池规格不能为空", groups = {add.class, update.class})
    private Integer batSinModel;

    /**
     * 蓄电池品牌
     */
    @Excel(name = "蓄电池品牌")
    private String batBrand;

    /**
     * 蓄电池型号规格
     */
    @Excel(name = "蓄电池型号规格")
    private String batModel;

    /**
     * 电池容量
     */
    @Excel(name = "电池容量")
    @NotNull(message = "电池容量不能为空", groups = {add.class, update.class})
    private Double batCapacity;

    /**
     * 电池状态(1=浮充 2=放电 3=充电 4=停电 5=核容)
     */
    @Excel(name = "电池状态(1=浮充 2=放电 3=充电 4=停电 5=核容)")
    private Integer status;

    /**
     * 是否启用0是1否
     */
    @Excel(name = "是否启用0是1否")
    private Integer isEnabled;
    /**
     * 是否允许备电测试0是1否
     */
    @NotNull(message = "是否允许备电测试不能为空", groups = {add.class, update.class})
    private Integer isAllowPower;

    /** 是否告警 0-是，1-否 */
    private Integer alarm;

    // 投产时间
    private String productionTime;

    /**
     * 浮充阶段电压极差值
     */
    private Integer voltageRange;

    /**
     * 连接条是否显示
     */
    private Integer isShowConnect;

    /**
     * 获取表头
     */
    public static List<List<String>> getHeads(Integer batSinSize) {
        List<String> heads = new ArrayList<>();
        heads.add("上报时间");
        heads.add("蓄电池编号");
        heads.add("组电压");
        heads.add("组电流");
        heads.add("环境温度");

        if (null != batSinSize) {
            for (int i = 1; i <= batSinSize; i++) heads.add("电压" + i);
            for (int i = 1; i <= batSinSize; i++) heads.add("温度" + i);
            for (int i = 1; i <= batSinSize; i++) heads.add("内阻" + i);
        }

        return heads.stream().map(Lists::newArrayList).collect(Collectors.toList());
    }

}
