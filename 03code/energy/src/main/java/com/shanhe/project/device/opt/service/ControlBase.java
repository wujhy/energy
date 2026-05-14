package com.shanhe.project.device.opt.service;

import com.shanhe.common.exception.ServiceException;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.framework.enums.ConnectionStatusEnum;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.service.IConfigService;
import com.shanhe.project.device.host.domain.Host;
import com.shanhe.project.device.host.service.IHostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import oshi.util.Util;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * 控制基类
 *
 * @author wjh
 * @since 2025/7/10
 */
@Service
public class ControlBase {

    protected static Logger logger = LoggerFactory.getLogger(ControlBase.class);

    @Resource
    public IConfigService configService;
    @Resource
    private IHostService hostService;

    /**
     * 获取在线的主机
     */
    public Host getHost() {
        Host host = hostService.onlineHost();
        if (host == null || !CommServer.isOpen()) {
            throw new ServiceException("主机未在线，操作执行失败！");
        }
        return host;
    }

    /**
     * 获取设备
     */
    public Config getConfig() {
        Config config = configService.selectDefaultConfig();
        if (config == null) {
            throw new ServiceException("设备不存在，请求失败！");
        }
        return config;
    }

    /**
     * 设置控制状态
     *
     * @param config 设备
     * @param dynCid C3自定义协议编号
     * @return 缓存key
     */
    public String setControlStatus(Config config, String dynCid, CacheKeyEnum cacheKeyEnum) {
        return setControlStatus(config, null, dynCid, cacheKeyEnum);
    }

    /**
     * 设置控制状态
     *
     * @param config 设备
     * @param packNum 组
     * @param dynCid C3自定义协议编号
     * @return 缓存key
     */
    public String setControlStatus(Config config, Integer packNum, String dynCid, CacheKeyEnum cacheKeyEnum) {
        // 判断是否存在记录
        String resultKey;
        switch (cacheKeyEnum) {
            case RESULT:
                resultKey = String.format(cacheKeyEnum.getKey(), config.getConfigId(), packNum == null ? 0 : packNum, dynCid);
                break;
            case RESULT_CX:
                resultKey = String.format(cacheKeyEnum.getKey(), config.getType(), config.getPort(), config.getChannel(), dynCid);
                break;
            default:
                resultKey = String.format(cacheKeyEnum.getKey(), config.getConfigId(), dynCid);
                break;
        }

        Object result = CacheUtils.get(cacheKeyEnum.getCache(), resultKey);
        if (result != null && Objects.equals(result, -1)) {
            throw new ServiceException("已下发指令，请勿重复操作！");
        }

        //设置等待
        CacheUtils.put(cacheKeyEnum.getCache(), resultKey, -1);

        return resultKey;
    }

    /**
     * 监听控制指令执行结果
     *
     * @param resultKey 缓存key
     * @return 结果
     */
    public AjaxResult getControlResult(String resultKey, CacheKeyEnum cacheKeyEnum) {
        String msg;
        //延迟等待设备响应
        boolean isSuccess = true;
        while (true) {
            Object result = CacheUtils.get(cacheKeyEnum.getCache(), resultKey);
            if (result == null) {
                isSuccess = false;
                msg = "指令响应超时！";
                break;
            } else {
                if(!Objects.equals(result, -1)){
                    if(Objects.equals(result, 1)) {
                        isSuccess = false;
                        msg = "指令设置失败！";
                    } else {
                        msg = "指令设置成功！";
                    }
                    // 响应处理完成，清除缓存
                    CacheUtils.remove(cacheKeyEnum.getCache(), resultKey);
                    break;
                }
            }
            Util.sleep(500L);
        }

        return isSuccess ? AjaxResult.success(msg) : AjaxResult.error(msg);
    }


}
