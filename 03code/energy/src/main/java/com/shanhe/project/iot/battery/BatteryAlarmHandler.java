package com.shanhe.project.iot.battery;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.shanhe.framework.enums.AlarmLevelEnum;
import com.shanhe.framework.enums.ItemCode;
import com.shanhe.framework.comm.tcp.model.DeviceData;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import com.shanhe.project.device.alarm.service.IAlarmLogService;
import com.shanhe.project.device.config.domain.BatteryReportLog;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.service.BatteryReportLogService;
import com.shanhe.project.iot.model.BatteryWarnInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * 电池报警记录数据
 */
@Service
public class BatteryAlarmHandler {

    protected static Logger logger = LoggerFactory.getLogger(BatteryAlarmHandler.class);

    @Resource
    private IAlarmLogService alarmLogService;
    @Resource
    private BatteryReportLogService batteryReportLogService;


    /** 单体电池故障属性 **/
    private static final List<String> BATTERY_FAULT_CODE = Arrays.asList(
            ItemCode.DTLJTGJ.getCode(),
            ItemCode.DTGB.getCode(),
            ItemCode.DTLYGJ.getCode(),
            ItemCode.DTWDCGQGZ.getCode(),
            ItemCode.DTTXZT.getCode());
    /** 单体电池告警属性 **/
    private static final List<String> BATTERY_WARN_CODE = Arrays.asList(
            ItemCode.DTDCWDD.getCode(),
            ItemCode.DTDCWDG.getCode(),
            ItemCode.DTNZGX.getCode(),
            ItemCode.DTNZGD.getCode(),
            ItemCode.DTDYGF.getCode(),
            ItemCode.DTDYGC.getCode(),
            ItemCode.DTLJTGJ.getCode(),
            ItemCode.DTDCKL.getCode(),
            ItemCode.DTFCDYD.getCode(),
            ItemCode.DTFCDYG.getCode(),
            ItemCode.DTNZBJ.getCode(),
            ItemCode.DTDCWDBJ.getCode(),
            ItemCode.DTDYBJ.getCode());

    /**
     * 上传蓄电池报警数据
     *
     * @param config 设备配置
     * @param deviceData 告警数据
     */
    public void uploadBatteryWarnData(Config config, DeviceData deviceData) {
        BatteryWarnInfo warnInfo = toWarnDecoder(deviceData.getInfo());
        //响应结果
        if (warnInfo==null) {
            logger.error("上传蓄电池报警数据出错！info={}", deviceData.getInfo());
            return;
        }

        // 电池组报警状态
        JSONObject packStatus = warnInfo.getPackStatus();
        if(packStatus==null) {
            return;
        }
        // 一般告警内容
        JSONObject commonly = packStatus.getJSONObject("commonly");
        if(commonly == null){
            return;
        }
        JSONObject abnormal = packStatus.getJSONObject("abnormal");
        JSONObject serious = packStatus.getJSONObject("serious");
        //一般报警
        String commonlyStatus = commonly.getString("status1").substring(2) + commonly.getString("status2");
        //异常报警
        String abnormalStatus = abnormal.getString("status1").substring(2) + abnormal.getString("status2");
        //严重报警
        String seriousStatus = serious.getString("status1").substring(2) + serious.getString("status2");

        // 最新电池组上报记录
        BatteryReportLog batteryReportLog = batteryReportLogService.lastCache(config.getConfigId(), warnInfo.getBatteryPackNumber());

        // 电池组告警参数
        Map<String, String> warnParam = new HashMap<>(14);
        this.dealPackWarnParam(seriousStatus, AlarmLevelEnum._1.getDictValue(), warnParam);
        this.dealPackWarnParam(abnormalStatus, AlarmLevelEnum._1.getDictValue(), warnParam);
        this.dealPackWarnParam(commonlyStatus, AlarmLevelEnum._1.getDictValue(), warnParam);
        if (!warnParam.isEmpty()) {
            alarmLogService.alarmBattery(config, warnInfo.getBatteryPackNumber(), null, warnParam, batteryReportLog);
        }

        //处理单体电池信息 获取单体电池报警个数
        JSONArray packBatteryStatus = warnInfo.getPackBatteryStatus();

        // 排除单体电池序号
        List<Integer> excludeModelNum = new ArrayList<>();

        // 如果单体电池未告警，则清除改包下所有告警记录
        if (packBatteryStatus == null || packBatteryStatus.isEmpty()) {
            alarmLogService.alarmFix(config.getConfigId(), warnInfo.getBatteryPackNumber(), true, excludeModelNum, BATTERY_WARN_CODE);
            return;
        }
        int alarmSum = packBatteryStatus.size();
        JSONObject batteryStatus;
        String seriousStatus1;
        String abnormalStatus1;
        String commonlyStatus1;
        for (int i = 0; i < alarmSum; i++) {
            batteryStatus = packBatteryStatus.getJSONObject(i);
            if (batteryStatus == null) {
                continue;
            }
            //严重报警
            seriousStatus1 = batteryStatus.getJSONObject("serious").getString("status1").substring(2) + batteryStatus.getJSONObject("serious").getString("status2");
            //异常报警
            abnormalStatus1 = batteryStatus.getJSONObject("abnormal").getString("status1").substring(2) + batteryStatus.getJSONObject("abnormal").getString("status2");
            //一般报警
            commonlyStatus1 = batteryStatus.getJSONObject("commonly").getString("status1").substring(2) + batteryStatus.getJSONObject("commonly").getString("status2");
            Integer batteryNumber = batteryStatus.getInteger("batteryNumber");

            // 单体电池告警参数
            Map<String, String> warnParam1 = new HashMap<>(14);
            this.dealSingleWarnParam(seriousStatus1, AlarmLevelEnum._1.getDictValue(), warnParam1);
            this.dealSingleWarnParam(abnormalStatus1, AlarmLevelEnum._1.getDictValue(), warnParam1);
            this.dealSingleWarnParam(commonlyStatus1, AlarmLevelEnum._1.getDictValue(), warnParam1);
            if (!warnParam1.isEmpty()) {
                alarmLogService.alarmBattery(config, warnInfo.getBatteryPackNumber(), batteryNumber, warnParam1, batteryReportLog);
            }
            excludeModelNum.add(batteryNumber);
        }

        // 修复平台正在报警，但是设备没有报警的单体记录
        alarmLogService.alarmFix(config.getConfigId(), warnInfo.getBatteryPackNumber(), true, excludeModelNum, BATTERY_WARN_CODE);
    }

