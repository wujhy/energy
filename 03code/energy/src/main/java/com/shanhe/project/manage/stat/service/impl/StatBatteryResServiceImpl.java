package com.shanhe.project.manage.stat.service.impl;


import com.alibaba.excel.EasyExcel;
import com.google.common.collect.Lists;
import com.shanhe.common.constant.Constants;
import com.shanhe.common.utils.file.FileUtils;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.manage.config.domain.BatteryPack;
import com.shanhe.project.manage.config.service.IBatteryPackService;
import com.shanhe.project.manage.stat.domain.DevBatteryMonomer;
import com.shanhe.project.manage.stat.domain.StatBatteryRes;
import com.shanhe.project.manage.stat.mapper.StatBatteryResMapper;
import com.shanhe.project.manage.stat.service.IDevBatteryMonomerService;
import com.shanhe.project.manage.stat.service.IStatBatteryResService;
import lombok.extern.slf4j.Slf4j;
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
 * @author wjh
 * @since 2026-05-25
 */
@Slf4j
@Service
public class StatBatteryResServiceImpl implements IStatBatteryResService {

    /** 电池内阻统计数据访问层。 */
    private final StatBatteryResMapper statBatteryResMapper;
    /** 电池单体配置服务。 */
    private final IDevBatteryMonomerService devBatteryMonomerService;
    /** 电池组服务。 */
    @Resource
    private IBatteryPackService batteryPackService;


    public StatBatteryResServiceImpl(StatBatteryResMapper statBatteryResMapper,
                                     IDevBatteryMonomerService devBatteryMonomerService) {
        this.statBatteryResMapper = statBatteryResMapper;
        this.devBatteryMonomerService = devBatteryMonomerService;
    }

    @Override
    public Map<String, Object> getResistanceReport(Integer packNum) {
        Map<String, Object> result = new HashMap<>(16);

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
        List<StatBatteryRes> statBatteryRes = statBatteryResMapper.selectList(packNum, null);

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

    /** 获取时间 */
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
            Map<String, Object> row = new HashMap<>(16);

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
            Map<String, Integer> resList = resByBatNumMap.getOrDefault(i, new HashMap<>(8));

            for (String testDate : time) {
                Integer resistance = resList.get(testDate);

                row.put(testDate, getResistanceValue(resistance));
            }

            data.add(row);
        }
        return data;
    }

    /** 获取基准值 */
    private Map<Integer, Integer> getBaseValueMap(Integer packNum) {
        List<DevBatteryMonomer> devBatteryMonomers = devBatteryMonomerService.selectList(packNum);
        if (devBatteryMonomers == null || devBatteryMonomers.isEmpty()) {
            return new HashMap<>(0);
        }

        // 按单体编号分组的基准值
        return devBatteryMonomers.stream()
                .collect(Collectors.toMap(DevBatteryMonomer::getBatNum, DevBatteryMonomer::getResistance, (v1, v2) -> v2));
    }

    /** 获取最新的内阻测试记录（每个单体） */
    private static Map<Integer, Integer> getLatestResMap(List<StatBatteryRes> statBatteryRes, SimpleDateFormat dateFormat) {
        if (statBatteryRes == null || statBatteryRes.isEmpty()) {
            return new HashMap<>(0);
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
    public void initRealtime(Integer packNum, List<BatteryModuleCellRealtime> cells) {
        if (!isCompleteRealtimeCells(packNum, cells)) {
            return;
        }
        List<StatBatteryRes> statBatteryResList = generateRealtimeStatBatteryRes(packNum, cells);
        if (statBatteryResList.isEmpty()) {
            return;
        }
        try {
            statBatteryResMapper.insertList(statBatteryResList);
        } catch (Exception e) {
            log.error("插入数据异常", e);
        }
    }

    private boolean isCompleteRealtimeCells(Integer packNum, List<BatteryModuleCellRealtime> cells) {
        if (cells == null || cells.isEmpty()) {
            return false;
        }
        Integer expectedCellCount = resolveExpectedCellCount(packNum);
        Set<Integer> validBatNums = new HashSet<>();
        for (BatteryModuleCellRealtime cell : cells) {
            if (cell == null || cell.getBatNum() == null || cell.getResistance() == null) {
                return false;
            }
            validBatNums.add(cell.getBatNum());
        }
        if (expectedCellCount == null || expectedCellCount <= 0) {
            return true;
        }
        for (int batNum = 1; batNum <= expectedCellCount; batNum++) {
            if (!validBatNums.contains(batNum)) {
                return false;
            }
        }
        return true;
    }

    private Integer resolveExpectedCellCount(Integer packNum) {
        if (batteryPackService == null || packNum == null) {
            return null;
        }
        try {
            BatteryPack batteryPack = batteryPackService.selectBatteryInfoByPackNum(packNum);
            return batteryPack == null ? null : batteryPack.getBatSinSize();
        } catch (Exception e) {
            log.debug("获取电池组单体数失败, packNum={}", packNum, e);
            return null;
        }
    }

    @Override
    public Map<Integer, Integer> last(Integer packNum) {
        // 每次测试内阻值
        List<StatBatteryRes> statBatteryRes = statBatteryResMapper.selectList(packNum, null);
        return getLatestResMap(statBatteryRes, new SimpleDateFormat("yyyy-MM-dd"));
    }

    @Override
    public List<StatBatteryRes> listResistance(Integer packNum, Integer batNum) {
        return statBatteryResMapper.selectList(packNum, batNum);
    }

    @Override
    public void deleteByPackNum(Integer packNum) {
        statBatteryResMapper.deleteByPackNum(packNum);
    }

    @Override
    public void export(Integer packNum, String exportPath) {
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
        List<StatBatteryRes> statBatteryRes = statBatteryResMapper.selectList(packNum, null);

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

    /** 转换为导出格式的数据。 */
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
    /** 生成标准实时模型内阻值。 */
    private static List<StatBatteryRes> generateRealtimeStatBatteryRes(Integer packNum, List<BatteryModuleCellRealtime> cells) {
        List<StatBatteryRes> statBatteryResList = new ArrayList<>();
        for (BatteryModuleCellRealtime cell : cells) {
            if (cell == null || cell.getBatNum() == null || cell.getResistance() == null) {
                continue;
            }
            StatBatteryRes statBatteryRes = new StatBatteryRes();
            statBatteryRes.setConfigId(Constants.DEFAULT_CONFIG_ID);
            statBatteryRes.setPackNum(packNum);
            statBatteryRes.setBatNum(cell.getBatNum());
            statBatteryRes.setResistance(cell.getResistance());
            statBatteryResList.add(statBatteryRes);
        }
        return statBatteryResList;
    }
    /** 计算内阻变化率。 */
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

    /** 格式化内阻值。 */
    private String getResistanceValue(Integer value) {
        if (null == value) {
            return "N/A";
        }
        return value + "uΩ";
    }
}
