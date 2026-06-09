package com.shanhe.project.modbus.service;

import com.shanhe.project.collector.battery.service.BatteryCollectorCommandService;
import com.shanhe.project.collector.battery.model.BatteryCollectorCommandResult;
import com.shanhe.project.device.opt.service.ControlBatterySet;
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

    /** 写寄存器起始地址：单体均衡。 */
    private static final int BALANCE_START = 404901;
    /** 写寄存器起始地址：手动编号。 */
    private static final int MANUAL_ADDR_START = 404921;

    @Resource
    private BatteryCollectorCommandService commandService;

    @Resource
    private ControlBatterySet controlBatterySet;

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
            throw new IllegalArgumentException("电池组编号无效");
        }
        if (referenceAddress >= MANUAL_ADDR_START && referenceAddress < MANUAL_ADDR_START + 2) {
            return writeManualAddress(packNum, value);
        }
        if (referenceAddress >= BALANCE_START && referenceAddress < BALANCE_START + 245) {
            return writeBalance(packNum, referenceAddress - BALANCE_START + 1, value);
        }
        throw new IllegalArgumentException("不支持的写寄存器地址: " + referenceAddress);
    }

    /** 写单体均衡控制。 */
    private boolean writeBalance(Integer packNum, int modelNum, int value) {
        if (modelNum < 1 || modelNum > 245) {
            throw new IllegalArgumentException("单体地址超出范围: " + modelNum);
        }
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException("均衡值超出范围: " + value);
        }
        String channelName = commandService.resolveChannelName(packNum);
        if (channelName == null) {
            throw new IllegalArgumentException("未找到电池组 " + packNum + " 对应的采集通道");
        }
        BatteryCollectorCommandResult result = commandService.singleBatteryBalance(
                channelName, packNum, modelNum, value, null);
        log.info("Modbus 写均衡命令已入队, packNum={}, modelNum={}, value={}, success={}",
                packNum, modelNum, value, result.isSuccess());
        return result.isSuccess();
    }

    /** 写手动编号请求。 */
    private boolean writeManualAddress(Integer packNum, int value) {
        if (value < 1 || value > 245) {
            throw new IllegalArgumentException("模块地址超出范围: " + value);
        }
        throw new IllegalArgumentException("手动编号需要连续寄存器写入，暂未开放单寄存器写入");
    }
}
