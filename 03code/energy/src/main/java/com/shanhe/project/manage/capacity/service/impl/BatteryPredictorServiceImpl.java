package com.shanhe.project.manage.capacity.service.impl;

import com.alibaba.fastjson.JSON;
import com.shanhe.common.constant.Constants;
import com.shanhe.common.utils.DateUtils;
import com.shanhe.common.utils.StringUtils;
import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.manage.config.domain.BatteryPack;
import com.shanhe.project.manage.config.service.IBatteryPackService;
import com.shanhe.project.manage.opt.domain.OptLog;
import com.shanhe.project.manage.opt.service.OptLogService;
import com.shanhe.project.manage.capacity.service.BatteryPredictorService;
import com.shanhe.project.manage.capacity.service.DataPointService;
import com.shanhe.project.manage.capacity.service.PreBatteryGroupService;
import com.shanhe.project.manage.capacity.tool.FilterProcessor;
import com.shanhe.project.manage.capacity.tool.RateCapacityConverter;
import com.shanhe.project.manage.capacity.vo.DataPoint;
import com.shanhe.project.manage.capacity.vo.PreBatteryGroup;
import com.shanhe.project.manage.capacity.vo.PreBatteryVo;
import com.shanhe.project.manage.stat.service.IDevBatteryMonomerService;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 蓄电池预测服务实现类
 *
 * @author wjh
 * @since 2026-05-25
 */
@Slf4j
@Service
public class BatteryPredictorServiceImpl implements BatteryPredictorService {

    /** 数据点服务。 */
    @Resource
    private DataPointService dataPointService;
    /** 电池组信息服务。 */
    @Resource
    private IBatteryPackService devBatteryInfoService;
    /** 预估电池组服务。 */
    @Resource
    private PreBatteryGroupService preBatteryGroupService;
    /** 操作日志服务。 */
    @Resource
    private OptLogService optLogService;
    /** 蓄电池单体服务。 */
    @Resource
    private IDevBatteryMonomerService devBatteryMonomerService;

    @Async
    @Override
    public void doTotalBatteryStep(BatteryModuleGroupRealtime group, List<BatteryModuleCellRealtime> cells) {
        if (group == null || group.getPackNum() == null) {
            return;
        }

        Integer packNum = group.getPackNum();
        OptLog optLog = optLogService.lastType(packNum, BatteryTestEnum._5.getDictValue());
        if (optLog == null) {
            return;
        }

        log.error("放电结束，开始预估电池容量==========================");
        PreBatteryGroup preBatteryGroup = calcPredictorBatCapacity(packNum, optLog.getCreateTime(), group, cells);
        if (preBatteryGroup == null) {
            return;
        }

        // 更新操作日志
        optLogService.updateBatteryCapacity(optLog.getId(), preBatteryGroup.getDischargeCapacity(), preBatteryGroup.getBcapacity(), preBatteryGroup.getCurrent(), preBatteryGroup.getEndTime());
    }

    /** 计算电池组预估容量。 */
    private PreBatteryGroup calcPredictorBatCapacity(Integer packNum, Date startTime,
                                                     BatteryModuleGroupRealtime group,
                                                     List<BatteryModuleCellRealtime> cells) {
        Date endTime = new Date();
        int diffMills = DateUtils.differentMillsByMillisecond(startTime, endTime);
        // 30分钟
        if (diffMills < 30) {
            log.error("放电测试低于30分钟，无法预估电池容量");
            return null;
        }
        // 电池基本信息
        BatteryPack batteryInfo = devBatteryInfoService.selectBatteryInfoByPackNum(packNum);
        if (batteryInfo == null) {
            log.error("未找到电池基本信息");
            return null;
        }

        // 获取规格
        double spec = getSpec(batteryInfo);
        // 2V
        int specSize = 1;
        //12V电池
        if (Double.compare(spec, 12.0) == 0) {
            specSize = 6;
        }

        // 额定容量
        Double aCapacity = batteryInfo.getBatCapacity();
        if (aCapacity == null || aCapacity == 0) {
            log.error("电池额定容量为空，无法预估电池容量");
            return null;
        }
        // 电池组充放电电流,获取阶段内的平均电流
        Double current = dataPointService.getAvgCurrent(packNum, startTime, endTime);
        if (current == null) {
            current = group.getChargeDischargeCurrent();
        }
        log.info("放电预估容量统计，电流：{}", current);
        if (current == null || current == 0) {
            return null;
        }
        current = Math.round(current * 100.0) / 100.0;

        // 获取所有单体预估容量
        Map<String, PreBatteryVo> batteryVoMap = getPreBatteryVoMap(packNum, cells, current, startTime, endTime, aCapacity, specSize);

        // 获取电池组预估容量
        PreBatteryGroup groupVo = initPreBatteryGroupVo(batteryInfo, current, startTime, endTime, spec, batteryVoMap);
        preBatteryGroupService.insert(groupVo);
        return groupVo;
    }


