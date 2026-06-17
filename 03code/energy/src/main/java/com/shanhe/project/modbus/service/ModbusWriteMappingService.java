package com.shanhe.project.modbus.service;

import com.shanhe.project.collector.battery.service.BatteryCollectorCommandService;
import com.shanhe.project.collector.battery.model.BatteryCollectorCommandResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * Modbus 写寄存器映射服务。
 * <p>
 * 将允许的 Modbus 写操作转换为内部控制服务调用，禁止拼旧 980 命令。
 * 写入成功语义为"命令已入队"，不表示硬件已完成。
 *
 * @author wjh
 * @since 2026-06-04
 */
@Slf4j
@Service
public class ModbusWriteMappingService {

    /**
     * 写寄存器地址：M460 modbus.c 中
     * BATTERY_ARRAY_STATE_REGISTER_START_ADDRESS(0x1324) + 14。
     */
    private static final int BALANCE_REGISTER = 404915;
    /** 16 位寄存器最大值。 */
    private static final int UNSIGNED_SHORT_MAX = 0xFFFF;

    @Resource
    private BatteryCollectorCommandService commandService;

    /**
     * 处理写单寄存器请求。
     *
     * @param packNum 电池组编号
     * @param referenceAddress 文档参考寄存器号
     * @param value 写入值
     * @return true 表示命令已入队，false 表示拒绝
     * @throws IllegalArgumentException 非法地址或值
     */
    public boolean writeSingleRegister(Integer packNum, int referenceAddress, int value) {
        if (packNum == null || packNum <= 0) {
            throw new ModbusIllegalDataValueException("电池组编号无效");
        }
        if (value < 0 || value > UNSIGNED_SHORT_MAX) {
            throw new ModbusIllegalDataValueException("写入值超出范围: " + value);
        }
        if (referenceAddress == BALANCE_REGISTER) {
            return writeBalance(packNum, value);
        }
        throw new ModbusIllegalDataAddressException("不支持的写寄存器地址: " + referenceAddress);
    }

    /** 写单体均衡控制。 */
    private boolean writeBalance(Integer packNum, int value) {
        int balanceValue = (value >> 8) & 0xFF;
        int modelNum = value & 0xFF;
        if (modelNum < 1 || modelNum > 245) {
            throw new ModbusIllegalDataValueException("单体地址超出范围: " + modelNum);
        }
        String channelName = commandService.resolveChannelName(packNum);
        if (channelName == null) {
            throw new ModbusIllegalDataAddressException("未找到电池组 " + packNum + " 对应的采集通道");
        }
        BatteryCollectorCommandResult result = commandService.singleBatteryBalance(
                channelName, packNum, modelNum, balanceValue, null);
        log.info("Modbus 写均衡命令已入队, packNum={}, modelNum={}, balanceValue={}, success={}",
                packNum, modelNum, balanceValue, result.isSuccess());
        return result.isSuccess();
    }
}
