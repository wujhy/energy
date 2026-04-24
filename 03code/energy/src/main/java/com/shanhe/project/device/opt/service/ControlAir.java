package com.shanhe.project.device.opt.service;

import cn.hutool.core.util.StrUtil;
import com.shanhe.common.exception.ServiceException;
import com.shanhe.common.utils.Crc16m;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.enums.*;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.domain.ConfigAttribute;
import com.shanhe.project.device.config.service.IConfigAttributeService;
import com.shanhe.project.device.history.service.IHistoryLogService;
import com.shanhe.project.device.host.domain.Host;
import com.shanhe.project.device.opt.cmd.DeviceModel;
import com.shanhe.project.device.opt.vo.PrecisionAirVO;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * 空调控制类
 *
 * @author wjh
 * @since 2025/7/10
 */
@Service
public class ControlAir extends ControlBase {

    protected static Logger logger = LoggerFactory.getLogger(ControlAir.class);
    /** 缓存结果 **/
    CacheKeyEnum cacheKeyEnum = CacheKeyEnum.RESULT_CX;

    @Resource
    private IConfigAttributeService configAttributeService;
    @Resource
    private IHistoryLogService historyLogService;

    /**
     * 空调控制
     *
     * @param configId      设备ID
     * @param cmdId         操作命令 1：制冷 2：制热 3：除湿 4：送风 5：开机 6：关机 7：减小 8：增大
     * @param temperature   温度
     */
    public AjaxResult doControlAir(Long configId, String cmdId, String temperature) {
        // 主机信息
        Host host = super.getHost();
        // 设备信息
        Config config = super.getConfig(configId);

        // 指令类型
        AirCmdEnum airCmdEnum = AirCmdEnum.getAirEnum(cmdId);
        // 避免重复请求（请求结果KEY）
        String resultKey = super.setControlStatus(config, TcpCidEnum._E0.getDictValue(), cacheKeyEnum);
        // 空调缓存KEY
        String airModelKey = String.format(CacheKeyEnum.AIR_MODEL.getKey(), config.getConfigId());

        // 其他类型替换为最后tempModel操作模式
        if (Objects.equals(airCmdEnum.getType(), AirCmdTypeEnum._3.getCode())) {
            Object airModel = CacheUtils.get(CacheKeyEnum.AIR_MODEL.getCache(), airModelKey);
            cmdId = airModel == null ? AirCmdEnum._1.getCode() : (String)airModel;
            airCmdEnum = AirCmdEnum.getAirEnum(cmdId);
        }

        // 生成指令下发
        if (StrUtil.equals(config.getTypeCode(), AirEnum.AC180S.getCode())) {
            // 亚和智能空调开关
            this.cmdAirAc180s(host, config, airCmdEnum, temperature);
        } else {
            // 普通空调
            this.cmdAir(host, config, cmdId, temperature);
        }

        // 结果监控
        AjaxResult ajaxResult = super.getControlResult(resultKey, cacheKeyEnum);

        // 响应成功，保存历史记录
        if (Objects.equals(ajaxResult.get(AjaxResult.CODE_TAG), AjaxResult.Type.SUCCESS.value())) {
            // 保存温度历史
            if (StrUtil.isNotBlank(temperature)) {
                ConfigAttribute setWd = configAttributeService.getCacheBy(config.getConfigId(), AirAttrEnum.WD.getCode());
                if (setWd != null) {
                    historyLogService.insertHistoryLog(setWd, temperature, true);
                }
            }
            // 空调开关机状态
            if (Objects.equals(airCmdEnum.getType(), AirCmdTypeEnum._2.getCode())) {
                ConfigAttribute switchStatus = configAttributeService.getCacheBy(config.getConfigId(), AirAttrEnum.STATUS.getCode());
                if (switchStatus != null) {
                    historyLogService.insertHistoryLog(switchStatus, cmdId, true);
                }
            }
            // 保存操作模式
            if (Objects.equals(airCmdEnum.getType(), AirCmdTypeEnum._1.getCode())) {
                ConfigAttribute tempModel = configAttributeService.getCacheBy(config.getConfigId(), AirAttrEnum.MODEL.getCode());
                if (tempModel != null) {
                    historyLogService.insertHistoryLog(tempModel, cmdId, true);
                }

                // 最后关机状态需同时开启
                String statusLast = historyLogService.getCacheBy(config.getConfigId(), null, AirAttrEnum.STATUS.getCode());
                if (Objects.equals(statusLast, AirCmdEnum._6.getCode())) {
                    ConfigAttribute switchStatus = configAttributeService.getCacheBy(config.getConfigId(), AirAttrEnum.STATUS.getCode());
                    if (switchStatus != null) {
                        historyLogService.insertHistoryLog(switchStatus, AirCmdEnum._5.getCode(), true);
                    }
                }
                // 缓存空调模式
                CacheUtils.put(CacheKeyEnum.AIR_MODEL.getCache(), airModelKey, cmdId);
            }
        }
        return ajaxResult;
    }

