package com.shanhe.framework.consts;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * 系统配置
 *
 * @author wjh
 * @since 2024/12/17
 */
@Data
@Configuration
@Lazy(false)
public class SysConst {
    /**
     * 系统名称
     */
    public static String name;
    /**
     * 应用包名称
     */
    public static String applicationName;
    /**
     * 应用端口号
     */
    public static Integer port;
    /**
     * 系统版本
     */
    public static String version;
    /**
     * 系统上传路径
     */
    public static String profile;
    /**
     * 网卡名称
     */
    public static String networkCardName;
    /**
     * 获取ip地址开关
     */
    public static Boolean addressEnabled = false;
    /**
     * 用户管理-账号初始密码
     */
    public static String initPassword = "888888";
    /**
     * 主机管理-账号初始密码
     */
    public static String initHostPassword = "shanHe168";
    /**
     * 项目部署根路径
     */
    public static String deployPath;
    /**
     * 项目部署日期yyyyMMdd
     */
    public static String deployDay;

    /**
     * 获取导入上传路径
     */
    public static String getImportPath() {
        return profile + "/import";
    }
    /**
     * 获取下载路径
     */
    public static String getDownloadPath() {
        return profile + "/download/";
    }

    public static String getSoftDownloadPath() {
        return String.format("%s/softDownload/%s.jar", profile, applicationName);
    }
    /**
     * 获取上传路径
     */
    public static String getUploadPath() {
        return profile + "/upload";
    }
    /**
     * 项目配置路径
     */
    public static String getConfigPath() {
        return deployPath + "/config";
    }
    /**
     * 项目脚本
     */
    public static String getScriptFilePath() {
        return "/opt/energy/energy.sh";
    }


    @Value("${sys.name:蓄电池监控系统}")
    public void setName(String name) {
        SysConst.name = name;
    }

    @Value("${spring.application.name}")
    public void setApplicationName(String applicationName) {
        SysConst.applicationName = applicationName;
    }

    @Value("${server.port:8080}")
    public void setPort(Integer port) {
        SysConst.port = port;
    }

    @Value("${spring.application.version:V1.0}")
    public void setVersion(String version) {
        SysConst.version = version;
    }

    @Value("${sys.networkCardName:eth0}")
    public void setNetworkCardName(String networkCardName) {
        SysConst.networkCardName = networkCardName;
    }

    @Value("${sys.deployPath}")
    public void setDeployPath(String deployPath) {
        SysConst.deployPath = deployPath;
        SysConst.profile = deployPath + "/files";
    }

    @Value("${sys.deployDay}")
    public void setDeployDay(String deployDay) {
        SysConst.deployDay = deployDay;
    }
}
