package com.shanhe.project.sync.common;

import cn.hutool.core.util.StrUtil;
import com.shanhe.framework.enums.AlarmLevelEnum;
import com.shanhe.framework.enums.DataTypeEnum;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.device.config.domain.ConfigAttribute;
import com.shanhe.project.sync.domain.AlarmItemLevelVo;
import com.shanhe.project.sync.domain.AlarmItemVo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 属性转换工具类
 *
 * @author wjh
 * @since 2025/5/30
 */
public class AttributeUtil {

    /**
     * 属性参数
     *
     * @param configAttribute 本地
     * @param alarmItem 远程
     */
    public static void setAttributeParam(ConfigAttribute configAttribute, AlarmItemVo alarmItem) {
        configAttribute.setConfigAttrId(alarmItem.getItemId());
        configAttribute.setConfigId(alarmItem.getDevId());
        configAttribute.setPackNum(alarmItem.getPackNum());
        configAttribute.setCode(alarmItem.getItemCode());
        configAttribute.setName(alarmItem.getItemName());
        configAttribute.setUnit(alarmItem.getItemUnit());
        configAttribute.setPoint(alarmItem.getItemPoint());
        configAttribute.setType(alarmItem.getItemType());
        configAttribute.setSort(alarmItem.getSortNum());
        configAttribute.setListDisplay(YesNoEnum.YES.getDictValue());
        configAttribute.setScreenDisplay(YesNoEnum.YES.getDictValue());
        configAttribute.setPack(Objects.equals(alarmItem.getGroupFlag(), YesNoEnum.NO.getDictValue()) ?
                YesNoEnum.YES.getDictValue() : YesNoEnum.NO.getDictValue());
        configAttribute.setStatus(Objects.equals(alarmItem.getStatus(), YesNoEnum.YES.getDictValue()) ?
                YesNoEnum.NO.getDictValue() : YesNoEnum.YES.getDictValue());
        configAttribute.setAlarmConfig(Objects.equals(alarmItem.getIsMonitor(), YesNoEnum.NO.getDictValue()) ?
                YesNoEnum.YES.getDictValue() : YesNoEnum.NO.getDictValue());

        configAttribute.setScreenDisplay(Objects.equals(alarmItem.getDisplayHome(), YesNoEnum.NO.getDictValue()) ?
                YesNoEnum.YES.getDictValue() : YesNoEnum.NO.getDictValue());

        configAttribute.setVal0(alarmItem.getVal0());
        configAttribute.setVal1(alarmItem.getVal1());
        configAttribute.setIsLinear(Objects.equals(alarmItem.getIsLinear(), YesNoEnum.NO.getDictValue()) ?
                YesNoEnum.YES.getDictValue() : YesNoEnum.NO.getDictValue());
        configAttribute.setMinOrigRange(alarmItem.getMinOrigRange());
        configAttribute.setMaxOrigRange(alarmItem.getMaxOrigRange());
        configAttribute.setMinTargetRange(alarmItem.getMinTargetRange());
        configAttribute.setMaxTargetRange(alarmItem.getMaxTargetRange());
        configAttribute.setSpk(alarmItem.getSpk());
        configAttribute.setSpb(alarmItem.getSpb());
        configAttribute.setRemark(alarmItem.getRemark());

        // 开关量
        if (Objects.equals(configAttribute.getType(), DataTypeEnum._1.getDictValue())) {
            List<AlarmItemLevelVo> listLevel = new ArrayList<>();
            // 0值
            AlarmItemLevelVo level0 = new AlarmItemLevelVo();
            level0.setLevelCode(StrUtil.equals(alarmItem.getBjVal(), "0") ? alarmItem.getAlarmLevel() : AlarmLevelEnum._0.getDictValue());
            level0.setDictId("0");
            level0.setDictName(configAttribute.getVal0());
            level0.setAlarmDesc(StrUtil.equals(alarmItem.getBjVal(), "0") ? alarmItem.getRemark() : "");
            listLevel.add(level0);

            // 1值
            AlarmItemLevelVo level1 = new AlarmItemLevelVo();
            level1.setLevelCode(StrUtil.equals(alarmItem.getBjVal(), "1") ? alarmItem.getAlarmLevel() : AlarmLevelEnum._0.getDictValue());
            level1.setDictId("1");
            level1.setDictName(configAttribute.getVal1());
            level1.setAlarmDesc(StrUtil.equals(alarmItem.getBjVal(), "1") ? alarmItem.getRemark() : "");
            listLevel.add(level1);

            configAttribute.setListLevel(listLevel);
        } else {
            configAttribute.setListLevel(alarmItem.getListLevel());
        }
    }

