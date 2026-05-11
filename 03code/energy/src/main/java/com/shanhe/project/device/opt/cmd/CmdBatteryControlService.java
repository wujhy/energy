package com.shanhe.project.device.opt.cmd;

import com.shanhe.framework.enums.BatteryCidEnum;
import com.shanhe.framework.enums.TcpCharEnum;
import com.shanhe.framework.enums.TcpCidEnum;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.domain.DevBatteryOpt;
import com.shanhe.project.device.host.service.IHostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Calendar;

/**
 * 生成蓄电池控制指令
 */
@Service
public class CmdBatteryControlService {

    protected static Logger logger = LoggerFactory.getLogger(CmdBatteryControlService.class);

    @Resource
    private IHostService hostService;

    /**
     * 浮充管理配置
     *
     * @param config 设备配置
     * @param opt 请求参数
     * @return 指令
     */
    public String getCmd31(Config config, DevBatteryOpt opt) {
        // 指令内容
        StringBuilder info = new StringBuilder();
        // 默认地址、指令编码
        info.append("01").append("31");
        // 长度，默认10
        info.append(CodingUtil.stringToHexString("10",2));
        //电池组编号
        info.append(CodingUtil.integerToHexString(opt.getPackNum(), 2));
        //是否开启
        info.append(CodingUtil.integerToHexString(opt.getIsEnabled(), 2));
        //截止电压
        String endVoltage = CodingUtil.integerToHexString(Float.floatToIntBits(opt.getEndVoltage().floatValue()), 8);
        info.append(endVoltage, 6, 8);
        info.append(endVoltage, 4, 6);
        info.append(endVoltage, 2, 4);
        info.append(endVoltage, 0, 2);
        //恢复电压
        String recVoltage = CodingUtil.integerToHexString(Float.floatToIntBits(opt.getRecVoltage().floatValue()), 8);
        info.append(recVoltage, 6, 8);
        info.append(recVoltage, 4, 6);
        info.append(recVoltage, 2, 4);
        info.append(recVoltage, 0, 2);

        // 追加校验码、头尾等
        this.appendHeadAndEnd(info);
        // 生成完整指令
        return DeviceModel.getCmd(hostService.getDetail(), config, info.toString(), TcpCidEnum._54.getDictValue(), BatteryCidEnum._E1.getDictValue());
    }

    /**
     * 统一加校验和、指令头尾
     */
    private void appendHeadAndEnd(StringBuilder info) {
        // 校验和
        info.append(CodingUtil.energyCheckSum(info.toString()));
        // 添加头尾
        info.insert(0, TcpCharEnum.HEAD_53.getDictValue());
        info.append(TcpCharEnum.END_0D.getDictValue());
    }

    /**
     * 内阻测试配置
     *
     * @param config 设备配置
     * @param opt 请求参数
     * @return 指令
     */
    public String getCmd32(Config config, DevBatteryOpt opt) {
        // 指令内容
        StringBuilder info = new StringBuilder();
        // 默认地址、指令编码
        info.append("01").append("32");
        // 长度，默认13
        info.append(CodingUtil.stringToHexString("13",2));
        //电池组编号
        info.append(CodingUtil.integerToHexString(opt.getPackNum(), 2));
        //是否开启
        info.append(CodingUtil.integerToHexString(opt.getIsEnabled(), 2));
        //时间：年月日时分秒
        info.append(CodingUtil.stringToHexString(opt.getReplaceTime().substring(0, 4), 4));
        info.append(CodingUtil.stringToHexString(opt.getReplaceTime().substring(4, 6), 2));
        info.append(CodingUtil.stringToHexString(opt.getReplaceTime().substring(6, 8), 2));
        info.append(CodingUtil.stringToHexString(opt.getReplaceTime().substring(8, 10), 2));
        info.append(CodingUtil.stringToHexString(opt.getReplaceTime().substring(10, 12), 2));
        info.append(CodingUtil.stringToHexString(opt.getReplaceTime().substring(12, 14), 2));
        //相隔天数
        info.append(CodingUtil.integerToHexString(opt.getIntervalDays(), 4));
        //执行次数
        info.append(CodingUtil.integerToHexString(opt.getExecCount(), 4));

        // 追加校验码、头尾等
        this.appendHeadAndEnd(info);
        // 生成完整指令
        return DeviceModel.getCmd(hostService.getDetail(), config, info.toString(), TcpCidEnum._54.getDictValue(), BatteryCidEnum._E2.getDictValue());
    }

