package com.shanhe.project.device.config.domain;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.shanhe.project.sync.domain.AlarmItemLevelVo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.shanhe.framework.aspectj.lang.annotation.Excel;
import com.shanhe.framework.web.domain.BaseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备属性对象 dev_config_attribute
 * 
 * @author wjh
 * @since 2024-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ConfigAttribute extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 设备属性id */
    private Long configAttrId;

    /** 设备id */
    private Long configId;

    /** 包序号 */
    @Excel(name = "包序号")
    private Integer packNum;

    /** 属性编码 */
    @Excel(name = "属性编码")
    private String code;

    /** 属性名 */
    @Excel(name = "属性名")
    private String name;

    /** 属性值 */
    private String value;

    /** 属性单位 */
    @Excel(name = "属性单位")
    private String unit;

    /** 小数位 */
    @Excel(name = "小数位")
    private Integer point;

    /** 数据类型 */
    @Excel(name = "数据类型 1:开关量 2:模拟量")
    private Integer type;

    /** 序号 */
    @Excel(name = "序号")
    private Integer sort;

    /** 列表显示 0-是，1-否 */
    @Excel(name = "列表显示 0-是，1-否")
    private Integer listDisplay;

    /** 大屏显示 0-是，1-否 */
    @Excel(name = "大屏显示 0-是，1-否")
    private Integer screenDisplay;

    /** 历史跟踪 0-是，1-否 */
    @Excel(name = "历史跟踪 0-是，1-否")
    private Integer track;

    /** 是否包字段 0-是，1-否 */
    @Excel(name = "是否包字段 0-是，1-否")
    private Integer pack;

    /** 启用状态 0-是，1-否 */
    @Excel(name = "启用状态 0-是，1-否")
    private Integer status;

    /** 是否告警 0-是，1-否 */
    @Excel(name = "是否告警 0-是，1-否")
    private Integer alarm;

    /** 是否告警配置 0-是，1-否 */
    @Excel(name = "是否告警配置 0-是，1-否")
    private Integer alarmConfig;

    /** 回差保护值 */
    private Double protValue;
    /** 值为0时的描述 */
    private String val0;
    /** 值为1时的描述 */
    private String val1;
    /** 是否需要线性转换0是1否 */
    private Integer isLinear=1;
    /** 原量程最小值 */
    private Double minOrigRange=0.0;
    /** 目标量程最小值 */
    private Double minTargetRange;
    /** 原量程最大值 */
    private Double maxOrigRange=0.0;
    /** 目标量程最大值 */
    private Double maxTargetRange;
    /** 斜率值 */
    private Double spk=0.0;
    /** 修正值 */
    private Double spb;

    /** 告警配置列表 */
    List<AlarmItemLevelVo> listLevel;
    /** 告警等级 */
    private String levelList;

    private List<String> excludeCodes;

    public void setListLevel(List<AlarmItemLevelVo> listLevel) {
        this.levelList = listLevel != null ? JSON.toJSONString(listLevel) : null;
        this.listLevel = listLevel;
    }

    public List<AlarmItemLevelVo> getListLevel() {
        return listLevel != null ? listLevel : new ArrayList<>();
    }

    public void setLevelList(String levelList) {
        // 转列表
        if (StrUtil.isNotBlank(levelList)) {
            listLevel = JSON.parseArray(levelList, AlarmItemLevelVo.class);
        }
        this.levelList = levelList;
    }
}
