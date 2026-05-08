package com.shanhe.project.iot.battery;

import cn.hutool.core.util.StrUtil;
import com.shanhe.common.constant.Constants;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.framework.comm.tcp.model.DeviceData;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import com.shanhe.project.device.config.domain.BatteryMonitor;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.domain.BatteryReportLog;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.service.BatteryReportLogService;
import com.shanhe.project.device.config.service.IBatteryPackService;
import com.shanhe.project.device.config.service.IConfigService;
import com.shanhe.project.device.opt.service.OptLogService;
import com.shanhe.project.energy.capacity.service.BatteryPredictorService;
import com.shanhe.project.energy.capacity.service.PreBatteryGroupService;
import com.shanhe.project.energy.capacity.vo.PreBatteryGroup;
import com.shanhe.project.energy.capacity.vo.PreBatteryVo;
import com.shanhe.project.energy.stat.service.IStatBatteryPackService;
import com.shanhe.project.energy.stat.service.IStatBatteryResService;
import com.shanhe.project.iot.service.DataService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * 蓄电池数据处理类
 */
@Slf4j
@Service
public class BatteryPackHandler {
    protected static Logger logger = LoggerFactory.getLogger(BatteryPackHandler.class);

    @Resource
    private IBatteryPackService batteryPackService;
    @Resource
    private IConfigService configService;
    @Resource
    private BatteryReportLogService batteryReportLogService;
    @Resource
    private OptLogService optLogService;
    @Resource
    private IStatBatteryPackService statBatteryPackService;
    @Resource
    private IStatBatteryResService statBatteryResService;
    @Resource
    private BatteryPredictorService batteryPredictorService;
    @Resource
    private PreBatteryGroupService preBatteryGroupService;
    @Resource
    private DataService dataService;


