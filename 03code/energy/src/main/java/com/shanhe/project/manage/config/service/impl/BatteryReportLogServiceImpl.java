package com.shanhe.project.manage.config.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageHelper;
import com.shanhe.common.constant.Constants;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.file.FileUtils;
import com.shanhe.common.utils.text.Convert;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.manage.alarm.domain.AlarmLog;
import com.shanhe.project.manage.alarm.service.IAlarmLogService;
import com.shanhe.project.manage.config.domain.BatteryMonitor;
import com.shanhe.project.manage.config.domain.BatteryPack;
import com.shanhe.project.manage.config.domain.BatteryReportLog;
import com.shanhe.project.manage.config.mapper.BatteryReportLogMapper;
import com.shanhe.project.manage.config.service.BatteryReportLogService;
import com.shanhe.project.manage.config.service.IBatteryPackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 蓄电池上报日志
 *
 * @author wjh
 * @since 2025/7/9
 */
@Slf4j
@Service
public class BatteryReportLogServiceImpl implements BatteryReportLogService {

    /** 电池上报日志映射。 */
    @Resource
    private BatteryReportLogMapper batteryReportLogMapper;
    /** 告警日志服务。 */
    @Resource
    private IAlarmLogService alarmLogService;
    /** 电池组服务。 */
    @Resource
    private IBatteryPackService batteryPackService;

    CacheKeyEnum reportCache = CacheKeyEnum.BATTERY_REPORT;

    /**
     * 异步插入蓄电池上报日志
     *
     * @param packNum 电池组编号
     * @param packParam 电池组参数
     * @param batteryList 单体电池数据
     * @param isInsert 是否入库
     */
    @Async
    @Override
    public void insert(Integer packNum, Map<String, Object> packParam, List<BatteryMonitor> batteryList, boolean isInsert) {
        // 直接新增
        BatteryReportLog batteryReportLog = new BatteryReportLog();
        batteryReportLog.setConfigId(Constants.DEFAULT_CONFIG_ID);
        batteryReportLog.setPackNum(packNum);
        batteryReportLog.setPackParam(packParam);
        batteryReportLog.setBatteryList(batteryList);
        batteryReportLog.setCreateTime(new Date());

        batteryReportLog.setPackData(JSON.toJSONString(packParam));
        batteryReportLog.setMonitorData(JSON.toJSONString(batteryList));
        if (isInsert) {
            batteryReportLogMapper.insert(batteryReportLog);
        } else {
            log.info("数据未达到存储间隔:{}", packNum);
        }
        // 缓存
        String key = String.format(reportCache.getKey(), batteryReportLog.getPackNum());
        CacheUtils.put(reportCache.getCache(), key, batteryReportLog);
    }

    /**
     * 查询最新一条上报记录（含告警信息）
     *
     * @param packNum 电池组编号
     * @return 上报日志
     */
    @Override
    public BatteryReportLog selectLastHasAlarm(Integer packNum) {
        // 先取缓存
        BatteryReportLog log = this.lastCache(packNum);
        if (log == null) {
            log = batteryReportLogMapper.selectLast(packNum);
            if (log == null) {
                return null;
            }
        }
        // 单体数据
        if (StrUtil.isNotBlank(log.getMonitorData())) {
            if (log.getBatteryList() == null) {
                log.setBatteryList(JSON.parseArray(log.getMonitorData(), BatteryMonitor.class));
            }
            if (!log.getBatteryList().isEmpty()) {

                // 告警记录
                List<AlarmLog> alarmLogs = alarmLogService.selectBatteryAlarmLogListCache(packNum);
                Map<Integer, List<AlarmLog>> batAlarmMap = alarmLogs.stream()
                        .filter(item -> item.getModelNum() != null)
                        .collect(Collectors.groupingBy(AlarmLog::getModelNum));

                // 单体电池告警记录
                log.getBatteryList().forEach(entity -> entity.setAlarmList(batAlarmMap.getOrDefault(entity.getBatNum(), new ArrayList<>())));

                log.setAlarmList(alarmLogs);
                log.setAlarm(alarmLogs.isEmpty() ? 1 : 0);
            }
        }
        // 包数据
        if (StrUtil.isNotBlank(log.getPackData()) && log.getPackParam() == null) {
            log.setPackParam(JSON.parseObject(log.getPackData()));
        }
        return log;
    }

