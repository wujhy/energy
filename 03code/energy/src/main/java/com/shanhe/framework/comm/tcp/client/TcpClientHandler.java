package com.shanhe.framework.comm.tcp.client;

import com.shanhe.project.sync.domain.RequestVo;
import com.shanhe.project.sync.service.ClientDeviceService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 客户端通道解析
 *
 * @author wjh
 * @since 2025/7/19
 */
public class TcpClientHandler extends SimpleChannelInboundHandler<Object> {

	private static final Logger log = LoggerFactory.getLogger(TcpClientHandler.class);

	/**
	 * 注入实现类
	 */
	private final ClientDeviceService clientDeviceService;

	public TcpClientHandler(ClientDeviceService clientDeviceService) {
		this.clientDeviceService = clientDeviceService;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		// 通道建立时
		log.debug("TcpClientHandler.channelActive");
		ctx.flush();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
		clientDeviceService.readByTcp((RequestVo)msg);
//		ReferenceCountUtil.release(msg);
	}
}  