    /**
     * 上传电池组实时数据
     *
     * @param config 设备信息
     * @param deviceData 上报信息
     */
    public void uploadBatteryPackData(Config config, DeviceData deviceData) {
        // 电池数据内容
        String info = deviceData.getInfo().substring(16, deviceData.getInfo().length() - 4);
        // 应答结果
        String binary = CodingUtil.hexString2binaryString(info.substring(0, 2));
        String res = String.valueOf(CodingUtil.binaryToDecimal(binary.substring(0, 1)));
        if (StrUtil.equals(res, "1")) {
            logger.error("上传蓄电池实时数据出错！ {}:{}", config.getConfigId(), deviceData.getInfo());
            return;
        }

        // 单体电池列表
        List<BatteryMonitor> batteryList = new ArrayList<>();
        //电池组型号
        String serviceType = String.valueOf(CodingUtil.binaryToDecimal(binary.substring(1, 5)));
        //电池组编号
        int packNum = CodingUtil.binaryToDecimal(binary.substring(5, 8));
        // 截去头尾完整指令
        String dataStr = deviceData.getInfo().substring(2, deviceData.getInfo().length() - 2);

        BatteryPack batteryPack = batteryPackService.selectBatteryInfoByPackNum(config.getConfigId(), packNum);
        if (batteryPack == null) {
            logger.error("上传蓄电池实时数据出错！未找到该电池组 {}:{}", config.getConfigId(), packNum);
            return;
        }

        // 解析电池组参数
        Map<String, Object> packMap = this.getBatteryPackInfo(info);
        if (packMap == null) {
            logger.error("上传蓄电池实时数据出错！电池组：{}，info={}", packNum, deviceData.getInfo());
            return;
        }

        // 解析单体电池
        int index82 = 124;
        switch (serviceType) {
            case "1":  //铅酸电池采集主机
            case "2":
            case "5":  //铅酸电池采集主机（带核容功能）
                break;
            case "3": {//3: 铅酸电池采集主机（带电池鼓包漏液检测功能）
                index82 = index82 + 14;
                // 单体电池个数
                int num = Integer.parseInt(dataStr.substring(12, 14), 16);
                // 所有单体电池INFO
                String batteryInfos = info.substring(index82, index82 + num * 13 * 2);
                // 解析单体电池
                this.getBatteryInfo3(num, batteryInfos, config.getConfigId(), packNum, batteryList);

                break;
            }
            case "4": { //大屏蓄电池主机

                String bcapacity = null;
                String backupDuration = null;


                // 从预估容量中获取容量和预估备电时长
                PreBatteryGroup preBatteryGroupVo = preBatteryGroupService.lastCache(config.getConfigId(), packNum);
                Map<String, PreBatteryVo> batteryVoMap = null;
                if (preBatteryGroupVo != null) {
                    if (null != preBatteryGroupVo.getBackUpDuration()) {
                        backupDuration = Double.toString(preBatteryGroupVo.getBackUpDuration());
                    }
                    if (null != preBatteryGroupVo.getBcapacity()) {
                        bcapacity = Double.toString(preBatteryGroupVo.getBcapacity());
                    }
                    batteryVoMap = preBatteryGroupVo.getMapBattery();
                }

                if (null == bcapacity) {
                    backupDuration = String.valueOf(CodingUtil.hexStringToInteger(info.substring(138, 142)));
                    bcapacity = CodingUtil.decimal(CodingUtil.hexStringToInteger(info.substring(142, 146)), "#.0", 10);
                }

                //电池组备电时长
                packMap.put("backupDuration", backupDuration);
                //电池组核容值
                packMap.put("bcapacity", bcapacity);
                //放电容量
                packMap.put("disChargeCapacity", CodingUtil.decimal(CodingUtil.hexStringToInteger(info.substring(146, 150)), "#.0", 10));
                //放电时长
                packMap.put("disChargeDuration", String.valueOf(CodingUtil.hexStringToInteger(info.substring(150, 154))));

                //电池组部分信息长度
                index82 = 89 * 2;
                // 单体电池个数
                int num = Integer.parseInt(CodingUtil.hexStringToString(dataStr.substring(12, 14)));
                int lastSize = index82 + num * 13 * 2;

                if (info.length() < lastSize) {
                    log.error("解析单体数据，info内容长度{}小于单体数据大小{}", info.length(), lastSize);
                    return;
                }
                //所有单体电池INFO
                String batteryInfos = info.substring(index82, lastSize);
                // 解析单体电池
                this.getBatteryInfo4(num, batteryInfos, config.getConfigId(), packNum, batteryList, batteryVoMap);

                break;
            }
            default: {
                index82 = index82 + 14;
                // 单体电池个数
                int num = Integer.parseInt(dataStr.substring(12, 14), 16);
                // 所有单体电池INFO
                String batteryInfos = info.substring(index82, index82 + num * 9 * 2);
                // 默认解析
                this.getBatteryInfoDefault(num, batteryInfos, config.getConfigId(), packNum, batteryList);
                break;
            }
        }

        if (batteryList.isEmpty()) {
            log.error("上传蓄电池实时数据出错，无单体数据！电池组：{}，info={}", packNum, deviceData.getInfo());
            return;
        }
        Date date = new Date();
        // 设备在线
        CacheUtils.put(String.format(CacheKeyEnum.CONFIG_ONLINE.getKey(), deviceData.getC0(), deviceData.getC1(), deviceData.getC2()), date);

        // 获取旧状态并确保类型安全
        String batteryPackStatus = (String) packMap.get("batteryPackStatus");


        BatteryReportLog oldInfo = null;
        try {
            oldInfo = batteryReportLogService.lastCache(config.getConfigId(), packNum);
            // 超过 5 分钟，不做联动
            if (null == oldInfo || null == oldInfo.getCreateTime() || System.currentTimeMillis() - oldInfo.getCreateTime().getTime() > 5 * 60 * 1000) {
                oldInfo = null;
            }

        } catch (Exception e) {
            log.error("获取电池组信息异常 imei {} 电池组编号 {} ", config.getConfigId(), packNum, e);
        }

        boolean isInsert = dataService.isInsert(config.getConfigId(), packNum + "", true);
        // 实时记录
        batteryReportLogService.insert(config.getConfigId(), packNum, packMap, batteryList, isInsert);

        try {
            // 统计电池过程
            batteryPredictorService.doTotalBatteryStep(config.getConfigId(), packNum, batteryPackStatus, oldInfo);
        } catch (Exception e) {
            log.error("统计电池过程异常 imei {} 电池组编号 {} ", config.getConfigId(), packNum, e);
        }

        // 保存蓄电池组状态
        try {
            optLogService.insertBattery(config.getConfigId(), packNum, packMap, oldInfo);
        } catch (Exception e) {
            log.error("保存操作日志异常 imei {} 电池组编号 {} ", config.getConfigId(), packNum, e);
        }

        // 数据迁移
        try {
            statBatteryPackService.insertList(config.getConfigId(), packNum, packMap, batteryList);
        } catch (Exception e) {
            log.error("数据迁移异常 imei {} 电池组编号 {} ", config.getConfigId(), packNum, e);
        }

        // 结束内阻测试，生成一次内阻值
        try {
            statBatteryResService.init(config.getConfigId(), packNum, packMap, batteryList, oldInfo);
        } catch (Exception e) {
            log.error("电池组内阻计算异常 imei {} 电池组编号 {} ", config.getConfigId(), packNum, e);
        }

        // 浮充结束，存储最后一次的电压极差值
        try {
            updateVoltageRange(batteryPack, packMap, batteryList, oldInfo);
        } catch (Exception e) {
            log.error("电池组电压极差计算异常 imei {} 电池组编号 {} ", config.getConfigId(), packNum, e);
        }
    }

