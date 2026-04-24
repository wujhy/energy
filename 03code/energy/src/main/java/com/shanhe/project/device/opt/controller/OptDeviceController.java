package com.shanhe.project.device.opt.controller;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.shanhe.framework.aspectj.lang.annotation.Log;
import com.shanhe.framework.enums.BusinessType;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.device.opt.service.*;
import com.shanhe.project.device.opt.vo.CmdDebugVO;
import com.shanhe.project.device.opt.vo.PrecisionAirVO;
import com.shanhe.project.device.opt.vo.SwitchVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Objects;

/**
 * 设备控制
 *
 * @author wjh
 * @since 2025/4/17
 */
@RestController
@RequestMapping("/optCmd")
public class OptDeviceController extends BaseController {

    protected static Logger logger = LoggerFactory.getLogger(OptDeviceController.class);

    @Resource
    private ControlDevice controlDevice;
    @Resource
    private ControlAir controlAir;
    @Resource
    private ControlSwitch controlSwitch;
    @Resource
    private ControlInfraredLearning controlInfraredLearning;

    /**
     * 操作空调
     */
    @Log(title = "操作空调", businessType = BusinessType.UPDATE)
    @PostMapping("/doAirCmd")
    public AjaxResult doAirCmd(@RequestBody JSONObject params) {
        try {
            Long configId = params.getLong("configId");
            // 控制指令 1：制冷 2：制热 3：除湿 4：送风 5：开机 6：关机 7：减小 8：增大
            String cmdId = params.getString("cmdId");
            if (Objects.isNull(configId) || StrUtil.isBlank(cmdId)) {
                return error("设置空调指令错误，本次设置失败！");
            }
            Double temperature = params.getDouble("temperature");
            if (temperature == null) {
                temperature = 24D;
            }
            if (temperature < 16 || temperature > 30) {
                return error("设置空调温度在16至30之间，本次设置失败！");
            }
            return controlAir.doControlAir(configId, cmdId, String.valueOf(temperature));
        } catch (Exception e) {
            logger.error("操作空调处理数据失败，error:{}", e.getMessage());
            return error(e.getMessage());
        }
    }

    /**
     * 海信精密空调控制
     */
    @Log(title = "海信精密空调控制", businessType = BusinessType.UPDATE)
    @PostMapping("/doPrecisionAirCmd")
    public AjaxResult doPrecisionAirCmd(@RequestBody @Validated PrecisionAirVO params) {
        try {
            return controlAir.doPrecisionAirCmd(params);
        } catch (Exception e) {
            logger.error("操作精密空调处理数据失败，error:{}", e.getMessage());
            return error(e.getMessage());
        }
    }

    @Log(title = "红外学习", businessType = BusinessType.UPDATE)
    @PostMapping("/doInfraredLearning")
    public AjaxResult doInfraredLearning(@RequestBody Map<String, Object> params) {
        try {
            return controlInfraredLearning.doInfraredLearning(params);
        } catch (Exception e) {
            logger.error("红外学习处理数据失败，error:{}", e.getMessage());
            return error(e.getMessage());
        }
    }

    @Log(title = "开关控制", businessType = BusinessType.UPDATE)
    @PostMapping("/doSwitchCmd")
    public AjaxResult doSwitchCmd(@RequestBody @Validated SwitchVO switchVO) {
        try {
            //输出开关控制
            return controlSwitch.doControlSwitch(switchVO);
        } catch (Exception e) {
            logger.error("操作输出开关处理数据失败，error:{}", e.getMessage());
            return error(e.getMessage());
        }
    }

    @Log(title = "指令调试功能", businessType = BusinessType.CLEAN)
    @PostMapping("/cmdDebug")
    public AjaxResult cmdDebug(@RequestBody @Validated CmdDebugVO params) {
        try {
            return controlDevice.cmdDebug(params);
        } catch (Exception e) {
            logger.error("指令调试处理数据失败，error:{}", e.getMessage());
            return error(e.getMessage());
        }
    }
}
