package com.shanhe.common.utils;

import cn.hutool.core.util.StrUtil;
import com.shanhe.common.exception.ServiceException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import javax.servlet.http.HttpServletRequest;

/**
 * 获取IP方法
 *
 * @author wjh
 * @since 2025/6/6
 */
public class IpUtils
{
    private static final String INTERNAL_IP = "127.0.0.1";

    public static String getIpAddr(HttpServletRequest request)
    {
        if (request == null) {
            return "unknown";
        }
        String ip = request.getHeader("x-forwarded-for");
        if (StrUtil.isBlank(ip) || StrUtil.equalsIgnoreCase(ip, "unknown")) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || StrUtil.equalsIgnoreCase(ip, "unknown")) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (StrUtil.isBlank(ip) || StrUtil.equalsIgnoreCase(ip, "unknown")) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || StrUtil.equalsIgnoreCase(ip, "unknown")) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StrUtil.isBlank(ip) || StrUtil.equalsIgnoreCase(ip, "unknown")) {
            ip = request.getRemoteAddr();
        }

        return "0:0:0:0:0:0:0:1".equals(ip) ? INTERNAL_IP : ip;
    }

    public static boolean internalIp(String ip)
    {
        if (StrUtil.isBlank(ip) || StrUtil.equals(ip, INTERNAL_IP)) {
            return true;
        }
        byte[] addr = textToNumericFormatV4(ip);
        return internalIp(addr);
    }

    private static boolean internalIp(byte[] addr)
    {
        if (StringUtils.isNull(addr) || addr.length < 2)
        {
            return true;
        }
        final byte b0 = addr[0];
        final byte b1 = addr[1];
        // 10.x.x.x/8
        final byte section1 = 0x0A;
        // 172.16.x.x/12
        final byte section2 = (byte) 0xAC;
        final byte section3 = (byte) 0x10;
        final byte section4 = (byte) 0x1F;
        // 192.168.x.x/16
        final byte section5 = (byte) 0xC0;
        final byte section6 = (byte) 0xA8;
        switch (b0)
        {
            case section1:
                return true;
            case section2:
                if (b1 >= section3 && b1 <= section4)
                {
                    return true;
                }
            case section5:
                if (b1 == section6) {
                    return true;
                }
            default:
                return false;
        }
    }

    /**
     * 将IPv4地址转换成字节
     *
     * @param text IPv4地址
     * @return byte 字节
     */
    public static byte[] textToNumericFormatV4(String text)
    {
        if (StrUtil.isBlank(text)) {
            return null;
        }

        byte[] bytes = new byte[4];
        String[] elements = text.split("\\.", -1);
        try
        {
            long l;
            int i;
            switch (elements.length)
            {
                case 1:
                    l = Long.parseLong(elements[0]);
                    if ((l < 0L) || (l > 4294967295L)) {
                        return null;
                    }
                    bytes[0] = (byte) (int) (l >> 24 & 0xFF);
                    bytes[1] = (byte) (int) ((l & 0xFFFFFF) >> 16 & 0xFF);
                    bytes[2] = (byte) (int) ((l & 0xFFFF) >> 8 & 0xFF);
                    bytes[3] = (byte) (int) (l & 0xFF);
                    break;
                case 2:
                    l = Integer.parseInt(elements[0]);
                    if ((l < 0L) || (l > 255L)) {
                        return null;
                    }
                    bytes[0] = (byte) (int) (l & 0xFF);
                    l = Integer.parseInt(elements[1]);
                    if ((l < 0L) || (l > 16777215L)) {
                        return null;
                    }
                    bytes[1] = (byte) (int) (l >> 16 & 0xFF);
                    bytes[2] = (byte) (int) ((l & 0xFFFF) >> 8 & 0xFF);
                    bytes[3] = (byte) (int) (l & 0xFF);
                    break;
                case 3:
                    for (i = 0; i < 2; ++i)
                    {
                        l = Integer.parseInt(elements[i]);
                        if ((l < 0L) || (l > 255L)) {
                            return null;
                        }
                        bytes[i] = (byte) (int) (l & 0xFF);
                    }
                    l = Integer.parseInt(elements[2]);
                    if ((l < 0L) || (l > 65535L)) {
                        return null;
                    }
                    bytes[2] = (byte) (int) (l >> 8 & 0xFF);
                    bytes[3] = (byte) (int) (l & 0xFF);
                    break;
                case 4:
                    for (i = 0; i < 4; ++i)
                    {
                        l = Integer.parseInt(elements[i]);
                        if ((l < 0L) || (l > 255L)) {
                            return null;
                        }
                        bytes[i] = (byte) (int) (l & 0xFF);
                    }
                    break;
                default:
                    return null;
            }
        }
        catch (NumberFormatException e)
        {
            return null;
        }
        return bytes;
    }

    /**
     * 取本机IP
     */
    public static String getHostIp()
    {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ignored) {}
        return "127.0.0.1";
    }

    /**
     * 取本机网络名
     */
    public static String getHostName()
    {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ignored) { }
        return "未知";
    }

    /**
     * 长度转IP掩码格式
     */
    public static String calcMaskByPrefixLength(int length) {
        int mask = -1 << (32 - length);
        int partsNum = 4;
        int bitsOfPart = 8;
        int[] maskParts = new int[partsNum];
        int selector = 0x000000ff;

        for (int i = 0; i < maskParts.length; i++) {
            int pos = maskParts.length - 1 - i;
            maskParts[pos] = (mask >> (i * bitsOfPart)) & selector;
        }

        StringBuilder result = new StringBuilder();
        result.append(maskParts[0]);
        for (int i = 1; i < maskParts.length; i++) {
            result.append(".").append(maskParts[i]);
        }
        return result.toString();
    }

    /**
     * 通过IP、掩码计算网关IP（广播地址加1）
     */
    public static String calcSubnetAddress(InetAddress ipAddress, String mask) {
        StringBuilder result = new StringBuilder();
        try {
            // calc sub-net IP
            InetAddress maskAddress = InetAddress.getByName(mask);

            byte[] ipRaw = ipAddress.getAddress();
            byte[] maskRaw = maskAddress.getAddress();

            int unsignedByteFilter = 0x000000ff;
            int[] resultRaw = new int[ipRaw.length];
            for (int i = 0; i < resultRaw.length; i++) {
                resultRaw[i] = (ipRaw[i] & maskRaw[i] & unsignedByteFilter);
            }

            // make result string
            result.append(resultRaw[0]);
            for (int i = 1; i < resultRaw.length; i++) {
                result.append(".").append(i == 3 ? resultRaw[i] + 1 : resultRaw[i]);
            }
        } catch (UnknownHostException ignored) {}
        return result.toString();
    }

    /**
     * IP转整数
     */
    public static int ipToInteger(InetAddress address) {
        byte[] addrBytes = address.getAddress();
        return ((addrBytes[0] & 0xFF) << 24) | ((addrBytes[1] & 0xFF) << 16) | ((addrBytes[2] & 0xFF) << 8) | (addrBytes[3] & 0xFF);
    }

    /**
     * 子IP转数值
     *
     * @param subIp 子网掩码
     * @return CIDR格式
     */
    public static int subIpToInt(String subIp) throws UnknownHostException {
        try {
            byte[] maskBytes = InetAddress.getByName(subIp).getAddress();
            int maskInt = 0;
            for (byte maskByte : maskBytes) {
                int mask = maskByte & 0xFF;
                while (mask != 0) {
                    if ((mask & 1) == 1) {
                        maskInt++;
                    }
                    mask >>= 1;
                }
            }
            return maskInt;
        } catch (Exception e) {
            throw new ServiceException("子网掩码格式错误");
        }
    }

    /**
     * 计算广播IP
     */
    public static String broadcastIp(int ipInt, int maskInt) {
        try {

            // 应用反码掩码
            int broadcastInt = ipInt | (~maskInt);
            InetAddress inetAddress = integerToIp(broadcastInt);
            return inetAddress.getHostAddress();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 整数转IP
     */
    public static InetAddress integerToIp(int ipInt) throws UnknownHostException {
        byte[] addrBytes = new byte[] {
                (byte) ((ipInt >> 24) & 0xFF),
                (byte) ((ipInt >> 16) & 0xFF),
                (byte) ((ipInt >> 8) & 0xFF),
                (byte) (ipInt & 0xFF)
        };
        return InetAddress.getByAddress(addrBytes);
    }

    public static boolean telnet(String hostname, int port, int timeout) {
        Socket socket = new Socket();
        boolean isConnected = false;
        try {
            // 建立连接
            socket.connect(new InetSocketAddress(hostname, port), timeout);
            // 通过现有方法查看连通状态
            isConnected = socket.isConnected();
        } catch (IOException e) {
            // 当连不通时，直接抛异常，异常捕获即可
        } finally {
            try {
                // 关闭连接
                socket.close();
            } catch (IOException ignored) {
            }
        }
        return isConnected;
    }

}