    /**
     * 浮充结束更新电压极差
     */
    private void updateVoltageRange(BatteryPack batteryPack, Map<String, Object> packMap, List<BatteryMonitor> batteryList, BatteryReportLog oldInfo) {
        if (null == oldInfo) {
            return;
        }
        Map<String, Object> oldPackParam = oldInfo.getPackParam();
        if (oldPackParam == null) {
            return;
        }

        // 电池状态0：监控1：充电2：停电3：核容4：未连接5：备电6：空闲
        String batteryPackStatus = (String) oldPackParam.get("batteryPackStatus");
        if (!StrUtil.equals("6", batteryPackStatus)) {
            return;
        }

        // 内阻测试未完成
        String newBatteryPackStatus = (String) packMap.get("batteryPackStatus");
        // 如果状态未发生变化，则不需要处理
        if (StrUtil.equals(batteryPackStatus, newBatteryPackStatus)) {
            return;
        }

        if (batteryList == null || batteryList.isEmpty()) {
            return;
        }
        // 电压极差（mV） = 单体最高电压(V) - 单体最低电压(V)
        // 使用 summaryStatistics 避免两次流操作，同时处理可能的空流情况
        DoubleSummaryStatistics voltageStats = batteryList.stream()
                .mapToDouble(BatteryMonitor::getVoltage)
                .summaryStatistics();

        if (voltageStats.getCount() == 0) {
            return;
        }

        double maxVoltage = voltageStats.getMax();
        double minVoltage = voltageStats.getMin();

        // 单位转换 V 转换 mV，不保留小数点
        Integer voltageRange = (int) ((maxVoltage - minVoltage) * 1000);

        batteryPack.setVoltageRange(voltageRange);
        batteryPackService.update(batteryPack);
    }

    /**
     * 负数处理
     */
    private int negative(int num) {
        if (num >= 32768) {
            num = num - 65536;
        }
        return num;
    }

