package com.shanhe.framework.comm.tcp.codec;

import cn.hutool.core.util.StrUtil;
import com.shanhe.framework.comm.CommServerDecoder;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 动环解码器
 *
 * @author wjh
 * @since 2025/3/17
 */
@Slf4j
public class RotatingServerDecoder extends ByteToMessageDecoder {
    /** 指令头字节长度 */
    public static final Integer HEAD_LENGTH = 13;

    /**
     * 解码
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        try {
            // 判断是否存在可读数据
            if (!in.isReadable()) {
                return;
            }
            // 检查是否有足够的字节读取头部，长度不足继续等待
            if (in.readableBytes() < HEAD_LENGTH) {
                return;
            }
            byte[] req = new byte[in.readableBytes()];
            in.readBytes(req);
            // 定义收到数据的字符串
            String reqStr = CodingUtil.bytesToHexString(req).toUpperCase();
            log.info("收到数据包解析成字符串:{}", reqStr);
            if (StrUtil.isBlank(reqStr)) {
                return;
            }

            // 执行解码
            CommServerDecoder.toDecode(reqStr, HEAD_LENGTH, out);
        } catch (Exception e) {
            log.error("解码异常", e);
        }
    }
}