    /**
     * 空调控制
     *
     * @param host          主机信息
     * @param config        设备信息
     * @param cmdId         操作命令 1：制冷 2：制热 3：除湿 4：送风 5：开机 6：关机 7：减小 8：增大
     * @param temperature   温度
     */
    private void cmdAir(Host host, Config config, String cmdId, String temperature) {
        String info = CodingUtil.stringToHexString(cmdId, 2)
                + CodingUtil.stringToHexString(temperature.replace(".", ""), 4);
        CommServer.returnCmd(DeviceModel.getCmd(host, config, info, TcpCidEnum._30.getDictValue(), TcpCidEnum._E0.getDictValue()));
    }

    /**
     * AC180S空调控制
     *
     * @param host          主机信息
     * @param config        设备信息
     * @param airCmdEnum    操作命令 1：制冷 2：制热 3：除湿 4：送风 5：开机 6：关机 7：减小 8：增大
     * @param temperature   温度
     */
    private void cmdAirAc180s(Host host, Config config, AirCmdEnum airCmdEnum, String temperature) {
        StringBuilder info = new StringBuilder();
        // 串口
        info.append(CodingUtil.integerToHexString(config.getChannel(), 2)).append("10");
        // 起始位
        info.append(CodingUtil.stringToHexString("42", 4));
        if (Objects.equals(airCmdEnum.getType(), AirCmdTypeEnum._2.getCode())) {
            // 寄存器数量
            info.append(CodingUtil.integerToHexString(1, 4));
            // 字节计数 寄存器数量*2
            info.append(CodingUtil.integerToHexString(2, 2));
            // 寄存器值（开关机）
            info.append(CodingUtil.integerToHexString(StrUtil.equals(airCmdEnum.getCode(), AirCmdEnum._5.getCode()) ? 0 : 1, 4));
        } else {
            // 寄存器数量
            info.append(CodingUtil.integerToHexString(3, 4));
            // 字节计数 寄存器数量*2
            info.append(CodingUtil.integerToHexString(6, 2));
            // 寄存器值（开机）
            info.append(CodingUtil.integerToHexString(0, 4));

            //寄存器值（模式 0自动  1制冷  2除湿  3送风  4制热  其他 关机）
            switch (airCmdEnum) {
                case _1:
                    info.append(CodingUtil.integerToHexString(1, 4));
                    break;
                case _2:
                    info.append(CodingUtil.integerToHexString(4, 4));
                    break;
                case _3:
                    info.append(CodingUtil.integerToHexString(2, 4));
                    break;
                case _4:
                    info.append(CodingUtil.integerToHexString(3, 4));
                    break;
                default:
                    info.append(CodingUtil.integerToHexString(0, 4));
                    break;
            }
            // 寄存器值（温度 0=16度  1=17度  2=18度....以此类推 14=30度）
            info.append(CodingUtil.integerToHexString((int)Math.floor(Double.parseDouble(temperature)) - 16, 4));
        }
        //CRC校验码
        byte[] sendBuf = Crc16m.getSendBuf(info.toString());
        CommServer.returnCmd(DeviceModel.getCmd(host, config, Crc16m.getBufHexStr(sendBuf), TcpCidEnum._54.getDictValue(), TcpCidEnum._E0.getDictValue()));
    }

    /**
     * 精密空调控制
     */
    public AjaxResult doPrecisionAirCmd(PrecisionAirVO params) {
        // 主机信息
        Host host = super.getHost();
        // 设备信息
        Config config = super.getConfig(params.getConfigId());

        // 避免重复请求
        String resultKey = super.setControlStatus(config, TcpCidEnum._E0.getDictValue(), cacheKeyEnum);

        // 生成指令下发
        CommServer.returnCmd(this.doPrecisionAirCmd(host, config, params));

        // 结果监控
        return super.getControlResult(resultKey, cacheKeyEnum);
    }

