package com.shanhe.framework.comm.tcp.client;

import com.shanhe.project.sync.domain.RequestVo;
import com.shanhe.project.sync.service.ClientDeviceService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * 客户端通道解析
 *
 * @author wjh
 * @since 2025/7/19
 */
@Slf4j
public class TcpClientHandler extends SimpleChannelInboundHandler<Object> {

	/** 注入实现类 */
	private final ClientDeviceService clientDeviceService;

	public TcpClientHandler(ClientDeviceService clientDeviceService) {
		this.clientDeviceService = clientDeviceService;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		// 通道建立时
		log.debug("TCP客户端通道已激活");
		ctx.flush();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
		clientDeviceService.readByTcp((RequestVo)msg);
	}
}
