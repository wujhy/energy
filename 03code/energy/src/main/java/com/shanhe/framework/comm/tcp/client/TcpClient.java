package com.shanhe.framework.comm.tcp.client;

import cn.hutool.core.util.StrUtil;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.framework.comm.tcp.codec.RotatingClientDecoder;
import com.shanhe.framework.comm.tcp.codec.RotatingClientEncoder;
import com.shanhe.project.device.host.domain.Host;
import com.shanhe.project.sync.service.ClientDeviceService;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * tcp 客户端
 *
 * @author wjh
 * @since 2025/4/28
 */
@Slf4j
@Component
public class TcpClient {

    CacheKeyEnum hostCache = CacheKeyEnum.HOST;

    @Resource
    private ClientDeviceService clientDeviceService;

    public static Bootstrap bootstrap;

    public static Channel channel;

    public void setBootstrap(){
        bootstrap = new Bootstrap();
        bootstrap.group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class);
        bootstrap.handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) {
                ChannelPipeline pipeline = ch.pipeline();
//                pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
//                pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
                pipeline.addLast("decoder", new RotatingClientDecoder());
                pipeline.addLast("encoder", new RotatingClientEncoder());
                pipeline.addLast("handler", new TcpClientHandler(clientDeviceService));
            }
        });
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.option(ChannelOption.SO_REUSEADDR, true);
        bootstrap.option(ChannelOption.SO_LINGER, 3);
        // 使用池化分配器（重要性能优化）
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        // 自适应接收缓冲区
        bootstrap.option(ChannelOption.RCVBUF_ALLOCATOR,  new AdaptiveRecvByteBufAllocator());
    }

    public void getChannel() {
        // 通过主机获取服务端IP
        Host host = this.getHost();
        // 是否同步上报
        if (host == null || !Objects.equals(host.getNeedReport(), YesNoEnum.YES.getDictValue())) {
            return;
        }
        if (StrUtil.isBlank(host.getReportIp()) || Objects.isNull(host.getReportPort())) {
            return;
        }
        // 设置通道
        getChannel(host.getReportIp(), host.getReportPort());
    }

    public Boolean getChannel(String reportIp, Integer reportPort){
        AtomicReference<Boolean> isTrue = new AtomicReference<>(true);

        // 初始化
        if (bootstrap == null) {
            setBootstrap();
        }

        // 创建通道
        try {
            if (channel != null && !channel.isOpen()) {
                ChannelFuture closeFuture = channel.close();
                // 等待最多3秒
                boolean completed = closeFuture.awaitUninterruptibly(3, TimeUnit.SECONDS);
                if (!completed || !closeFuture.isSuccess()) {
                    // 可能需要强制关闭
                    channel.unsafe().closeForcibly();
                }
            }

            bootstrap.connect(reportIp, reportPort).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    channel = future.channel();
                    log.info("连接服务器{}:{}成功", reportIp, reportPort);
                    isTrue.set(true);
                } else {
                    isTrue.set(false);
                }
            }).sync();
        } catch (Exception e) {
            log.error("连接服务器{}:{}失败", reportIp, reportPort);
            isTrue.set(false);
        }

        return isTrue.get();
    }

    /**
     * 发送消息
     */
    public void sendMsg(String msg) {
        try {
            if (channel == null || !channel.isOpen()) {
                getChannel();
            }
            if(channel != null && channel.isOpen()) {
                channel.writeAndFlush(msg).addListeners((ChannelFutureListener) arg0 -> {
                    if (arg0.isSuccess()) {
                        log.debug("上报数据成功：{}", msg);
                    } else {
                        Throwable cause = arg0.cause();
                        if (cause != null) {
                            log.error("上报数据失败：{}，原因：", msg, cause);
                        }
                    }
                });
            }
        } catch (Exception e) {
            log.error("消息发送失败：{}", e.getMessage());
        }
    }

    /**
     * 通道是否建立
     *
     * @return 结果
     */
    public Boolean isOpen() {
        return channel != null && channel.isOpen();
    }

    public Host getHost() {
        return (Host) CacheUtils.get(hostCache.getCache(), hostCache.getKey());
    }

    /**
     * 关闭netty
     */
    @PreDestroy
    public void stop() {
        try {
            // 关闭channel并释放所有资源
            if (channel != null) {
                channel.close();
            }
            // 关闭bootstrap，释放所有资源
            if (bootstrap != null) {
                bootstrap.config().group().shutdownGracefully();
            }
        } catch (Exception e) {
            log.error("关闭tcp client失败，异常信息：{}", e.getMessage());
        }
    }
}
