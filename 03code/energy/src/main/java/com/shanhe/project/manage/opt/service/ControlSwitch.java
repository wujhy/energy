package com.shanhe.project.manage.opt.service;

import com.shanhe.common.exception.ServiceException;
import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import com.shanhe.framework.enums.TcpCidEnum;
import com.shanhe.project.manage.host.domain.Host;
import com.shanhe.project.manage.host.service.IHostService;
import com.shanhe.project.manage.opt.cmd.DeviceModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 开关量控制类
 *
 * @author wjh
 * @since 2025/7/10
 */
@Slf4j
@Service
public class ControlSwitch {
    @Resource
    private IHostService hostService;
    /**
     * 输出开关控制
     *
     * @param post 串口号
     * @param paramValue 开关值
     */
    public void doControlSwitch(Integer post, Integer paramValue) {
        Host host = getHost();
        String info = CodingUtil.integerToHexString(post, 2)
                + CodingUtil.integerToHexString(paramValue, 2);
        CommServer.returnCmd(DeviceModel.getCmd(host, info, TcpCidEnum._58.getDictValue(), TcpCidEnum._D8.getDictValue()));
    }

    /** 获取在线的主机 */
    public Host getHost() {
        Host host = hostService.onlineHost();
        if (host == null || !CommServer.isOpen()) {
            throw new ServiceException("主机未在线，操作执行失败！");
        }
        return host;
    }
}
