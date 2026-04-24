package com.shanhe.project.sync.handler;

import com.alibaba.fastjson.JSONObject;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.domain.ConfigProtocol;
import com.shanhe.project.device.config.domain.ConfigProtocolAttribute;
import com.shanhe.project.device.config.service.IConfigProtocolService;
import com.shanhe.project.device.config.service.IConfigService;
import com.shanhe.project.sync.consts.MethodEnum;
import com.shanhe.project.sync.domain.AlarmOrderVo;
import com.shanhe.project.sync.domain.AlarmRuleVo;
import com.shanhe.project.sync.domain.RequestVo;
import com.shanhe.project.sync.domain.ResponseVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 设备协议处理类
 *
 * @author wjh
 * @since 2025/5/23
 */
@Service
public class ProtocolHandler {

    private static final Logger log = LoggerFactory.getLogger(ProtocolHandler.class);

    @Resource
    IConfigService configService;
    @Resource
    private IConfigProtocolService configProtocolService;

    /**
     * 同步设备协议
     */
    public ResponseVo synAlarmOrder(RequestVo request) {
        String msg = null;
        try {
            String contentStr = JSONObject.toJSONString(request.getContent());
            log.debug("同步设备协议信息：{}", contentStr);
            AlarmOrderVo alarmOrder = JSONObject.parseObject(contentStr, AlarmOrderVo.class);
            // 查设备
            Config config = configService.selectConfigByConfigId(alarmOrder.getDevId());
            if (config == null) {
                return new ResponseVo(request.getImei(), MethodEnum._22.getDictValue(), request.getBusinessId(), "设备不存在");
            }
            // 通过编码匹配，设备存在删除
            ConfigProtocol configProtocol = configProtocolService.selectBy(alarmOrder.getDevId(), alarmOrder.getEnCode());
            if (configProtocol != null) {
                configProtocolService.deleteBySync(configProtocol.getProtocolId());
            }
            // 通过ID匹配，设备存在删除
            configProtocol = configProtocolService.findByProtocolId(alarmOrder.getTemCmdId());
            if (configProtocol != null) {
                configProtocolService.deleteBySync(configProtocol.getProtocolId());
            }
            if (configProtocol == null) {
                configProtocol = new ConfigProtocol();
            }
            // 设置参数
            this.setProtocolParam(configProtocol, alarmOrder);

            // 插入
            configProtocolService.insertBySync(config, configProtocol);
        } catch (Exception e) {
            msg = String.format("同步设备协议异常：%s", e.getMessage());
            log.error(msg);
        }
        return new ResponseVo(request.getImei(), MethodEnum._22.getDictValue(), request.getBusinessId(), msg);
    }

    /**
     * 设置协议参数
     *
     * @param configProtocol 本地
     * @param alarmOrder 远程
     */
    private void setProtocolParam(ConfigProtocol configProtocol, AlarmOrderVo alarmOrder) {
        configProtocol.setProtocolId(alarmOrder.getTemCmdId());
        configProtocol.setConfigId(alarmOrder.getDevId());
        configProtocol.setProtocolType(alarmOrder.getProtocolType());
        configProtocol.setProtocolCode(alarmOrder.getEnCode());
        configProtocol.setCmdType(alarmOrder.getCmdType());
        configProtocol.setCmdName(alarmOrder.getCmdName());
        configProtocol.setCmdContent(alarmOrder.getCmdContent());
        configProtocol.setStatus(Objects.equals(alarmOrder.getStatus(), YesNoEnum.YES.getDictValue())
                ? YesNoEnum.NO.getDictValue() : YesNoEnum.YES.getDictValue());
        configProtocol.setTemplate(YesNoEnum.NO.getDictValue());

        List<AlarmRuleVo> ruleList = alarmOrder.getListRule();
        if (ruleList == null || ruleList.isEmpty()) {
            return;
        }
        List<ConfigProtocolAttribute> attributeList = new ArrayList<>(ruleList.size());
        for (AlarmRuleVo alarmRule : ruleList) {
            ConfigProtocolAttribute protocolAttribute = new ConfigProtocolAttribute();
            protocolAttribute.setProtocolAttrId(alarmRule.getRuleId());
            protocolAttribute.setProtocolId(alarmOrder.getTemCmdId());
            protocolAttribute.setAttrCode(alarmRule.getItemCode());
            protocolAttribute.setDataType(alarmRule.getDataType());
            protocolAttribute.setStartPoint(alarmRule.getStartPoint());
            protocolAttribute.setEndPoint(alarmRule.getEndPoint());
            protocolAttribute.setAnyFlag(alarmRule.getAnyFlag());
            protocolAttribute.setAnyExpress(alarmRule.getAnyExpress());
            protocolAttribute.setHasPoint(Objects.equals(alarmRule.getHasPoint(), YesNoEnum.YES.getDictValue())
                    ? YesNoEnum.NO.getDictValue() : YesNoEnum.YES.getDictValue());
            protocolAttribute.setIsComplement(Objects.equals(alarmRule.getIsComplement(), YesNoEnum.YES.getDictValue())
                    ? YesNoEnum.NO.getDictValue() : YesNoEnum.YES.getDictValue());
            attributeList.add(protocolAttribute);
        }
        configProtocol.setAttributeList(attributeList);
    }

    /**
     * 同步设备协议
     */
    public ResponseVo delAlarmOrder(RequestVo request) {
        String msg = null;
        try {
            String contentStr = JSONObject.toJSONString(request.getContent());
            log.debug("删除设备协议信息：{}", contentStr);
            JSONObject param = JSONObject.parseObject(contentStr);
            ConfigProtocol configProtocol = configProtocolService.findByProtocolId(param.getLong("temCmdId"));
            if (configProtocol != null) {
                // 查设备
                Config config = configService.selectConfigByConfigId(configProtocol.getConfigId());
                if (config == null) {
                    msg = String.format("删除设备协议失败，设备ID不存在：%s", configProtocol.getConfigId());
                } else {
                    configProtocolService.deleteBySync(configProtocol.getProtocolId());
                }
            } else {
                msg = String.format("删除设备协议失败，协议ID不存在：%s", param.getLong("temCmdId"));
            }
        } catch (Exception e) {
            msg = String.format("删除设备协议异常：%s", e.getMessage());
            log.error(msg);
        }
        return new ResponseVo(request.getImei(), MethodEnum._24.getDictValue(), request.getBusinessId(), msg);
    }
}
