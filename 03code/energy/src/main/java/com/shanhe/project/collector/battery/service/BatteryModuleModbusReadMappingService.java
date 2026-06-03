package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.mapper.BatteryModuleRealtimeMapper;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryDeviceState;
import com.shanhe.project.collector.battery.model.BatteryDeviceStateConstants;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import static com.shanhe.project.collector.battery.protocol.BatteryModuleProtocolConstants.UNSIGNED_SHORT_MAX;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SH 980 Modbus只读寄存器映射服务。
 *
 * @author wjh
 * @since 2026-05-06
 */
@Service
public class BatteryModuleModbusReadMappingService {

    /** 单体寄存器数量，246固定作为组模块，不进入单体区间。 */
    private static final int CELL_COUNT = 245;

    /** 单体电压起始参考寄存器。 */
    private static final int CELL_VOLTAGE_START = 410004;

    /** 单体内阻起始参考寄存器。 */
    private static final int CELL_RESISTANCE_START = 410252;

    /** 单体温度起始参考寄存器。 */
    private static final int CELL_TEMPERATURE_START = 410500;

    /** 单体鼓包电压起始参考寄存器。 */
    private static final int CELL_SWOLLEN_VOLTAGE_START = 410748;

    /** 设备状态寄存器起始地址。 */
    private static final int STATUS_START = 411483;

    /** 设备状态寄存器数量。 */
    private static final int STATUS_COUNT = 6;

    /** Modbus RTU单次读保持寄存器的常见上限。 */
    private static final int MAX_READ_QUANTITY = 125;

    /** 600节采集模块实时数据Mapper。 */
    private final BatteryModuleRealtimeMapper realtimeMapper;

    /** 设备状态服务，用于读取状态寄存器。 */
    private final BatteryDeviceStateService batteryDeviceStateService;

    /** 采集配置，用于按 packNum 解析通道名称。 */
    private final BatteryCollectorProperties properties;

    public BatteryModuleModbusReadMappingService(BatteryModuleRealtimeMapper realtimeMapper,
                                                  BatteryDeviceStateService batteryDeviceStateService,
                                                  BatteryCollectorProperties properties) {
        this.realtimeMapper = realtimeMapper;
        this.batteryDeviceStateService = batteryDeviceStateService;
        this.properties = properties;
    }

    /**
     * 按SH 980文档参考寄存器号读取保持寄存器值。
     *
     * @param packNum 电池组编号
     * @param referenceAddress 文档参考寄存器号
     * @param quantity 读取数量
     * @return 16位无符号寄存器值
     * @throws IllegalArgumentException 参数无效或地址不支持
     * @throws IllegalStateException 首次数据未就绪（对应 Modbus 异常码 03）
     */
    public int[] readHoldingRegisters(Integer packNum, int referenceAddress, int quantity) {
        if (packNum == null) {
            throw new IllegalArgumentException("电池组编号不能为空");
        }
        if (quantity <= 0 || quantity > MAX_READ_QUANTITY) {
            throw new IllegalArgumentException("读取数量必须在1到" + MAX_READ_QUANTITY + "之间");
        }

        ModbusReadSnapshot snapshot = loadSnapshot(packNum);
        if (!snapshot.isDataReady()) {
            throw new IllegalStateException("电池组 " + packNum + " 实时数据未就绪");
        }
        int[] values = new int[quantity];
        for (int i = 0; i < quantity; i++) {
            values[i] = resolveRegister(snapshot, referenceAddress + i);
        }
        return values;
    }

    /**
     * 加载指定电池组的单体和组实时快照。
     *
     * @param packNum 电池组编号
     * @return Modbus读取快照
     */
    private ModbusReadSnapshot loadSnapshot(Integer packNum) {
        String channelName = resolveChannelName(packNum);
        List<BatteryModuleCellRealtime> cells = realtimeMapper.selectCells(packNum);
        BatteryModuleGroupRealtime group = realtimeMapper.selectGroup(packNum);
        return new ModbusReadSnapshot(cells, group, packNum, channelName);
    }

    /** 按 packNum 解析通道名称。 */
    private String resolveChannelName(Integer packNum) {
        if (packNum == null || properties == null || properties.getChannels() == null) {
            return null;
        }
        for (BatteryCollectorChannelConfig channel : properties.getChannels()) {
            if (channel != null
                    && Boolean.TRUE.equals(channel.getEnabled())
                    && packNum.equals(channel.getBatteryGroup())) {
                return channel.getName();
            }
        }
        return null;
    }

