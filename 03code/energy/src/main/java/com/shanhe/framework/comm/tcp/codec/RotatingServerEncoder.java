package com.shanhe.framework.comm.tcp.codec;

import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * 动环编码器
 *
 * @author wjh
 * @since 2025/3/17
 */
public class RotatingServerEncoder extends MessageToByteEncoder<String> {

    /**
     * 编码
     *
     * @param ctx 上下文
     * @param returnMsg 设备指令
     * @param out 操作的对象
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, String returnMsg, ByteBuf out) throws Exception {
        // 字符串转为二进制数组，输出
        out.writeBytes(CodingUtil.hexToByte(returnMsg));
    }
}
