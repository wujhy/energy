package com.shanhe.project.energy.stat.service.impl;


import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.github.pagehelper.PageHelper;
import com.google.common.collect.Lists;
import com.shanhe.common.constant.Constants;
import com.shanhe.common.utils.file.FileUtils;
import com.shanhe.common.utils.uuid.IdUtils;
import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.project.device.config.domain.BatteryMonitor;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.service.IBatteryPackService;
import com.shanhe.project.device.opt.domain.OptLog;
import com.shanhe.project.device.opt.service.OptLogService;
import com.shanhe.project.energy.stat.domain.StatBatteryBat;
import com.shanhe.project.energy.stat.domain.StatBatteryPack;
import com.shanhe.project.energy.stat.mapper.StatBatteryPackMapper;
import com.shanhe.project.energy.stat.service.IStatBatteryBatService;
import com.shanhe.project.energy.stat.service.IStatBatteryPackService;
import com.shanhe.project.iot.data.MessageFactory;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 电池组统计Service业务层处理
 *
 * @author zhoubin
 * @date 2025-07-15
 */
@Service
public class StatBatteryPackServiceImpl implements IStatBatteryPackService {
    // 提取为静态常量，避免重复创建
    private static final String[] BATTERY_PACK_STATUS_ARR = {"1", "3", "5", "6"};

    @Resource
    private StatBatteryPackMapper statBatteryPackMapper;
    @Resource
    private OptLogService optLogService;
    @Resource
    private IStatBatteryBatService statBatteryBatService;
    @Resource
    private IBatteryPackService batteryPackService;

    /**
     # 电池状态转变为浮充时
     * 浮充数据迁移时长 毫秒
     */
    @Value("${stat.battery.floating:7200000}")
    private long batteryFloatingTime;


    @Override
    public List<StatBatteryPack> selectList(StatBatteryPack statBatteryPack) {
        List<StatBatteryPack> statBatteryPacks = statBatteryPackMapper.selectList(statBatteryPack);
        translateBattery(statBatteryPacks);
        return statBatteryPacks;
    }

    private void translateBattery(List<StatBatteryPack> result) {
        if (org.springframework.util.CollectionUtils.isEmpty(result)) {
            return;
        }
        // 获取 result 中最小、最大的时间，缩小子表范围
        Date startTime = result.stream().map(StatBatteryPack::getCreateTime).min(Date::compareTo).get();
        Date endTime = result.stream().map(StatBatteryPack::getCreateTime).max(Date::compareTo).get();

        List<Long> packIds = result.stream().map(StatBatteryPack::getId).collect(Collectors.toList());

        Map<Long, List<StatBatteryBat>> packMap = statBatteryBatService.selectList(packIds, startTime, endTime)
                .stream().collect(Collectors.groupingBy(StatBatteryBat::getPackId));

        result.forEach(item -> item.setStatBatteryList(packMap.getOrDefault(item.getId(), Collections.emptyList())));
    }

    @Async
    @Override
    public void insertList(Integer packNum, Map<String, Object> packMap, List<BatteryMonitor> batteryList) {
        // 参数校验
        if (packMap == null || batteryList == null) {
            return;
        }

        // 0表示不在内阻测试、6表示正在内阻测试、7表示内阻测试正常结束、8表示内阻测试异常结束
        String resistanceTestStatus = (String) packMap.get("resistanceTestStatus");
        if (StrUtil.equals("6", resistanceTestStatus)) {
            // 记录迁移
            insert(packNum, packMap, batteryList);
            return;
        }

        // 电池状态0：监控1：充电2：停电3：核容4：未连接5：备电6：空闲
        String batteryPackStatus = (String) packMap.get("batteryPackStatus");

        // 浮充状态只记录 2 小时
        if (StrUtil.equals("6", batteryPackStatus)) {
            OptLog optLog = optLogService.selectNotFinishedCacheLog(packNum, 1);
            if (optLog == null || optLog.getCreateTime() == null) {
                return;
            }
            if (System.currentTimeMillis() - optLog.getCreateTime().getTime() > batteryFloatingTime) {
                return;
            }
        }
        if (ArrayUtils.contains(BATTERY_PACK_STATUS_ARR, batteryPackStatus)) {
            // 记录迁移
            insert(packNum, packMap, batteryList);
        }
    }

    @Override
    public void deleteByConfigId(Integer packNum) {
        statBatteryPackMapper.deleteByConfigId(Constants.DEFAULT_CONFIG_ID, packNum);
    }

    @Override
    public void export(StatBatteryPack params) {
        params.setConfigId(Constants.DEFAULT_CONFIG_ID);
        BatteryPack batteryPack = batteryPackService.selectBatteryInfoByPackNum(params.getPackNum());
        if (batteryPack == null) {
            throw new RuntimeException("请选择电池组");
        }
        Long count = statBatteryPackMapper.selectCount(params);

        String exportName = getExportName(params);

        String fileName = FileUtils.getUsbPath(params.getExportPath()) + exportName + ".xlsx";
        // 确保目录存在
        File file = new File(fileName);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        try (ExcelWriter excelWriter = EasyExcel.write(fileName).head(BatteryPack.getHeads(batteryPack.getBatSinSize())).build()) {
            // 这里注意 如果同一个sheet只要创建一次
            WriteSheet writeSheet = EasyExcel.writerSheet("模板").build();
            for (int i = 0; i < count; i += 1000) {
                PageHelper.startPage(i / 1000 + 1, 1000, true, false, true);
                List<StatBatteryPack> list = selectList(params);

                /*数据组装*/
                List<List<Object>> excelDataList = list.stream().map(item -> batteryPackMonitorOfExcel(item, batteryPack.getBatSinSize())).collect(Collectors.toList());

                excelWriter.write(excelDataList, writeSheet);
            }
        }

    }

