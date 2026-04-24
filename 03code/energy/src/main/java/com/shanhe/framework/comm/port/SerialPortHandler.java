package com.shanhe.framework.comm.port;

import cn.hutool.core.util.StrUtil;
import com.fazecast.jSerialComm.SerialPort;
import com.shanhe.common.utils.ServletUtils;
import com.shanhe.common.utils.Threads;
import com.shanhe.framework.comm.CommServerDecoder;
import com.shanhe.framework.manager.AsyncTaskManager;
import com.shanhe.framework.comm.tcp.model.DeviceData;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import com.shanhe.project.iot.service.DeviceService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

/**
 * 串口通讯解析
 *
 * @author wjh
 * @since 2025/8/19
 */
@Slf4j
public class SerialPortHandler {

    /** 串口 */
    public static SerialPort serialPort;
    /** 注入设备消费方法 */
    public static DeviceService deviceService;
    /** 设备IMEI */
    private static String deviceImei;
    /** 指令头字节长度 */
    public static final Integer HEAD_LENGTH = 13;

    private static final ThreadLocal<List<Object>> DECODER_OUTPUT_CACHE = ThreadLocal.withInitial(ArrayList::new);

    /**
     * 串口读取数据
     */
    public static void readFromPort() {
        try {
            // 分包
            if (serialPort.bytesAvailable() <= 0) {
                return;
            }

            // 读取数据
            byte[] readBuffer = new byte[serialPort.bytesAvailable()];
            int numRead = serialPort.readBytes(readBuffer, readBuffer.length);
            if (numRead > 0) {
                String reqStr = CodingUtil.bytesToHexString(readBuffer).toUpperCase();
                log.info("串口收到数据包解析成字符串:{}", reqStr);
                dataDecoder(reqStr);
            }
        } catch (Exception e) {
            log.error("从串口读取数据异常", e);
        }
    }

    /**
     * 数据解码
     *
     * @param msg 数据包
     */
    public static void dataDecoder(String msg) {
        List<Object> deviceDataList = DECODER_OUTPUT_CACHE.get();
        // 复用前清空内容
        deviceDataList.clear();

        CommServerDecoder.toDecode(msg, HEAD_LENGTH, deviceDataList);
        if (deviceDataList.isEmpty()) {
            return;
        }
        for (Object deviceData : deviceDataList) {
            dataAvailable((DeviceData) deviceData);
        }
        // 复用前清空内容
        deviceDataList.clear();
    }

    /**
     * 指令处理
     *
     * @param deviceData 通道消息内容
     */
    public static void dataAvailable(DeviceData deviceData) {
        try{
            // 是否新的设备或连接
            boolean isNewDevice = StrUtil.isBlank(deviceImei)
                    || !StrUtil.equals(deviceImei, deviceData.getImei());
            if (isNewDevice) {
                deviceImei = deviceData.getImei();
            }

            // 异步处理收到的指令
            AsyncTaskManager.me().execute(
                    new TimerTask() {
                        @Override
                        public void run() {
                            try {
                                deviceService.tcpDevice(deviceData);
                            } catch (Exception e) {
                                log.error("数据解析异常：{}", e.getMessage(), e);
                            }
                        }
                    }
            );
        } catch (Exception e){
            log.error("通道数据消费异常：{}", e.getMessage(), e);
        }
    }

    /**
     * 给设备下发指令
     *
     * @param cmd 指令
     */
    public static synchronized void returnCmd(String cmd) {
        // 未启用串口连接，不处理
        if (serialPort == null) {
            log.debug("未启用串口连接！！！");
            return;
        }

        if (!isOpen()) {
            log.debug("无法打开串口建立连接，无法下发指令CMD：{}", cmd);
            return;
        }

        // 判断串口是否打开，如果没打开，就打开串口。打开串口的函数会返回一个boolean值，用于表明串口是否成功打开了
        boolean isCommOpened = serialPort.openPort();
        if (!isCommOpened) {

            // 重新打开
            serialPort.closePort();
            isCommOpened = serialPort.openPort();

            if (!isCommOpened) {
                log.debug("串口打开失败，无法下发指令CMD：{}", cmd);
                return;
            }
        }

        try {
            ServletUtils.getRequest().setAttribute("deviceCmd", cmd);
        } catch (Exception ignored) {}

        byte[] content = CodingUtil.hexToByte(cmd);
        int written = serialPort.writeBytes(content, content.length);

        if (written != content.length) {
            log.warn("指令写入不完整，期望写入 {} 字节，实际写入 {} 字节", content.length, written);
        }

        log.info("下发指令成功 imei：{} CMD：{} 长度：{} ", deviceImei, cmd,written);

        // 下发指令后延迟处理，避免设备响应不及时
        Threads.sleep(1000);
    }

    /**
     * 通道是否开启
     *
     * @return true 开启
     */
    public static Boolean isOpen() {
        // 未启用串口连接，不处理
        if (serialPort == null) {
            return false;
        }
        // 串口未开启则继续尝试
        if (!serialPort.isOpen()) {
            serialPort.openPort();
        }
        // 设备id一样，通道开启
//        return StrUtil.isNotBlank(deviceImei) && serialPort.isOpen();
        return serialPort.isOpen();
    }

    /**
     * 当前设备ID
     */
    public static String getImei() {
        return deviceImei;
    }
}
