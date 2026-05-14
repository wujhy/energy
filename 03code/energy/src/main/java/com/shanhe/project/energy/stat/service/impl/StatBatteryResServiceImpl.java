package com.shanhe.project.energy.stat.service.impl;


import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.github.pagehelper.PageHelper;
import com.google.common.collect.Lists;
import com.shanhe.common.constant.Constants;
import com.shanhe.common.utils.file.FileUtils;
import com.shanhe.project.device.config.domain.BatteryMonitor;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.domain.BatteryReportLog;
import com.shanhe.project.device.config.service.IBatteryPackService;
import com.shanhe.project.energy.stat.domain.DevBatteryMonomer;
import com.shanhe.project.energy.stat.domain.StatBatteryPack;
import com.shanhe.project.energy.stat.domain.StatBatteryRes;
import com.shanhe.project.energy.stat.mapper.StatBatteryResMapper;
import com.shanhe.project.energy.stat.service.IDevBatteryMonomerService;
import com.shanhe.project.energy.stat.service.IStatBatteryResService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 单体内阻变化统计（内阻测试后）Service业务层处理
 *
 * @author zhoubin
 * @date 2025-07-21
 */
@Service
public class StatBatteryResServiceImpl implements IStatBatteryResService {

    protected static Logger logger = LoggerFactory.getLogger(StatBatteryResServiceImpl.class);

    private final StatBatteryResMapper statBatteryResMapper;
    private final IDevBatteryMonomerService devBatteryMonomerService;
    @Resource
    private IBatteryPackService batteryPackService;


    public StatBatteryResServiceImpl(StatBatteryResMapper statBatteryResMapper,
                                     IDevBatteryMonomerService devBatteryMonomerService) {
        this.statBatteryResMapper = statBatteryResMapper;
        this.devBatteryMonomerService = devBatteryMonomerService;
    }

    @Override
    public Map<String, Object> getResistanceReport(Integer packNum) {
        Long configId = Constants.DEFAULT_CONFIG_ID;
        Map<String, Object> result = new HashMap<>();

        BatteryPack batteryPack = batteryPackService.selectBatteryInfoByPackNum(packNum);
        if (batteryPack == null) {
            return result;
        }
        // 单体 #1 为 字段 batNum
        // 内阻上涨比 = statBatteryRes 时间最大的内阻 / 基准值

        // 数据组装
        // 序号      单体      基准值     内阻上涨比       2025-04-01      2025-03-01  2025-02-01
        // 1         #1      604uΩ      50%            1208uΩ          1208uΩ      1208uΩ

        // 基准值
        Map<Integer, Integer> baseValueMap = getBaseValueMap(packNum);
        if (null == baseValueMap || baseValueMap.isEmpty()) {
            return result;
        }

        // 每次测试内阻值
        List<StatBatteryRes> statBatteryRes = statBatteryResMapper.selectList(configId, packNum, null);

        // 按时间分组的内阻测试数据
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        // key 为单体编号
        // value  key 为时间 value 为内阻值
        Map<Integer, Map<String, Integer>> resByBatNumMap = statBatteryRes.stream()
                .collect(Collectors.groupingBy(StatBatteryRes::getBatNum,
                        Collectors.toMap(res -> dateFormat.format(res.getCreateTime()), StatBatteryRes::getResistance, (v1, v2) -> v1)));

        // 最新内阻值
        Map<Integer, Integer> latestResMap = getLatestResMap(statBatteryRes, dateFormat);

        // 获取时间
        Set<String> time = getTime(statBatteryRes, dateFormat);

        // 组装数据
        List<Map<String, Object>> data = getData(batteryPack.getBatSinSize(), baseValueMap, latestResMap, resByBatNumMap, time);
        result.put("data", data);
        result.put("head", time);
        return result;
    }

    /**
     * 获取时间
     */
    private static Set<String> getTime(List<StatBatteryRes> statBatteryRes, SimpleDateFormat dateFormat) {
        Set<String> time = new LinkedHashSet<>();

        // 按时间排序的所有测试时间点
        statBatteryRes.stream().map(StatBatteryRes::getCreateTime)
                .sorted(Comparator.reverseOrder())
                .forEach(date -> time.add(dateFormat.format(date)));
        return time;
    }