    /**
     * 从缓存获取最新上报记录
     *
     * @param packNum 电池组编号
     * @return 上报日志
     */
    @Override
    public BatteryReportLog lastCache(Integer packNum) {
        Object log = CacheUtils.get(reportCache.getCache(), String.format(reportCache.getKey(), packNum));
        if (log == null) {
            return null;
        }
        BatteryReportLog result = (BatteryReportLog) log;
        // 单体数据
        if (StrUtil.isNotBlank(result.getMonitorData())) {
            result.setBatteryList(JSON.parseArray(result.getMonitorData(), BatteryMonitor.class));
        }
        // 包数据
        if (StrUtil.isNotBlank(result.getPackData())) {
            result.setPackParam(JSON.parseObject(result.getPackData()));
        }
        return result;
    }

    /**
     * 查询上报日志列表
     *
     * @param batteryReportLog 查询条件
     * @return 上报日志列表
     */
    @Override
    public List<BatteryReportLog> selectBatteryReportLog(BatteryReportLog batteryReportLog) {
        List<BatteryReportLog> list = batteryReportLogMapper.selectBatteryReportLog(batteryReportLog);
        for (BatteryReportLog log : list) {
            if (StrUtil.isNotBlank(log.getMonitorData())) {
                log.setBatteryList(JSON.parseArray(log.getMonitorData(), BatteryMonitor.class));
            }
            if (StrUtil.isNotBlank(log.getPackData())) {
                log.setPackParam(JSON.parseObject(log.getPackData()));
            }
        }
        return list;
    }

    /**
     * 根据ID批量删除上报日志
     *
     * @param ids 日志ID字符串，逗号分隔
     * @return 结果
     */
    @Override
    public int deleteByIds(String ids) {
        return batteryReportLogMapper.deleteByIds(Convert.toStrArray(ids));
    }

    /**
     * 删除指定天数之前的上报日志
     *
     * @param dayNum 天数
     */
    @Override
    public void deleteByDays(Integer dayNum) {
        batteryReportLogMapper.deleteByDays(dayNum);
    }

    /** 更新上报日志缓存 */
    @Override
    public void updateCache() {
        // 旧缓存
        List<String> startKeys = new ArrayList<>();
        Set<String> oldKeys = CacheUtils.getCacheKeys(reportCache.getCache());

        // 蓄电池组
        List<BatteryPack> batteryPackList = batteryPackService.selectBatteryPackList(YesNoEnum.YES.getDictValue());
        for (BatteryPack batteryPack : batteryPackList) {
            // 查询最新一条记录
            BatteryReportLog reportLog = this.selectLast(batteryPack.getPackNum());
            if (reportLog == null) {
                continue;
            }

            /* 缓存 */
            String key = String.format(reportCache.getKey(), reportLog.getPackNum());
            if (CacheUtils.get(reportCache.getCache(), key) == null) {
                CacheUtils.put(reportCache.getCache(), key, reportLog);
            }
            startKeys.add(key);
        }

        // 删除
        for (String key : oldKeys) {
            if (!startKeys.contains(key)) {
                CacheUtils.remove(reportCache.getCache(), key);
            }
        }
    }


    /**
     * 删除指定电池组的上报日志
     *
     * @param packNum 电池组编号
     */
    @Override
    public void deleteByPackNum(Integer packNum) {
        batteryReportLogMapper.deleteByPackNum(packNum);
    }