    /** 获取所有单体预估容量 */
    private Map<String, PreBatteryVo> getPreBatteryVoMap(Integer packNum, List<BatteryModuleCellRealtime> cells, Double current,
                                                         Date startTime, Date endTime,
                                                         Double aCapacity, int specSize) {
        // 单体预估容量
        Map<String, PreBatteryVo> result = new HashMap<>(specSize);
        // 放电倍率
        double crate = current / aCapacity;
        crate = StringUtils.formatToDouble(crate, 2);
        Integer intervalTime = null;
        double diffSlope;
        if (Math.abs(crate) > 0.1) {
            diffSlope = RateCapacityConverter.calculateSlopeRelationship(0.1, crate);
        } else {
            diffSlope = RateCapacityConverter.calculateSlopeRelationship(crate, 0.1);
        }
        log.debug("=====相差斜率========{}", String.format("%.6f", Math.abs(diffSlope)));

        for (BatteryModuleCellRealtime bat : cells) {
            if (bat == null || bat.getBatNum() == null) {
                continue;
            }
            PreBatteryVo vo = processSingleBattery(bat, packNum, startTime, endTime,
                    aCapacity, crate, current, specSize, diffSlope, intervalTime);
            if (vo != null) {
                result.put(Constants.CAP_BAT + bat.getBatNum(), vo);
                // 记录间隔时间（只计算一次）
                if (intervalTime == null) {
                    List<DataPoint> dataPoints = dataPointService.findCurrentDataPoint(packNum, bat.getBatNum(), startTime, endTime);
                    if (dataPoints != null && dataPoints.size() >= 2) {
                        intervalTime = DateUtils.differentSecondByMillisecond(startTime, endTime) / dataPoints.size();
                    }
                }
            }
        }
        return result;
    }

    /** 处理单个电池的预估容量计算 */
    private PreBatteryVo processSingleBattery(BatteryModuleCellRealtime bat, Integer packNum,
                                               Date startTime, Date endTime,
                                               Double aCapacity, double crate, Double current,
                                               int specSize, double diffSlope, Integer intervalTime) {
        // 查找电池的放电数据
        List<DataPoint> dataPoints = dataPointService.findCurrentDataPoint(packNum, bat.getBatNum(), startTime, endTime);
        if (dataPoints == null || dataPoints.size() < 2) {
            log.error("电池编号 {} 放电数据不足", bat.getBatNum());
            return null;
        }
        // 对数据进行滤波处理
        dataPoints = FilterProcessor.movingAverageFilter(dataPoints, 2);
        DataPoint firstPoint = dataPoints.get(0);
        DataPoint lastPoint = dataPoints.get(dataPoints.size() - 1);
        if (lastPoint.getVoltage() > (2.1 * specSize)) {
            log.error("电池编号 {} 放电截止电压 {} V 大于 {} V，不做预估！", bat.getBatNum(), lastPoint.getVoltage(), 2.1 * specSize);
            return null;
        }
        int totalSize = dataPoints.size();
        // 间隔时间
        if (intervalTime == null) {
            intervalTime = DateUtils.differentSecondByMillisecond(startTime, endTime) / totalSize;
        }

        // 获取斜率
        double slope = this.calculateDischargeSlope(selectSlopePoints(dataPoints, lastPoint.getVoltage(), specSize));
        log.debug(lastPoint.getVoltage() + "=====斜率0=========" + String.format("%.5f", slope));

        // 计算预估点数
        int preTotalSize = calculatePreTotalSize(lastPoint.getVoltage(), slope, specSize, diffSlope);

        // 因为到临界点，数据预测的点数越少，需要补偿不同点相同电压的情况
        if (lastPoint.getVoltage() <= (1.92 * specSize) && lastPoint.getVoltage() >= (1.86 * specSize)) {
            preTotalSize = (int) Math.round(preTotalSize * 1.05);
        }
        int totalSecond = (totalSize * intervalTime) + (preTotalSize * 60);
        double bCapacity = this.preCapacity(aCapacity, crate, current, totalSecond);
        log.debug(bat.getBatNum() + "==========" + lastPoint.getVoltage() + "======长度======" + totalSize + "======时间====" + ((double) totalSecond / 3600) + "小时" + "=====预估容量===" + bCapacity);

        // 组装对象
        return initPreBatteryVo(bat, aCapacity, firstPoint.getVoltage(), lastPoint.getVoltage(), bCapacity, new Date());
    }

