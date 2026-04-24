package com.shanhe.project.iot.CM03N;

import cn.hutool.core.util.StrUtil;
import com.shanhe.common.utils.Crc16m;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.StringUtils;
import com.shanhe.framework.enums.*;
import com.shanhe.framework.comm.tcp.model.DeviceData;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import com.shanhe.project.device.alarm.service.IAlarmLogService;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.domain.ConfigAttribute;
import com.shanhe.project.device.config.domain.ConfigProtocol;
import com.shanhe.project.device.config.domain.ConfigProtocolAttribute;
import com.shanhe.project.device.config.service.IConfigAttributeService;
import com.shanhe.project.device.config.service.IConfigProtocolService;
import com.shanhe.project.device.config.service.IConfigService;
import com.shanhe.project.device.history.service.IHistoryLogService;
import com.shanhe.project.iot.service.DataService;
import com.shanhe.project.iot.service.HandlerUtils;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.MapContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 数据上报服务类
 */
@Service
public class DataUploadHandler {
    protected static Logger logger = LoggerFactory.getLogger(DataUploadHandler.class);
    @Resource
    private IConfigService configService;
    @Resource
    private BatteryHandler batteryHandler;
    @Resource
    private IConfigProtocolService configProtocolService;
    @Resource
    private IConfigAttributeService configAttributeService;
    @Resource
    private IHistoryLogService historyLogService;
    @Resource
    private IAlarmLogService alarmLogService;
    @Resource
    private DataService dataService;

    /**
     * 数据上报，自动上报
     *
     * @param deviceData 上报信息
     */
    public void cmdD3(DeviceData deviceData) {
        this.dealData(deviceData);
    }

    /**
     * 手动下发指令，数据上报
     *
     * @param deviceData 上报信息
     */
    public void cmdD4(DeviceData deviceData) {
        this.dealData(deviceData);
    }

    /**
     * 设备数据处理
     *
     * @param deviceData 接收数据信息
     */
    private void dealData(DeviceData deviceData){
        Config config = configService.getCacheBy(deviceData.getC0(), deviceData.getC1(), deviceData.getC2());
        if (config == null) {
            logger.error("设备不存在：{}", deviceData);
            return;
        }

        // 蓄电池数据单独处理
        if (Objects.equals(config.getType(), DeviceTypeEnum._1.getDictValue())) {
            batteryHandler.doUploadData(config, deviceData);
            return;
        }

        // 空调手动设置响应
        if (Objects.equals(deviceData.getC3(), TcpCidEnum._E0.getDictValue())) {
            String resultKey = String.format(CacheKeyEnum.RESULT_CX.getKey(), config.getType(), config.getPort(), config.getChannel(), deviceData.getC3());
            CacheUtils.put(CacheKeyEnum.RESULT_CX.getCache(), resultKey, 0);
            // 设备在线
            CacheUtils.put(String.format(CacheKeyEnum.CONFIG_ONLINE.getKey(), deviceData.getC0(), deviceData.getC1(), deviceData.getC2()), new Date());
            return;
        }

        // 其他设备类型，通用处理
        dealOtherData(config, deviceData);
    }

    /**
     * 处理其他设备实时上报数据
     *
     * @param config 设备信息
     * @param deviceData 接收数据信息
     */
    private void dealOtherData(Config config, DeviceData deviceData) {
        if (!Objects.equals(config.getStatus(), YesNoEnum.YES.getDictValue())) {
            return;
        }
        // 协议
        ConfigProtocol configProtocol = configProtocolService.getCacheBy(config.getConfigId(), deviceData.getC3());
        if (configProtocol == null) {
            logger.error("找不到对应的指令解析器deviceName={}，c3={}，info={}", config.getName(), deviceData.getC3(), deviceData.getInfo());
            return;
        }

        // 协议映射属性
        List<ConfigProtocolAttribute> protocolAttributes = configProtocol.getAttributeList();
        if (protocolAttributes == null || protocolAttributes.isEmpty()) {
            logger.error("deviceName={}，c3={}对应的解析字段配置为空", config.getName(), deviceData.getC3());
            return;
        }

        boolean isCheck = this.checkSum(configProtocol, deviceData.getInfo());
        if (!isCheck) {
            logger.error("{} 采集发生校验错误 c3={}，info={}", config.getName(), deviceData.getC3(), deviceData.getInfo());
            return;
        }
        boolean isInsert = dataService.isInsert(config.getConfigId(), deviceData.getC3(), false);

        //解析数据
        int i=0;
        for (ConfigProtocolAttribute protocolAttribute : protocolAttributes) {
            // 字段
            ConfigAttribute attribute = configAttributeService.getCacheBy(config.getConfigId(), protocolAttribute.getAttrCode());
            if (attribute == null) {
                logger.error("deviceName={}，c3={}的属性={}无效", config.getName(), deviceData.getC3(), protocolAttribute.getAttrCode());
                continue;
            }

            // 解析数据并检查边界
            String val = this.safeSubstring(deviceData.getInfo(), protocolAttribute.getStartPoint(), protocolAttribute.getEndPoint());
            if (val == null) {
                logger.error("deviceName={}，c3={}的数据解析超出范围，info={}", config.getName(), deviceData.getC3(),deviceData.getInfo());
                continue;
            }

            Object newVal = this.toNewValue(val, protocolAttribute, attribute, configProtocol.getProtocolType());
            if (newVal == null) {
                logger.error("deviceName={}，c3={}的数据转换失败", config.getName(), deviceData.getC3());
                continue;
            }
            // 保存历史
            historyLogService.insertHistoryLog(attribute, newVal.toString(), isInsert);
            // 校验是否告警处理
            alarmLogService.alarmValid(attribute, null, newVal.toString(), config.getType());
            i++;
        }
        if (i > 0) {
            // 设备在线
            CacheUtils.put(String.format(CacheKeyEnum.CONFIG_ONLINE.getKey(), deviceData.getC0(), deviceData.getC1(), deviceData.getC2()), new Date());
        }
    }