    /**
     * 连接条电阻测试配置
     *
     * @param config 设备配置
     * @param opt 请求参数
     * @return 指令
     */
    public String getCmd33(Config config, DevBatteryOpt opt) {
        // 指令内容
        StringBuilder info = new StringBuilder();
        // 默认地址、指令编码
        info.append("01").append("33");
        // 长度，默认13
        info.append(CodingUtil.stringToHexString("13",2));
        //电池组编号
        info.append(CodingUtil.integerToHexString(opt.getPackNum(), 2));
        //是否开启
        info.append(CodingUtil.integerToHexString(opt.getIsEnabled(), 2));
        //时间：年月日时分秒
        info.append(CodingUtil.stringToHexString(opt.getReplaceTime().substring(0, 4), 4));
        info.append(CodingUtil.stringToHexString(opt.getReplaceTime().substring(4, 6), 2));
        info.append(CodingUtil.stringToHexString(opt.getReplaceTime().substring(6, 8), 2));
        info.append(CodingUtil.stringToHexString(opt.getReplaceTime().substring(8, 10), 2));
        info.append(CodingUtil.stringToHexString(opt.getReplaceTime().substring(10, 12), 2));
        info.append(CodingUtil.stringToHexString(opt.getReplaceTime().substring(12, 14), 2));
        //相隔天数
        info.append(CodingUtil.integerToHexString(opt.getIntervalDays(), 4));
        //执行次数
        info.append(CodingUtil.integerToHexString(opt.getExecCount(), 4));

        // 追加校验码、头尾等
        this.appendHeadAndEnd(info);
        // 生成完整指令
        return DeviceModel.getCmd(hostService.getDetail(), config, info.toString(), TcpCidEnum._54.getDictValue(), BatteryCidEnum._E3.getDictValue());
    }

    /**
     * 核容测试配置
     *
     * @param config 设备配置
     * @param opt 请求参数
     * @return 指令
     */
    public String getCmd34(Config config, DevBatteryOpt opt) {
        // 指令内容
        StringBuilder info = new StringBuilder();
        // 默认地址、指令编码
        info.append("01").append("34");
        // 长度，默认17
        info.append(CodingUtil.stringToHexString("17",2));
        //电池组编号
        info.append(CodingUtil.integerToHexString(opt.getPackNum(), 2));
        //是否开启
        info.append(CodingUtil.integerToHexString(opt.getIsEnabled(), 2));
        //时间：年月日时分秒
        info.append(CodingUtil.stringToHexString(opt.getReplaceTime().substring(0, 4), 4));
        info.append(CodingUtil.stringToHexString(opt.getReplaceTime().substring(4, 6), 2));
        info.append(CodingUtil.stringToHexString(opt.getReplaceTime().substring(6, 8), 2));
        info.append(CodingUtil.stringToHexString(opt.getReplaceTime().substring(8, 10), 2));
        info.append(CodingUtil.stringToHexString(opt.getReplaceTime().substring(10, 12), 2));
        info.append(CodingUtil.stringToHexString(opt.getReplaceTime().substring(12, 14), 2));
        //截止电压
        String endVoltage = CodingUtil.integerToHexString(Float.floatToIntBits(opt.getEndVoltage().floatValue()), 8);
        info.append(endVoltage, 6, 8);
        info.append(endVoltage, 4, 6);
        info.append(endVoltage, 2, 4);
        info.append(endVoltage, 0, 2);
        //相隔天数
        info.append(CodingUtil.integerToHexString(opt.getIntervalDays(), 4));
        //执行次数
        info.append(CodingUtil.integerToHexString(opt.getExecCount(), 4));

        // 追加校验码、头尾等
        this.appendHeadAndEnd(info);
        // 生成完整指令
        return DeviceModel.getCmd(hostService.getDetail(), config, info.toString(), TcpCidEnum._54.getDictValue(), BatteryCidEnum._E4.getDictValue());
    }