    /**
     * 组装数据
     * @param batSinSize 单体个数
     * @param baseValueMap 基准值
     * @param latestResMap 最新内阻值
     * @param resByBatNumMap 按单体编号分组的内阻值
     * @param time  时间
     * @return 数据
     */
    private List<Map<String, Object>> getData(Integer batSinSize, Map<Integer, Integer> baseValueMap,
                                              Map<Integer, Integer> latestResMap,
                                              Map<Integer, Map<String, Integer>> resByBatNumMap,
                                              Set<String> time) {
        List<Map<String, Object>> data = new ArrayList<>();
        for (int i = 1; i <= batSinSize; i++) {
            Integer baseResistance = baseValueMap.get(i);
            Map<String, Object> row = new HashMap<>();

            // 序号
            row.put("index", i);

            // 单体
            row.put("batNum", "#" + i);

            // 基准值
            row.put("baseValue", getResistanceValue(baseResistance));

            // 内阻上涨比
            Integer latestResistance = latestResMap.get(i);
            row.put("resistanceRatio", getResistanceRatio(latestResistance, baseResistance));

            // 各时间点的内阻值
            Map<String, Integer> resList = resByBatNumMap.getOrDefault(i, new HashMap<>());

            for (String testDate : time) {
                Integer resistance = resList.get(testDate);

                row.put(testDate, getResistanceValue(resistance));
            }

            data.add(row);
        }
        return data;
    }

    /**
     * 获取基准值
     */
    private Map<Integer, Integer> getBaseValueMap(Integer packNum) {
        List<DevBatteryMonomer> devBatteryMonomers = devBatteryMonomerService.selectList(packNum);
        if (devBatteryMonomers == null || devBatteryMonomers.isEmpty()) {
            return new HashMap<>();
        }

        // 按单体编号分组的基准值
        return devBatteryMonomers.stream()
                .collect(Collectors.toMap(DevBatteryMonomer::getBatNum, DevBatteryMonomer::getResistance, (v1, v2) -> v2));
    }

    /**
     * 获取最新的内阻测试记录（每个单体）
     */
    private static Map<Integer, Integer> getLatestResMap(List<StatBatteryRes> statBatteryRes, SimpleDateFormat dateFormat) {
        if (statBatteryRes == null || statBatteryRes.isEmpty()) {
            return new HashMap<>();
        }

        // 获取最新的内阻测试记录（每个单体）
        // 按时间倒序排序，获取最新的测试时间
        String latestTime = statBatteryRes.stream()
                .map(res -> dateFormat.format(res.getCreateTime()))
                .max(String::compareTo)
                .orElse(null);

        // key 为时间
        // value  key 为单体编号 value 为内阻值
        return statBatteryRes.stream().filter(res -> dateFormat.format(res.getCreateTime()).equals(latestTime))
                .collect(Collectors.toMap(StatBatteryRes::getBatNum, StatBatteryRes::getResistance, (v1, v2) -> v2));
    }

    @Async
    @Override
    public void init(Integer packNum, Map<String, Object> packMap, List<BatteryMonitor> batteryList, BatteryReportLog oldInfo) {
        Long configId = Constants.DEFAULT_CONFIG_ID;
        if (null == oldInfo) {
            return;
        }
        Map<String, Object> packParam = oldInfo.getPackParam();
        if (packParam == null) {
            return;
        }

        // 上一次不是内阻测试状态
        String resistanceTestStatus = (String) packParam.get("resistanceTestStatus");
        if (!StrUtil.equals("6", resistanceTestStatus)) {
            return;
        }

        // 内阻测试未完成
        String newResistanceTestStatus = (String) packMap.get("resistanceTestStatus");
        if (StrUtil.equals(resistanceTestStatus, newResistanceTestStatus)) {
            return;
        }

        if (batteryList == null || batteryList.isEmpty()) {
            return;
        }
        // 结束内阻测试，生成内阻值
        List<StatBatteryRes> statBatteryResList = generateStatBatteryRes(configId, packNum, batteryList);

        try {
            statBatteryResMapper.insertList(statBatteryResList);
        } catch (Exception e) {
            logger.error("插入数据异常", e);
        }
    }

    @Override
    public Map<Integer, Integer> last(Integer packNum) {
        Long configId = Constants.DEFAULT_CONFIG_ID;
        // 每次测试内阻值
        List<StatBatteryRes> statBatteryRes = statBatteryResMapper.selectList(configId, packNum, null);
        return getLatestResMap(statBatteryRes, new SimpleDateFormat("yyyy-MM-dd"));
    }

