package com.shanhe.project.sync.handler;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSONObject;
import com.shanhe.common.exception.ServiceException;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.domain.DevBatteryOpt;
import com.shanhe.project.device.config.service.IBatteryPackService;
import com.shanhe.project.device.opt.service.ControlBattery;
import com.shanhe.project.energy.stat.domain.DevBatteryMonomer;
import com.shanhe.project.energy.stat.service.IDevBatteryMonomerService;
import com.shanhe.project.sync.consts.MethodEnum;
import com.shanhe.project.sync.domain.*;
import com.shanhe.project.sync.service.ClientReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 电池设备处理
 */
@Service
public class BatterySyncHandler {

    private static final Logger log = LoggerFactory.getLogger(BatterySyncHandler.class);

    @Resource
    private ControlBattery controlBattery;
    @Resource
    private IBatteryPackService batteryPackService;
    @Resource
    private ClientReportService clientReportService;
    @Resource
    private IDevBatteryMonomerService devBatteryMonomerService;

    /**
     * 同步蓄电池操作计划
     *
     * @param request 请求信息
     */
    public ResponseVo syncBatteryOpt(RequestVo request) {
        String msg = null;
        try {
            String contentStr = JSONObject.toJSONString(request.getContent());
            log.info("同步蓄电池操作计划信息：{}", contentStr);

            BatteryOptVo optVo = JSONObject.parseObject(contentStr, BatteryOptVo.class);
            DevBatteryOpt batteryOpt = BeanUtil.copyProperties(optVo, DevBatteryOpt.class);
            batteryOpt.setConfigId(optVo.getDevId());
            batteryOpt.setIsSync(true);

            AjaxResult ajaxResult;
            // 是否立即执行：0否1是
            if (Objects.equals(optVo.getIsNow(), YesNoEnum.YES.getDictValue())) {
                // 计划执行
                ajaxResult = controlBattery.toSendCmdToOat(batteryOpt);
            } else {
                // 立即执行
                ajaxResult = controlBattery.toSendBatteryCmdToOat(batteryOpt);
            }
            // 失败
            if (!Objects.equals(ajaxResult.get(AjaxResult.CODE_TAG), AjaxResult.Type.SUCCESS.value())) {
                msg = ajaxResult.get(AjaxResult.MSG_TAG).toString();
            }
        } catch (Exception e) {
            msg = String.format("同步设备信息异常：%s", e.getMessage());
            log.error(msg);
        }
        return new ResponseVo(request.getImei(), MethodEnum._43.getDictValue(), request.getBusinessId(), msg);
    }

    public ResponseVo syncBatteryMonomer(RequestVo request) {
        String msg = null;
        try {
            String contentStr = JSONObject.toJSONString(request.getContent());

            if (!clientReportService.canSend()) {
                throw new ServiceException("未与服务端建立连接");
            }
            log.debug("同步内阻初装值：{}", contentStr);

            BatteryMonomerPackVo batteryOpt = JSONObject.parseObject(contentStr, BatteryMonomerPackVo.class);

            BatteryPack batteryPack = batteryPackService.selectBatteryInfoByPackNum(batteryOpt.getDevId(), batteryOpt.getPackNum());
            if (batteryPack == null) {
                return new ResponseVo(request.getImei(), MethodEnum._45.getDictValue(), request.getBusinessId(), "未找到该电池组");
            }

            devBatteryMonomerService.init(batteryPack, batteryOpt.getChildDev());

        } catch (Exception e) {
            msg = String.format("主动同步设备异常：%s", e.getMessage());
            log.error(msg);
        }
        return new ResponseVo(request.getImei(), MethodEnum._45.getDictValue(), request.getBusinessId(), msg);
    }

    public ResponseVo reportSynBatteryMonomer(RequestVo request) {
        String msg = null;
        try {
            String contentStr = JSONObject.toJSONString(request.getContent());

            if (!clientReportService.canSend()) {
                throw new ServiceException("未与服务端建立连接");
            }
            log.debug("下发内阻初装值：{}", contentStr);

            JSONObject param = JSONObject.parseObject(contentStr);
            Long configId =  param.getLong("devId");
            Integer packNum = param.getInteger("packNum");

            BatteryPack batteryPack = batteryPackService.selectBatteryInfoByPackNum(configId, packNum);
            if (batteryPack == null) {
                return new ResponseVo(request.getImei(), MethodEnum._47.getDictValue(), request.getBusinessId(), "未找到该电池组");
            }

            List<DevBatteryMonomer> devBatteryMonomers = devBatteryMonomerService.selectList(configId, packNum);
            if (devBatteryMonomers == null || devBatteryMonomers.isEmpty()) {
                return new ResponseVo(request.getImei(), MethodEnum._47.getDictValue(), request.getBusinessId(), "未找到该电池初装值");
            }
            clientReportService.uploadBatteryMonomer(configId, packNum, devBatteryMonomers, request.getImei());

        } catch (Exception e) {
            msg = String.format("主动同步设备异常：%s", e.getMessage());
            log.error(msg);
        }
        return new ResponseVo(request.getImei(), MethodEnum._47.getDictValue(), request.getBusinessId(), msg);
    }
}
