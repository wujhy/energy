package com.shanhe.project.energy.stat.service.impl;

import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import com.shanhe.common.constant.Constants;
import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.framework.enums.ItemCode;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.device.alarm.domain.AlarmLog;
import com.shanhe.project.device.alarm.service.IAlarmLogService;
import com.shanhe.project.device.config.domain.BatteryMonitor;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.domain.BatteryReportLog;
import com.shanhe.project.device.config.domain.ConfigAttribute;
import com.shanhe.project.device.config.service.BatteryReportLogService;
import com.shanhe.project.device.config.service.IBatteryPackService;
import com.shanhe.project.device.config.service.IConfigAttributeService;
import com.shanhe.project.device.opt.service.OptLogService;
import com.shanhe.project.energy.capacity.service.PreBatteryGroupService;
import com.shanhe.project.energy.capacity.vo.PreBatteryGroup;
import com.shanhe.project.energy.stat.service.IConfigurationBatteryService;
import com.shanhe.project.energy.stat.service.IDevBatteryMonomerService;
import com.shanhe.project.energy.stat.vo.BatteryHealthReport;
import com.shanhe.project.energy.stat.vo.EvaluationFactors;
import com.shanhe.project.sync.domain.AlarmItemLevelVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author zhoubin
 * @date 2025/9/26
 */
@Slf4j
@Service
public class ConfigurationBatteryServiceImpl implements IConfigurationBatteryService {
    @Resource
    private IBatteryPackService batteryPackService;
    @Resource
    private BatteryReportLogService batteryReportLogService;
    @Resource
    private OptLogService optLogService;
    @Resource
    private IAlarmLogService alarmLogService;
    @Resource
    private IDevBatteryMonomerService devBatteryMonomerService;
    @Resource
    private IConfigAttributeService configAttributeService;
    @Resource
    private PreBatteryGroupService preBatteryGroupService;

    @Override
    public BatteryHealthReport getBatteryHealthReport(Integer packNum) {
        BatteryHealthReport batteryHealthReport = new BatteryHealthReport();
        BatteryPack batteryPack = batteryPackService.selectBatteryInfoByPackNum(packNum);
        if (batteryPack == null) {
            return batteryHealthReport;
        }
        PreBatteryGroup preBatteryGroup = preBatteryGroupService.lastCache(batteryPack.getPackNum());

        populateBasicInfo(batteryHealthReport, batteryPack);

        populateSohInfo(batteryHealthReport, preBatteryGroup);

        Map<String, List<AlarmLog>> logMap = populateAlarmInfo(batteryHealthReport, packNum);

        BatteryReportLog batteryReportLog = batteryReportLogService.lastCache(packNum);
        populateBackupDuration(batteryHealthReport, batteryReportLog);

        List<EvaluationFactors> evaluationFactorsList = buildEvaluationFactors(
                batteryPack, batteryReportLog, batteryHealthReport, logMap, preBatteryGroup);

        batteryHealthReport.setEvaluationFactors(evaluationFactorsList);

        handleSohAlarm(batteryHealthReport);

        // 评估建议
        batteryHealthReport.setAssessAdvice(getAssessAdvice(batteryHealthReport));

        return batteryHealthReport;
    }

    @Override
    public Map<String, Object> getTempWarnLine(Integer packNum) {
        Map<String, Object> packMap = new HashMap<>();
        ConfigAttribute wdgAttribute = configAttributeService.getCacheBy(packNum, ItemCode.DTDCWDG.getCode());
        if (wdgAttribute != null && wdgAttribute.getListLevel() != null) {
            for (AlarmItemLevelVo level : wdgAttribute.getListLevel()) {
                if (level.getHightValue() != null) {
                    packMap.put("tempUpLine", level.getHightValue());
                    break;
                }
            }
        }
        ConfigAttribute wddAttribute = configAttributeService.getCacheBy(packNum, ItemCode.DTDCWDD.getCode());
        if (wddAttribute != null && wddAttribute.getListLevel() != null) {
            for (AlarmItemLevelVo level : wddAttribute.getListLevel()) {
                if (level.getLowValue() != null) {
                    packMap.put("tempLowerLine", level.getLowValue());
                    break;
                }
            }
        }
        return packMap;
    }

