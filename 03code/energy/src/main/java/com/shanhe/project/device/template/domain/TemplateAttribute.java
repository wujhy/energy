package com.shanhe.project.device.template.domain;

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
 * 模板属性对象 dev_template_attribute
 * 
 * @author wjh
 * @since 2024-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TemplateAttribute extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 模板属性id */
    private Long tmplAttrId;

    /** 模板id */
    private Long tmplId;

    /** 模式 */
    private Integer model;

    /** 属性编码 */
    @Excel(name = "属性编码")
    private String code;

    /** 属性名 */
    @Excel(name = "属性名")
    private String name;

    /** 属性单位 */
    @Excel(name = "属性单位")
    private String unit;

    /** 小数点位 */
    @Excel(name = "小数点位")
    private Integer point;

    /** 数据类型 */
    @Excel(name = "数据类型")
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
    @Excel(name = "是否告警配置 0-是，1-否")
    private Integer alarmConfig;

    @Excel(name = "告警名")
    private String alarmName;


    /** 一般告警最小值 */
    private Double generalMin;

    /** 一般告警最大值 */
    private Double generalMax;

    /** 恢复值比对方式1大于2小于3等于 */
    @Excel(name = "一般告警恢复值比对方式1大于2小于3等于")
    private Integer generalRecFlag;

    /** 恢复值 */
    @Excel(name = "一般告警恢复值")
    private Double generalRecValue;

    /** 重要告警最小值 */
    private Double importantMin;

    /** 重要告警最大值 */
    private Double importantMax;

    /** 恢复值比对方式1大于2小于3等于 */
    @Excel(name = "重要告警恢复值比对方式1大于2小于3等于")
    private Integer importantRecFlag;

    /** 恢复值 */
    @Excel(name = "重要告警恢复值")
    private Double importantRecValue;

    /** 紧急告警最小值 */
    private Double emergencyMin;

    /** 紧急告警最大值 */
    private Double emergencyMax;

    /** 恢复值比对方式1大于2小于3等于 */
    @Excel(name = "恢复值比对方式1大于2小于3等于")
    private Integer emergencyRecFlag;

    /** 恢复值 */
    @Excel(name = "恢复值")
    private Double emergencyRecValue;

    /** 告警说明 */
    @Excel(name = "告警说明")
    private String alarmDesc;

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
