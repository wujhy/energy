package com.shanhe.project.device.opt.service;

import com.shanhe.common.utils.CacheUtils;
import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.framework.enums.TcpCharEnum;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.device.opt.vo.CmdDebugVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import oshi.util.Util;

import java.util.Objects;

/**
 * 设备控制类
 *
 * @author wjh
 * @since 2025/7/10
 */
@Service
public class ControlDevice extends ControlBase {

    protected static Logger logger = LoggerFactory.getLogger(ControlDevice.class);

    /**
     * 下发测试指令
     */
    public AjaxResult cmdDebug(CmdDebugVO params) {
        // 不需回调，直接下发指令
        if (Objects.equals(params.getCallback(), YesNoEnum.NO.getDictValue())) {
            CommServer.returnCmd(params.getCmd());
            return AjaxResult.success();
        }

        // 回调，重新组装指令修改C3
        StringBuilder cmd = new StringBuilder();
        cmd.append(params.getCmd(), 0, 8);
        cmd.append(TcpCharEnum._FF.getDictValue());
        cmd.append(params.getCmd(), 10, params.getCmd().length() - 4);
        // 校验码
        cmd.append(CodingUtil.energyCheckSum(cmd.substring(2)));
        cmd.append(params.getCmd(), params.getCmd().length() - 2, params.getCmd().length());
        // 设置等待
        CacheUtils.put(CacheKeyEnum.RESULT_DEBUG.getCache(), CacheKeyEnum.RESULT_DEBUG.getKey(), TcpCharEnum._FF.getDictValue());
        // 直接下发指令
        CommServer.returnCmd(cmd.toString());

        //延迟等待设备响应
        boolean isSuccess = true;
        String result;
        while (true) {
            result = (String)CacheUtils.get(CacheKeyEnum.RESULT_DEBUG.getCache(), CacheKeyEnum.RESULT_DEBUG.getKey());
            if (result == null) {
                isSuccess = false;
                break;
            } else {
                if(!Objects.equals(result, TcpCharEnum._FF.getDictValue())){
                    // 响应处理完成，清除缓存
                    CacheUtils.remove(CacheKeyEnum.RESULT_DEBUG.getCache(), CacheKeyEnum.RESULT_DEBUG.getKey());
                    break;
                }
            }
            Util.sleep(500L);
        }
        return isSuccess ? AjaxResult.success(result) : AjaxResult.error("指令调试失败！");
    }
}
