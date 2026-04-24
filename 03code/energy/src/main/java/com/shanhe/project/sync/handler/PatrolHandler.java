package com.shanhe.project.sync.handler;

import com.alibaba.fastjson.JSONObject;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.monitor.patrol.domain.PatrolTemplate;
import com.shanhe.project.monitor.patrol.domain.PatrolTemplateContent;
import com.shanhe.project.monitor.patrol.service.IPatrolTemplateContentService;
import com.shanhe.project.monitor.patrol.service.IPatrolTemplateService;
import com.shanhe.project.sync.consts.MethodEnum;
import com.shanhe.project.sync.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 巡检处理
 *
 * @author wjh
 * @since 2025/7/2
 */
@Service
public class PatrolHandler {

    private static final Logger log = LoggerFactory.getLogger(PatrolHandler.class);

    @Resource
    IPatrolTemplateService patrolTemplateService;
    @Resource
    IPatrolTemplateContentService patrolTemplateContentService;

    /**
     * 同步设备协议
     */
    public ResponseVo getPatrolTemplateRes(RequestVo request) {
        String msg = null;
        try {
            String contentStr = JSONObject.toJSONString(request.getContent());
            log.debug("同步设备协议信息：{}", contentStr);
            PatrolTemplateVo templateVo = JSONObject.parseObject(contentStr, PatrolTemplateVo.class);
            PatrolTemplate patrolTemplate = patrolTemplateService.selectById(templateVo.getId());
            if (patrolTemplate != null) {
                patrolTemplate.setName(templateVo.getName());
                patrolTemplate.setStatus(YesNoEnum.YES.getDictValue());
                patrolTemplateService.update(patrolTemplate);
            } else {
                patrolTemplate = new PatrolTemplate();
                patrolTemplate.setName(templateVo.getName());
                patrolTemplate.setStatus(YesNoEnum.YES.getDictValue());
                patrolTemplate.setId(templateVo.getId());
                patrolTemplateService.insert(patrolTemplate);
            }

            // 删除巡检项
            patrolTemplateContentService.deleteByTemplateId(templateVo.getId());
            if (templateVo.getChildDev() != null && !templateVo.getChildDev().isEmpty()) {
                for (String content : templateVo.getChildDev()) {
                    PatrolTemplateContent patrolTemplateContent = new PatrolTemplateContent();
                    patrolTemplateContent.setTemplateId(templateVo.getId());
                    patrolTemplateContent.setContent(content);
                    patrolTemplateContentService.save(patrolTemplateContent);
                }
            }
        } catch (Exception e) {
            msg = String.format("同步巡检清单异常：%s", e.getMessage());
            log.error(msg);
        }
        return new ResponseVo(request.getImei(), MethodEnum._22.getDictValue(), request.getBusinessId(), msg);
    }
}
