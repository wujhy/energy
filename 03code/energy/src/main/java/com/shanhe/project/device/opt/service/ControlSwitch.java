package com.shanhe.project.device.opt.service;

import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.framework.enums.TcpCidEnum;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.host.domain.Host;
import com.shanhe.project.device.opt.cmd.DeviceModel;
import com.shanhe.project.device.opt.vo.SwitchVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 开关量控制类
 *
 * @author wjh
 * @since 2025/7/10
 */
@Service
public class ControlSwitch extends ControlBase {

    protected static Logger logger = LoggerFactory.getLogger(ControlSwitch.class);
    /** 缓存结果 **/
    CacheKeyEnum cacheKeyEnum = CacheKeyEnum.RESULT_CX;
    /**
     * 输出开关控制
     *
     * @param switchVO  开关信息
     */
    public AjaxResult doControlSwitch(SwitchVO switchVO) {
        // 主机信息
        Host host = super.getHost();
        // 设备信息
        Config config = super.getConfig(switchVO.getConfigId());

        // 避免重复请求
        String resultKey = super.setControlStatus(config, TcpCidEnum._D8.getDictValue(), cacheKeyEnum);

        // 生成指令
        String info = CodingUtil.integerToHexString(config.getPort(), 2)
                + CodingUtil.integerToHexString(switchVO.getParamValue(), 2);
        CommServer.returnCmd(DeviceModel.getCmd(host, info, TcpCidEnum._58.getDictValue(), TcpCidEnum._D8.getDictValue()));

        // 结果监控
        return super.getControlResult(resultKey, cacheKeyEnum);
    }

    /**
     * 输出开关控制
     *
     * @param post 串口号
     * @param paramValue 开关值
     */
    public void doControlSwitch(Integer post, Integer paramValue) {
        // 主机信息
        Host host = super.getHost();
        // 生成指令
        String info = CodingUtil.integerToHexString(post, 2)
                + CodingUtil.integerToHexString(paramValue, 2);
        CommServer.returnCmd(DeviceModel.getCmd(host, info, TcpCidEnum._58.getDictValue(), TcpCidEnum._D8.getDictValue()));
    }
}
