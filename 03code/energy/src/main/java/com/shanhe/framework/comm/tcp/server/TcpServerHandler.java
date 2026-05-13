package com.shanhe.framework.comm.tcp.server;

import cn.hutool.core.util.StrUtil;
import com.shanhe.common.utils.ServletUtils;
import com.shanhe.common.utils.Threads;
import com.shanhe.framework.comm.tcp.model.DeviceData;
import com.shanhe.framework.enums.HostTypeEnum;
import com.shanhe.framework.enums.TcpCidEnum;
import com.shanhe.project.iot.service.DeviceService;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * tcp通道解析
 *
 * @author wjh
 * @since 2025/3/17
 */
public class TcpServerHandler extends SimpleChannelInboundHandler<Object> {
    private static final Logger logger = LoggerFactory.getLogger(TcpServerHandler.class);

    /** 设备注册通道 */
    private static Channel deviceChannel;
    /** 设备IMEI */
    private static String deviceImei;
    /** 注入设备消费方法 */
    private final DeviceService deviceService;
    /**
     * 构造方法，注入
     */
    public TcpServerHandler(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    /**
     * 读管道数据
     * @param ctx 通道上下文
     * @param msg 通道消息内容
     */
    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        ctx.channel().eventLoop().execute(() -> {
            try{
                DeviceData deviceData = (DeviceData) msg;
                deviceData.setDeviceModel(HostTypeEnum._2CM03N.getDictValue());

                // 是否新的设备或连接
                boolean isNewDevice = deviceChannel == null || StrUtil.isBlank(deviceImei)
                        || !Objects.equals(deviceChannel.hashCode(), ctx.channel().hashCode())
                        || !StrUtil.equals(deviceImei, deviceData.getImei());
                if (isNewDevice) {
                    // 更新上线信息
                    this.onLine(deviceData, ctx.channel());
                    return;
                }

                // 通道及设备已经注册，接收处理所有指令
                deviceService.tcpDevice(deviceData);
            } catch (Exception e){
                logger.error("通道数据消费异常：{}", e.getMessage(), e);
            }
        });
    }

    /**
     * 注册上线
     *
     * @param deviceData 设备参数
     * @param channel 通道
     */
    private void onLine(DeviceData deviceData, Channel channel) {
        // 非注册包，不处理
        if (StrUtil.isBlank(deviceData.getImei())
                || !StrUtil.equals(deviceData.getCid(), TcpCidEnum._88.getDictValue())) {
            return;
        }
        // 记录最新设备及通道
        deviceChannel = channel;
        deviceImei = deviceData.getImei();

        // 如果是注册，补充地址
        if(channel.remoteAddress() != null) {
            InetSocketAddress socket = (InetSocketAddress) channel.remoteAddress();
            deviceData.setIp(socket.getHostName());
            deviceData.setPort(socket.getPort());
        }

        // 设备上线处理（校验缓存）
        deviceService.tcpDeviceOnline(deviceData);
    }

    /**
     * 设备下线处理
     *
     * @param channel 通道
     */
    public synchronized void offLine(Channel channel) {
        // 如果下线的是当前设备通道
        if (deviceChannel != null && Objects.equals(channel.hashCode(), deviceChannel.hashCode())) {
            logger.debug("移除通道 imei --> {} --> 通道ID --> {}", deviceImei, channel.hashCode());

            deviceImei = null;
            deviceChannel = null;
            try {
                channel.disconnect();
                channel.close();
            } catch (Exception e) {
                logger.error("移除通道异常：{}", e.getMessage());
            }

            // 更新资产不在线
            deviceService.tcpDeviceOffline();
        }
    }

    /**
     * 给设备下发指令
     *
     * @param cmd 指令
     */
    public static synchronized void returnCmd(String cmd) {
        try {
            ServletUtils.getRequest().setAttribute("deviceCmd", cmd);
        } catch (Exception ignored) {}

        if (deviceChannel == null || !deviceChannel.isOpen()) {
            logger.debug("尚未与设备建立连接，无法下发指令CMD：{}", cmd);
            return;
        }
        deviceChannel.writeAndFlush(cmd).addListeners((ChannelFutureListener) arg0 -> {
            if (arg0.isSuccess()) {
                logger.info("下发指令成功 imei：{} CMD：{}", deviceImei, cmd);
            } else {
                Throwable cause = arg0.cause();
                if (cause != null) {
                    logger.error("下发指令失败 imei：{} CMD：{}，原因：", deviceImei, cmd, cause);
                }
            }
        });

        // 下发指令后延迟处理，避免设备响应不及时
        Threads.sleep(1000);
    }

    /**
     * 通道是否开启
     *
     * @return true 开启
     */
    public static Boolean isOpen() {
        // 设备id一样，通道开启
        return StrUtil.isNotBlank(deviceImei) && deviceChannel != null && deviceChannel.isOpen();
    }

    /**
     * 当前设备ID
     */
    public static String getImei() {
        return deviceImei;
    }

    /**
     * 读数据完成
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        logger.debug("数据读取完成，hashcode->{}", ctx.channel().hashCode());
    }

    /**
     * 定时心跳检测，做下线处理
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        // 处理IdleState.READER_IDLE时间
        if(evt instanceof IdleStateEvent) {
            IdleState idleState = ((IdleStateEvent) evt).state();
            // 如果是触发的是读空闲时间，说明已经超过n秒没有收到客户端心跳包，下线处理
            if(idleState == IdleState.READER_IDLE) {
                this.offLine(ctx.channel());
            }
        }
    }

    /**
     * 连接异常
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // 下线处理
        this.offLine(ctx.channel());
    }
}
