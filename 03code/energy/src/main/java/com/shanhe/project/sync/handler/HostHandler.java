package com.shanhe.project.sync.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson.JSONObject;
import com.shanhe.common.exception.ServiceException;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.uuid.IdUtils;
import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.consts.SysConst;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.framework.manager.AsyncTaskManager;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.device.host.domain.Host;
import com.shanhe.project.device.host.service.IHostService;
import com.shanhe.project.device.opt.service.ControlAir;
import com.shanhe.project.sync.consts.MethodEnum;
import com.shanhe.project.sync.domain.RequestVo;
import com.shanhe.project.sync.domain.ResponseVo;
import com.shanhe.project.sync.service.ClientReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 主机处理类
 *
 * @author wjh
 * @since 2025/5/23
 */
@Service
public class HostHandler {

    private static final Logger log = LoggerFactory.getLogger(HostHandler.class);

    @Resource
    private IHostService hostService;
    @Resource
    private ControlAir controlAir;
    @Resource
    private ClientReportService clientReportService;

    CacheKeyEnum tokenCache = CacheKeyEnum.HOST_TOKEN;

    /**
     * 主机认证token
     */
    public void validToken(RequestVo request) {
        if (StrUtil.equals(request.getMethod(), MethodEnum._98.getDictValue())) {
            return;
        }
        if (StrUtil.isEmpty(request.getToken())) {
            throw new ServiceException("token不能为空");
        }
        if (!Objects.equals(request.getValidType(), YesNoEnum.YES.getDictValue())) {
            if (!StrUtil.equals(request.getToken(), (String) CacheUtils.get(tokenCache.getCache(), tokenCache.getKey()))) {
                throw new ServiceException("token验证失败");
            }
        }
    }

    /**
     * 获取token
     */
    public ResponseVo getToken(RequestVo request, Host host) {
        try {
            String contentStr = JSONObject.toJSONString(request.getContent());
            log.debug("获取token信息：{}", contentStr);
            JSONObject param = JSONObject.parseObject(contentStr);

            // 秘钥
            String signDigest = param.getString("signDigest");
            // 生成秘钥 MD5(imei+“&”+设备秘钥+“&”+timestamp)
            String sign = DigestUtil.md5Hex(String.format("%s&%s&%s", host.getImei(), host.getPassword(), request.getTimestamp()));
            if (!StrUtil.equals(signDigest, sign)) {
                throw new ServiceException("秘钥验证失败");
            }

            String token = IdUtils.randomUuid();
            // 保存缓存
            CacheUtils.put(tokenCache.getCache(), tokenCache.getKey(), token);

            // 响应结果
            Map<String, String> content = new HashMap<>(1);
            content.put("token", token);
            return new ResponseVo(request.getImei(), request.getMethod(), request.getBusinessId(), content);
        } catch (Exception e) {
            String msg = String.format("获取token信息异常：%s", e.getMessage());
            log.error(msg);
            return new ResponseVo(request.getImei(), request.getMethod(), request.getBusinessId(), msg);
        }
    }

    /**
     * 修改设备IP
     */
    public ResponseVo editDevIp(RequestVo request, Host host) {
        String msg = null;
        try {
            String contentStr = JSONObject.toJSONString(request.getContent());
            log.debug("修改设备IP信息：{}", contentStr);
            JSONObject param = JSONObject.parseObject(contentStr);

            // 秘钥
            String ip = param.getString("ip");
            if (StrUtil.isBlank(ip)) {
                throw new ServiceException("ip不能为空");
            }

            if (StrUtil.equals(host.getIp(), ip)) {
                throw new ServiceException("ip不变，无需修改");
            }

            host.setIp(ip);
            hostService.updateReportIp(host);
        } catch (Exception e) {
            msg = String.format("修改设备IP信息异常：%s", e.getMessage());
            log.error(msg);
        }
        return new ResponseVo(request.getImei(), MethodEnum._12.getDictValue(), request.getBusinessId(), msg);
    }