    /**
     * 解析单个参考寄存器值。
     *
     * @param snapshot Modbus读取快照
     * @param address 文档参考寄存器号
     * @return 16位无符号寄存器值
     */
    private int resolveRegister(ModbusReadSnapshot snapshot, int address) {
        if (isCellAddress(address, CELL_VOLTAGE_START)) {
            BatteryModuleCellRealtime cell = snapshot.getCell(cellIndex(address, CELL_VOLTAGE_START));
            // 电压×1000编码(mV分辨率)，与SH 980文档一致
            return scale(cell == null ? null : cell.getVoltage(), 1000d);
        }
        if (isCellAddress(address, CELL_RESISTANCE_START)) {
            BatteryModuleCellRealtime cell = snapshot.getCell(cellIndex(address, CELL_RESISTANCE_START));
            return unsigned16(cell == null ? null : cell.getResistance());
        }
        if (isCellAddress(address, CELL_TEMPERATURE_START)) {
            BatteryModuleCellRealtime cell = snapshot.getCell(cellIndex(address, CELL_TEMPERATURE_START));
            // 温度+50°C偏移编码，与600模块协议一致
            return scaleWithOffset(cell == null ? null : cell.getTemperature(), 50d, 10d);
        }
        if (isCellAddress(address, CELL_SWOLLEN_VOLTAGE_START)) {
            BatteryModuleCellRealtime cell = snapshot.getCell(cellIndex(address, CELL_SWOLLEN_VOLTAGE_START));
            return scale(cell == null ? null : cell.getSwollenVoltage(), 10d);
        }
        return resolveGroupRegister(snapshot.getGroup(), address, snapshot);
    }

    /**
     * 解析组测量寄存器值。
     *
     * @param group 组实时数据
     * @param address 文档参考寄存器号
     * @return 16位无符号寄存器值
     */
    private int resolveGroupRegister(BatteryModuleGroupRealtime group, int address, ModbusReadSnapshot snapshot) {
        if (isStatusAddress(address)) {
            return resolveStatusRegister(address, snapshot);
        }
        if (!isSupportedGroupAddress(address)) {
            throw new IllegalArgumentException("不支持的Modbus参考地址: " + address);
        }
        if (group == null) {
            return 0;
        }
        switch (address) {
            case 411729:
                return scale(first(group.getBatteryPackOuterVoltage(), group.getExternalVoltage()), 10d);
            case 411730:
                return scaleWithOffset(first(group.getPackCurrent(), group.getChargeDischargeCurrent()), 3000d, 10d);
            case 411731:
                return scaleWithOffset(first(group.getBatteryPackFloatCurrent(), group.getFloatCurrent()), 10d, 1000d);
            case 411732:
                return scaleWithOffset(group.getEnvironmentTemperature1(), 50d, 10d);
            case 411733:
                return scaleWithOffset(group.getEnvironmentTemperature2(), 50d, 10d);
            case 411734:
                return unsigned16(group.getMaxVoltageBatNum());
            case 411735:
                return scale(group.getMaxCellVoltage(), 1000d);
            case 411736:
                return unsigned16(group.getMinVoltageBatNum());
            case 411737:
                return scale(group.getMinCellVoltage(), 1000d);
            case 411738:
                return scale(group.getAvgCellVoltage(), 1000d);
            case 411739:
                return scale(group.getBatteryVoltageDeviation(), 1000d);
            case 411740:
                return scale(first(group.getBatteryVoltageRange(), group.getVoltageRange()), 1000d);
            case 411741:
                return unsigned16(group.getMaxResistanceBatNum());
            case 411742:
                return unsigned16(group.getMaxInternalResistance());
            case 411743:
                return unsigned16(group.getMinResistanceBatNum());
            case 411744:
                return unsigned16(group.getMinInternalResistance());
            case 411745:
                return scale(group.getAvgInternalResistance(), 1d);
            case 411746:
                return unsigned16(group.getMaxTemperatureBatNum());
            case 411747:
                return scaleWithOffset(group.getMaxCellTemperature(), 50d, 10d);
            case 411748:
                return unsigned16(group.getMinTemperatureBatNum());
            case 411749:
                return scaleWithOffset(group.getMinCellTemperature(), 50d, 10d);
            case 411750:
                return scaleWithOffset(first(group.getBatteryAvgTemperature(), group.getAvgCellTemperature()), 50d, 10d);
            case 411751:
                return scale(group.getBatteryPackSoc(), 10d);
            case 411752:
                return scale(group.getBatteryPackSoh(), 10d);
            case 411762:
                return unsigned16(group.getBatteryPackStatus());
            case 411763:
                return unsigned16(group.getBackupDuration());
            case 411764:
                return scale(first(group.getBcapacity(), group.getCapacity()), 10d);
            case 411765:
                return unsigned16(group.getDisChargeDuration());
            case 411766:
                return scale(group.getDisChargeCapacity(), 10d);
            default:
                throw new IllegalArgumentException("不支持的Modbus参考地址: " + address);
        }
    }