    /**
     * 导出上报日志到Excel
     *
     * @param params 查询条件
     */
    @Override
    public void export(BatteryReportLog params) {
        params.setConfigId(Constants.DEFAULT_CONFIG_ID);
        Integer batSinSize = batteryPackService.getBatteryMaxNumber(params.getPackNum());
        if (batSinSize == null) {
            throw new RuntimeException("查询不到单体节数");
        }
        Long count = batteryReportLogMapper.selectCount(params);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String exportName = "历史数据_" + sdf.format(new Date());

        String fileName = FileUtils.getUsbPath(params.getExportPath()) + exportName + ".xlsx";
        // 确保目录存在
        File file = new File(fileName);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        try (ExcelWriter excelWriter = EasyExcel.write(fileName).head(BatteryPack.getHeads(batSinSize)).build()) {
            // 这里注意 如果同一个sheet只要创建一次
            WriteSheet writeSheet = EasyExcel.writerSheet("模板").build();
            for (int i = 0; i < count; i += 1000) {
                PageHelper.startPage(i / 1000 + 1, 1000, true, false, true);
                List<BatteryReportLog> list = selectBatteryReportLog(params);

                /*数据组装*/
                List<List<Object>> excelDataList = list.stream().map(item -> batteryPackMonitorOfExcel(item, batSinSize)).collect(Collectors.toList());

                excelWriter.write(excelDataList, writeSheet);
            }
        }
    }

    /** 转换格式 */
    public static List<Object> batteryPackMonitorOfExcel(BatteryReportLog item, Integer batteryMaxNumber) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<Object> excelData = new ArrayList<>();
        // 数据时间
        excelData.add(simpleDateFormat.format(item.getCreateTime()));
        excelData.add(item.getPackNum());

        // 组电压
        Map<String, Object> packParam = item.getPackParam();
        if (packParam != null) {
            excelData.add(packParam.get("packVoltage") + "");
            // 组电流
            excelData.add(packParam.get("packCurrent") + "");
            // 环境温度
            excelData.add(packParam.get("environmentTemperature1") + "");
        } else {
            excelData.add("");
            excelData.add("");
            excelData.add("");
        }
        batterMonitor(item.getBatteryList(), batteryMaxNumber, excelData);
        return excelData;
    }

    /**
     * 单体电池数据
     *
     * @param batteryMonitorList 单体
     * @param batteryMaxNumber   最大单数数量
     * @param excelData          excel
     */
    private static void batterMonitor(List<BatteryMonitor> batteryMonitorList, int batteryMaxNumber, List<Object> excelData) {
        if (CollectionUtil.isEmpty(batteryMonitorList)) {
            return;
        }
        if (0 == batteryMaxNumber) {
            return;
        }
        Map<Integer, BatteryMonitor> batteryMonitorMap = batteryMonitorList.stream().collect(Collectors.toMap(BatteryMonitor::getBatNum, Function.identity()));

        List<Object> voltageList = new ArrayList<>();
        List<Object> temperatureList = new ArrayList<>();
        List<Object> resistanceList = new ArrayList<>();
        for (int i = 1; i <= batteryMaxNumber; i++) {
            BatteryMonitor bm = batteryMonitorMap.get(i);
            if (bm == null) {
                voltageList.add(0.0D);
                temperatureList.add(0.0D);
                resistanceList.add(0.0D);
            } else {
                voltageList.add(bm.getVoltage());
                temperatureList.add(bm.getTemperature());
                resistanceList.add(bm.getResistance());
            }
        }
        //单体电压
        excelData.addAll(voltageList);
        //单体温度
        excelData.addAll(temperatureList);
        //单体内阻
        excelData.addAll(resistanceList);
    }

    /** 查询最新一条记录 */
    private BatteryReportLog selectLast(Integer packNum) {
        BatteryReportLog log = batteryReportLogMapper.selectLast(packNum);
        if (log == null) {
            return null;
        }
        // 单体数据
        if (StrUtil.isNotBlank(log.getMonitorData())) {
            log.setBatteryList(JSON.parseArray(log.getMonitorData(), BatteryMonitor.class));
        }
        // 包数据
        if (StrUtil.isNotBlank(log.getPackData())) {
            log.setPackParam(JSON.parseObject(log.getPackData()));
        }
        return log;
    }
}