    private List<DataPoint> selectSlopePoints(List<DataPoint> dataPoints, double lastVoltage, int specSize) {
        int totalSize = dataPoints.size();
        if (totalSize <= 2) {
            return dataPoints;
        }
        int fromIndex;
        if (lastVoltage >= (2 * specSize)) {
            fromIndex = Math.min(5, totalSize - 2);
        } else {
            fromIndex = Math.max(0, totalSize - 10);
        }
        return dataPoints.subList(fromIndex, totalSize);
    }

    /** 计算预估点数 */
    private int calculatePreTotalSize(double voltage, double slope, int specSize, double diffSlope) {
        int preTotalSize = 0;
        // 分段处理, 2V一个拐点
        if (voltage >= (2 * specSize)) {
            int p1 = this.calcPrePointTime(voltage, 2 * specSize, slope);
            slope = adjustSlope(slope, specSize, diffSlope, 0.00036, 0.0036);
            log.debug("{}=====斜率1========{}", voltage, String.format("%.6f", slope));
            int p2 = this.calcPrePointTime(2.0 * specSize, 1.88 * specSize, slope);
            int p3 = this.calcPrePointTime(1.88 * specSize, 1.8 * specSize, slope * 3);
            preTotalSize = p1 + p2 + p3;
            log.debug("阶段1=={} 阶段2=={} 阶段3=={}", p1, p2, p3);
        // 1.88V一个拐点
        } else if (voltage >= (1.88 * specSize)) {
            slope = adjustSlope(slope, specSize, diffSlope, 0.00036, 0.0036);
            log.debug("{}=====斜率2========{}", voltage, String.format("%.6f", slope));
            int p2 = this.calcPrePointTime(voltage, 1.88 * specSize, slope);
            int p3 = this.calcPrePointTime(1.88 * specSize, 1.8 * specSize, slope * 3);
            preTotalSize = p2 + p3;
            log.debug("阶段2=={} 阶段3=={}", p2, p3);
        // 1.79结束
        } else if (voltage >= (1.79 * specSize)) {
            slope = adjustSlope(slope, specSize, diffSlope, 0.00108, 0.0108);
            log.debug("{}=====斜率3========{}", voltage, String.format("%.6f", slope));
            int p3 = this.calcPrePointTime(voltage, 1.8 * specSize, slope);
            preTotalSize = p3;
            log.debug("阶段3=={}", p3);
        }
        return preTotalSize;
    }

    /** 调整斜率 */
    private double adjustSlope(double slope, int specSize, double diffSlope, double threshold2V, double threshold12V) {
        if (specSize == 1) {
            if (Math.abs(slope) < threshold2V) {
                slope = -threshold2V;
                slope = slope * diffSlope;
            }
        } else if (specSize == 6) {
            if (Math.abs(slope) < threshold12V) {
                slope = -threshold12V;
                slope = slope * diffSlope;
            }
        }
        return slope;
    }

