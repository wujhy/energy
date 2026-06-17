package com.shanhe.project.modbus.service;

/**
 * Modbus 写请求访问了未开放或不存在的寄存器地址。
 *
 * @author wjh
 * @since 2026-06-17
 */
public class ModbusIllegalDataAddressException extends IllegalArgumentException {

    public ModbusIllegalDataAddressException(String message) {
        super(message);
    }
}