    /**
     * 解码报警状态信息
     *
     * @param dataInfo 指令信息
     */
    public static BatteryWarnInfo toWarnDecoder(String dataInfo) {
        String info = dataInfo.substring(16, dataInfo.length() - 4);
        String binary87 = CodingUtil.hexString2binaryString(info.substring(0, 2));
        //应答结果
        String res = String.valueOf(CodingUtil.binaryToDecimal(binary87.substring(0, 4)));
        if(StrUtil.equals(res, "1")) {
            return null;
        }
        //验证数据长度，实际上需要验证数据的有效性
        if(info.length()<16){
            return null;
        }

        BatteryWarnInfo batteryWarnInfo = new BatteryWarnInfo();
        //电池组编号
        batteryWarnInfo.setBatteryPackNumber(CodingUtil.binaryToDecimal(binary87.substring(4, 8)));
        //单体电池报警数量
        batteryWarnInfo.setAlarmBatterySum(CodingUtil.hexStringToInteger(info.substring(2, 4)));
        int point;
        // 组状态
        JSONObject packStatus = new JSONObject();
        String psInfos = info.substring(4, 16);
        for (int i = 0; i < 3; i++) {
            point = i * 4;
            JSONObject ps = new JSONObject();
            ps.put("status1", CodingUtil.hexString2binaryString(psInfos.substring(point, point + 2)));
            ps.put("status2", CodingUtil.hexString2binaryString(psInfos.substring(point + 2, point + 4)));

            // 告警类型
            int status = CodingUtil.binaryToDecimal(ps.getString("status1").substring(0, 2));
            if (status == 1) {
                packStatus.put("commonly", ps);  //一般告警
            } else if (status == 2) {
                packStatus.put("abnormal", ps);  //异常报警
            } else if (status == 3) {
                packStatus.put("serious", ps);  //严重报警
            }
        }
        batteryWarnInfo.setPackStatus(packStatus);

        // 单体电池状态
        JSONArray packBatteryStatus = new JSONArray();
        for (int i = 0; i < batteryWarnInfo.getAlarmBatterySum(); i++) {
            JSONObject batteryStatusOjb = new JSONObject();
            String pbsInfos = info.substring(16 + i * 18, 16 + (i + 1) * 18);
            for (int j = 0; j < 3; j++) {
                point = j * 6;
                JSONObject ps = new JSONObject();
                batteryStatusOjb.put("batteryNumber", CodingUtil.hexStringToInteger(pbsInfos.substring(point, point + 2)));
                ps.put("status1", CodingUtil.hexString2binaryString(pbsInfos.substring(point + 2, point + 4)));
                ps.put("status2", CodingUtil.hexString2binaryString(pbsInfos.substring(point + 4, point + 6)));

                // 告警类型
                int status = CodingUtil.binaryToDecimal(ps.getString("status1").substring(0, 2));
                if (status == 1) {
                    batteryStatusOjb.put("commonly", ps);  //一般告警
                } else if (status == 2) {
                    batteryStatusOjb.put("abnormal", ps);  //异常报警
                } else if (status == 3) {
                    batteryStatusOjb.put("serious", ps);  //严重报警
                }
            }
            packBatteryStatus.add(batteryStatusOjb);
        }
        batteryWarnInfo.setPackBatteryStatus(packBatteryStatus);
        return batteryWarnInfo;
    }