    /**
     * 电池组信息
     *
     * @param info 指令
     */
    private Map<String, Object> getBatteryPackInfo(String info) {
        if (info.length() < 120) {
            log.error("指令长度不足 120 {}", info);
            return null;
        }
        Map<String, Object> packMap = new HashMap<>();
        //电池组组压
        int packVoltage = this.negative(CodingUtil.hexStringToInteger(info.substring(2, 6)));
        packMap.put("packVoltage", CodingUtil.decimal(packVoltage, "#.0", 10));
        //电池组外组压
        int packOuterVoltage = this.negative(CodingUtil.hexStringToInteger(info.substring(6, 10)));
        packMap.put("batteryPackOuterVoltage", CodingUtil.decimal(packOuterVoltage, "#.0", 10));
        //电池组充放电电流
        int packCurrent = this.negative(CodingUtil.hexStringToInteger(info.substring(10, 14)));
        packMap.put("packCurrent", CodingUtil.decimal(packCurrent, "#.0", 10));
        //电池组浮充电流
        int batteryPackFloatCurrent = this.negative(CodingUtil.hexStringToInteger(info.substring(14, 18)));
        packMap.put("batteryPackFloatCurrent", CodingUtil.decimal(batteryPackFloatCurrent, "#.000", 1000));
        //环境温度1
        int et1 = this.negative(CodingUtil.hexStringToInteger(info.substring(18, 22)));
        packMap.put("environmentTemperature1", CodingUtil.decimal(et1, "#.0", 10));
        //环境温度2
        int et2 = this.negative(CodingUtil.hexStringToInteger(info.substring(22, 26)));
        packMap.put("environmentTemperature2", CodingUtil.decimal(et2, "#.0", 10));

        // 组外压、组电流、组压、温度
        boolean validPack = Objects.equals(packOuterVoltage, 0)
                && Objects.equals(packCurrent, 0) && Objects.equals(packVoltage, 0)
                && (Objects.equals(et1, 0) || Objects.equals(et1, -150));
        if (validPack) {
            log.error("电池组数据无效（组外压、组电压、组电流、环境温度），不进行存储");
            return null;
        }

        //最高电压电池号
        packMap.put("maxVoltageBatteryNumber", CodingUtil.hexStringToString(info.substring(26, 28)));
        //最高电池电压值
        packMap.put("batteryMaxVoltage", CodingUtil.decimal(CodingUtil.hexStringToInteger(info.substring(28, 32)), "#.000", 1000));
        //最低电压电池号
        packMap.put("minVoltageBatteryNumber", CodingUtil.hexStringToString(info.substring(32, 34)));
        //最低电池电压值
        packMap.put("batteryMinVoltage", CodingUtil.decimal(CodingUtil.hexStringToInteger(info.substring(34, 38)), "#.000", 1000));
        //电池平均单体电压
        packMap.put("batteryAvgVoltage", CodingUtil.decimal(CodingUtil.hexStringToInteger(info.substring(38, 42)), "#.000", 1000));
        //电池电压均差值单位
        packMap.put("batteryVoltageDeviation", CodingUtil.decimal(CodingUtil.hexStringToInteger(info.substring(42, 46)), "#.000", 1000));
        //电池电压极差值
        packMap.put("batteryVoltageRange", CodingUtil.decimal(CodingUtil.hexStringToInteger(info.substring(46, 50)), "#.000", 1000));
        //最高内阻电池号
        packMap.put("maxResistanceBatteryNumber", CodingUtil.hexStringToString(info.substring(50, 52)));
        //最高电池内阻值
        packMap.put("batteryMaxResistance", String.valueOf(CodingUtil.hexStringToInteger(info.substring(52, 56))));
        //最低内阻电池号
        packMap.put("minResistanceBatteryNumber", CodingUtil.hexStringToString(info.substring(56, 58)));
        //最低电池内阻值
        packMap.put("batteryMinEsistance", String.valueOf(CodingUtil.hexStringToInteger(info.substring(58, 62))));
        //平均电池内阻值
        packMap.put("batteryAvgResistance", String.valueOf(CodingUtil.hexStringToInteger(info.substring(62, 66))));
        //最高温度电池号
        packMap.put("maxTemperatureBatteryNumber", CodingUtil.hexStringToString(info.substring(66, 68)));
        //最高电池温度值
        int bMaxT = this.negative(CodingUtil.hexStringToInteger(info.substring(68, 72)));
        packMap.put("batteryMaxTemperature", CodingUtil.decimal(bMaxT, "#.0", 10));
        //最低温度电池号
        packMap.put("minTemperatureBatteryNumber", CodingUtil.hexStringToString(info.substring(72, 74)));
        //最低电池温度值
        int bMinT = this.negative(CodingUtil.hexStringToInteger(info.substring(74, 78)));
        packMap.put("batteryMinTemperature", CodingUtil.decimal(bMinT, "#.0", 10));
        //平均电池温度值
        int bAvgT = this.negative(CodingUtil.hexStringToInteger(info.substring(78, 82)));
        packMap.put("batteryAvgTemperature", CodingUtil.decimal(bAvgT, "#.0", 10));
        //组SOC
        packMap.put("batteryPackSoc", CodingUtil.decimal(CodingUtil.hexStringToInteger(info.substring(82, 86)), "#.0", 10));
        //组SOH
        packMap.put("batteryPackSoh", CodingUtil.decimal(CodingUtil.hexStringToInteger(info.substring(86, 90)), "#.0", 10));
        //剩余放电时长
        packMap.put("residualDischargeDuration", String.valueOf(CodingUtil.hexStringToInteger(info.substring(90, 94))));
        //纹波电压
        packMap.put("rippleVoltage", CodingUtil.decimal(CodingUtil.hexStringToInteger(info.substring(94, 98)), "#.000", 1000));
        //氢气浓度
        packMap.put("hydrogenConcentration", CodingUtil.decimal(CodingUtil.hexStringToInteger(info.substring(98, 102)), "#.0", 10));
        //绝缘正电阻
        packMap.put("positiveinsulationResistance", String.valueOf(CodingUtil.hexStringToInteger(info.substring(102, 106))));
        //绝缘负电阻
        packMap.put("negativeinsulationResistance", String.valueOf(CodingUtil.hexStringToInteger(info.substring(106, 110))));
        //接地电池号上限
        packMap.put("groundingBatteryUpperLimit", String.valueOf(CodingUtil.hexStringToInteger(info.substring(110, 112))));
        //接地电池号下限
        packMap.put("groundingBatteryLowerLimit", String.valueOf(CodingUtil.hexStringToInteger(info.substring(112, 114))));
        //内阻最大变化率电池号
        packMap.put("maxResistanceRateChangeBatteryNumber", String.valueOf(CodingUtil.hexStringToInteger(info.substring(114, 116))));
        //内阻最大变化率值
        packMap.put("maxResistanceRateChange", CodingUtil.decimal(CodingUtil.hexStringToInteger(info.substring(116, 120)), "#.00", 100));
        /*电池组状态寄存器*/
        String binary2 = CodingUtil.hexString2binaryString(info.substring(120, 122));
        //设备工作状态
        packMap.put("deviceWorkStatus", String.valueOf(CodingUtil.binaryToDecimal(binary2.substring(0, 3))));
        //设备工作IO状态
        packMap.put("deviceWorkIOStatus", String.valueOf(CodingUtil.valueOfInteger(binary2.substring(3, 4))));
        //电池状态字
        packMap.put("batteryPackStatus", String.valueOf(CodingUtil.binaryToDecimal(binary2.substring(4))));
        //电池容量测试标志位：0表示不在内阻测试、1表示通讯故障、2表示浮充电流异常、4表示放电电流异常、6表示正在内阻测试、7表示内阻测试正常结束、8表示内阻测试异常结束、9表示回路电压异常
        packMap.put("resistanceTestStatus", String.valueOf(CodingUtil.hexStringToInteger(info.substring(122, 124))));

        // 放电
        if (packCurrent < 0) {
            packMap.put("batteryPackStatus", "5");
        }
        return packMap;
    }

