package com.shanhe.project.monitor.server.service;

import cn.hutool.core.util.StrUtil;
import com.shanhe.common.exception.ServiceException;
import com.shanhe.common.utils.IpUtils;
import com.shanhe.framework.consts.SysConst;
import com.shanhe.framework.enums.IpAddrEnum;
import com.shanhe.project.device.host.domain.Host;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.*;

/**
 * 系统服务
 *
 * @author wjh
 * @since 2025/5/6
 */
public class SystemService {
    private static final Logger logger = LoggerFactory.getLogger(SystemService.class);

    /**
     * 更新本地IP
     */
    public static void updateConnectionIp(Host host) {
        try {
            Process process;
            if(isWin()) {
                return;
            }

            String ipName = IpAddrEnum.findLabelByValue(host.getIpAddr());
            if(StrUtil.isEmpty(ipName)) {
                throw new ServiceException("未找到IP地址对应的连接名称！");
            }

            // 输出网关配置
            StringBuilder command = new StringBuilder();
            // 连接名称
            command.append("sudo nmcli connection modify '").append(ipName).append("' ");
            // IP地址/子网掩码（CIDR格式）
            command.append("ipv4.addresses ").append(host.getIp()).append("/").append(IpUtils.subIpToInt(host.getSubIp())).append(" ");
            // 网关配置
            command.append("ipv4.gateway ").append(host.getNetIp()).append(" ");
            // DNS服务器（空格分隔）
            command.append("ipv4.dns '8.8.8.8 8.8.4.4' ");
            // 设置为静态IP
            command.append("ipv4.method manual");
            logger.info("更新本机IP指令 {}", command);
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command.toString());
            process = pb.start();
            logger.debug("更新本机IP指令结果 {} ：{}", process.waitFor(), command);
            if (!Objects.equals(process.waitFor(), 0)) {
                throw new ServiceException("更新本机IP失败！");
            }

            // 清空
            command.setLength(0);

            // 重启连接
            command.append("sudo nmcli connection down '").append(ipName).append("' && ");
            command.append("sudo nmcli connection up '").append(ipName).append("'");
            logger.info("生效本机IP指令 {}", command);
            asyncExeLocalCmd(null, new ProcessBuilder("bash", "-c", command.toString()));
        } catch (Exception e) {
            throw new ServiceException("更新本机IP失败！");
        }
    }

    /**
     * 获取本机IP
     */
    public static void getIp(Host host) {
        try {
            if (StrUtil.isBlank(host.getIpAddr()) || isWin() ) {
                return;
            }
            NetworkInterface networkInterface = NetworkInterface.getByName(host.getIpAddr());
            // 网卡不存在或未设置IP，置空
            if (networkInterface == null || networkInterface.getInterfaceAddresses() == null
                    || networkInterface.getInterfaceAddresses().isEmpty()) {
                throw new ServiceException("获取IP异常，请检查网络接口是否正确！");
            }
            // 获取IP
            for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                if (interfaceAddress.getAddress() instanceof Inet4Address) {

                    // ip不一样才填充
                    if (!StrUtil.equals(host.getIp(), interfaceAddress.getAddress().getHostAddress())) {
                        host.setIp(interfaceAddress.getAddress().getHostAddress());
                        host.setSubIp(IpUtils.calcMaskByPrefixLength(interfaceAddress.getNetworkPrefixLength()));
                        host.setNetIp(IpUtils.calcSubnetAddress(interfaceAddress.getAddress(), host.getSubIp()));
                    }

                    byte[] mac = networkInterface.getHardwareAddress();
                    StringBuilder macAddress = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        macAddress.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));
                    }

                    host.setMac(macAddress.toString());
                    //host.setNetIp(interfaceAddress.getBroadcast() != null ? interfaceAddress.getBroadcast().getHostAddress() : "");
                    host.setPort(SysConst.port);
                    logger.info("获取IP成功：{}, {}, {}, {}, {}", host.getIp(), host.getSubIp(), host.getNetIp(), host.getPort(), host.getMac());
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("获取IP异常：{}", e.getMessage());
        }
    }

    /**
     * 同步服务器时间
     *
     * @param datetime 格式化的日志 yyyyMMddHHmmss
     */
    public static void syncServerTime(String datetime) {
        // 先更新本机服务时间
        try {
            String date = datetime.substring(0,8);
            String time = datetime.substring(8,10) + ":" + datetime.substring(10,12) + ":" + datetime.substring(12,14);

            if(isWin()) {
                Runtime.getRuntime().exec("cmd /c date " + date);
                Runtime.getRuntime().exec("cmd /c time " + time);
            } else {
                // 更新本机服务器时间
                Process process;
                String command = String.format("sudo date -s '%s-%s-%s %s'", date.substring(0, 4), date.substring(4, 6), date.substring(6, 8), time);
                ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
                process = pb.start();
                logger.info("更新本机服务器时间 {} ：{}", process.waitFor(), command);
                if (!Objects.equals(process.waitFor(), 0)) {
                    throw new ServiceException("更新服务器时间失败！");
                }

                // 生效
                pb = new ProcessBuilder("bash", "-c", "hwclock -w");
                process = pb.start();
                logger.info("更新本机硬件时间 {} ：{}", process.waitFor(), "hwclock -w");
            }
            logger.info("syncServerTime---->{}", datetime);
        } catch (Exception e) {
            throw new ServiceException("执行脚本失败！");
        }
    }

    /**
     * 开启浏览器客户端
     */
    public static void chromiumApp() {
        try {
            if(isWin()) {
                return;
            }
            // 执行脚本方法
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", SysConst.getScriptFilePath(), "chromium");
            // 异步执行
            asyncExeLocalCmd(new File("/opt/energy/logs/sys-user.log"), processBuilder);
        } catch (Exception e) {
            throw new ServiceException("开启浏览器客户端脚本失败！");
        }
    }

    /**
     * 开启看门狗
     */
    public static void openWatchDog() {
        try {
            if(isWin()) {
                return;
            }
            String command = "echo 1 >/sys/class/gzpeite/user/watch_dog";
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            Process process = pb.start();
            logger.info("开启看门狗指令 {} ：{}", process.waitFor(), command);
            if (!Objects.equals(process.waitFor(), 0)) {
                throw new ServiceException("开启看门狗失败！");
            }
        } catch (Exception e) {
            throw new ServiceException("开启看门狗脚本失败！");
        }
    }

    /**
     * 喂狗
     */
    public static void feedWatchDog() {
        try {
            if(isWin()) {
                return;
            }
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", "echo 2 >/sys/class/gzpeite/user/watch_dog");
            Process process = pb.start();
            if (!Objects.equals(process.waitFor(), 0)) {
                throw new ServiceException("喂狗失败！");
            }
        } catch (Exception e) {
            throw new ServiceException("喂狗失败，未开启！");
        }
    }

    /**
     * 关闭看门狗
     */
    public static void closeWatchDog() {
        try {
            if(isWin()) {
                return;
            }
            String command = "echo 0 >/sys/class/gzpeite/user/watch_dog";
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            Process process = pb.start();
            logger.info("关闭看门狗指令 {} ：{}", process.waitFor(), command);
            if (!Objects.equals(process.waitFor(), 0)) {
                throw new ServiceException("关闭看门狗失败！");
            }
        } catch (Exception e) {
            throw new ServiceException("关闭看门狗脚本失败！");
        }
    }

    /**
     * 重启浏览器客户端
     */
    public static void resChromiumApp() {
        try {
            if(isWin()) {
                return;
            }
            // 执行脚本方法
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", SysConst.getScriptFilePath(), "restartChromium");
            // 异步执行
            asyncExeLocalCmd(new File("/opt/rotating/logs/sys-user.log"), processBuilder);
        } catch (Exception e) {
            throw new ServiceException("开启浏览器客户端脚本失败！");
        }
    }

    /**
     * 触发升级
     */
    public static void deploy() {
        try {
            if(isWin()) {
                return;
            }
            ProcessBuilder sh = new ProcessBuilder("/bin/bash", SysConst.getScriptFilePath(), "deploy");
//            ProcessBuilder sh = new ProcessBuilder("systemctl reload energy.service");
//            Runtime.getRuntime().exec("nohup /bin/bash -c '/opt/energy/energy.sh deploy' > /opt/energy/logs/sys-script.log 2>&1 &");
            asyncExeLocalCmd(new File("/opt/energy/logs/sys-script.log"), sh);
        } catch (Exception e) {
            throw new ServiceException("执行升级脚本失败！");
        }
    }

    /**
     * 判断当前操作系统是否为Windows
     */
    public static Boolean isWin() {
        return StrUtil.containsIgnoreCase(System.getProperty("os.name"), "win");
    }

    /**
     * 异步执行本地脚本
     *
     * @param file 文件
     * @param pb 命令
     * @throws IOException 异常
     */
    public static void asyncExeLocalCmd(File file, ProcessBuilder pb) throws IOException {
        // 不使用Runtime.getRuntime().exec(command)的方式,因为无法设置以下特性
        // Java执行本地命令是启用一个子进程处理,默认情况下子进程与父进程I/O通过管道相连(默认ProcessBuilder.Redirect.PIPE)
        // 当服务执行自身重启的命令时,父进程关闭导致管道连接中断,将导致子进程也崩溃,从而无法完成后续的启动
        // 解决方式,(1)设置子进程IO输出重定向到指定文件;(2)设置属性子进程的I/O源或目标将与当前进程的相同,两者相互独立
        if (file == null || !file.exists()) {
            // 设置属性子进程的I/O源或目标将与当前进程的相同,两者相互独立
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
        } else {
            // 设置子进程IO输出重定向到指定文件
            // 错误输出与标准输出,输出到一块
            pb.redirectErrorStream(true);
            // 设置输出日志
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(file));
        }
        // 执行命令进程
        pb.start();
    }

}
