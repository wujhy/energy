package com.shanhe.framework.comm;

import com.shanhe.framework.comm.port.SerialPortHandler;
import com.shanhe.framework.comm.tcp.server.TcpServerHandler;

/**
 * 服务端通讯
 *
 * @author wjh
 * @since 2025/8/20
 */
public class CommServer {

    /**
     * 判断是否打开
     */
    public static Boolean isOpen() {
        return SerialPortHandler.isOpen() || TcpServerHandler.isOpen();
    }

    /**
     * 获取IMEI
     */
    public static String getImei() {
        if (SerialPortHandler.isOpen()) {
            return SerialPortHandler.getImei();
        } else if (TcpServerHandler.isOpen()) {
            return TcpServerHandler.getImei();
        }
        return null;
    }

    /**
     * 发送指令
     *
     * @param cmd 指令
     */
    public static void returnCmd(String cmd) {
        if (SerialPortHandler.isOpen()) {
            SerialPortHandler.returnCmd(cmd);
        } else if (TcpServerHandler.isOpen()) {
            TcpServerHandler.returnCmd(cmd);
        }
    }
}
