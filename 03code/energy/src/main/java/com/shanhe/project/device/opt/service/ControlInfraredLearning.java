package com.shanhe.project.device.opt.service;

import cn.hutool.core.util.StrUtil;
import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.enums.*;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.host.domain.Host;
import com.shanhe.project.device.opt.cmd.DeviceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Objects;

/**
 * 红外控制类
 *
 * @author wjh
 * @since 2025/7/10
 */
@Service
public class ControlInfraredLearning extends ControlBase {

    protected static Logger logger = LoggerFactory.getLogger(ControlInfraredLearning.class);
    /** 缓存结果 **/
    CacheKeyEnum cacheKeyEnum = CacheKeyEnum.RESULT_CX;
    @Resource
    private OptLogService optLogService;

    /**
     * 红外控制控制
     *
     * @param params      请求参数
     *  configId      设备ID
     *  cmdId         工作模式 1：制冷，2：制热，3：除湿，4：送风 5：开机，6：关机，7：减小，8：增大
     *  temperature   温度
     */
    public AjaxResult doInfraredLearning(Map<String, Object> params) {
        // 主机信息
        Host host = super.getHost();
        // 请求参数
        Long configId = Long.valueOf((String)params.get("configId"));
        Double temperature = Double.parseDouble(String.valueOf(params.get("temperature")));
        String cmdId = String.valueOf(params.get("cmdId"));
        if (StrUtil.isBlank(cmdId)) {
            return AjaxResult.error("红外学习指令错误，本次设置失败！");
        }
        // 设备信息
        Config config = super.getConfig(configId);

        // 避免重复请求
        String resultKey = super.setControlStatus(config, TcpCidEnum._D9.getDictValue(), cacheKeyEnum);

        // 发送指令
        String info = CodingUtil.stringToHexString(cmdId, 2)
                + CodingUtil.stringToHexString(String.valueOf(temperature).replace(".", ""), 4);
        CommServer.returnCmd(DeviceModel.getCmd(host, config, info, TcpCidEnum._59.getDictValue(), TcpCidEnum._D9.getDictValue()));

        // 结果监控
        AjaxResult ajaxResult = super.getControlResult(resultKey, cacheKeyEnum);

        // 响应成功，保存历史记录
        if (Objects.equals(ajaxResult.get(AjaxResult.CODE_TAG), AjaxResult.Type.SUCCESS.value())) {
            optLogService.insert(config.getConfigId(), params, YesNoEnum.YES.getDictValue());
        } else {
            optLogService.insert(config.getConfigId(), params, YesNoEnum.NO.getDictValue());
        }
        // 结果监控
        return ajaxResult;
    }
}