    /** 初始化 预测电池组对象 */
    private PreBatteryGroup initPreBatteryGroupVo(BatteryPack batteryInfo, Double current, Date startTime, Date endTime,
                                                  double spec, Map<String, PreBatteryVo> batteryVoMap) {
        PreBatteryGroup groupVo = preBatteryGroupService.lastCache(batteryInfo.getPackNum());
        if (groupVo == null) {
            groupVo = PreBatteryGroup.getNewPreBatteryGroupInfo();
        }

        groupVo.setAcapacity(batteryInfo.getBatCapacity());
        groupVo.setCurrent(current);
        groupVo.setConfigId(batteryInfo.getConfigId());
        groupVo.setPackNum(batteryInfo.getPackNum());
        groupVo.setSpec(spec);

        groupVo.setStartTime(startTime);
        groupVo.setEndTime(endTime);

        groupVo.setStaticTime(new Date());

        groupVo.setBackUpDuration(null);
        groupVo.setMapBattery(batteryVoMap);
        groupVo.setMapBatteryData(JSON.toJSONString(batteryVoMap));

        //循环检查单体并设置单体的容量，有可能本轮测试时，电压未达到指定电压，没有预测值，需要拿上一次的预测值
        Map<String, PreBatteryVo> map2 = groupVo.getMapBattery() == null ? Collections.emptyMap() : groupVo.getMapBattery();
        //寻找最低容量，作为电池组的参考值
        Integer minBat = null;
        Double groupCapacity = null;
        for (int i = 1; i <= batteryInfo.getBatSinSize(); i++) {
            PreBatteryVo vo = batteryVoMap.get(Constants.CAP_BAT + i);
            if (vo == null) {
                vo = map2.get(Constants.CAP_BAT + i);
            }
            if (vo == null) {
                continue;
            }

            batteryVoMap.put(Constants.CAP_BAT + i, vo);

            if (groupCapacity == null) {
                minBat = vo.getBatNum();
                groupCapacity = vo.getBcapacity();
            } else {
                if (groupCapacity > vo.getBcapacity()) {
                    groupCapacity = vo.getBcapacity();
                    minBat = vo.getBatNum();
                }
            }
        }
        groupVo.setMapBattery(batteryVoMap);
        groupVo.setMapBatteryData(JSON.toJSONString(batteryVoMap));

        if (minBat == null || groupCapacity == null) {
            return groupVo;
        }

        groupVo.setMinVoltageNum(minBat);
        groupVo.setBcapacity(groupCapacity);

        // 获取放电容量
        groupVo.setDischargeCapacity(getDischargeCapacity(current, startTime, endTime, groupCapacity));

        // 获取SOH
        groupVo.setSoh(getSoh(batteryInfo, groupCapacity));

        // 获取剩余时间
        groupVo.setBackUpDuration(getDuration(current, groupCapacity));
        return groupVo;
    }

    /** 获取剩余时间 */
    private int getDuration(Double current, Double groupCapacity) {
        double dt = (groupCapacity / current) * 60 * -1;
        return (int) Math.round(dt);
    }

    /** 获取放电容量 = packInfo.getPackCurrent() * 放电时间（小时） */
    private double getDischargeCapacity(Double current, Date startTime, Date endTime, Double groupCapacity) {
        double diffMills = DateUtils.differentMillsByMillisecond(startTime, endTime);
        // 放电容量 = 放电电流 * 放电时间（h）
        double dischargeCapacity = Math.abs(current * diffMills / 60);
        // 放电容量不能大于组容量
        dischargeCapacity = Math.min(dischargeCapacity, groupCapacity);
        // 放电容量取整
        return Math.round(dischargeCapacity);
    }

    private double getSoh(BatteryPack batteryInfo, Double groupCapacity) {
        // SOH  = SOH_容量*0.85 + SOH_内阻*0.1 + SOH_时间*0.05
        double soh = getCapacity(batteryInfo.getBatCapacity(), groupCapacity) * 0.85 + getSohResistance(batteryInfo) * 0.1 + getSohTime(batteryInfo) * 0.05;
        return Math.min(Math.round(soh), 100);
    }

    private double getCapacity(Double batCapacity, Double groupCapacity) {
        // SOH_容量=  电池容量/额定容量 * 100%
        return Math.min(batCapacity, groupCapacity) / batCapacity * 100.0;
    }

    /**
     * 计算SOH内阻
     * 1、每次内阻测试后，计算变化率
     * 2、内阻变化率= ( (R_measured - R_initial) / R_initial ) × 100%
     * SOH内阻 ≈ 1 - 内阻变化率 / 2 * 100%
     * 实测内阻最小不能超过初始内阻，实测内阻最高只能为初始内阻的1倍
     */
    private double getSohResistance(BatteryPack batteryInfo) {
        Double maxResistance = devBatteryMonomerService.getMaxResistance(batteryInfo.getPackNum());
        if (maxResistance == null) {
            return 100;
        }
        return (1 - maxResistance) * 100;
    }