    /**
     * 校验和
     */
    private boolean checkSum(ConfigProtocol configProtocol, String info) {
        // modbus协议才需校验和
        if (!Objects.equals(configProtocol.getProtocolType(), ProtocolTypeEnum._2.getDictValue())) {
            return true;
        }
        if (StrUtil.isBlank(info)) {
            return false;
        }

        // 校验算法 0 无校验 1 CRC16 2 杉和蓄电池累加和算法 3 模256的和
        ProtocolAlgorithmEnum algorithmEnum = ProtocolAlgorithmEnum.find(configProtocol.getChecksumAlgorithm());
        switch (algorithmEnum) {
            case _1:
                // 不够 2 个字节校验位
                if (info.length() < 4) {
                    return false;
                }
                String newInfo = Crc16m.getBufHexStr(Crc16m.getSendBuf(info.substring(0, info.length() - 4)));
                return StrUtil.equalsIgnoreCase(info, newInfo);
            case _3:
                // 不够 1 个字节校验位
                if (info.length() < 2) {
                    return false;
                }
                String checkSum = CodingUtil.check256(info.substring(0, info.length() - 2));
                return StrUtil.equalsIgnoreCase(checkSum, info.substring(info.length() - 2));
            case _0:
            case _2:
            default:
                return true;
        }
    }

    /**
     * 安全的substring方法，防止越界
     */
    private String safeSubstring(String str, int start, int end) {
        if (str == null || start < 0 || end > str.length() || start > end) {
            return null;
        }
        return str.substring(start, end);
    }

    /**
     * 获得转换值
     */
    private Object toNewValue(String val, ConfigProtocolAttribute protocolAttribute, ConfigAttribute attribute, Integer protocolType) {
        // 转换数据类型
        Object obj;
        ProtocolTypeEnum protocolTypeEnum = ProtocolTypeEnum.find(protocolType);
        switch (protocolTypeEnum) {
            case _3:
                // DL/T645-2007
                val = val.replaceAll(" ", "");
                // 1. 将16进制字符串转换为字节数组（每2位一个字节）
                byte[] bytes = CodingUtil.hexToByte(val);

                // 2. 每个字节减去 0x33 并反转顺序，同时构建结果字符串
                StringBuilder result = new StringBuilder();
                for (int i = bytes.length - 1; i >= 0; i--) {
                    int value = (bytes[i] & 0xFF) - 0x33;
                    if (value < 0) {
                        value += 256;
                    }
                    result.append(String.format("%02X", value & 0xFF));
                }
                obj = result.toString();
                break;
            case _1:
                // 自定义协议
            case _2:
                // 标准modbus协议
            default:
                obj = convertByDataType(val, protocolAttribute.getDataType());
                break;
        }

        // 是否转码
        if (Objects.equals(protocolAttribute.getIsComplement(), YesNoEnum.YES.getDictValue())) {
            int bits = (protocolAttribute.getEndPoint() - protocolAttribute.getStartPoint()) / 2;
            obj = CodingUtil.parseComplement(obj.toString(), 10, bits);
        }

        // 处理自定义逻辑
        return processCustomLogic(obj, protocolAttribute, attribute);
    }