    /**
     * 上报属性参数
     *
     * @param configAttribute 本地
     */
    public static AlarmItemVo uploadItem(ConfigAttribute configAttribute) {
        AlarmItemVo alarmItem = new AlarmItemVo();
        alarmItem.setItemId(configAttribute.getConfigAttrId());
        alarmItem.setDevId(configAttribute.getConfigId());
        alarmItem.setPackNum(configAttribute.getPackNum());
        alarmItem.setItemCode(configAttribute.getCode());
        alarmItem.setItemName(configAttribute.getName());
        alarmItem.setItemUnit(configAttribute.getUnit());
        alarmItem.setItemPoint(configAttribute.getPoint());
        alarmItem.setItemType(configAttribute.getType());
        alarmItem.setSortNum(configAttribute.getSort());
        alarmItem.setGroupFlag(Objects.equals(configAttribute.getPack(), YesNoEnum.NO.getDictValue()) ?
                YesNoEnum.YES.getDictValue() : YesNoEnum.NO.getDictValue());
        alarmItem.setStatus(Objects.equals(configAttribute.getStatus(), YesNoEnum.YES.getDictValue()) ?
                YesNoEnum.NO.getDictValue() : YesNoEnum.YES.getDictValue());
        alarmItem.setIsMonitor(Objects.equals(configAttribute.getAlarmConfig(), YesNoEnum.NO.getDictValue()) ?
                YesNoEnum.YES.getDictValue() : YesNoEnum.NO.getDictValue());

        alarmItem.setDisplayHome(Objects.equals(configAttribute.getScreenDisplay(), YesNoEnum.NO.getDictValue()) ?
                YesNoEnum.YES.getDictValue() : YesNoEnum.NO.getDictValue());

        alarmItem.setVal0(configAttribute.getVal0());
        alarmItem.setVal1(configAttribute.getVal1());
        alarmItem.setIsLinear(Objects.equals(configAttribute.getIsLinear(), YesNoEnum.NO.getDictValue()) ?
                YesNoEnum.YES.getDictValue() : YesNoEnum.NO.getDictValue());
        alarmItem.setMinOrigRange(configAttribute.getMinOrigRange());
        alarmItem.setMinTargetRange(configAttribute.getMinTargetRange());
        alarmItem.setMaxOrigRange(configAttribute.getMaxOrigRange());
        alarmItem.setMaxTargetRange(configAttribute.getMaxTargetRange());
        alarmItem.setSpk(configAttribute.getSpk());
        alarmItem.setSpb(configAttribute.getSpb());
        alarmItem.setRemark(configAttribute.getRemark());

        // 设置告警等级
        if (configAttribute.getListLevel() == null || configAttribute.getListLevel().isEmpty()) {
            return alarmItem;
        }

        // 开关量
        if (Objects.equals(configAttribute.getType(), DataTypeEnum._1.getDictValue())) {
            for (AlarmItemLevelVo levelVo : configAttribute.getListLevel()) {
                // 存在告警
                if (!StrUtil.equals(levelVo.getLevelCode(), AlarmLevelEnum._0.getDictValue())) {
                    alarmItem.setAlarmLevel(levelVo.getLevelCode());
                    alarmItem.setBjVal(levelVo.getDictId());
                    alarmItem.setRemark(levelVo.getAlarmDesc());
                }

                if (StrUtil.equals(levelVo.getDictId(), "0")) {
                    alarmItem.setVal0(levelVo.getDictName());
                } else if (StrUtil.equals(levelVo.getDictId(), "1")) {
                    alarmItem.setVal1(levelVo.getDictName());
                }
            }
        } else {
            alarmItem.setListLevel(configAttribute.getListLevel());
        }

        return alarmItem;
    }
}
