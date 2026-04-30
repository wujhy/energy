package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.mapper.BatteryModuleRealtimeMapper;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 电池组指标计算服务。
 *
 * @author wjh
 * @since 2026-04-28
 */
@Service
public class BatteryModuleGroupCalculationService {

    /**
     * 默认单体实时数据新鲜度阈值。
     */
    private static final long DEFAULT_STALE_THRESHOLD_MS = 180_000L;

    /**
     * 实时数据 Mapper。
     */
    @Resource
    private BatteryModuleRealtimeMapper realtimeMapper;

    /**
     * 按默认新鲜度阈值计算并保存电池组指标。
     *
     * @param batteryGroup 电池组编号
     * @return 计算结果
     */
    public BatteryModuleGroupRealtime calculateAndSave(Integer batteryGroup) {
        return calculateAndSave(batteryGroup, null, null, DEFAULT_STALE_THRESHOLD_MS);
    }

    /**
     * 计算并保存电池组指标。
     *
     * @param batteryGroup 电池组编号
     * @param staleThresholdMs 单体数据新鲜度阈值
     * @return 计算结果
     */
    public BatteryModuleGroupRealtime calculateAndSave(Integer batteryGroup, long staleThresholdMs) {
        return calculateAndSave(batteryGroup, null, null, staleThresholdMs);
    }

    /**
     * 按指定轮询批次计算并保存电池组指标。
     *
     * @param batteryGroup 电池组编号
     * @param pollBatchNo 轮询批次号
     * @param pollStartedAt 轮询开始时间
     * @param staleThresholdMs 单体数据新鲜度阈值
     * @return 计算后的组实时数据
     */
    public BatteryModuleGroupRealtime calculateAndSave(Integer batteryGroup,
                                                       String pollBatchNo,
                                                       Date pollStartedAt,
                                                       long staleThresholdMs) {
        List<BatteryModuleCellRealtime> cells = pollBatchNo == null
                ? realtimeMapper.selectCells(batteryGroup)
                : realtimeMapper.selectCellsByBatch(batteryGroup, pollBatchNo);
        BatteryModuleGroupRealtime group = realtimeMapper.selectGroup(batteryGroup);
        BatteryModuleGroupRealtime calculation = buildCalculation(batteryGroup, cells, group,
                new Date(), pollBatchNo, pollStartedAt, staleThresholdMs);
        realtimeMapper.updateGroupCalculation(calculation);
        return calculation;
    }

