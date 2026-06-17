package com.shanhe.project.modbus.service;

/**
 * Modbus 写请求携带了超出协议范围的数据值。
 *
 * @author wjh
 * @since 2026-06-17
 */
public class ModbusIllegalDataValueException extends IllegalArgumentException {

    public ModbusIllegalDataValueException(String message) {
        super(message);
    }
}