    @Override
    public Map<String, Object> getResWarnLine(Integer packNum) {
        Map<String, Object> packMap = new HashMap<>();
        ConfigAttribute dAttribute = configAttributeService.getCacheBy(packNum, ItemCode.DTNZGD.getCode());
        if (null == dAttribute) {
            dAttribute = configAttributeService.getBy(packNum, ItemCode.DTNZGD.getCode());
        }
        if (dAttribute != null && dAttribute.getListLevel() != null) {
            for (AlarmItemLevelVo level : dAttribute.getListLevel()) {
                if (level.getHightValue() != null && level.getStandValue() != null) {
                    packMap.put("resUpWarnLine", level.getHightValue() * level.getStandValue());
                }
                if (level.getStandValue() != null) {
                    packMap.put("resReferLine", level.getStandValue());
                }
            }

        }
        return packMap;
    }

    private void handleSohAlarm(BatteryHealthReport batteryHealthReport) {
        // SOH 告警
        if (Objects.equals(0, batteryHealthReport.getIsGblyAlarm())
                || Objects.equals(0, batteryHealthReport.getIsZwdgAlarm())
                || Objects.equals(0, batteryHealthReport.getIsResistance())
                || Objects.equals(0, batteryHealthReport.getIsBatWdgAlarm())
                || Objects.equals(0, batteryHealthReport.getIsVoltageAlarm())) {
            batteryHealthReport.setSohAlarm(0);
            batteryHealthReport.setSoh(null);
        }
    }

    private List<EvaluationFactors> buildEvaluationFactors(BatteryPack batteryPack,
                                                           BatteryReportLog batteryReportLog,
                                                           BatteryHealthReport batteryHealthReport,
                                                           Map<String, List<AlarmLog>> logMap,
                                                           PreBatteryGroup preBatteryGroup) {

        List<Integer> types = Lists.newArrayList(
                BatteryTestEnum._5.getDictValue(),
                BatteryTestEnum._7.getDictValue());
        Integer count = optLogService.count(batteryPack.getPackNum(), types);


        List<EvaluationFactors> evaluationFactorsList = Lists.newArrayList();
        evaluationFactorsList.add(new EvaluationFactors("充放电次数", count + "次", 1));
        evaluationFactorsList.add(new EvaluationFactors("电池容量", getCapacity(batteryPack, preBatteryGroup), 1));
        evaluationFactorsList.add(getVoltageRangeStr(batteryPack, batteryReportLog, batteryHealthReport));
        evaluationFactorsList.add(getResistanceStr(batteryPack, batteryHealthReport, logMap));
        evaluationFactorsList.add(getWdStr(logMap, ItemCode.ZWDG.getCode(), batteryReportLog, batteryHealthReport));
        evaluationFactorsList.add(getWdStr(logMap, ItemCode.DTDCWDG.getCode(), batteryReportLog, batteryHealthReport));
        evaluationFactorsList.add(getGblyStr(logMap, batteryHealthReport));

        evaluationFactorsList.add(new EvaluationFactors("使用年限", getLx(batteryPack), 1));
        evaluationFactorsList.add(new EvaluationFactors("告警次数", batteryHealthReport.getAlarmCount() + "次", 1));

        return evaluationFactorsList;
    }

    private void populateBasicInfo(BatteryHealthReport batteryHealthReport, BatteryPack batteryPack) {
        batteryHealthReport.setConfigId(batteryPack.getConfigId());
        batteryHealthReport.setPackNum(batteryPack.getPackNum());
        batteryHealthReport.setBatBrand(batteryPack.getBatBrand());
        batteryHealthReport.setBatModel(batteryPack.getBatModel());
        batteryHealthReport.setBatSinModel(batteryPack.getBatSinModel());
        batteryHealthReport.setBatCapacity(batteryPack.getBatCapacity());
        batteryHealthReport.setBatSinSize(batteryPack.getBatSinSize());
        batteryHealthReport.setProductionTime(batteryPack.getProductionTime());
    }