    /**
     * 备电时长测试配置
     *
     * @param config 设备配置
     * @param opt 请求参数
     * @return 指令
     */
    public String getCmd35(Config config, DevBatteryOpt opt) {
        // 指令内容
        StringBuilder info = new StringBuilder();
        // 默认地址、指令编码
        info.append("01").append(BatteryCidEnum._35.getDictValue());
        // 长度，默认17
        info.append(CodingUtil.stringToHexString("19",2));
        //电池组编号
        info.append(CodingUtil.integerToHexString(opt.getPackNum(), 2));
        //是否开启
        info.append(CodingUtil.integerToHexString(opt.getIsEnabled(), 2));
        //时间：年月日时分秒
        info.append(CodingUtil.stringToHexString(opt.getReplaceTime().substring(0, 4), 4));
        info.append(CodingUtil.stringToHexString(opt.getReplaceTime().substring(4, 6), 2));
        info.append(CodingUtil.stringToHexString(opt.getReplaceTime().substring(6, 8), 2));
        info.append(CodingUtil.stringToHexString(opt.getReplaceTime().substring(8, 10), 2));
        info.append(CodingUtil.stringToHexString(opt.getReplaceTime().substring(10, 12), 2));
        info.append(CodingUtil.stringToHexString(opt.getReplaceTime().substring(12, 14), 2));
        //放电时长
        info.append(CodingUtil.integerToHexString(opt.getDischargeTime(), 4));
        //截止电压
        String endVoltage = CodingUtil.integerToHexString(Float.floatToIntBits(opt.getEndVoltage().floatValue()), 8);
        info.append(endVoltage, 6, 8);
        info.append(endVoltage, 4, 6);
        info.append(endVoltage, 2, 4);
        info.append(endVoltage, 0, 2);
        //相隔天数
        info.append(CodingUtil.integerToHexString(opt.getIntervalDays(), 4));
        //执行次数
        info.append(CodingUtil.integerToHexString(opt.getExecCount(), 4));

        // 追加校验码、头尾等
        this.appendHeadAndEnd(info);
        // 生成完整指令
        return DeviceModel.getCmd(hostService.getDetail(), config, info.toString(), TcpCidEnum._54.getDictValue(), BatteryCidEnum._E5.getDictValue());
    }

    /**
     * 单体内阻测试
     *
     * @param config 设备配置
     * @param opt 请求参数
     * @return 指令
     */
    public String getCmd36(Config config, DevBatteryOpt opt) {
        // 指令内容
        StringBuilder info = new StringBuilder();
        // 默认地址、指令编码
        info.append("01").append(BatteryCidEnum._36.getDictValue()).append("02");
        // 电池组编号
        info.append(CodingUtil.integerToHexString(opt.getPackNum(), 2));
        // 单体编号
        info.append(CodingUtil.integerToHexString(opt.getModelNum(), 2));

        // 追加校验码、头尾等
        this.appendHeadAndEnd(info);

        // 生成完整指令
        return DeviceModel.getCmd(hostService.getDetail(), config, info.toString(), TcpCidEnum._54.getDictValue(), BatteryCidEnum._E6.getDictValue());
    }

    /**
     * 内阻测试
     *
     * @param config 设备配置
     * @param paramNumber 请求参数
     * @param paramValue 请求参数值
     * @return 指令
     */
    public String genCmd05(Config config, String paramNumber, String paramValue) {
        // 指令内容
        StringBuilder info = new StringBuilder();
        // 默认地址、指令编码
        info.append("01").append("05");
        // 值长度
        int valueNum;
        switch (paramNumber) {
            case "72":
            case "81":
                valueNum = 4;
                break;
            case "78":
                valueNum = 14;
                break;
            case "79":
            case "80":
            case "83":
                valueNum = 2;
                break;
            default:
                return null;
        }
        // 长度（编号 + 值）
        info.append(CodingUtil.integerToHexString(1 + (valueNum / 2), 2));
        // 编号
        info.append(paramNumber);
        // 值
        info.append(CodingUtil.stringToHexString(paramValue, valueNum));

        // 追加校验码、头尾等
        this.appendHeadAndEnd(info);
        // 生成完整指令
        return DeviceModel.getCmd(hostService.getDetail(), config, info.toString(), TcpCidEnum._54.getDictValue(), BatteryCidEnum._85.getDictValue());
    }