    private static String getExportName(StatBatteryPack params) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String exportName = "历史数据_";
        BatteryTestEnum batteryTestEnum = BatteryTestEnum.find(params.getType());
        if (null != batteryTestEnum) {
            exportName = batteryTestEnum.getDictLabel() + "数据_";
        }
        Object beginTime = params.getParams().get("beginTime");
        Object endTime = params.getParams().get("endTime");
        if (null == beginTime) {
            exportName += sdf.format(new Date());
        } else {
            exportName += format(beginTime);
            if (null != endTime) {
                exportName += "_" + format(endTime);
            }
        }
        return exportName;
    }

    private static String format(Object beginTime) {
        return beginTime.toString().replaceAll("-", "").replaceAll(":", "").replaceAll(" ", "_");
    }

    /**
     * 转换格式
     */
    public static List<Object> batteryPackMonitorOfExcel(StatBatteryPack item, Integer batteryMaxNumber) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<Object> excelData = new ArrayList<>();
        // 数据时间
        excelData.add(simpleDateFormat.format(item.getCreateTime()));
        excelData.add(item.getPackNum() + "");

        // 组电压
        excelData.add(getDouble(item.getPackVoltage()));
        // 组电流
        excelData.add(getDouble(item.getPackCurrent()));
        // 环境温度
        excelData.add(getDouble(item.getEnvironmentTemperature1()));

        batterMonitor(item.getStatBatteryList(), batteryMaxNumber, excelData);
        return excelData;
    }

    /**
     * 单体电池数据
     *
     * @param batteryMonitorList 单体
     * @param batteryMaxNumber   最大单数数量
     * @param excelData          excel
     */
    private static void batterMonitor(List<StatBatteryBat> batteryMonitorList, int batteryMaxNumber, List<Object> excelData) {
        if (CollectionUtil.isEmpty(batteryMonitorList)) {
            return;
        }
        if (0 == batteryMaxNumber) {
            return;
        }
        Map<Integer, StatBatteryBat> batteryMonitorMap = batteryMonitorList.stream().collect(Collectors.toMap(StatBatteryBat::getBatNum, Function.identity()));

        List<Object> voltageList = new ArrayList<>();
        List<Object> temperatureList = new ArrayList<>();
        List<Object> resistanceList = new ArrayList<>();
        for (int i = 1; i <= batteryMaxNumber; i++) {
            StatBatteryBat bm = batteryMonitorMap.get(i);
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

    /**
     * double
     */
    private static Double getDouble(Double value) {
        if (null == value || Double.isNaN(value)) {
            return 0.0D;
        }
        return value;
    }


    private void insert(Integer packNum, Map<String, Object> packMap, List<BatteryMonitor> batteryList) {
        StatBatteryPack statBatteryPack = new StatBatteryPack();
        statBatteryPack.setId(IdUtils.getSnowflakeId());
        statBatteryPack.setConfigId(Constants.DEFAULT_CONFIG_ID);
        statBatteryPack.setPackNum(packNum);
        statBatteryPack.setPackVoltage(getPackVoltage(packMap));
        statBatteryPack.setPackCurrent(packMap.get("packCurrent") == null ? null : Double.parseDouble((String) packMap.get("packCurrent")));

        statBatteryPack.setBatteryPackFloatCurrent(packMap.get("batteryPackFloatCurrent") == null ? null : Double.parseDouble((String) packMap.get("batteryPackFloatCurrent")));
        statBatteryPack.setEnvironmentTemperature1(packMap.get("environmentTemperature1") == null ? null : Double.parseDouble((String) packMap.get("environmentTemperature1")));
        statBatteryPack.setEnvironmentTemperature2(packMap.get("environmentTemperature2") == null ? null : Double.parseDouble((String) packMap.get("environmentTemperature2")));
        statBatteryPack.setBcapacity(packMap.get("bcapacity") == null ? null : Double.parseDouble((String) packMap.get("bcapacity")));
        statBatteryPack.setHydrogenConcentration(packMap.get("hydrogenConcentration") == null ? null : Double.parseDouble((String) packMap.get("hydrogenConcentration")));

        List<StatBatteryBat> statBatteries = batteryList.stream().map(StatBatteryBat::of).collect(Collectors.toList());

        statBatteries.forEach(item -> item.setPackId(statBatteryPack.getId()));

        MessageFactory.pushData(statBatteryPack);
        MessageFactory.pushData(statBatteries);
    }

    /**
     * 组电压
     */
    private Double getPackVoltage( Map<String, Object> packMap) {
        // 组电压
        String voltage = (String) packMap.get("batteryPackOuterVoltage");
        if (voltage != null && Double.parseDouble(voltage) != 0) {
            return Double.parseDouble(voltage);
        }
        voltage = (String) packMap.get("packVoltage");
        if (voltage != null && Double.parseDouble(voltage) != 0) {
            return Double.parseDouble(voltage);
        }
        return 0.0;
    }
}