    /**
     * 解析单体电池（铅酸电池采集主机）
     *
     * @param num 个数
     * @param batteryInfo 内容
     * @param configId 设备id
     * @param packNum 组编码
     * @param batteryList 电池列表
     */
    private void getBatteryInfo3(int num, String batteryInfo, Long configId, Integer packNum, List<BatteryMonitor> batteryList) {
        // 起始位置
        int point;
        for (int i = 0; i < num; i++) {
            point = i * 26;
            BatteryMonitor battery = new BatteryMonitor();
            battery.setConfigId(configId);
            battery.setPackNum(packNum);

            //单体电池编号
            battery.setBatNum(CodingUtil.hexStringToInteger(batteryInfo.substring(point, point + 2)));

            //单体电压值
            int voltage = CodingUtil.hexStringToInteger(batteryInfo.substring(point + 2, point + 6));
            battery.setVoltage(CodingUtil.decimalDouble(voltage, 1000));

            //单体内阻值
            battery.setResistance(CodingUtil.hexStringToInteger(batteryInfo.substring(point + 6, point + 10)));

            //单体温度值
            int temperature = CodingUtil.hexStringToInteger(batteryInfo.substring(point + 10, point + 14));
            battery.setTemperature(CodingUtil.decimalDouble(this.negative(temperature), 10));

            //电池内阻变化率
            int resistanceRateChange = CodingUtil.hexStringToInteger(batteryInfo.substring(point + 14, point + 18));
            battery.setResistanceRateChange(CodingUtil.decimalDouble(resistanceRateChange, 100));

            //电池连接条电阻
            battery.setResistancerageslip((double) CodingUtil.hexStringToInteger(batteryInfo.substring(point + 18, point + 22)));

            //电池鼓包电压值
            int gbvoltage = CodingUtil.hexStringToInteger(batteryInfo.substring(point + 22, point + 26));
            battery.setGbvoltage(CodingUtil.decimalDouble(gbvoltage, 10));

            batteryList.add(battery);
        }
    }

