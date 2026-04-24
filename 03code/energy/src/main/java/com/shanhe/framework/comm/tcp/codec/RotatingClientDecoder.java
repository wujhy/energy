package com.shanhe.framework.comm.tcp.codec;

import com.alibaba.fastjson.JSONObject;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import com.shanhe.project.sync.domain.RequestVo;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.List;

/**
 * 动环解码器
 *
 * @author wjh
 * @since 2025/3/17
 */
public class RotatingClientDecoder extends ByteToMessageDecoder {

    private static final Logger logger = LoggerFactory.getLogger(RotatingClientDecoder.class);
    /** 字节长度 */
    public static final Integer HEAD_LENGTH = 50;
    public static final Integer MAX_LENGTH = 1024 * 1024;

    // 解码
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (!in.isReadable()) {
            return;
        }

        //读取数据
        try {
            // 检查是否有足够的字节，长度不足继续等待
            if (in.readableBytes() < HEAD_LENGTH) {
                return;
            }
            // 标记 ByteBuf 读指针位置
            in.markReaderIndex();
            // 读取长度
            int len = in.readInt();
            // 可读字节数少于消息字节数，考虑数据丢失，分包
            if (in.readableBytes() < len) {
                if (len > MAX_LENGTH) {
                    // 数据长度超过最大长度，可能数据损坏，清除数据
                    in.clear();
                } else {
                    // 先重置 ByteBuf 读指针位置，等待新的数据提交
                    in.resetReaderIndex();
                }
                return;
            }

            // 读取内容
            byte[] req = new byte[len];
            in.readBytes(req);

            // 解码
            String reqStr = CodingUtil.bytesToString(Base64.getMimeDecoder().decode(req),"UTF-8");
            logger.debug("收到数据包解析成字符串:{}", reqStr);

            // 执行解码
            out.add(JSONObject.parseObject(reqStr, RequestVo.class));
        } catch (Exception e) {
            logger.error("接收服务端消息异常：{}", e.getMessage());
            // 出现异常时，清除数据
            in.clear();
        }
    }
}