    /**
     * 处理自定义逻辑
     *
     * @param obj 转换后的对象
     * @param protocolAttribute 解析规则
     * @param attribute 数据项配置
     * @return 处理后的对象
     */
    private Object processCustomLogic(Object obj, ConfigProtocolAttribute protocolAttribute, ConfigAttribute attribute) {
        // 指令类处理
        if (Objects.equals(protocolAttribute.getAnyFlag(), AnyFlagEnum._2.getDictValue())) {
            try {
                // 动态加载类路径
                Class<?> clazz = HandlerUtils.class;
                Method method = clazz.getMethod(protocolAttribute.getAnyExpress(), Object.class);
                // 调用静态方法
                return method.invoke(clazz.newInstance(), obj);
            } catch (Exception e) {
                logger.error("处理自定义逻辑失败", e);
                return null;
            }
        }
        // 应用自定义公式
        obj = applyCustomFormula(obj, protocolAttribute);

        // 模拟量数据
        if (Objects.equals(attribute.getType(), DataTypeEnum._2.getDictValue())) {
            // 格式化小数点
            obj = formatDecimal(obj, attribute.getPoint(), protocolAttribute.getHasPoint());
        }

        return obj;
    }

    /**
     * 格式化小数点
     *
     * @param obj 转换后的对象
     * @param itemPoint 数据项配置
     * @param hasPoint 解析规则
     * @return 格式化后的对象
     */
    private Object formatDecimal(Object obj, Integer itemPoint, Integer hasPoint) {
        // 是否设置小数点
        if (!Objects.equals(hasPoint, YesNoEnum.YES.getDictValue()) || itemPoint == null || itemPoint == 0) {
            if (obj instanceof String) {
                String val = obj.toString();
                if (!StringUtils.isNumeric(val)) {
                    return obj;
                }
                return Integer.parseInt(val);
            }
            return obj;
        }

        if (obj instanceof Integer) {
            return CodingUtil.shiftDecimal((Integer) obj, itemPoint);
        }
        if (obj instanceof Long) {
            return CodingUtil.shiftDecimal(((Long) obj).intValue(), itemPoint);
        }
        if (obj instanceof Float) {
            return CodingUtil.shiftDecimal(((Float) obj).intValue(), itemPoint);
        }
        if (obj instanceof Double) {
            return CodingUtil.shiftDecimal(((Double) obj).intValue(), itemPoint);
        }
        if (obj instanceof String) {
            String val = obj.toString();
            if (!StringUtils.isNumeric(val)) {
                return obj;
            }

            // 小数点转换
            return CodingUtil.shiftDecimal(Integer.parseInt(val), itemPoint);
        }
        return obj;
    }
    /**
     * 应用自定义公式
     *
     * @param obj 转换后的对象
     * @param protocolAttribute 解析规则
     * @return 计算结果
     */
    private Object applyCustomFormula(Object obj, ConfigProtocolAttribute protocolAttribute) {
        // 非自定义公式类型
        if (!Objects.equals(protocolAttribute.getAnyFlag(), AnyFlagEnum._1.getDictValue())) {
            return obj;
        }

        // 自定义公式
        if (StrUtil.isBlank(protocolAttribute.getAnyExpress())) {
            logger.error("协议 {} 属性 {} 自定义公式为空", protocolAttribute.getProtocolId(), protocolAttribute.getAttrCode());
            return obj;
        }
        JexlScript expr = new JexlBuilder().create().createScript(protocolAttribute.getAnyExpress());
        MapContext context = new MapContext();
        context.set("x", obj);

        return expr.execute(context);
    }

    /**
     * 根据数据类型转换值（DataDecimalEnum）
     *
     * @param val 输入值
     * @param dataType 解析规则
     * @return 转换后的对象
     */
    private Object convertByDataType(String val, Integer dataType) {
        DataDecimalEnum dataDecimalEnum = DataDecimalEnum.find(dataType);
        switch (dataDecimalEnum) {
            case _0:
//                return CodingUtil.hexStringToString(val);
                return Integer.parseInt(val, 16);
            case _2:
                return CodingUtil.binaryToDecimal(val);
            case _3:
                return CodingUtil.hexToAscii(val);
            case _4:
                // 浮点数（大端序）
                return CodingUtil.hexToFloatBigEndian(val);
            case _5:
                // 浮点数（小端序）
                return CodingUtil.hexToFloatLittleEndian(val);
            case _6:
                // 十六进制字符串转uint32
                return CodingUtil.hexToU32(val);
            case _7:
                // 十六进制字符串转uint16
                return CodingUtil.hexToU16(val);
            case _8:
                // 十六进制字符串转int32
                return CodingUtil.hexToI32(val);
            case _9:
                // 十六进制字符串转int
                return CodingUtil.hexToI16(val);
            case _1:
            default:
                return val;
        }
    }
}
