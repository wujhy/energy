package com.shanhe.project.device.opt.service;

import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import com.shanhe.framework.enums.TcpCidEnum;
import com.shanhe.project.device.host.domain.Host;
import com.shanhe.project.device.opt.cmd.DeviceModel;
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

    /**
     * 输出开关控制
     *
     * @param post 串口号
     * @param paramValue 开关值
     */
    public void doControlSwitch(Integer post, Integer paramValue) {
        Host host = super.getHost();
        String info = CodingUtil.integerToHexString(post, 2)
                + CodingUtil.integerToHexString(paramValue, 2);
        CommServer.returnCmd(DeviceModel.getCmd(host, info, TcpCidEnum._58.getDictValue(), TcpCidEnum._D8.getDictValue()));
    }
}