    /**
     * 连接条电阻测试
     *
     * @param config 设备配置
     * @param opt 请求参数
     * @return 指令
     */
    public String genCmd0F(Config config, DevBatteryOpt opt) {
        // 指令内容
        StringBuilder info = new StringBuilder();
        // 默认地址、指令编码、长度
        info.append("01").append("0F").append("01");
        //电池组编号
        info.append(CodingUtil.integerToHexString(opt.getPackNum(), 2));

        // 追加校验码、头尾等
        this.appendHeadAndEnd(info);
        // 生成完整指令
        return DeviceModel.getCmd(hostService.getDetail(), config, info.toString(), TcpCidEnum._54.getDictValue(), BatteryCidEnum._8F.getDictValue());
    }

    /**
     * 设置核容模块工作模式
     *
     * @param config 设备配置
     * @param packNum 组编码
     * @param workModel 模式（1：备电，2：核容，3、充电，4：停止备电、核容）
     * @param dischargeTime 备电时长
     * @param endVoltage 截止电压
     * @return 指令
     */
    public String genCmd30(Config config, Integer packNum, String workModel, Integer dischargeTime, Double endVoltage) {
        // 指令内容
        StringBuilder info = new StringBuilder();
        // 默认地址、指令编码、长度
        info.append("01").append("30").append("08");
        // 电池组编号
        info.append(CodingUtil.integerToHexString(packNum, 2));
        // 工作模式
        info.append(CodingUtil.stringToHexString(workModel, 2));
        // 备电时长
        info.append(CodingUtil.integerToHexString(dischargeTime, 4));
        // 截止电压
        String stopVoltage = CodingUtil.integerToHexString(Float.floatToIntBits(endVoltage.floatValue()), 8);
        info.append(stopVoltage, 6, 8);
        info.append(stopVoltage, 4, 6);
        info.append(stopVoltage, 2, 4);
        info.append(stopVoltage, 0, 2);
        // 追加校验码、头尾等
        this.appendHeadAndEnd(info);
        // 生成完整指令
        return DeviceModel.getCmd(hostService.getDetail(), config, info.toString(), TcpCidEnum._54.getDictValue(), BatteryCidEnum._E0.getDictValue());
    }

    /**
     * 修改日期时间
     *
     * @return 指令
     */
    public String genCmd37(Config config) {
        // 指令内容
        StringBuilder info = new StringBuilder();
        // 默认地址、指令编码、长度
        info.append("01").append("37").append("08").append("04");
        Calendar calendar = Calendar.getInstance();
        info.append(CodingUtil.integerToHexString(calendar.get(Calendar.YEAR), 4));
        info.append(CodingUtil.integerToHexString(calendar.get(Calendar.MONTH) + 1, 2));
        info.append(CodingUtil.integerToHexString(calendar.get(Calendar.DAY_OF_MONTH), 2));
        info.append(CodingUtil.integerToHexString(calendar.get(Calendar.HOUR_OF_DAY), 2));
        info.append(CodingUtil.integerToHexString(calendar.get(Calendar.MINUTE), 2));
        info.append(CodingUtil.integerToHexString(calendar.get(Calendar.SECOND), 2));
        // 追加校验码、头尾等
        this.appendHeadAndEnd(info);

        // 生成完整指令
        return DeviceModel.getCmd(hostService.getDetail(), config, info.toString(), TcpCidEnum._54.getDictValue(), BatteryCidEnum._E7.getDictValue());
    }

    /**
     * 获取屏蔽报警参数
     *
     * @param config 设备配置
     * @param batteryPackNumber 电池组编号
     * @return 指令
     */
    public String genCmd39(Config config, Integer batteryPackNumber) {
        // 指令内容
        StringBuilder info = new StringBuilder();
        // 默认地址、指令编码、长度
        info.append("01").append("39").append("01");
        //电池组编号
        info.append(CodingUtil.integerToHexString(batteryPackNumber, 2));
        // 追加校验码、头尾等
        this.appendHeadAndEnd(info);
        // 生成完整指令
        return DeviceModel.getCmd(hostService.getDetail(), config, info.toString(), TcpCidEnum._54.getDictValue(), BatteryCidEnum._E9.getDictValue());
    }

