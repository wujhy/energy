package com.shanhe.project.energy.capacity.service.impl;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.shanhe.common.constant.Constants;
import com.shanhe.common.utils.DateUtils;
import com.shanhe.common.utils.StringUtils;
import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.project.device.config.domain.BatteryMonitor;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.domain.BatteryReportLog;
import com.shanhe.project.device.config.service.BatteryReportLogService;
import com.shanhe.project.device.config.service.IBatteryPackService;
import com.shanhe.project.device.opt.domain.OptLog;
import com.shanhe.project.device.opt.service.OptLogService;
import com.shanhe.project.energy.capacity.service.BatteryPredictorService;
import com.shanhe.project.energy.capacity.service.DataPointService;
import com.shanhe.project.energy.capacity.service.PreBatteryGroupService;
import com.shanhe.project.energy.capacity.tool.FilterProcessor;
import com.shanhe.project.energy.capacity.tool.RateCapacityConverter;
import com.shanhe.project.energy.capacity.vo.DataPoint;
import com.shanhe.project.energy.capacity.vo.PreBatteryGroup;
import com.shanhe.project.energy.capacity.vo.PreBatteryVo;
import com.shanhe.project.energy.stat.service.IDevBatteryMonomerService;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 蓄电池预测服务类
 * @author xuxw
 */
@Service
public class BatteryPredictorServiceImpl implements BatteryPredictorService {

    protected static Logger logger = LoggerFactory.getLogger(BatteryPredictorServiceImpl.class);

    @Resource
    private DataPointService dataPointService;
    @Resource
    private IBatteryPackService devBatteryInfoService;
    @Resource
    private BatteryReportLogService batteryReportLogService;
    @Resource
    private PreBatteryGroupService preBatteryGroupService;
    @Resource
    private OptLogService optLogService;
    @Resource
    private IDevBatteryMonomerService devBatteryMonomerService;

    @Async
    @Override
    public void doTotalBatteryStep(Long configId, Integer packNum, String batteryStatus, BatteryReportLog oldInfo) {
        if (oldInfo == null) {
            return;
        }
        Map<String, Object> oldPackParam = oldInfo.getPackParam();
        if (oldPackParam == null) {
            return;
        }

        // 电池状态0：监控1：充电2：停电3：核容4：未连接5：备电6：空闲
        String batteryPackStatus = (String) oldPackParam.get("batteryPackStatus");
        if (!StrUtil.equals("5", batteryPackStatus)) {
            return;
        }

        // 如果状态未发生变化，则不需要处理
        if (StrUtil.equals(batteryPackStatus, batteryStatus)) {
            return;
        }

        OptLog optLog = optLogService.lastType(configId, packNum, BatteryTestEnum._5.getDictValue());
        if (optLog == null) {
            return;
        }

        // 放电结束，计算电池容量
        Date endTime = optLog.getUpdateTime();
        if (endTime == null) {
            endTime = new Date();
        }

        logger.error("放电结束，开始预估电池容量==========================");
        PreBatteryGroup preBatteryGroup = calcPredictorBatCapacity(configId, packNum, optLog.getCreateTime(), endTime);
        if (preBatteryGroup == null) {
            return;
        }

        // 更新操作日志
        optLogService.updateBatteryBcapacity(optLog.getId(), preBatteryGroup.getDischargeCapacity(), preBatteryGroup.getBcapacity(), preBatteryGroup.getCurrent(), preBatteryGroup.getEndTime());
    }