    /**
     * 解析组告警参数
     *
     * @param status 告警字段
     * @param level 告警等级
     * @param warnParam 告警参数
     */
    private void dealPackWarnParam(String status, String level, Map<String, String> warnParam) {
        if (StrUtil.isBlank(status)) {
            return;
        }
        //status 字节低6位表示报警状态：0:表示正常/1:表示报警 格式14位（去掉了2位）是：00000000000000
        //bit5:为环境温度低               bit4:为环境温度高
        //bit3:为放电电流大               bit2:为充电电流大
        //bit1:为组压低                   bit0:为组压高
        //第2字节 0:表示正常/1:表示报警
        //bit7:为蓄电池脱离母线            bit6:为氢气浓度过高
        //bit5:为电池热失控               bit4:为SOH低
        //bit3:为SOC低                   bit2:为压差过大
        //bit1:为浮充组压低                bit0:为浮充组压高

        // 环境温度低
        this.setLevel(status, 0, level, ItemCode.ZWDD.getCode(), warnParam);
        // 环境温度高
        this.setLevel(status, 1, level, ItemCode.ZWDG.getCode(), warnParam);
        // 充过电流
        this.setLevel(status, 3, level, ItemCode.ZCGDLGJ.getCode(), warnParam);
        // 组压低
        this.setLevel(status, 4, level, ItemCode.ZDYGF.getCode(), warnParam);
        // 组压高
        this.setLevel(status, 5, level, ItemCode.ZDYGC.getCode(), warnParam);
        // 连接条异常
        this.setLevel(status, 6, level, ItemCode.ZTLMXGJ.getCode(), warnParam);
        // SOH低
        this.setLevel(status, 9, level, ItemCode.ZSOHDGJ.getCode(), warnParam);
        // SOH低
        this.setLevel(status, 10, level, ItemCode.ZSOCDGJ.getCode(), warnParam);
        // 浮充过低
        this.setLevel(status, 12, level, ItemCode.ZFCDYGD.getCode(), warnParam);
        // 浮充组压高
        this.setLevel(status, 13, level, ItemCode.ZFCDYGG.getCode(), warnParam);
    }

    /**
     * 解析单体电池告警参数
     *
     * @param status 告警字段
     * @param level 告警等级
     * @param warnParam 告警参数
     */
    private void dealSingleWarnParam(String status, String level, Map<String, String> warnParam) {
        //status 字节低6位表示报警状态：0:表示正常/1:表示报警 格式14位（去掉了2位）是：00000000000000
        //字节低6位表示报警状态：0:表示正常/1:表示报警
        //bit5:为单体温度低               bit4:为单体温度高
        //bit3:为单体内阻低               bit2:为单体内阻高
        //bit1:为单体电压低               bit0:为单体电压高
        //第3字节 0:表示正常/1:表示报警
        //bit7:无数据                    bit6:为电池接地告警
        //bit5:为电池开路                 bit4:为浮充电压低
        //bit3:为浮充电压高               bit2:为单体内阻不均
        //bit1:为单体温度不均              bit0:为单体电压不均
        // 单体温度低
        this.setLevel(status, 0, level, ItemCode.DTDCWDD.getCode(), warnParam);
        // 单体温度高
        this.setLevel(status, 1, level, ItemCode.DTDCWDG.getCode(), warnParam);
        // 单体内阻低
        this.setLevel(status, 2, level, ItemCode.DTNZGX.getCode(), warnParam);
        // 单体内阻高
        this.setLevel(status, 3, level, ItemCode.DTNZGD.getCode(), warnParam);
        // 单体电压低
        this.setLevel(status, 4, level, ItemCode.DTDYGF.getCode(), warnParam);
        // 单体电压低
        this.setLevel(status, 5, level, ItemCode.DTDYGC.getCode(), warnParam);
        // 电池接地告警
        this.setLevel(status, 7, level, ItemCode.DTLJTGJ.getCode(), warnParam);
        // 电池开路
        this.setLevel(status, 8, level, ItemCode.DTDCKL.getCode(), warnParam);
        // 浮充电压低
        this.setLevel(status, 9, level, ItemCode.DTFCDYD.getCode(), warnParam);
        // 浮充电压高
        this.setLevel(status, 10, level, ItemCode.DTFCDYG.getCode(), warnParam);
        // 单体内阻不均
        this.setLevel(status, 11, level, ItemCode.DTNZBJ.getCode(), warnParam);
        // 单体温度不均
        this.setLevel(status, 12, level, ItemCode.DTDCWDBJ.getCode(), warnParam);
        // 单体电压不均
        this.setLevel(status, 13, level, ItemCode.DTDYBJ.getCode(), warnParam);
    }


