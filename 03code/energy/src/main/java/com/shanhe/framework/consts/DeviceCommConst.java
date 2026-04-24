package com.shanhe.framework.consts;

import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 设备通讯配置
 *
 * @author wjh
 * @since 2024/12/17
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "device-comm")
public class DeviceCommConst {
    /**
     * 与设备通讯类型
     */
    public String commType;

    /*-----------------------------------------------tcp配置----------------------------------------------------------*/
    /**
     * TCP监听端口
     */
    public Integer tcpPost = 22188;
    /**
     * 处理客户端连接的线程组
     */
    public Integer tcpBossThread = 1;
    /**
     * 处理网络读写的线程组
     */
    public Integer tcpWorkerThread = 2;
    /**
     * 客户端连接等待队列的长度
     */
    public Integer tcpBackLog = 200;
    /**
     * netty 心跳检测 间隔时间 / 秒
     */
    public Integer tcpIntervalTime = 120;
    /**
     * tcp连接
     */
    public Boolean isTcp() {
        return CommTypeEnum.TCP.getCode().equals(commType);
    }

    /*-----------------------------------------------串口配置----------------------------------------------------------*/
    /**
     * 串口名称
     */
    public String portName;
    /**
     * 波特率
     */
    public Integer portBaudRate = 9600;
    /**
     * 数据位
     */
    public Integer portDataBits = 8;
    /**
     * 停止位
     */
    public Integer portStopBits = 1;
    /**
     * 校验位
     */
    public Integer portParity = 0;
    /**
     * 串口超时时间
     */
    public Integer portTimeout = 1000;
    /**
     * 串口读取缓冲区大小
     */
    public Integer readBufferSize = 1024;
    /**
     * 串口写入缓冲区大小
     */
    public Integer writeBufferSize = 1024;
    /**
     * 串口连接
     */
    public Boolean isSerialPort() {
        return CommTypeEnum.PORT.getCode().equals(commType);
    }

    /**
     * 枚举：与设备通讯方式
     */
    @Getter
    public enum CommTypeEnum {
        TCP("tcp", "tcp连接"),
        PORT("port", "串口连接"),
        MQTT("mqtt", "mqtt连接");
        private final String code;
        private final String desc;

        CommTypeEnum(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }
    }
}