    /**
     * 解析单体电池（大屏蓄电池主机）
     *
     * @param num 个数
     * @param batteryInfo 内容
     * @param configId 设备id
     * @param packNum 组编码
     * @param batteryList 电池列表
     */
    private void getBatteryInfo4(int num, String batteryInfo, Long configId, Integer packNum, List<BatteryMonitor> batteryList,
                                 Map<String, PreBatteryVo> batteryVoMap) {
        // 起始位置
        int point;
        for (int i = 0; i < num; i++) {
            point = i * 26;
            BatteryMonitor battery = new BatteryMonitor();
            battery.setConfigId(configId);
            battery.setPackNum(packNum);
            //单体电池编号
            battery.setBatNum(CodingUtil.hexStringToInteger(batteryInfo.substring(point, point + 2)));

            // 电池核容值
            Double bcapacity = null;
            if (batteryVoMap != null) {
                PreBatteryVo bVo = batteryVoMap.get(Constants.CAP_BAT + battery.getBatNum());
                if (bVo != null) {
                    bcapacity = bVo.getBcapacity();
                }
            }
            if (bcapacity == null) {
                int bcapacityInt = CodingUtil.hexStringToInteger(batteryInfo.substring(point + 14, point + 18));
                bcapacity = CodingUtil.decimalDouble(bcapacityInt, 10);
            }
            battery.setBcapacity(bcapacity);

            //单体电压值
            int voltage = CodingUtil.hexStringToInteger(batteryInfo.substring(point + 2, point + 6));
            battery.setVoltage(CodingUtil.decimalDouble(voltage, 1000));

            //单体内阻值
            battery.setResistance(CodingUtil.hexStringToInteger(batteryInfo.substring(point + 6, point + 10)));

            //单体温度值
            int temperature = CodingUtil.hexStringToInteger(batteryInfo.substring(point + 10, point + 14));
            battery.setTemperature(CodingUtil.decimalDouble(this.negative(temperature), 10));


            //电池连接条电阻
            battery.setResistancerageslip((double) CodingUtil.hexStringToInteger(batteryInfo.substring(point + 18, point + 22)));

            //电池鼓包电压值
            int gbvoltage = CodingUtil.hexStringToInteger(batteryInfo.substring(point + 22, point + 26));
            battery.setGbvoltage(CodingUtil.decimalDouble(gbvoltage, 10));

            batteryList.add(battery);
        }
    }

