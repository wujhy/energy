package com.shanhe.framework.comm;

import cn.hutool.core.util.StrUtil;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.framework.enums.TcpCharEnum;
import com.shanhe.framework.comm.tcp.model.DeviceData;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 服务端解码
 *
 * @author wjh
 * @since 2025/8/22
 */
@Slf4j
public class CommServerDecoder {

    /**
     * 粘包处理
     */
    private static final CacheKeyEnum stickyCache = CacheKeyEnum.STICKY;

    /**
     * 解析数据，粘包处理
     */
    public static void toDecode(String currentData, Integer headLength, List<Object> out) {
        String reqStr = currentData;
        // 起始位置、数据长度
        int startPosition = 0;
        int dataLen;
        try {
            // 存在粘包数据，先拼接数据
            String stickyBag = (String) CacheUtils.get(stickyCache.getCache(), stickyCache.getKey());
            if (StrUtil.isNotBlank(stickyBag)) {
                CacheUtils.remove(stickyCache.getCache(), stickyCache.getKey());
                reqStr = stickyBag + reqStr;
            }

            // 循环解析数据
            do {
                // 指令起始符
                String subHeader = reqStr.substring(startPosition, startPosition + TcpCharEnum._AA.getDictValue().length());
                // 指令请求内容
                reqStr = reqStr.substring(startPosition);
                // 切割完成后, 起点游标归位
                startPosition = 0;
                dataLen = reqStr.length();
                // 包含头部起始信息
                if (StrUtil.equals(TcpCharEnum._AA.getDictValue(), subHeader)) {
                    // 数据长度小于HEAD_LENGTH*2，则是不完整包，放入缓存并跳出循环，等待后续数据提交
                    if (dataLen < headLength * 2) {
                        CacheUtils.put(stickyCache.getCache(), stickyCache.getKey(), reqStr);
                        return;
                    }

                    // 获取完整指令长度
                    int endIndex = getLength(reqStr);
                    // 获取的数据长度小于指令长度，不完整包，放入缓存退出循环
                    if (dataLen < endIndex) {
                        CacheUtils.put(stickyCache.getCache(), stickyCache.getKey(), reqStr);
                        return;
                    }

                    // 截取完整的指令记录
                    String dataContent = reqStr.substring(startPosition, endIndex);
                    // 解析指令
                    DeviceData deviceData = toDataDevice(dataContent);
                    if (deviceData != null) {
                        out.add(deviceData);
                    }

                    // 计算剩余的 数据长度 和 偏移量（如果数据粘包，剩余数据长度dataLen大于0，继续循环取指令）
                    dataLen -= endIndex;
                    startPosition += endIndex;

                } else {
                    // 非起始标识，截取从起始标识开始的指令
                    if(reqStr.contains(TcpCharEnum._AA.getDictValue())){
                        //截取掉前部分数据
                        reqStr = reqStr.substring(reqStr.indexOf(TcpCharEnum._AA.getDictValue()));
                    } else {
                        log.error("无效数据 无上半包的情况：{}", reqStr);
                        return;
                    }
                }
            } while (dataLen > TcpCharEnum._AA.getDictValue().length());
        } catch (Exception e) {
            log.error("解包时出现错误:{} reqStr:{}", e.getMessage(), reqStr, e);
        }
    }

    /**
     * 指令解析为对象属性
     */
    private static DeviceData toDataDevice(String str) {

        // 截去指令起始符、校验和、结束符
        String dataStr = str.substring(2, str.length() - 4);
        // 指令校验和
        String checkSum = str.substring(str.length() - 4, str.length() - 2);

        // 计算的指令校验和
        String countCheckSum = CodingUtil.energyCheckSum(dataStr);
        if (!StrUtil.equals(countCheckSum, checkSum)) {
            log.info("指令校验和错误：{}，上传校验和：{}，服务器计算校验和：{}", dataStr, checkSum, countCheckSum);
            return null;
        }

        // 封装协议信息
        DeviceData deviceData = new DeviceData();
        deviceData.setC0(CodingUtil.hexStringToInteger(dataStr.substring(0, 2)));
        deviceData.setC1(CodingUtil.hexStringToInteger(dataStr.substring(2, 4)));
        deviceData.setC2(CodingUtil.hexStringToInteger(dataStr.substring(4, 6)));
        deviceData.setC3(dataStr.substring(6, 8));
        deviceData.setImei(dataStr.substring(8, 18));
        deviceData.setCid(dataStr.substring(18, 20));
        deviceData.setLength(dataStr.substring(20, 24));
        // 数据内容
        deviceData.setInfo(dataStr.substring(24));
        deviceData.setDataTime(System.currentTimeMillis());
        return deviceData;
    }

    /**
     * 获取指令长度（部分指令长度特殊处理）
     *
     * @param reqStr 指令
     * @return 指令长度
     */
    private static int getLength(String reqStr){
        // 截取指令长度，该长度是info的长度位数，16进制数据
        int length = Integer.parseInt(reqStr.substring(22, 26), 16);
        // 请求头5 + IMEI5 + CID1 + 长度2 + 内容 + 校验码1 + 请求尾1
        return (length + 15) * 2;
    }
}