    @Override
    public List<StatBatteryRes> listResistance(Integer packNum, Integer batNum) {
        Long configId = Constants.DEFAULT_CONFIG_ID;
        return statBatteryResMapper.selectList(configId, packNum, batNum);
    }

    @Override
    public void deleteByConfigId(Integer packNum) {
        Long configId = Constants.DEFAULT_CONFIG_ID;
        statBatteryResMapper.deleteByConfigId(configId, packNum);
    }

    @Override
    public void export(Integer packNum, String exportPath) {
        Long configId = Constants.DEFAULT_CONFIG_ID;
        BatteryPack batteryPack = batteryPackService.selectBatteryInfoByPackNum(packNum);
        if (batteryPack == null) {
            return;
        }
        // 单体 #1 为 字段 batNum
        // 内阻上涨比 = statBatteryRes 时间最大的内阻 / 基准值

        // 数据组装
        // 序号      单体      基准值     内阻上涨比       2025-04-01      2025-03-01  2025-02-01
        // 1         #1      604uΩ      50%            1208uΩ          1208uΩ      1208uΩ

        // 基准值
        Map<Integer, Integer> baseValueMap = getBaseValueMap(packNum);
        if (null == baseValueMap || baseValueMap.isEmpty()) {
            return;
        }

        // 每次测试内阻值
        List<StatBatteryRes> statBatteryRes = statBatteryResMapper.selectList(configId, packNum, null);

        // 按时间分组的内阻测试数据
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        // key 为单体编号
        // value  key 为时间 value 为内阻值
        Map<Integer, Map<String, Integer>> resByBatNumMap = statBatteryRes.stream()
                .collect(Collectors.groupingBy(StatBatteryRes::getBatNum,
                        Collectors.toMap(res -> dateFormat.format(res.getCreateTime()), StatBatteryRes::getResistance, (v1, v2) -> v1)));

        // 最新内阻值
        Map<Integer, Integer> latestResMap = getLatestResMap(statBatteryRes, dateFormat);

        // 获取时间
        List<String> head = Lists.newArrayList("单体", "基准值", "内阻上涨比");
        Set<String> time = getTime(statBatteryRes, dateFormat);
        head.addAll(time);
        List<List<String>> heads = head.stream().map(Lists::newArrayList).collect(Collectors.toList());

        // 组装数据
        List<Map<String, Object>> data = getData(batteryPack.getBatSinSize(), baseValueMap, latestResMap, resByBatNumMap, time);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");

        String fileName = FileUtils.getUsbPath(exportPath) + "单体内阻记录_" + sdf.format(new Date()) + ".xlsx";

        // 确保目录存在
        File file = new File(fileName);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        EasyExcel.write(fileName).head(heads).sheet("单体内阻记录").doWrite(getExportData(data, time));
    }

    private List<List<Object>> getExportData(List<Map<String, Object>> data, Set<String> time) {
        List<List<Object>> list = new ArrayList<>();
        for (Map<String, Object> map : data) {
            List<Object> row = new ArrayList<>();
            row.add(map.get("batNum"));
            row.add(map.get("baseValue"));
            row.add(map.get("resistanceRatio"));
            for (String timeStr : time) {
                row.add(map.get(timeStr));
            }
            list.add(row);
        }
        return list;
    }


    /**
     * 生成内阻值
     */
    private static List<StatBatteryRes> generateStatBatteryRes(Long configId, Integer packNum, List<BatteryMonitor> batteryList) {
        List<StatBatteryRes> statBatteryResList = new ArrayList<>();
        for (BatteryMonitor batteryInfo : batteryList) {
            StatBatteryRes statBatteryRes = new StatBatteryRes();
            statBatteryRes.setConfigId(configId);
            statBatteryRes.setPackNum(packNum);

            statBatteryRes.setBatNum(batteryInfo.getBatNum());
            statBatteryRes.setResistance(batteryInfo.getResistance());

            statBatteryResList.add(statBatteryRes);
        }
        return statBatteryResList;
    }


    private String getResistanceRatio(Integer latestResistance, Integer baseResistance) {
        if (latestResistance == null || baseResistance == null || baseResistance == 0) {
            return "N/A";
        }
        if (latestResistance <= baseResistance) {
            return "0%";
        }
        double ratio = ((latestResistance - baseResistance) * 100.0) / latestResistance;
        return String.format("%.2f%%", ratio);
    }

    private String getResistanceValue(Integer value) {
        if (null == value) {
            return "N/A";
        }
        return value + "uΩ";
    }
}