    /**
     * 读取电池组报警参数
     *
     * @param config 设备配置
     * @param batteryPackNumber 电池组编号
     * @param alarmLevel 等级
     * @return 指令
     */
    public String genCmd0B(Config config, Integer batteryPackNumber, int alarmLevel) {
        // 指令内容
        StringBuilder info = new StringBuilder();
        // 默认地址、指令编码、长度
        info.append("01").append("0B").append("02");
        //电池组编号
        info.append(CodingUtil.integerToHexString(batteryPackNumber, 2));
        // 报警等级
        info.append(CodingUtil.integerToHexString(alarmLevel, 2));
        // 追加校验码、头尾等
        this.appendHeadAndEnd(info);
        // 生成完整指令
        return DeviceModel.getCmd(hostService.getDetail(), config, info.toString(), TcpCidEnum._54.getDictValue(), BatteryCidEnum._8B.getDictValue());
    }

    /**
     * 屏蔽报警
     *
     * @param config 设备配置
     * @param batteryPackNumber 蓄电池编号
     * @param alarmLevel 告警等级
     * @param alarmShield 告警屏蔽
     * @param paramNumber 参数
     * @return 指令
     */
    public String genCmd0A(Config config, Integer batteryPackNumber, int alarmLevel, int alarmShield, String paramNumber) {
        // 指令内容
        StringBuilder info = new StringBuilder();
        // 默认地址、指令编码、长度
        info.append("01").append(BatteryCidEnum._0A.getDictValue()).append("03");
        //电池组编号
        info.append(CodingUtil.integerToHexString(batteryPackNumber, 2));
        // 报警等级
        info.append(alarmLevel);
        // 报警屏蔽
        info.append(alarmShield);
        // 参数
        info.append(paramNumber);
        // 追加校验码、头尾等
        this.appendHeadAndEnd(info);
        // 生成完整指令
        return DeviceModel.getCmd(hostService.getDetail(), config, info.toString(), TcpCidEnum._54.getDictValue(), BatteryCidEnum._8A.getDictValue());
    }

    /**
     * 读取电池组配置信息
     *
     * @param config 设备配置
     * @return 指令
     */
    public String genCmd06(Config config) {
        // 指令内容
        StringBuilder info = new StringBuilder();
        // 默认地址、指令编码、长度
        info.append("01").append("06").append("00");
        // 追加校验码、头尾等
        this.appendHeadAndEnd(info);
        // 生成完整指令
        return DeviceModel.getCmd(hostService.getDetail(), config, info.toString(), TcpCidEnum._54.getDictValue(), BatteryCidEnum._86.getDictValue());
    }

    /**
     * 获取设备型号及软件版本号
     *
     * @param config 设备配置
     * @return 指令
     */
    public String genCmd0E(Config config) {
        // 指令内容
        StringBuilder info = new StringBuilder();
        // 默认地址、指令编码、长度
        info.append("01").append("0E").append("00");
        // 追加校验码、头尾等
        this.appendHeadAndEnd(info);
        // 生成完整指令
        return DeviceModel.getCmd(hostService.getDetail(), config, info.toString(), TcpCidEnum._54.getDictValue(), BatteryCidEnum._8E.getDictValue());
    }

    /**
     * 设置电池组报警参数
     *
     * @param config 设备配置
     * @param packNum 组编号
     * @param level 等级
     * @param paramNumber 参数编号
     * @param value 参数值
     * @return 指令
     */
    public String genCmd03(Config config, Integer packNum, String level, String paramNumber, Double value) {
        // 指令内容
        StringBuilder info = new StringBuilder();
        // 默认地址、指令编码、长度
        info.append("01").append(BatteryCidEnum._03.getDictValue()).append("05");
        //电池组编号
        info.append(CodingUtil.integerToHexString(packNum, 2));
        // 报警等级
        info.append(CodingUtil.stringToHexString(level, 2));
        // 参数编号
        info.append(paramNumber);
        // 参数值
        info.append(CodingUtil.stringToHexString(formatBatteryPackParam(paramNumber, value), 4));

        // 追加校验码、头尾等
        this.appendHeadAndEnd(info);
        // 生成完整指令
        return DeviceModel.getCmd(hostService.getDetail(), config, info.toString(), TcpCidEnum._54.getDictValue(), BatteryCidEnum._83.getDictValue());
    }