    /**
     * 设置属性等级
     *
     * @param status 指令状态
     * @param num 序号
     * @param level 等级
     * @param code 属性编码
     * @param warnParam 告警参数
     */
    private void setLevel(String status, int num, String level, String code, Map<String, String> warnParam) {
        String paramLevel = warnParam.get(code);
        // 有值则不更改
        if (paramLevel != null && !StrUtil.equals(paramLevel, "0")) {
            return;
        }
        // 长度不足直接补0 不告警
        if (StrUtil.isBlank(status) || status.length() < num) {
            warnParam.put(code, "0");
            return;
        }
        // 如果指定位置为0则不告警
        warnParam.put(code, status.charAt(num) == '0' ? "0" : level);
    }

    /**
     * 处理设备故障数据
     *
     * @param config 设备
     * @param deviceData 指令
     */
    public void deviceFaultAlarmUpload(Config config, DeviceData deviceData) {
        BatteryWarnInfo batteryWarnInfo = toFailDecoder(deviceData.getInfo());
        //响应结果
        if (batteryWarnInfo == null) {
            logger.error("上传蓄电池故障报警数据出错！info={}", deviceData.getInfo());
            return;
        }

        // 最新电池组上报记录
        BatteryReportLog batteryReportLog = batteryReportLogService.lastCache(config.getConfigId(), batteryWarnInfo.getBatteryPackNumber());

        /*电池组故障状态*/
        String dfs = batteryWarnInfo.getDeviceFaultStatus();
        if(dfs != null) {
            //0:表示正常/1:表示报警
            //bit7:为网络故障                  bit6:为氢气传感器故障
            //bit5:为氢气模块通信异常/停电故障    bit4:为环境温度传感器2故障
            //bit3:为环境温度传感器1故障         bit2:为组压模块通信异常
            //bit1:为电流温度监测模块通信异常     bit0:为热失控模块通信异常
            Map<String, String> warnParam = new HashMap<>(5);
            // 网络故障
            warnParam.put(ItemCode.ZWLGZ.getCode(), String.valueOf(dfs.charAt(0)));
            // 氢气模块通信异常/停电故障
            warnParam.put(ItemCode.ZTDGJ.getCode(), String.valueOf(dfs.charAt(2)));
            // 环境温度传感器2故障
            warnParam.put(ItemCode.ZWDCGQ2GZ.getCode(), String.valueOf(dfs.charAt(3)));
            // 环境温度传感器1故障
            warnParam.put(ItemCode.ZWDCGQ1GZ.getCode(), String.valueOf(dfs.charAt(4)));
            // 组压模块通信异常
            warnParam.put(ItemCode.TXZT.getCode(), String.valueOf(dfs.charAt(5)));
            // 保存告警记录
            alarmLogService.alarmBattery(config, batteryWarnInfo.getBatteryPackNumber(), null, warnParam, batteryReportLog);
        }

        // 排除单体电池序号
        List<Integer> excludeModelNum = new ArrayList<>();

        // 没有故障，处理排除外的全部告警
        if (batteryWarnInfo.getAlarmBatterySum() == 0) {
            alarmLogService.alarmFix(config.getConfigId(), batteryWarnInfo.getBatteryPackNumber(), true, excludeModelNum, BATTERY_FAULT_CODE);
            return;
        }

        /*单体电池故障状态*/
        JSONArray dfbStatus = batteryWarnInfo.getDeviceFaultBatteryStatus();
        if(dfbStatus != null){
            int alarmSum = dfbStatus.size();
            //获取单体电池内容
            JSONObject batteryStatus;
            for (int i = 0; i < alarmSum; i++) {
                batteryStatus = dfbStatus.getJSONObject(i);
                String status = batteryStatus.getString("status");
                Integer batteryNumber = batteryStatus.getInteger("batteryNumber");
                //0:表示正常/1:表示报警
                //bit7:为连接条电阻故障          bit6:为电池鼓包故障
                //bit5:为电池漏液故障            bit4:为电池温度传感器故障
                //bit3:为单体电池监测模块通信异常  bit2:为内阻测试浮充电流异常
                //bit1:为内阻测试放电电流异常      bit0:为内阻测试电池电压异常
                Map<String, String> warnParam = new HashMap<>(5);
                // 连接条电阻故障
                warnParam.put(ItemCode.DTLJTGJ.getCode(), String.valueOf(status.charAt(0)));
                // 电池鼓包故障
                warnParam.put(ItemCode.DTGB.getCode(), String.valueOf(status.charAt(1)));
                // 电池漏液故障
                warnParam.put(ItemCode.DTLYGJ.getCode(), String.valueOf(status.charAt(2)));
                // 电池温度传感器故障
                warnParam.put(ItemCode.DTWDCGQGZ.getCode(), String.valueOf(status.charAt(3)));
                // 单体电池监测模块通信异常
                warnParam.put(ItemCode.DTTXZT.getCode(), String.valueOf(status.charAt(4)));
                // 保存告警记录
                alarmLogService.alarmBattery(config, batteryWarnInfo.getBatteryPackNumber(), batteryNumber, warnParam, batteryReportLog);
                // 不存在的故障
                excludeModelNum.add(batteryNumber);
            }
        }

        // 修复平台正在报警，但是设备没有报警的单体记录
        alarmLogService.alarmFix(config.getConfigId(), batteryWarnInfo.getBatteryPackNumber(), true, excludeModelNum, BATTERY_FAULT_CODE);
    }