    BatteryModuleGroupRealtime buildCalculation(Integer batteryGroup,
                                                List<BatteryModuleCellRealtime> cells,
                                                BatteryModuleGroupRealtime group,
                                                Date now,
                                                String pollBatchNo,
                                                Date pollStartedAt,
                                                long staleThresholdMs) {
        BatteryModuleGroupRealtime calculation = new BatteryModuleGroupRealtime();
        calculation.setPackNum(batteryGroup);
        calculation.setPollBatchNo(pollBatchNo);
        calculation.setPollStartedAt(pollStartedAt);
        calculation.setCellCount(cells == null ? 0 : cells.size());

        long nowMs = now == null ? System.currentTimeMillis() : now.getTime();
        int onlineCount = 0;
        int staleCount = 0;

        Accumulator voltage = new Accumulator();
        Accumulator temperature = new Accumulator();
        IntAccumulator resistance = new IntAccumulator();
        Date latestCellUpdateTime = null;

        if (cells != null) {
            for (BatteryModuleCellRealtime cell : cells) {
                if (cell == null) {
                    continue;
                }
                Date sampleTime = cell.getCreateTime();
                latestCellUpdateTime = latest(latestCellUpdateTime, sampleTime);
                boolean online = true;
                // 超过新鲜度阈值的最新值不参与在线数量判断。
                if (sampleTime == null || nowMs - sampleTime.getTime() > staleThresholdMs) {
                    staleCount++;
                    online = false;
                }
                if (online) {
                    onlineCount++;
                }
                voltage.accept(cell.getBatNum(), cell.getVoltage());
                temperature.accept(cell.getBatNum(), cell.getTemperature());
                resistance.accept(cell.getBatNum(), cell.getResistance());
            }
        }

        calculation.setOnlineCellCount(onlineCount);
        calculation.setStaleCellCount(staleCount);
        calculation.setDataFresh(calculation.getCellCount() > 0 && staleCount == 0);
        calculation.setLatestCellUpdateTime(latestCellUpdateTime);

        voltage.applyVoltage(calculation);
        temperature.applyTemperature(calculation);
        resistance.apply(calculation);

        if (group != null) {
            calculation.setPackVoltage(group.getPackVoltage());
            calculation.setPackCurrent(group.getPackCurrent());
            calculation.setBatteryPackFloatCurrent(group.getBatteryPackFloatCurrent());
            calculation.setBatteryPackOuterVoltage(group.getBatteryPackOuterVoltage());
            calculation.setExternalVoltage(group.getExternalVoltage());
            calculation.setChargeDischargeCurrent(group.getChargeDischargeCurrent());
            calculation.setFloatCurrent(group.getFloatCurrent());
            calculation.setEnvironmentTemperature1(group.getEnvironmentTemperature1());
            calculation.setEnvironmentTemperature2(group.getEnvironmentTemperature2());
            calculation.setLatestGroupUpdateTime(group.getCreateTime());
            calculation.setGroupModuleFresh(pollBatchNo != null
                    && pollBatchNo.equals(group.getPollBatchNo())
                    && Boolean.TRUE.equals(group.getGroupModuleFresh()));
        } else {
            calculation.setGroupModuleFresh(Boolean.FALSE);
        }
        return calculation;
    }

    private Date latest(Date current, Date candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null || candidate.after(current)) {
            return candidate;
        }
        return current;
    }

    private static class Accumulator {
        private Integer minAddress;
        private Integer maxAddress;
        private Double min;
        private Double max;
        private double sum;
        private int count;

        void accept(Integer address, Double value) {
            if (value == null) {
                return;
            }
            if (min == null || value < min) {
                min = value;
                minAddress = address;
            }
            if (max == null || value > max) {
                max = value;
                maxAddress = address;
            }
            sum += value;
            count++;
        }

        void applyVoltage(BatteryModuleGroupRealtime calculation) {
            calculation.setMinVoltageBatNum(minAddress);
            calculation.setMinCellVoltage(min);
            calculation.setMaxVoltageBatNum(maxAddress);
            calculation.setMaxCellVoltage(max);
            calculation.setAvgCellVoltage(avg());
            calculation.setVoltageRange(range());
        }

        void applyTemperature(BatteryModuleGroupRealtime calculation) {
            calculation.setMinTemperatureBatNum(minAddress);
            calculation.setMinCellTemperature(min);
            calculation.setMaxTemperatureBatNum(maxAddress);
            calculation.setMaxCellTemperature(max);
            calculation.setAvgCellTemperature(avg());
            calculation.setTemperatureRange(range());
        }

        private Double avg() {
            return count == 0 ? null : sum / count;
        }

        private Double range() {
            return min == null || max == null ? null : max - min;
        }
    }

    private static class IntAccumulator {
        private Integer minAddress;
        private Integer maxAddress;
        private Integer min;
        private Integer max;
        private long sum;
        private int count;

        void accept(Integer address, Integer value) {
            if (value == null) {
                return;
            }
            if (min == null || value < min) {
                min = value;
                minAddress = address;
            }
            if (max == null || value > max) {
                max = value;
                maxAddress = address;
            }
            sum += value;
            count++;
        }

        void apply(BatteryModuleGroupRealtime calculation) {
            calculation.setMinResistanceBatNum(minAddress);
            calculation.setMinInternalResistance(min);
            calculation.setMaxResistanceBatNum(maxAddress);
            calculation.setMaxInternalResistance(max);
            calculation.setAvgInternalResistance(count == 0 ? null : (double) sum / count);
            calculation.setResistanceRange(min == null || max == null ? null : max - min);
        }
    }
}
