package com.shanhe.framework.comm.port;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.shanhe.framework.consts.DeviceCommConst;
import com.shanhe.project.iot.service.DeviceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 串口配置
 *
 * @author wjh
 * @since 2025/8/19
 */
@Slf4j
@Order(3)
@Component
public class SerialPortConfig implements ApplicationRunner {

    @Resource
    private DeviceCommConst commConst;
    @Resource
    DeviceService deviceService;
    /** 串口 **/
    SerialPort serialPort;

    /**
     * 启动串口
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            // 开启串口通讯
            if (!commConst.isSerialPort()) {
                return;
            }

            // 检查串口是否可用
            List<String> portNameList = this.findPorts();
            if (portNameList.isEmpty() || !portNameList.contains(commConst.getPortName())) {
                log.error("当前可用串口【{}】没有找到串口 {}", String.join(",", portNameList), commConst.getPortName());
            }

            // 串口
            serialPort = SerialPort.getCommPort(commConst.getPortName());
            // 串口参数：波特率、数据位、停止位、校验位
            serialPort.setComPortParameters(commConst.getPortBaudRate(), commConst.getPortDataBits(), commConst.getPortStopBits(), commConst.getPortParity());
            // 串口读写超时时间
            serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, commConst.getPortTimeout(), commConst.getPortTimeout());
            // 串口流量控制
            serialPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
            // 串口监听
            serialPort.addDataListener(new SerialPortDataListener() {
                @Override
                public int getListeningEvents() {
                    return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
                }

                @Override
                public void serialEvent(SerialPortEvent event) {
                    if (event.getEventType() == SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
                        // 读取串口数据
                        SerialPortHandler.readFromPort();
                    } else if (event.getEventType() == SerialPort.LISTENING_EVENT_BREAK_INTERRUPT) {
                        log.info("串口【{}】断开连接", serialPort.getDescriptivePortName());
                    } else if (event.getEventType() == SerialPort.LISTENING_EVENT_DATA_WRITTEN) {
                        log.info("串口【{}】写入完成", serialPort.getDescriptivePortName());
                    }
                }
            });

            //开启串口
            boolean b = serialPort.openPort();
            if (!b) {
                log.error("串口【{}】开启失败，设置错误或被占用", serialPort.getDescriptivePortName());
            }
            if (serialPort.isOpen()) {
                log.info("串口【{}】开启成功", serialPort.getDescriptivePortName());
            } else {
                log.error("串口【{}】开启失败，设置错误或被占用", serialPort.getDescriptivePortName());
            }

            SerialPortHandler.deviceService = deviceService;
            SerialPortHandler.serialPort = serialPort;
        } catch (Exception e) {
            log.error("创建串口失败，异常信息：{}", e.getMessage());
        }
    }

    /**
     * 关闭串口连接
     */
    @PreDestroy
    public void stop() throws Exception {
        try {
            if (serialPort != null && serialPort.isOpen()){
                serialPort.removeDataListener();
                serialPort.closePort();
            }
        } catch (Exception e) {
            log.error("关闭串口失败，异常信息：{}", e.getMessage());
            throw e;
        }
    }

    /**
     * 查找所有可用端口
     *
     * @return List<String> 可用端口列表
     */
    public List<String> findPorts() {
        // 获得当前所有可用串口
        SerialPort[] serialPorts = SerialPort.getCommPorts();

        // 将可用串口名添加到List并返回该List
        List<String> portNameList = new ArrayList<>();
        for(SerialPort serialPort : serialPorts) {
            portNameList.add(serialPort.getSystemPortName());
        }
        log.info("当前可用串口：{}", String.join(",", portNameList));

        //去重
        return portNameList.stream().distinct().collect(Collectors.toList());
    }
}