    /**
     * 解析蓄电池故障类数据
     *
     * @param dataInfo 指令内容
     */
    public static BatteryWarnInfo toFailDecoder(String dataInfo) {
        String info = dataInfo.substring(16, dataInfo.length() - 4);
        //第一位
        String binary8D = CodingUtil.hexString2binaryString(info.substring(0, 2));
        //应答结果
        String res = String.valueOf(CodingUtil.binaryToDecimal(binary8D.substring(0, 1)));
        if(StrUtil.equals(res, "1")) {
            return null;
        }

        BatteryWarnInfo batteryWarnInfo = new BatteryWarnInfo();
        //设备型号
        //String.valueOf(CodingUtil.binaryToDecimal(binary8D.substring(1, 5)));
        //电池组编号
        batteryWarnInfo.setBatteryPackNumber(CodingUtil.binaryToDecimal(binary8D.substring(5, 8)));
        //第二位单体电池报警数量
        batteryWarnInfo.setAlarmBatterySum(Integer.parseInt(CodingUtil.hexStringToString(info.substring(2, 4))));
        //第三位设备故障电池状态
        int index8D = 4;
        JSONArray deviceFaultBatteryStatus = new JSONArray();
        for (int i = 0; i < batteryWarnInfo.getAlarmBatterySum(); i++) {
            JSONObject batteryStatusOjb = new JSONObject();
            String dfbInfo = info.substring(index8D + i * 4, index8D + (i + 1) * 4);
            int batteryNumber = Integer.parseInt(CodingUtil.hexStringToString(dfbInfo.substring(0, 2)));
            String binary8D1 = CodingUtil.hexString2binaryString(dfbInfo.substring(2, 4));
            batteryStatusOjb.put("batteryNumber", batteryNumber);
            batteryStatusOjb.put("status", binary8D1);
            deviceFaultBatteryStatus.add(batteryStatusOjb);
        }
        batteryWarnInfo.setDeviceFaultBatteryStatus(deviceFaultBatteryStatus);

        //设备故障状态
        index8D = index8D + batteryWarnInfo.getAlarmBatterySum() * 4;
        batteryWarnInfo.setDeviceFaultStatus(CodingUtil.hexString2binaryString(info.substring(index8D, index8D + 2)));
        return batteryWarnInfo;
    }
}