    /**
     * 计算SOH时间
     * 已使用时间最大值5年
     * SOH_时间 = (1 - 已使用时间（年） / 设计寿命) × 100%
     */
    private double getSohTime(BatteryPack batteryInfo) {
        // 计算差距多少年，四舍五入
        LocalDate currentDate = LocalDate.now();
        LocalDate pastLocalDate = getProductionTime(batteryInfo).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        long months = ChronoUnit.MONTHS.between(pastLocalDate, currentDate);
        // 四舍五入
        int roundedYears = (int) Math.round(months / 12.0);
        log.debug("差距年份: {}", roundedYears);
        roundedYears = Math.min(roundedYears, 5);
        return (1 - (double) roundedYears / 5) * 100;
    }

    /** 获取电池组的投产时间 */
    private Date getProductionTime(BatteryPack batteryInfo) {
        String productionTime = batteryInfo.getProductionTime();
        if (StringUtils.isEmpty(productionTime)) {
            if (batteryInfo.getCreateTime() == null) {
                return new Date();
            }
            return batteryInfo.getCreateTime();
        }
        return DateUtils.parseDate(productionTime);
    }

    /** 初始化 预测电池对象 */
    private static PreBatteryVo initPreBatteryVo(BatteryModuleCellRealtime bat, Double aCapacity,
                                                 Double startVoltage, Double endVoltage,
                                                 double bCapacity, Date staticTime) {
        PreBatteryVo vo = PreBatteryVo.getNewPreBatteryInfo();
        vo.setBatNum(bat.getBatNum());
        vo.setTemperature(bat.getTemperature());
        vo.setResistance(bat.getResistance());

        vo.setStartVoltage(StringUtils.formatToDouble(startVoltage, 3));
        vo.setEndVoltage(StringUtils.formatToDouble(endVoltage, 3));

        vo.setAcapacity(aCapacity);
        vo.setBcapacity(bCapacity);

        vo.setStaticTime(staticTime);
        return vo;
    }

    /** 获取规格 */
    private static double getSpec(BatteryPack batteryInfo) {
        double spec = 2.0;
        Integer sinModel = batteryInfo.getBatSinModel();
        // 12V电池
        if (sinModel == 8) {
            spec = 12.0;
        }
        return spec;
    }

    public double calculateDischargeSlope(List<DataPoint> dischargeData) {
        if (dischargeData == null || dischargeData.size() < 2) {
            throw new IllegalArgumentException("需要至少2个数据点");
        }
        SimpleRegression regression = new SimpleRegression();
        int i = 1;
        for (DataPoint point : dischargeData) {
            regression.addData(i, point.getVoltage());
            i++;
        }

        double slope = regression.getSlope();
        // 数据存在波动，可能存在正的斜率
        if(slope>=0){
            slope = -0.00001;
        }
        // 负斜率表示电压下降
        return slope;
    }

    /** 预测当前斜率下，下一个点的时间 */
    private int calcPrePointTime(double startVoltage, double endVoltage, double slope) {
        // 防止除零异常
        if (slope == 0) {
            throw new IllegalArgumentException("斜率不能为零");
        }
        // 计算逻辑 y = kx + b ，直接返回四舍五入结果
        return (int) Math.round((endVoltage - startVoltage) / slope);
    }


    /**
     * 折算容量
     * @param aCapacity 额定容量
     * @param crate 实际放电倍率
     * @param current 实际放低那电流
     * @param totalSecond 耗费总时间
     */
    private double preCapacity(double aCapacity, double crate,double current, int totalSecond) {
        if (totalSecond == -1) {
            return aCapacity;
        }

        // 边界条件检查
        if (totalSecond <= 0) {
            return aCapacity;
        }
        double bcapacity;
        bcapacity = (totalSecond * current * -1) / 3600;
        bcapacity = StringUtils.formatToDouble(bcapacity, 1);
        //转换到0.1C
        bcapacity = RateCapacityConverter.convertTo01C(bcapacity,crate);
        //四舍五入，取整数
        bcapacity = Math.round(bcapacity);
        // 剩余容量不能大于额定容量
        return Math.min(bcapacity, aCapacity);
    }

}