    private PreBatteryGroup calcPredictorBatCapacity(Long configId, Integer packNum, Date startTime, Date endTime) {
        int diffMills = DateUtils.differentMillsByMillisecond(startTime, endTime);
        // 30分钟
        if (diffMills < 30) {
            logger.error("放电测试低于30分钟，无法预估电池容量");
            return null;
        }
        // 电池基本信息
        BatteryPack batteryInfo = devBatteryInfoService.selectBatteryInfoByPackNum(configId, packNum);
        if (batteryInfo == null) {
            logger.error("未找到电池基本信息");
            return null;
        }
        // 查询所有单体数据
        BatteryReportLog packInfo = batteryReportLogService.lastCache(configId, packNum);
        if (packInfo == null) {
            logger.error("Redis找不到电池组实时数据!");
            return null;
        }
        // 电池组充放电电流
        Map<String, Object> packParam = packInfo.getPackParam();
        if (packParam == null) {
            return null;
        }


        List<BatteryMonitor> list = packInfo.getBatteryList();
        if (list == null || list.isEmpty()) {
            logger.error("Redis找不到电池组实时数据!");
            return null;
        }

        // 获取规格
        double spec = getSpec(batteryInfo);
        // 2V
        int specSize = 1;
        //12V电池
        if (spec == 12.0) {
            specSize = 6;
        }

        // 获取当前电池组的基础数据
        // 额定容量
        Double aCapacity = batteryInfo.getBatCapacity();
        // 电池组充放电电流,获取阶段内的平均电流
        Double current = dataPointService.getAvgCurrent(configId, packNum, startTime, endTime);
        if (current == null) {
            current = MapUtil.getDouble(packParam, "packCurrent");
        }
        logger.error("放电预估容量统计，电流：" + current);
        if (current == null || current == 0) {
            return null;
        }
        current = Math.round(current * 100.0) / 100.0;
        packParam.put("packCurrent", current);

        // 获取所有单体预估容量
        Map<String, PreBatteryVo> batteryVoMap = getPreBatteryVoMap(packInfo, current, startTime, endTime, aCapacity, specSize);
//
        // 获取电池组预估容量
        PreBatteryGroup groupVo = initPreBatteryGroupVo(batteryInfo, current, startTime, endTime, spec, batteryVoMap);
        preBatteryGroupService.insert(groupVo);
        return groupVo;
    }


