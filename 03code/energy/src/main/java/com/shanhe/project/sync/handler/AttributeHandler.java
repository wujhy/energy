package com.shanhe.project.sync.handler;

import com.alibaba.fastjson.JSONObject;
import com.shanhe.common.exception.ServiceException;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.domain.ConfigAttribute;
import com.shanhe.project.device.config.service.IConfigAttributeService;
import com.shanhe.project.device.config.service.IConfigService;
import com.shanhe.project.sync.common.AttributeUtil;
import com.shanhe.project.sync.consts.MethodEnum;
import com.shanhe.project.sync.domain.AlarmItemVo;
import com.shanhe.project.sync.domain.RequestVo;
import com.shanhe.project.sync.domain.ResponseVo;
import com.shanhe.project.sync.service.ClientReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * 主机处理类
 *
 * @author wjh
 * @since 2025/5/23
 */
@Service
public class AttributeHandler {

    private static final Logger log = LoggerFactory.getLogger(AttributeHandler.class);

    @Resource
    private IConfigAttributeService configAttributeService;
    @Resource
    private ClientReportService clientReportService;
    @Resource
    IConfigService configService;
    /**
     * 同步设备属性
     */
    public ResponseVo synAlarmConfigItem(RequestVo request) {
        String msg = null;
        try {
            String contentStr = JSONObject.toJSONString(request.getContent());
            log.debug("同步设备属性信息：{}", contentStr);
            AlarmItemVo alarmItem = JSONObject.parseObject(contentStr, AlarmItemVo.class);
            // 校验设备
            Config config = configService.selectDefaultConfig();
            if (config == null) {
                return new ResponseVo(request.getImei(), MethodEnum._18.getDictValue(), request.getBusinessId(), "设备不存在");
            }
            // 设备属性
            ConfigAttribute configAttribute = configAttributeService.getBy(alarmItem.getPackNum(), alarmItem.getItemCode());
            boolean isUpdate = false;
            // 设备存在
            if (configAttribute != null) {
                // 如果ID不一致，删除旧记录，新建
                if (!Objects.equals(configAttribute.getConfigAttrId(), alarmItem.getItemId())) {
                    // 删除设备
                    configAttributeService.deleteConfigAttribute(configAttribute);
                } else {
                    // ID一致，只需更新
                    isUpdate = true;
                }
            }

            // 设备不存在，通过ID再确认设备是否存在
            if (!isUpdate) {
                configAttribute = configAttributeService.selectConfigAttributeByConfigAttrId(alarmItem.getItemId());
                // 设备存在，只做更新
                isUpdate = configAttribute != null;
            }
            if (configAttribute == null) {
                configAttribute = new ConfigAttribute();
            }

            // 设置参数
            AttributeUtil.setAttributeParam(configAttribute, alarmItem);

            if (isUpdate) {
                configAttributeService.updateConfigAttributeBySyn(configAttribute);
            } else {
                configAttributeService.insertConfigAttribute(configAttribute, false);
            }
        } catch (Exception e) {
            msg = String.format("同步设备属性异常：%s", e.getMessage());
            log.error(msg);
        }
        return new ResponseVo(request.getImei(), MethodEnum._18.getDictValue(), request.getBusinessId(), msg);
    }

    /**
     * 删除设备属性
     */
    public ResponseVo delAlarmConfigItem(RequestVo request) {
        String msg = null;
        try {
            String contentStr = JSONObject.toJSONString(request.getContent());
            log.debug("删除设备属性信息：{}", contentStr);
            JSONObject param = JSONObject.parseObject(contentStr);
            ConfigAttribute configAttribute = configAttributeService.selectConfigAttributeByConfigAttrId(param.getLong("itemId"));
            if (configAttribute != null) {
                configAttributeService.deleteConfigAttribute(configAttribute);
            } else {
                msg = String.format("删除设备属性失败，ID不存在：%s", contentStr);
            }
        } catch (Exception e) {
            msg = String.format("删除设备属性异常：%s", e.getMessage());
            log.error(msg);
        }
        return new ResponseVo(request.getImei(), MethodEnum._20.getDictValue(), request.getBusinessId(), msg);
    }

    /**
     * 同步设备属性
     */
    public ResponseVo reportSynAlarmConfigItem(RequestVo request) {
        String msg = null;
        try {
            if (!clientReportService.canSend()) {
                throw new ServiceException("未与服务端建立连接");
            }
            String contentStr = JSONObject.toJSONString(request.getContent());
            log.debug("主动同步设备属性信息：{}", contentStr);
            JSONObject param = JSONObject.parseObject(contentStr);
            List<ConfigAttribute> configAttributeList = configAttributeService.selectByConfigId();
            if (configAttributeList != null && !configAttributeList.isEmpty()) {
                for (ConfigAttribute configAttribute : configAttributeList) {
                    clientReportService.uploadAlarmConfigItem(configAttribute, request.getImei());
                }
            }
        } catch (Exception e) {
            msg = String.format("同步设备属性失败：%s", e.getMessage());
            log.error(msg);
        }
        return new ResponseVo(request.getImei(), request.getMethod(), request.getBusinessId(), msg);
    }
}