    private void populateSohInfo(BatteryHealthReport batteryHealthReport, PreBatteryGroup preBatteryGroup) {
        Double soh = 100.0;
        if (preBatteryGroup != null) {
            soh = preBatteryGroup.getSoh();
        }
        batteryHealthReport.setSoh(soh);
    }

    private Map<String, List<AlarmLog>> populateAlarmInfo(BatteryHealthReport batteryHealthReport,
                                                          Integer packNum) {
        AlarmLog params = new AlarmLog();
        params.setConfigId(Constants.DEFAULT_CONFIG_ID);
        params.setPackNum(packNum);
        params.setExcludeItemCodes(Lists.newArrayList(ItemCode.TXZT.getCode()));
        List<AlarmLog> alarmLogs = alarmLogService.selectAlarmLogList(params);

        batteryHealthReport.setAlarmCount(alarmLogs.size());

        return alarmLogs.stream().collect(Collectors.groupingBy(AlarmLog::getItemCode));
    }

    private void populateBackupDuration(BatteryHealthReport batteryHealthReport,
                                        BatteryReportLog batteryReportLog) {
        try {
            if (batteryReportLog != null && batteryReportLog.getPackParam() != null) {
                String backupDurationStr = (String) batteryReportLog.getPackParam().get("backupDuration");
                if (backupDurationStr != null) {
                    batteryHealthReport.setBackupDuration(Integer.parseInt(backupDurationStr));
                }
            }
        } catch (NumberFormatException e) {
            // 记录日志，但不中断流程
            log.warn("Failed to parse backup duration", e);
        }
    }

    private String getAssessAdvice(BatteryHealthReport batteryHealthReport) {
        // SOH 不告警
        if (!Objects.equals(0, batteryHealthReport.getSohAlarm())) {
            Double sohThreshold = getSohThreshold(batteryHealthReport.getPackNum());
            if (batteryHealthReport.getSoh() >= sohThreshold) {
                batteryHealthReport.setSohAlarm(2);
                return "电池组状态良好，正常使用，实时监测。";
            } else {
                batteryHealthReport.setSohAlarm(1);
                return "电池组健康度（" + batteryHealthReport.getSoh() + "%）已低于安全阈值（" + sohThreshold + "%），已无法满足备用时长要求，存在运行风险。";
            }
        }
        StringBuilder result = new StringBuilder("该电池组存在异常，主要表现为");

        if (Objects.equals(0, batteryHealthReport.getIsVoltageAlarm())) {
            result.append("电压均衡度失调、");
        }
        if (Objects.equals(0, batteryHealthReport.getIsZwdgAlarm())) {
            result.append("组环境温度过高告警、");
        }
        if (Objects.equals(0, batteryHealthReport.getIsBatWdgAlarm())) {
            result.append("极柱温度过高告警、");
        }
        if (Objects.equals(0, batteryHealthReport.getIsGbAlarm())) {
            result.append("鼓包、");
        }
        if (Objects.equals(0, batteryHealthReport.getIsLyAlarm())) {
            result.append("漏液、");
        }
        if (Objects.equals(0, batteryHealthReport.getIsResistanceTransfinite())) {
            result.append("内阻过大、");
        }
        if (Objects.equals(0, batteryHealthReport.getIsResistanceChange())) {
            result.append("内阻变化率过大、");
        }

        // 只有当存在告警时才移除末尾的逗号
        result.setLength(result.length() - 1);
        return result.append("。").toString();
    }

    private Double getSohThreshold(Integer packNum) {
        ConfigAttribute sohAttribute = configAttributeService.getCacheBy(packNum, ItemCode.ZSOHDGJ.getCode());
        if (null == sohAttribute || null == sohAttribute.getListLevel() || sohAttribute.getListLevel().isEmpty()) {
            return 80.0;
        }
        Double lowValue = sohAttribute.getListLevel().get(0).getLowValue();
        if (lowValue != null) {
            return lowValue * 100.0;
        }
        return 80.0;
    }

