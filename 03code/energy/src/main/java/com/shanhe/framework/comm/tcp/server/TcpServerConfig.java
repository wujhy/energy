package com.shanhe.framework.comm.tcp.server;

import com.shanhe.framework.consts.DeviceCommConst;
import com.shanhe.framework.comm.tcp.codec.RotatingServerDecoder;
import com.shanhe.framework.comm.tcp.codec.RotatingServerEncoder;
import com.shanhe.project.iot.service.DeviceService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * netty TCP 配置
 *
 * @author wjh
 * @since 2025/2/5
 */
@Slf4j
@Order(3)
@Component
public class TcpServerConfig implements ApplicationRunner {

    @Resource
    private DeviceCommConst deviceCommConst;
    @Resource
    DeviceService deviceService;

    // 用于监听端口并接受新的连接
    private ChannelFuture channelFuture;
    // 处理客户端连接的线程组
    private EventLoopGroup bossGroup;
    // 处理网络读写的线程组
    private EventLoopGroup workerGroup;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            // 开启TCP通讯
            if (!deviceCommConst.isTcp()) {
                return;
            }

            // 创建线程组
            bossGroup = new NioEventLoopGroup(deviceCommConst.getTcpBossThread());
            workerGroup = new NioEventLoopGroup(deviceCommConst.getTcpWorkerThread());

            ServerBootstrap tcpServer = new ServerBootstrap();
            // 线程组设置
            tcpServer.group(bossGroup, workerGroup);
            // 使用NIO的传输Channel
            tcpServer.channel(NioServerSocketChannel.class);
            /*
             * option设置bossGroup接收线程组，
             * childOption设置workerGroup发送线程组
             */
            // 客户端连接等待队列数
            tcpServer.option(ChannelOption.SO_BACKLOG, deviceCommConst.getTcpBackLog());
            // 允许重复使用本地地址和端口，端口被占用或未释放时不会报错
            tcpServer.option(ChannelOption.SO_REUSEADDR , true);
            // 是否开启TCP底层心跳机制，true表示开启，设置活动保持连接状态
            tcpServer.childOption(ChannelOption.SO_KEEPALIVE,true);
            // 缓冲区大小，用于保存接收、发送数据，直到处理成功
            tcpServer.option(ChannelOption.SO_RCVBUF, 64 * 1024)
                    .childOption(ChannelOption.SO_SNDBUF, 32 * 1024);
            // 使用对象池，重用缓冲区
            tcpServer.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

            // 通道配置 数据 处理 方式
            tcpServer.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) {
                    ChannelPipeline pipeline = ch.pipeline();
                    // 解码处理
                    pipeline.addLast(new RotatingServerDecoder());
                    // 编码处理
                    pipeline.addLast("encoder", new RotatingServerEncoder());
                    /*
                     * 心跳检测
                     * 在服务器端会每隔N秒检查channelRead方法被调用的情况，如果N秒内channelRead没有被触发就会调用userEventTriggered方法 intervalTime
                     */
                    pipeline.addLast(new IdleStateHandler(deviceCommConst.getTcpIntervalTime(), 0, 0, TimeUnit.SECONDS));
                    //设置2min的超时时间，如果某个通道2min内未发送信号，则抛出异常删除当前通道
//                        pipeline.addLast(new ReadTimeoutHandler(tcpConst.getIntervalTime()));
                    // 解析结果处理
                    pipeline.addLast(new TcpServerHandler(deviceService));
                }
            });

            // 绑定端口并开始接受连接，同步等待直到绑定完成
            channelFuture = tcpServer.bind(deviceCommConst.getTcpPost()).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    log.info("Netty启动成功，绑定端口:{}", deviceCommConst.getTcpPost());
                } else {
                    log.error("Netty启动异常：{}", future.cause().getMessage());
                }
            }).sync();
        } catch (InterruptedException e) {
            log.error("创建tcp失败，异常信息：{}", e.getMessage());
            // 处理中断异常，恢复中断状态以便上层调用者可以感知到中断的发生。
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 关闭netty
     */
    @PreDestroy
    public void stop() throws Exception {
        try {
            // 关闭channel并释放所有资源
            if (channelFuture != null) {
                channelFuture.channel().close();
            }
            // 关闭EventLoopGroup，释放所有资源
            if (bossGroup != null) {
                bossGroup.shutdownGracefully();
            }
            // 关闭EventLoopGroup，释放所有资源
            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
            }
        } catch (Exception e) {
            log.error("关闭tcp失败，异常信息：{}", e.getMessage());
            throw e;
        }
    }
}