    /**
     * 获取海信精密空调控制类的指令
     *
     * @param host   主机信息
     * @param config 设备配置
     * @param params 空调参数
     * @return 指令
     */
    public String doPrecisionAirCmd(Host host, Config config, PrecisionAirVO params) {
        /*
         * 控制指令
         *
         * cmdType 1:机组开关设定 2:机组模式设定 3:制冷温度设定值 4:制热温度设定值 5:温度报警上限 6:温度报警下限 7:内风机设定风速 8:首次上电压缩机延时时间
         * value 设置值
         * 地址	数据	                    数据说明	                                            数据格式
         * 5010	机组开关设定 	            10H开机、01H关机
         * 5013	机组模式设定	            00H:FAN　10H:制热  20H:COLD 30H:DEHUMI 40H:AUTO
         * 5015	制冷温度设定值	            整机温度设置值	                                        （x+20）*2的标准
         * 5017	制热温度设定值	            整机温度设置值	                                        （x+20）*2的标准
         * 5038	温度报警上限	            默认50度，50到25度范围	                                （x+20）*2的标准
         * 5039	温度报警下限	            默认1度，35到0度范围，不能超过温度报警上限。	            （x+20）*2的标准
         * 5142	内风机设定风速	            自动0， 低速 1，中速2，高速3
         * 5166	首次上电压缩机延时时间	    秒计时
         */

        // 指令内容
        StringBuilder info = new StringBuilder();
        // 默认地址、写入指令编码
        info.append("01").append("06");
        switch(params.getCmdType()) {
            case 1:
                if (!StrUtil.equalsAny(params.getValue(), "10", "01")) {
                    throw new RuntimeException("指令参数错误！");
                }
                info.append(CodingUtil.integerToHexString(5010, 4));
                info.append("00").append(params.getValue());
                break;
            case 2:
                if (!StrUtil.equalsAny(params.getValue(),"00", "10", "20", "30", "40")) {
                    throw new ServiceException("指令参数错误！");
                }
                info.append(CodingUtil.integerToHexString(5013, 4));
                info.append("00").append(params.getValue());
                break;
            case 3:
                info.append(CodingUtil.integerToHexString(5015, 4));
                info.append(CodingUtil.integerToHexString((Integer.parseInt(params.getValue()) + 20) * 2, 4));
                break;
            case 4:
                info.append(CodingUtil.integerToHexString(5017, 4));
                info.append(CodingUtil.integerToHexString((Integer.parseInt(params.getValue()) + 20) * 2, 4));
                break;
            case 5:
                info.append(CodingUtil.integerToHexString(5038, 4));
                info.append(CodingUtil.integerToHexString((Integer.parseInt(params.getValue()) + 20) * 2, 4));
                break;
            case 6:
                info.append(CodingUtil.integerToHexString(5039, 4));
                info.append(CodingUtil.integerToHexString((Integer.parseInt(params.getValue()) + 20) * 2, 4));
                break;
            case 7:
                if (!StrUtil.equalsAny(params.getValue(),"0", "1", "2", "3")) {
                    throw new ServiceException("指令参数错误！");
                }
                info.append(CodingUtil.integerToHexString(5142, 4));
                info.append("000").append(params.getValue());
                break;
            case 8:
                info.append(CodingUtil.integerToHexString(5166, 4));
                info.append(CodingUtil.stringToHexString(params.getValue(), 4));
                break;
            default:
                throw new RuntimeException("控制指令类型错误");
        }

        // 生成完整指令
        return DeviceModel.getCmd(host, config, Crc16m.getBufHexStr(Crc16m.getSendBuf(info.toString())),
                TcpCidEnum._54.getDictValue(), TcpCidEnum._E0.getDictValue());
    }

    /**
     * 空调枚举
     */
    public enum AirEnum {
        /** 空调产品 */
        AC180S("AC180S", "亚和智能空调开关"),
        _99("99", "默认");
        @Getter
        private final String code;
        @Getter
        private final String name;
        AirEnum(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    /**
     * 空调控制枚举
     */
    public enum AirCmdEnum {
        /** 1：制冷 2：制热 3：除湿 4：送风 5：开机 6：关机 7：减小 8：增大 */
        _1("1", "制冷", 1),
        _2("2", "制热", 1),
        _3("3", "除湿", 1),
        _4("4", "送风", 1),
        _5("5", "开机", 2),
        _6("6", "关机", 2),
        _7("7", "减小", 3),
        _8("8", "增大", 3),
        _99("99", "其他", 3);

        @Getter
        private final String code;
        @Getter
        private final String name;
        @Getter
        private final Integer type;

        AirCmdEnum(String code, String name, Integer type) {
            this.code = code;
            this.name = name;
            this.type = type;
        }

        public static AirCmdEnum getAirEnum(String code) {
            for (AirCmdEnum airCmdEnum : AirCmdEnum.values()) {
                if (StrUtil.equals(airCmdEnum.getCode(), code)) {
                    return airCmdEnum;
                }
            }
            return _99;
        }
    }

    /**
     * 空调控制指令类型枚举
     */
    public enum AirCmdTypeEnum {
        /** 1：模式，2、开关机，3，其他操作 */
        _1(1, "模式"),
        _2(2, "开关机"),
        _3(3, "其他");
        @Getter
        private final Integer code;
        @Getter
        private final String name;
        AirCmdTypeEnum(Integer code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    /**
     * 空调属性枚举
     */
    public enum AirAttrEnum {
        /** 属性枚举 */
        WD("setWd", "设置温度"),
        STATUS("switchStatus", "设置开关机"),
        MODEL("tempModel", "设置模式");
        @Getter
        private final String code;
        @Getter
        private final String name;
        AirAttrEnum(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }
}