    private String getLx(BatteryPack batteryPack) {
        if (batteryPack.getProductionTime() == null) {
            return "--";
        }
        // 使用了 x年x月
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date productionTime = sdf.parse(batteryPack.getProductionTime());
            Date now = new Date();

            // 使用现代日期时间API进行计算
            Calendar productionCalendar = Calendar.getInstance();
            productionCalendar.setTime(productionTime);
            Calendar nowCalendar = Calendar.getInstance();
            nowCalendar.setTime(now);

            int year = nowCalendar.get(Calendar.YEAR) - productionCalendar.get(Calendar.YEAR);
            int month = nowCalendar.get(Calendar.MONTH) - productionCalendar.get(Calendar.MONTH);

            // 调整年月差值
            if (month < 0) {
                year--;
                month += 12;
            }
            if (year == 0) {
                return month + "月";
            }

            return year + "年" + month + "月";
        } catch (ParseException ignored) {
        }
        return "--";
    }

    private EvaluationFactors getGblyStr(Map<String, List<AlarmLog>> logMap, BatteryHealthReport batteryHealthReport) {
        Set<Integer> dtgbLogs = logMap.getOrDefault(ItemCode.DTGB.getCode(), Collections.emptyList()).stream().map(AlarmLog::getModelNum).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Integer> dtlygjLogs = logMap.getOrDefault(ItemCode.DTLYGJ.getCode(), Collections.emptyList()).stream().map(AlarmLog::getModelNum).filter(Objects::nonNull).collect(Collectors.toSet());

        if (!dtgbLogs.isEmpty()) {
            batteryHealthReport.setIsGbAlarm(0);
        }
        if (!dtlygjLogs.isEmpty()) {
            batteryHealthReport.setIsLyAlarm(0);
        }

        dtgbLogs.addAll(dtlygjLogs);
        if (!dtgbLogs.isEmpty()) {
            batteryHealthReport.setIsGblyAlarm(0);
            return new EvaluationFactors("鼓包、漏液", "告警（" + dtgbLogs.size() + "节）", 0);
        }
        return new EvaluationFactors("鼓包、漏液", "无", 1);
    }

    private EvaluationFactors getWdStr(Map<String, List<AlarmLog>> logMap, String itemCode, BatteryReportLog batteryReportLog, BatteryHealthReport batteryHealthReport) {
        String str;
        if (ItemCode.ZWDG.getCode().equals(itemCode)) {
            str = "组环境高温告警";
        } else {
            str = "极柱高温告警";
        }


        List<AlarmLog> logList = logMap.get(itemCode);
        if (logList == null) {
            return new EvaluationFactors(str, "无", 1);
        }
        boolean hasActiveAlarm = logList.stream()
                .anyMatch(log -> YesNoEnum.NO.getDictValue().equals(log.getStatus()));

        if (hasActiveAlarm) {
            if (ItemCode.ZWDG.getCode().equals(itemCode)) {
                batteryHealthReport.setIsZwdgAlarm(0);
            } else {
                batteryHealthReport.setIsBatWdgAlarm(0);
            }
            return new EvaluationFactors(str, "告警（" + getWd(itemCode, batteryReportLog) + "）", 0);
        }
        return new EvaluationFactors(str, "无", 1);
    }

    private String getWd(String itemCode, BatteryReportLog batteryReportLog) {
        if (ItemCode.ZWDG.getCode().equals(itemCode)) {
            Map<String, Object> packParam = batteryReportLog.getPackParam();
            if (packParam != null) {
                return packParam.get("environmentTemperature1") + "℃";
            }
            return "40℃";
        } else {
            List<BatteryMonitor> batteryList = batteryReportLog.getBatteryList();
            if (batteryList == null || batteryList.isEmpty()) {
                return "50℃";
            }
            // 获取最高温度
            Double maxTemperature = batteryList.stream().mapToDouble(BatteryMonitor::getTemperature).max().orElse(0);
            return maxTemperature + "℃";
        }
    }

    /**
     * 获取内阻变化率
     */
    private EvaluationFactors getResistanceStr(BatteryPack batteryPack, BatteryHealthReport batteryHealthReport, Map<String, List<AlarmLog>> logMap) {

        long dtnzggCount = logMap.getOrDefault(ItemCode.DTNZGD.getCode(), Collections.emptyList()).size();
        if (dtnzggCount > 0) {
            batteryHealthReport.setIsResistance(0);
            batteryHealthReport.setIsResistanceTransfinite(0);
            return new EvaluationFactors("内阻变化率/内阻过大", "内阻过大", 0);
        }

        Double maxResistance = devBatteryMonomerService.getMaxResistance(batteryPack.getPackNum());
        if (maxResistance == null) {
            return new EvaluationFactors("内阻变化率/内阻过大", "--", 1);
        }
        maxResistance = maxResistance * 100.0;

        // 内阻变化率= ( (实测内阻 - 初始内阻) / 初始内阻 ) × 100%
        //  失效/危险：增加超过30%，，电池已超限，必须立即更换。
        //  实测内阻最小不能超过初始内阻，实测内阻最高只能为初始内阻的1倍
        if (maxResistance > 30) {
            batteryHealthReport.setIsResistance(0);
            batteryHealthReport.setIsResistanceChange(0);
            return new EvaluationFactors("内阻变化率/内阻过大", maxResistance + "%（异常）", 0);
        }

        return new EvaluationFactors("内阻变化率/内阻过大", maxResistance + "%", 1);
    }

    private String getCapacity(BatteryPack batteryPack, PreBatteryGroup preBatteryGroup) {
        if (preBatteryGroup == null || preBatteryGroup.getBcapacity() == null || preBatteryGroup.getBcapacity() == 0) {
            return "100%";
        }
        // 使用double类型进行计算以避免整数除法导致的精度丢失
        double percentage = preBatteryGroup.getBcapacity() / batteryPack.getBatCapacity() * 100;
        return (int) percentage + "%";
    }

    private EvaluationFactors getVoltageRangeStr(BatteryPack batteryPack, BatteryReportLog batteryReportLog, BatteryHealthReport batteryHealthReport) {
        Integer voltageRange = getRange(batteryPack, batteryReportLog);
        if (voltageRange == null) {
            return new EvaluationFactors("电压均衡度", "--", 1);
        }

        Integer threshold = batteryPackService.getVoltageBalance(batteryPack.getPackNum());

        if (voltageRange > threshold) {
            batteryHealthReport.setIsVoltageAlarm(0);
            return new EvaluationFactors("电压均衡度", voltageRange + "mV（异常）", 0);
        }
        return new EvaluationFactors("电压均衡度", voltageRange + "mV", 1);
    }

    private Integer getRange(BatteryPack batteryPack, BatteryReportLog oldInfo) {
        // 当前是浮充状态，取当前数据计算极差
        if (null == oldInfo || null == oldInfo.getPackParam() || null == oldInfo.getBatteryList() || oldInfo.getBatteryList().isEmpty()) {
            return batteryPack.getVoltageRange();
        }

        // 电池状态0：监控1：充电2：停电3：核容4：未连接5：备电6：空闲
        String batteryPackStatus = (String) oldInfo.getPackParam().get("batteryPackStatus");
        if (!StrUtil.equals("6", batteryPackStatus)) {
            return batteryPack.getVoltageRange();
        }

        // 电压极差（mV） = 单体最高电压(V) - 单体最低电压(V)
        // 使用 summaryStatistics 避免两次流操作，同时处理可能的空流情况
        DoubleSummaryStatistics voltageStats = oldInfo.getBatteryList().stream()
                .mapToDouble(BatteryMonitor::getVoltage)
                .summaryStatistics();

        if (voltageStats.getCount() == 0) {
            return batteryPack.getVoltageRange();
        }

        double maxVoltage = voltageStats.getMax();
        double minVoltage = voltageStats.getMin();

        // 单位转换 V 转换 mV，不保留小数点
        return (int) Math.round((maxVoltage - minVoltage) * 1000);
    }

}