    /**
     * 解析设备状态寄存器值。
     *
     * @param address 文档参考寄存器号
     * @param snapshot Modbus读取快照（含 packNum）
     * @return 16位无符号寄存器值
     */
    private int resolveStatusRegister(int address, ModbusReadSnapshot snapshot) {
        Integer packNum = snapshot.getPackNum();
        if (packNum == null) {
            return 0;
        }
        switch (address) {
            case 411483: // 通道在线状态：1=在线, 0=离线
                return readChannelOpenStatus(snapshot.getChannelName());
            case 411484: // 通道异常状态：1=异常, 0=正常
                return readChannelErrorStatus(snapshot.getChannelName());
            case 411485: // 模块活跃状态：1=有模块活跃, 0=全部无响应
                return readModuleActiveStatus(snapshot.getChannelName());
            case 411486: // 模块超时状态：1=存在超时, 0=正常
                return readModuleTimeoutStatus(snapshot.getChannelName());
            case 411487: // 246 新鲜度：1=新鲜, 0=过期
                return readGroup246Freshness(packNum);
            case 411488: // 工作模式：模式码
                return readWorkMode(packNum);
            default:
                return 0;
        }
    }

    /** 读取通道在线状态。 */
    private int readChannelOpenStatus(String channelName) {
        if (channelName == null || batteryDeviceStateService == null) {
            return 0;
        }
        BatteryDeviceState state = batteryDeviceStateService.selectByScope(
                BatteryDeviceStateConstants.ScopeType.CHANNEL, channelName,
                BatteryDeviceStateConstants.StateCode.CHANNEL_OPEN);
        return state != null && "open".equals(state.getStateValue()) ? 1 : 0;
    }

    /** 读取通道异常状态。 */
    private int readChannelErrorStatus(String channelName) {
        if (channelName == null || batteryDeviceStateService == null) {
            return 0;
        }
        BatteryDeviceState state = batteryDeviceStateService.selectByScope(
                BatteryDeviceStateConstants.ScopeType.CHANNEL, channelName,
                BatteryDeviceStateConstants.StateCode.CHANNEL_ERROR);
        return state != null && BatteryDeviceStateConstants.StateLevel.ERROR.equals(state.getStateLevel()) ? 1 : 0;
    }

    /** 读取模块活跃状态。 */
    private int readModuleActiveStatus(String channelName) {
        if (channelName == null || batteryDeviceStateService == null) {
            return 0;
        }
        List<BatteryDeviceState> states = batteryDeviceStateService.selectByChannelAndCode(
                channelName, BatteryDeviceStateConstants.StateCode.MODULE_ACTIVE);
        if (states != null) {
            for (BatteryDeviceState state : states) {
                if ("active".equals(state.getStateValue())) {
                    return 1;
                }
            }
        }
        return 0;
    }

    /** 读取模块超时状态。 */
    private int readModuleTimeoutStatus(String channelName) {
        if (channelName == null || batteryDeviceStateService == null) {
            return 0;
        }
        List<BatteryDeviceState> states = batteryDeviceStateService.selectByChannelAndCode(
                channelName, BatteryDeviceStateConstants.StateCode.MODULE_TIMEOUT);
        return states != null && !states.isEmpty() ? 1 : 0;
    }

    /** 读取 246 新鲜度。 */
    private int readGroup246Freshness(Integer packNum) {
        if (batteryDeviceStateService == null) {
            return 0;
        }
        BatteryDeviceState state = batteryDeviceStateService.selectByScope(
                BatteryDeviceStateConstants.ScopeType.PACK, String.valueOf(packNum),
                BatteryDeviceStateConstants.StateCode.GROUP_246_FRESHNESS);
        return state != null && "fresh".equals(state.getStateValue()) ? 1 : 0;
    }

