package com.shanhe.framework.comm.tcp.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.CharsetUtil;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 动环编码器
 *
 * @author wjh
 * @since 2025/3/17
 */
public class RotatingClientEncoder extends MessageToByteEncoder<String> {

    /**
     * 编码
     *
     * @param ctx 上下文
     * @param returnMsg 设备指令
     * @param out 操作的对象
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, String returnMsg, ByteBuf out) throws Exception {
        // 数据编码
        String base64String = Base64.getEncoder().encodeToString(returnMsg.getBytes(CharsetUtil.UTF_8));
        // 写入长度
        out.writeInt(base64String.length());
        // 写入数据
        out.writeBytes(base64String.getBytes(StandardCharsets.UTF_8));
    }
}
