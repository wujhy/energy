package com.shanhe.project.sync.handler;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSONObject;
import com.shanhe.common.exception.ServiceException;
import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.model.BatteryCollectorCommandResult;
import com.shanhe.project.collector.battery.service.BatteryCollectorCommandService;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.domain.DevBatteryOpt;
import com.shanhe.project.device.config.service.IBatteryPackService;
import com.shanhe.project.device.opt.service.ControlBattery;
import com.shanhe.project.energy.stat.domain.DevBatteryMonomer;
import com.shanhe.project.energy.stat.service.IDevBatteryMonomerService;
import com.shanhe.project.sync.consts.MethodEnum;
import com.shanhe.project.sync.domain.*;
import com.shanhe.project.sync.service.ClientReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * 电池设备处理
 *
 * @author wjh
 * @since 2026-05-25
 */
@Slf4j
@Service
public class BatterySyncHandler {

    @Resource
    private ControlBattery controlBattery;
    @Resource
    private BatteryCollectorProperties batteryCollectorProperties;
    @Resource
    private BatteryCollectorCommandService batteryCollectorCommandService;
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
            // 保留旧分支语义：YES 走测试计划/配置同步，其他值走立即执行。
            if (Objects.equals(optVo.getIsNow(), YesNoEnum.YES.getDictValue())) {
                ajaxResult = controlBattery.toSendCmdToOat(batteryOpt);
            } else {
                AjaxResult collectorResult = tryCollectorCommand(batteryOpt);
                if (collectorResult != null) {
                    if (!Objects.equals(collectorResult.get(AjaxResult.CODE_TAG), AjaxResult.Type.SUCCESS.value())) {
                        Object collectorMsg = collectorResult.get(AjaxResult.MSG_TAG);
                        msg = collectorMsg != null ? collectorMsg.toString() : "采集命令执行失败";
                    }
                    return new ResponseVo(request.getImei(), MethodEnum._43.getDictValue(), request.getBusinessId(), msg);
                }
                ajaxResult = controlBattery.toSendBatteryCmdToOat(batteryOpt);
            }
            // 失败
            if (!Objects.equals(ajaxResult.get(AjaxResult.CODE_TAG), AjaxResult.Type.SUCCESS.value())) {
                Object ajaxMsg = ajaxResult.get(AjaxResult.MSG_TAG);
                msg = ajaxMsg != null ? ajaxMsg.toString() : "命令执行失败";
            }
        } catch (Exception e) {
            msg = String.format("同步设备信息异常：%s", e.getMessage());
            log.error(msg);
        }
        return new ResponseVo(request.getImei(), MethodEnum._43.getDictValue(), request.getBusinessId(), msg);
    }

    private AjaxResult tryCollectorCommand(DevBatteryOpt batteryOpt) {
        if (!Boolean.TRUE.equals(batteryCollectorProperties.getJsonTcpModuleCommandEnabled())
                || batteryOpt == null
                || batteryCollectorCommandService == null) {
            return null;
        }
        String channelName = batteryCollectorCommandService.resolveChannelName(batteryOpt.getPackNum());
        if (channelName == null || channelName.trim().isEmpty()) {
            log.warn("独立采集模块未找到匹配通道，回退旧蓄电池控制链路，configId={}, packNum={}",
                    batteryOpt.getConfigId(),
                    batteryOpt.getPackNum());
            return null;
        }
        BatteryCollectorCommandResult result = executeCollectorCommand(channelName, batteryOpt);
        if (result == null) {
            return null;
        }
        if (result.isSuccess()) {
            return AjaxResult.success("独立采集模块命令已加入下发队列", result);
        }
        log.warn("独立采集模块命令未入队，回退旧蓄电池控制链路，configId={}, packNum={}, testType={}, result={}",
                batteryOpt.getConfigId(),
                batteryOpt.getPackNum(),
                batteryOpt.getTestType(),
                result.getMessage());
        return null;
    }

    private BatteryCollectorCommandResult executeCollectorCommand(String channelName, DevBatteryOpt batteryOpt) {
        BatteryTestEnum testEnum = BatteryTestEnum.find(batteryOpt.getTestType());
        switch (testEnum) {
            case _2:
                int batteryCount = resolveBatteryCount(batteryOpt.getPackNum());
                return batteryCollectorCommandService.connectResistanceTest(
                        channelName,
                        batteryOpt.getPackNum(),
                        batteryCount,
                        null);
            case _6:
                if (batteryOpt.getModelNum() == null) {
                    return null;
                }
                return batteryCollectorCommandService.singleInternalResistanceTest(
                        channelName,
                        batteryOpt.getPackNum(),
                        batteryOpt.getModelNum(),
                        null);
            default:
                return null;
        }
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

            BatteryPack batteryPack = batteryPackService.selectBatteryInfoByPackNum(batteryOpt.getPackNum());
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
            Integer packNum = param.getInteger("packNum");

            BatteryPack batteryPack = batteryPackService.selectBatteryInfoByPackNum(packNum);
            if (batteryPack == null) {
                return new ResponseVo(request.getImei(), MethodEnum._47.getDictValue(), request.getBusinessId(), "未找到该电池组");
            }

            List<DevBatteryMonomer> devBatteryMonomers = devBatteryMonomerService.selectList(packNum);
            if (devBatteryMonomers == null || devBatteryMonomers.isEmpty()) {
                return new ResponseVo(request.getImei(), MethodEnum._47.getDictValue(), request.getBusinessId(), "未找到该电池初装值");
            }
            clientReportService.uploadBatteryMonomer(packNum, devBatteryMonomers, request.getImei());

        } catch (Exception e) {
            msg = String.format("主动同步设备异常：%s", e.getMessage());
            log.error(msg);
        }
        return new ResponseVo(request.getImei(), MethodEnum._47.getDictValue(), request.getBusinessId(), msg);
    }

    /** 从电池组配置读取单体数量，默认 245。 */
    private int resolveBatteryCount(Integer packNum) {
        if (packNum == null) {
            return 245;
        }
        try {
            Integer count = batteryPackService.getBatteryMaxNumber(packNum);
            return count != null && count > 0 ? Math.min(count, 245) : 245;
        } catch (Exception e) {
            return 245;
        }
    }
}