    /**
     * 获取所有单体预估容量
     */
    private Map<String, PreBatteryVo> getPreBatteryVoMap(BatteryReportLog packInfo, Double current,
                                                         Date startTime, Date endTime,
                                                         Double aCapacity, int specSize) {
        // 单体预估容量
        Map<String, PreBatteryVo> result = new HashMap<>();
        // 放电倍率
        double crate = current / aCapacity;
        //格式化，保留2位小数
        crate = StringUtils.formatToDouble(crate, 2);
        List<DataPoint> dataPoints;
        // 放电开始
        DataPoint firstPoint;
        // 放电截止
        DataPoint lastPoint;
        // 电池组容量
        double bCapacity;
        Integer intervalTime = null;
        // 获取放电总时长（秒）
        int totalSecond = 0;
        // 单体预估电池容量
        PreBatteryVo vo;
        int totalSize = 0;
        double diffSlope;
        if(Math.abs(crate)>0.1){
            diffSlope = RateCapacityConverter.calculateSlopeRelationship(0.1,crate);
        }else{
            diffSlope = RateCapacityConverter.calculateSlopeRelationship(crate,0.1);
        }

        System.out.println("=====相差斜率========" + String.format("%.6f", Math.abs(diffSlope)));
        Date staticTime = new Date();

        for (BatteryMonitor bat : packInfo.getBatteryList()) {
            // 查找电池的放电数据
            dataPoints = dataPointService.findCurrentDataPoint(packInfo.getConfigId(), packInfo.getPackNum(), bat.getBatNum(), startTime, endTime);
            if (dataPoints == null || dataPoints.size() < 2) {
                logger.error("电池编号 {} 放电数据不足", bat.getBatNum());
                continue;
            }
            //对数据进行滤波处理
            dataPoints = FilterProcessor.movingAverageFilter(dataPoints, 2);
            firstPoint = dataPoints.get(0);
            lastPoint = dataPoints.get(dataPoints.size() - 1);
            if (lastPoint.getVoltage() > (2.1 * specSize)) {
                logger.error("电池编号 {} 放电截止电压 {} V 大于 {} V，不做预估！", bat.getBatNum(), lastPoint.getVoltage(), 2.1 * specSize);
                continue;
            }
            totalSize = dataPoints.size();
            //间隔时间
            if (intervalTime == null) {
                //相差多少秒
                intervalTime = DateUtils.differentSecondByMillisecond(startTime, endTime) / totalSize ;
            }

            //获取斜率,获取最近的斜率值，更加接近实际斜率
            double slope;
            if (lastPoint.getVoltage() >= (2 * specSize)) {
                slope = this.calculateDischargeSlope(dataPoints.subList(5, totalSize - 1));
            }else{
                slope = this.calculateDischargeSlope(dataPoints.subList(totalSize - 10, totalSize - 1));
            }
            System.out.println(lastPoint.getVoltage() + "=====斜率0=========" + String.format("%.5f", slope));

            int preTotalSize = 0;
            //分段处理,2V一个拐点
            if (lastPoint.getVoltage() >= (2 * specSize)) {
                //抛开前面快速下跌的节点
                int p1 = this.calcPrePointTime(lastPoint.getVoltage(), 2  * specSize, slope);
                if(specSize==1){
                    if (Math.abs(slope) < 0.00036) {
                        slope = -0.00036;
                        //斜率转换
                        slope = slope * diffSlope;
                    }
                }else if(specSize==6){
                    if (Math.abs(slope) < 0.0036) {
                        slope = -0.0036;
                        //斜率转换
                        slope = slope * diffSlope;
                    }
                }

                System.out.println(lastPoint.getVoltage() + "=====斜率1========" + String.format("%.6f", slope));
                int p2 = this.calcPrePointTime(2.0  * specSize, 1.88 * specSize, slope);
                int p3 = this.calcPrePointTime(1.88 * specSize, 1.8 * specSize, slope * 3);
                preTotalSize = p1 + p2 + p3;
                System.out.println("P1==" + p1);
                System.out.println("P2==" + p2);
                System.out.println("P3==" + p3);
            }else if (lastPoint.getVoltage() >= (1.88 * specSize)) { //1.88V一个拐点
                if(specSize==1){
                    if (Math.abs(slope) < 0.00036) {
                        slope = -0.00036;
                        //斜率转换
                        slope = slope * diffSlope;
                    }
                }else if(specSize==6){
                    if (Math.abs(slope) < 0.0036) {
                        slope = -0.0036;
                        //斜率转换
                        slope = slope * diffSlope;
                    }
                }
                System.out.println(lastPoint.getVoltage() + "=====斜率2========" + String.format("%.6f", slope));
                int p2 = this.calcPrePointTime(lastPoint.getVoltage(), 1.88 * specSize, slope);
                int p3 = this.calcPrePointTime(1.88 * specSize, 1.8 * specSize, slope * 3);
                preTotalSize = p2 + p3;
                System.out.println("P2==" + p2);
                System.out.println("P3==" + p3);
            } else if (lastPoint.getVoltage() >= (1.79 * specSize)) { //1.79结束
                if(specSize==1){
                    if (Math.abs(slope) < 0.00108) {
                        slope = -0.00108;
                        //斜率转换
                        slope = slope * diffSlope;
                    }
                }else if(specSize==6){
                    if (Math.abs(slope) < 0.0108) {
                        slope = -0.0108;
                        //斜率转换
                        slope = slope * diffSlope;
                    }
                }
                System.out.println(lastPoint.getVoltage() + "=====斜率3========" + String.format("%.6f", slope));
                int p3 = this.calcPrePointTime(lastPoint.getVoltage(), 1.8 * specSize, slope);
                preTotalSize = p3;
                System.out.println("P3==" + p3);
            }

            //因为到临界点，数据预测的点数越少，需要补偿不同点相同电压的情况
            if (lastPoint.getVoltage() <= (1.92 * specSize) && lastPoint.getVoltage() >= (1.86 * specSize)) {
                preTotalSize = (int) Math.round(preTotalSize * 1.05);
            }
            totalSecond = (totalSize * intervalTime) + (preTotalSize *60);
            bCapacity = this.preCapacity(aCapacity, crate,current, totalSecond);
            System.out.println(bat.getBatNum()+"=========="+lastPoint.getVoltage() + "======长度======" + totalSize + "======时间====" + ((double) totalSecond / 3600) + "小时" + "=====预估容量===" + bCapacity);
            // 组装对象
            vo = initPreBatteryVo(bat, aCapacity, firstPoint.getVoltage(), lastPoint.getVoltage(), bCapacity, staticTime);
            result.put(Constants.CAP_BAT + bat.getBatNum(), vo);
        }
        return result;
    }


