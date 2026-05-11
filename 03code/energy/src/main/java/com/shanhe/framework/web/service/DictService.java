package com.shanhe.framework.web.service;

import java.util.List;

import com.shanhe.common.utils.StringUtils;
import com.shanhe.common.utils.bean.Dict;
import com.shanhe.framework.enums.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 字典
 *
 * @author wjh
 * @since 2024/12/19
 */
@Order(4)
@Component
public class DictService implements CommandLineRunner {

    static List<Dict> operationTypeList;
    static List<Dict> resultList;
    static List<Dict> yesNoList;
    static List<Dict> deviceTypeList;
    static List<Dict> dataTypeList;
    static List<Dict> portTypeList;
    static List<Dict> baudRateList;
    static List<Dict> connectionStatusList;
    static List<Dict> alarmLevelList;
    static List<Dict> parityBitsList;
    static List<Dict> stopBitsList;
    static List<Dict> dataBitsList;
    static List<Dict> hostTypeList;
    static List<Dict> compareList;
    static List<Dict> batteryBrandList;
    static List<Dict> batteryModelList;
    static List<Dict> ipAddrList;

    @Override
    public void run(String... args) throws Exception {
        operationTypeList = OperationType.getDictList();
        resultList = ResultEnum.getDictList();
        yesNoList = YesNoEnum.getDictList();
        deviceTypeList = DeviceTypeEnum.getDictList();
        dataTypeList = DataTypeEnum.getDictList();
        portTypeList = PortTypeEnum.getDictList();
        baudRateList = BaudRateEnum.getDictList();
        connectionStatusList = ConnectionStatusEnum.getDictList();
        alarmLevelList = AlarmLevelEnum.getDictList();
        parityBitsList = ParityBitsEnum.getDictList();
        stopBitsList = StopBitsEnum.getDictList();
        dataBitsList = DataBitsEnum.getDictList();
        hostTypeList = HostTypeEnum.getDictList();
        compareList = CompareEnum.getDictList();
        batteryBrandList = BatteryBrandEnum.getDictList();
        batteryModelList = BatteryModelEnum.getDictList();
        ipAddrList = IpAddrEnum.getDictList();
    }

    /**
     * 根据字典类型查询字典数据信息
     *
     * @param dictType 字典类型
     * @return 参数键值
     */
    public static List<Dict> getType(String dictType) {
        DictType type = DictType.valueOf(dictType);
        switch (type) {
            case OPERATION_TYPE:
                return operationTypeList;
            case RESULT:
                return resultList;
            case YES_NO:
                return yesNoList;
            case DEVICE_TYPE:
                return deviceTypeList;
            case DATA_TYPE:
                return dataTypeList;
            case PORT_TYPE:
                return portTypeList;
            case BAUD_RATE:
                return baudRateList;
            case CONNECTION_STATUS:
                return connectionStatusList;
            case ALARM_LEVEL:
                return alarmLevelList;
            case PARITY_BITS:
                return parityBitsList;
            case STOP_BITS:
                return stopBitsList;
            case DATA_BITS:
                return dataBitsList;
            case HOST_TYPE:
                return hostTypeList;
            case COMPARE:
                return compareList;
            case BATTERY_BRAND:
                return batteryBrandList;
            case BATTERY_MODEL:
                return batteryModelList;
            case IP_ADDR:
                return ipAddrList;
            default:
                return null;
        }
    }

    /**
     * 根据字典类型和字典键值查询字典数据信息
     *
     * @param dictType 字典类型
     * @param dictValue 字典键值
     * @return 字典标签
     */
    public static String getLabel(String dictType, Object dictValue) {
        DictType type = DictType.valueOf(dictType);
        switch (type) {
            case OPERATION_TYPE:
                return OperationType.findByValue(dictValue);
            case RESULT:
                return ResultEnum.findByValue(dictValue);
            case YES_NO:
                return YesNoEnum.findByValue(dictValue);
            case DEVICE_TYPE:
                return DeviceTypeEnum.findByValue(dictValue);
            case DATA_TYPE:
                return DataTypeEnum.findByValue(dictValue);
            case PORT_TYPE:
                return PortTypeEnum.findByValue(dictValue);
            case BAUD_RATE:
                return BaudRateEnum.findByValue(dictValue);
            case CONNECTION_STATUS:
                return ConnectionStatusEnum.findByValue(dictValue);
            case ALARM_LEVEL:
                return AlarmLevelEnum.findByValue(dictValue);
            case PARITY_BITS:
                return ParityBitsEnum.findByValue(dictValue);
            case STOP_BITS:
                return StopBitsEnum.findByValue(dictValue);
            case DATA_BITS:
                return DataBitsEnum.findByValue(dictValue);
            case HOST_TYPE:
                return HostTypeEnum.findByValue(dictValue);
            case COMPARE:
                return CompareEnum.findByValue(dictValue);
            case BATTERY_BRAND:
                return BatteryBrandEnum.findByValue(dictValue);
            case BATTERY_MODEL:
                return BatteryModelEnum.findByValue(dictValue);
            case IP_ADDR:
                return IpAddrEnum.findByValue(dictValue);
            default:
                return null;
        }
    }

    /**
     * 根据字典类型和字典值获取字典标签
     *
     * @param dictType 字典类型
     * @param dictValue 字典值
     * @param separator 分隔符
     * @return 字典标签
     */
    public static String getDictLabel(String dictType, String dictValue, String separator) {
        if (StringUtils.containsAny(separator, dictValue)) {
            StringBuilder propertyString = new StringBuilder();
            for (String value : dictValue.split(separator)) {
                String label = getLabel(dictType, value);
                if (StringUtils.isNotBlank(label)) {
                    propertyString.append(label).append(separator);
                }
            }
            return StringUtils.stripEnd(propertyString.toString(), separator);
        } else {
            return getLabel(dictType, dictValue);
        }
    }
}