    /**
     * 修改设备IP
     */
    public ResponseVo editServerIp(RequestVo request, Host host) {
        String msg = null;
        try {
            String contentStr = JSONObject.toJSONString(request.getContent());
            log.debug("修改设备服务器IP信息：{}", contentStr);
            JSONObject param = JSONObject.parseObject(contentStr);

            // 秘钥
            String serverIp = param.getString("serverIp");
            String serverPort = param.getString("serverPort");
            if (StrUtil.isBlank(serverIp) || StrUtil.isBlank(serverPort)) {
                throw new ServiceException("服务器IP及端口号不能为空");
            }

            if (StrUtil.equals(host.getReportIp(), serverIp) && Objects.equals(host.getReportPort(), Integer.valueOf(serverPort))) {
                return new ResponseVo(request.getImei(), MethodEnum._12.getDictValue(), request.getBusinessId(), msg);
            }

            host.setReportIp(serverIp);
            host.setReportPort(Integer.valueOf(serverPort));
            hostService.updateReportIp(host);
        } catch (Exception e) {
            msg = String.format("修改设备IP信息异常：%s", e.getMessage());
            log.error(msg);
        }
        return new ResponseVo(request.getImei(), MethodEnum._12.getDictValue(), request.getBusinessId(), msg);
    }

    /**
     * 服务器时间
     */
    public ResponseVo sysDevDate(RequestVo request) {
        String msg = null;
        // 同步本机服务器时间
        try {
            String contentStr = JSONObject.toJSONString(request.getContent());
            log.debug("同步系统时间信息：{}", contentStr);
            JSONObject param = JSONObject.parseObject(contentStr);
            hostService.syncServerTime(param.getString("currentDate"));
        } catch (Exception e) {
            msg = String.format("同步系统时间异常：%s", e.getMessage());
            log.error(msg);
        }
        return new ResponseVo(request.getImei(), MethodEnum._16.getDictValue(), request.getBusinessId(), msg);
    }

    /**
     * 控制指令
     */
    public ResponseVo controlDev(RequestVo request) {
        String msg = null;
        try {
            if (!CommServer.isOpen()) {
                msg = "设备未建立连接，下发控制指令失败";
                return new ResponseVo(request.getImei(), MethodEnum._28.getDictValue(), request.getBusinessId(), msg);
            }

            String contentStr = JSONObject.toJSONString(request.getContent());
            log.debug("控制指令信息：{}", contentStr);
            JSONObject param = JSONObject.parseObject(contentStr);
            Long devId = param.getLong("devId");
            String cmd = param.getString("cmd");
            String classId = param.getString("classId");

            // 空调控制指令
            if (StrUtil.isNotBlank(classId) && StrUtil.equals(classId, "4")) {
                AjaxResult result = controlAir.doControlAir(devId,
                        String.valueOf(param.get("airMode")),
                        String.valueOf(param.get("temperature")));
                // 指令下发结果
                if (!Objects.equals(result.get(AjaxResult.CODE_TAG), AjaxResult.Type.SUCCESS.value())) {
                    msg = (String) result.get(AjaxResult.MSG_TAG);
                }
                return new ResponseVo(request.getImei(), MethodEnum._28.getDictValue(), request.getBusinessId(), msg);
            }

             // 直接下发指令
            CommServer.returnCmd(cmd);
        } catch (Exception e) {
            msg = String.format("控制指令异常：%s", e.getMessage());
            log.error(msg);
        }
        return new ResponseVo(request.getImei(), MethodEnum._28.getDictValue(), request.getBusinessId(), msg);
    }

    /**
     * 软件升级
     */
    public ResponseVo updateSoft(RequestVo request) {
        String msg = null;
        try {
            String contentStr = JSONObject.toJSONString(request.getContent());
            log.debug("软件升级信息：{}", contentStr);
            JSONObject param = JSONObject.parseObject(contentStr);
            String softVersion = param.getString("softVersion");
            if (StrUtil.isBlank(softVersion) || StrUtil.equals(SysConst.version, softVersion)) {
                throw new ServiceException("当前已是最新版本，无需升级");
            }
            String url = param.getString("url");
            if (StrUtil.isBlank(url) || !url.startsWith("http")) {
                throw new ServiceException("软件升级url不能为空");
            }

            // 缓存升级状态
            Boolean hasDeploy = (Boolean) CacheUtils.get(CacheKeyEnum.DEPLOY_DOWNLOAD.getCache(), CacheKeyEnum.DEPLOY_DOWNLOAD.getKey());
            if (hasDeploy != null && hasDeploy) {
                throw new ServiceException("软件升级中，请稍后");
            }
            CacheUtils.put(CacheKeyEnum.DEPLOY_DOWNLOAD.getCache(), CacheKeyEnum.DEPLOY_DOWNLOAD.getKey(), true);

            // 异步执行
            AsyncTaskManager.me().execute(DeployTask.deploy(request, softVersion, url, clientReportService));
        } catch (Exception e) {
            msg = String.format("软件升级失败：%s", e.getMessage());
            log.error(msg);
        }
        return new ResponseVo(request.getImei(), MethodEnum._30.getDictValue(), request.getBusinessId(), msg);
    }


}