    /**
     * 查找截止电压所在位置
     */
    public int getCuffVoltagePoint(List<DataPoint> dataPoints, double cuffVoltage) {
        int i = 0;
        for (DataPoint point : dataPoints) {
            if (point.getVoltage() <= cuffVoltage) {
                i++;
                break;
            } else {
                i++;
            }
        }
        return i;
    }

    /**
     * 初始化 预测电池组对象
     */
    private PreBatteryGroup initPreBatteryGroupVo(BatteryPack batteryInfo, Double current, Date startTime, Date endTime,
                                                  double spec, Map<String, PreBatteryVo> batteryVoMap) {
        PreBatteryGroup groupVo = preBatteryGroupService.lastCache(batteryInfo.getConfigId(), batteryInfo.getPackNum());
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

        groupVo.setBackUpDuration(0);
        groupVo.setMapBattery(batteryVoMap);
        groupVo.setMapBatteryData(JSON.toJSONString(batteryVoMap));

        //循环检查单体并设置单体的容量，有可能本轮测试时，电压未达到指定电压，没有预测值，需要拿上一次的预测值
        Map<String, PreBatteryVo> map2 = groupVo.getMapBattery();
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

    /**
     * 获取剩余时间
     */
    private int getDuration(Double current, Double groupCapacity) {
        double dt = (groupCapacity / current) * 60 * -1;
        return (int) Math.round(dt);
    }

    /**
     * 获取放电容量 = packInfo.getPackCurrent() * 放电时间（小时）
     */
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

    // 1、每次内阻测试后，计算变化率
    // 2、内阻变化率= ( (R_measured - R_initial) / R_initial ) × 100%
    // SOH内阻 ≈ 1 - 内阻变化率 / 2 * 100%
    // 实测内阻最小不能超过初始内阻，实测内阻最高只能为初始内阻的1倍
    private double getSohResistance(BatteryPack batteryInfo) {
        Double maxResistance = devBatteryMonomerService.getMaxResistance(batteryInfo.getConfigId(), batteryInfo.getPackNum());
        if (maxResistance == null) {
            return 100;
        }
        return (1 - maxResistance) * 100;
    }


    //
    // 已使用时间最大值5年
    // SOH_时间 = (1 - 已使用时间（年） / 设计寿命) × 100%
    private double getSohTime(BatteryPack batteryInfo) {
        // 计算差距多少年，四舍五入
        LocalDate currentDate = LocalDate.now();
        LocalDate pastLocalDate = getProductionTime(batteryInfo).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        long months = ChronoUnit.MONTHS.between(pastLocalDate, currentDate);
        // 四舍五入
        int roundedYears = (int) Math.round(months / 12.0);
        System.out.println("差距年份: " + roundedYears);
        roundedYears = Math.min(roundedYears, 5);
        return (1 - (double) roundedYears / 5) * 100;
    }

    /**
     * 获取电池组的投产时间
     */
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

    /**
     * 初始化 预测电池对象
     */
    private static PreBatteryVo initPreBatteryVo(BatteryMonitor bat, Double aCapacity,
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


    /**
     * 获取规格
     */
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
        if(slope>=0){ //数据存在波动，可能存在正的斜率
            slope = -0.00001;
        }
        return slope; // 负斜率表示电压下降
    }

    //预测当前斜率下，下一个点的时间
    private int calcPrePointTime(double startVoltage, double endVoltage, double slope) {
        // 防止除零异常
        if (slope == 0) {
            throw new IllegalArgumentException("slope cannot be zero");
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
        // 预估容量 / 额定容量 = 放电倍率 * 耗费总时间 / (0.1 * 10 * 3600)
//        bcapacity = (aCapacity * crate * totalSecond) / (0.1 * 10 * 3600);
//        bcapacity = Math.abs(bcapacity);
        bcapacity = StringUtils.formatToDouble(bcapacity, 1);
        //转换到0.1C
        bcapacity = RateCapacityConverter.convertTo01C(bcapacity,crate);
        //四舍五入，取整数
        bcapacity = Math.round(bcapacity);
        // 剩余容量不能大于额定容量
        return Math.min(bcapacity, aCapacity);
//        return bcapacity;
    }

}