    /**
     * 格式化电池组参数
     *
     * @param paramNumber 参数
     * @param paramValue 参数值
     * @return 新值
     */
    private static String formatBatteryPackParam(String paramNumber, Double paramValue) {
        String result = "";
        /*说明：全部参数都是两字节*/
        switch (paramNumber) {
            case "01":  //3位小数
            case "batteryOverchargeAlarm":
            case "02":
            case "batteryOverchargeRestore":
            case "03":
            case "batteryOverdischargeAlarm":
            case "04":
            case "batteryOverdischargeRestore":
            case "05":
            case "batteryFloatChargeOvertopAlarm":
            case "06":
            case "batteryFloatChargeOvertopRestore":
            case "07":
            case "batteryFloatChargeTooLowAlarm":
            case "08":
            case "batteryFloatChargeTooLowRestore":
            case "09":
            case "batteryVoltageUnevennessAlarm":
            case "10":
            case "batteryVoltageUnevennessRestore":
            case "11":
            case "batteryVoltageRangeAlarm":
            case "12":
            case "batteryVoltageRangeRestore":
                result = String.format("%05.3f", paramValue).replace(".", "");
                break;
            case "37":  //2位小数
            case "batteryPackOverchargeAlarm":
            case "38":
            case "batteryPackOverchargeRestore":
            case "39":
            case "batteryPackOverdischargeAlarm":
            case "40":
            case "batteryPackOverdischargeRestore":
            case "41":
            case "batteryPackFloatChargeOvertopAlarm":
            case "42":
            case "batteryPackFloatChargeOvertopRestore":
            case "batteryPackFloatChargeTooLowAlarm":
            case "batteryPackFloatChargeTooLowRestore":
                result = String.format("%05.2f", paramValue).replace(".", "");
                break;
            case "13":  //1位小数
            case "chargeOvercurrentAlarm":
            case "14":
            case "chargeOvercurrentRestore":
            case "15":
            case "dischargeOvercurrentAlarm":
            case "16":
            case "dischargeOvercurrentRestore":
            case "17":
            case "environmentHighTemperatureAlarm":
            case "18":
            case "environmentHighTemperatureRestore":
            case "19":
            case "20":
            case "environmentLowTemperatureAlarm":
            case "environmentLowTemperatureRestore":
            case "21":
            case "batteryHighTemperatureAlarm":
            case "22":
            case "batteryHighTemperatureRestore":
            case "23":
            case "batteryLowTemperatureAlarm":
            case "24":
            case "batteryLowTemperatureRestore":
            case "25":
            case "batteryTemperatureUnevennessAlarm":
            case "26":
            case "batteryTemperatureUnevennessRestore":
            case "27":
            case "resistanceTooBigAlarm":
            case "28":
            case "resistanceTooBigRestore":
            case "29":
            case "resistanceUnevennessAlarm":
            case "30":
            case "resistanceUnevennessRestore":
            case "31":
            case "resistanceTooSmallAlarm":
            case "32":
            case "resistanceTooSmallRestore":
            case "33":
            case "hydrogenPercentageUpperLimitAlarm":
            case "34":
            case "hydrogenPercentageUpperLimitRestore":
            case "35":
            case "36":
            case "49":
            case "SOCLowAlarm":
            case "50":
            case "SOCLowRestore":
            case "51":
            case "SOHLowAlarm":
            case "52":
            case "SOHLowRestore":
                result = String.format("%05.1f", paramValue).replace(".", "");
                break;
            case "53":  //无小数
            case "resistanceReferenceValue":
                result = String.format("%04d", paramValue.intValue());
                break;
            default:
        }
        return result;
    }
}