    /**
     * 解析单体电池（默认）
     *
     * @param num 个数
     * @param batteryInfo 内容
     * @param configId 设备id
     * @param packNum 组编码
     * @param batteryList 电池列表
     */
    private void getBatteryInfoDefault(int num, String batteryInfo, Long configId, Integer packNum, List<BatteryMonitor> batteryList) {
        // 起始位置
        int point;
        for (int i = 0; i < num; i++) {
            point = i * 18;
            // 组装电池信息
            BatteryMonitor battery = new BatteryMonitor();
            battery.setConfigId(configId);
            battery.setPackNum(packNum);

            //单体电池编号
            battery.setBatNum(CodingUtil.hexStringToInteger(batteryInfo.substring(point, point + 2)));

            //单体电压值
            int voltage = Integer.parseInt(CodingUtil.hexStringToString(batteryInfo.substring(point + 2, point + 6)));
            battery.setVoltage(CodingUtil.decimalDouble(voltage, 1000));

            //单体内阻值
            battery.setResistance(Integer.parseInt(CodingUtil.hexStringToString(batteryInfo.substring(point + 6, point + 10))));

            //单体温度值
            int temperature = Integer.parseInt(CodingUtil.hexStringToString(batteryInfo.substring(point + 10, point + 14)));
            battery.setTemperature(CodingUtil.decimalDouble(this.negative(temperature), 10));

            //电池内阻变化率
            int resistanceRateChange = Integer.parseInt(CodingUtil.hexStringToString(batteryInfo.substring(point + 14, point + 18)));
            battery.setResistanceRateChange(CodingUtil.decimalDouble(resistanceRateChange, 100));

            batteryList.add(battery);
        }
    }

    /**
     * 上传电池组注册
     */
    public void uploadBatterPack(Config config, DeviceData deviceData) {
        // 指令信息
        String info = deviceData.getInfo().substring(16, deviceData.getInfo().length() - 4);
        // 应答结果
        String binary86 = CodingUtil.hexString2binaryString(info.substring(0, 2));
        String res = String.valueOf(CodingUtil.binaryToDecimal(binary86.substring(0, 4)));
        if (StrUtil.equals(res, "1")) {
            log.error("86 上传电池组配置信息出错！info={}", deviceData.getInfo());
            return;
        }

        // 电池组数量
        int batteryPackNum = CodingUtil.binaryToDecimal(binary86.substring(4, 8));
        List<BatteryPack> packList = new ArrayList<>(batteryPackNum);
        for (int i = 0; i < batteryPackNum; i++) {
            int index86 = 2 + i * 10;
            String bpInfo86 = info.substring(index86, index86 + 10);
            // 电池组节数
            int batterySum = Integer.parseInt(CodingUtil.hexStringToString(bpInfo86.substring(4, 6)));
            if (batterySum == 0) {
                continue;
            }
            //电池组编号
            Integer packNum = Integer.parseInt(CodingUtil.hexStringToString(bpInfo86.substring(0, 2)));
            //规格
            Integer batterySpecs = Integer.parseInt(CodingUtil.hexStringToString(bpInfo86.substring(2, 4)));
            //电池组容量
            Double batteryPackCapacity = CodingUtil.valueOfDouble(String.valueOf(Integer.parseInt(CodingUtil.hexStringToString(bpInfo86.substring(6, 10)))));

            // 判断电池组是否存在
            BatteryPack batteryInfo = batteryPackService.selectBatteryInfoByPackNum(config.getConfigId(), packNum);
            if (batteryInfo == null) {
                batteryInfo = new BatteryPack();
                batteryInfo.setConfigId(config.getConfigId());
                batteryInfo.setPackNum(packNum);
                batteryInfo.setIsEnabled(YesNoEnum.YES.getDictValue());
                batteryInfo.setIsAllowPower(YesNoEnum.YES.getDictValue());
                batteryInfo.setIsShowConnect(YesNoEnum.YES.getDictValue());
            }

            batteryInfo.setBatSinSize(batterySum);
            batteryInfo.setBatSinModel(batterySpecs);
            batteryInfo.setBatCapacity(batteryPackCapacity);
            packList.add(batteryInfo);
        }

        // 更新设备电池组包
        if (!packList.isEmpty()) {
            config.setPackList(packList);
            configService.updatePack(config);
        }
    }
}