    /** 读取工作模式。 */
    private int readWorkMode(Integer packNum) {
        if (batteryDeviceStateService == null) {
            return 0;
        }
        BatteryDeviceState state = batteryDeviceStateService.selectByScope(
                BatteryDeviceStateConstants.ScopeType.PACK, String.valueOf(packNum),
                BatteryDeviceStateConstants.StateCode.WORK_MODE);
        if (state != null && state.getMode() != null) {
            return unsigned16(state.getMode());
        }
        return 0;
    }

    /**
     * 判断是否为已纳入草案的组寄存器地址。
     *
     * @param address 文档参考寄存器号
     * @return true表示支持
     */
    private boolean isSupportedGroupAddress(int address) {
        return address >= 411729 && address <= 411752
                || address >= 411762 && address <= 411766;
    }

    /** 判断是否为设备状态寄存器地址。 */
    private boolean isStatusAddress(int address) {
        return address >= STATUS_START && address < STATUS_START + STATUS_COUNT;
    }

    /**
     * 判断地址是否落在指定单体寄存器区间。
     *
     * @param address 文档参考寄存器号
     * @param start 区间起始参考寄存器号
     * @return true表示属于该单体区间
     */
    private boolean isCellAddress(int address, int start) {
        return address >= start && address < start + CELL_COUNT;
    }

    /**
     * 按单体区间起始地址计算单体编号。
     *
     * @param address 文档参考寄存器号
     * @param start 区间起始参考寄存器号
     * @return 单体编号
     */
    private int cellIndex(int address, int start) {
        return address - start + 1;
    }

    /**
     * 返回首个非空数值。
     *
     * @param first 优先值
     * @param second 备用值
     * @return 首个非空数值
     */
    private Double first(Double first, Double second) {
        return first != null ? first : second;
    }

    /**
     * 按偏置和倍率换算寄存器值。
     *
     * @param value 实际值
     * @param offset 偏置
     * @param multiplier 倍率
     * @return 16位无符号寄存器值
     */
    private int scaleWithOffset(Double value, double offset, double multiplier) {
        if (value == null) {
            return 0;
        }
        return unsigned16((int) Math.round((value + offset) * multiplier));
    }

    /**
     * 按倍率换算寄存器值。
     *
     * @param value 实际值
     * @param multiplier 倍率
     * @return 16位无符号寄存器值
     */
    private int scale(Double value, double multiplier) {
        if (value == null) {
            return 0;
        }
        return unsigned16((int) Math.round(value * multiplier));
    }

    /**
     * 校验并返回16位无符号寄存器值。
     *
     * @param value 原始整数值
     * @return 16位无符号寄存器值
     */
    private int unsigned16(Integer value) {
        if (value == null) {
            return 0;
        }
        if (value < 0) {
            return 0;
        }
        if (value > UNSIGNED_SHORT_MAX) {
            return UNSIGNED_SHORT_MAX;
        }
        return value;
    }

    /**
     * 单次Modbus读取使用的实时数据快照。
     */
    private static class ModbusReadSnapshot {

        /** 按单体编号缓存单体实时数据。 */
        private final Map<Integer, BatteryModuleCellRealtime> cellMap = new HashMap<>();

        /** 组实时数据。 */
        private final BatteryModuleGroupRealtime group;

        /** 是否有数据（首次采集完成后为 true）。 */
        private final boolean dataReady;

        /** 电池组编号。 */
        private final Integer packNum;

        /** 通道名称。 */
        private final String channelName;

        ModbusReadSnapshot(List<BatteryModuleCellRealtime> cells, BatteryModuleGroupRealtime group,
                           Integer packNum, String channelName) {
            if (cells != null) {
                for (BatteryModuleCellRealtime cell : cells) {
                    if (cell != null && cell.getBatNum() != null) {
                        cellMap.put(cell.getBatNum(), cell);
                    }
                }
            }
            this.group = group;
            this.dataReady = !cellMap.isEmpty() || group != null;
            this.packNum = packNum;
            this.channelName = channelName;
        }

        /**
         * 获取指定单体实时数据。
         *
         * @param batNum 单体编号
         * @return 单体实时数据
         */
        BatteryModuleCellRealtime getCell(int batNum) {
            return cellMap.get(batNum);
        }

        /**
         * 获取组实时数据。
         *
         * @return 组实时数据
         */
        BatteryModuleGroupRealtime getGroup() {
            return group;
        }

        /**
         * 判断数据是否就绪（至少有单体或组数据）。
         *
         * @return true 表示数据已就绪
         */
        boolean isDataReady() {
            return dataReady;
        }

        Integer getPackNum() {
            return packNum;
        }

        String getChannelName() {
            return channelName;
        }
    }
}
