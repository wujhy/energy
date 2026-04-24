package com.shanhe.project.device.template.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.shanhe.common.exception.ServiceException;
import com.shanhe.common.utils.DateUtils;
import com.shanhe.common.utils.bean.Dict;
import com.shanhe.common.utils.uuid.IdUtils;
import com.shanhe.project.device.config.domain.ConfigProtocol;
import com.shanhe.project.device.config.service.IConfigProtocolService;
import com.shanhe.project.device.template.domain.TemplateAttribute;
import com.shanhe.project.device.template.domain.TemplateCopyVO;
import com.shanhe.project.device.template.domain.TemplateDataVO;
import com.shanhe.project.device.template.mapper.TemplateAttributeMapper;
import org.springframework.stereotype.Service;
import com.shanhe.project.device.template.mapper.TemplateMapper;
import com.shanhe.project.device.template.domain.Template;
import com.shanhe.project.device.template.service.ITemplateService;
import com.shanhe.common.utils.text.Convert;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 模板Service业务层处理
 * 
 * @author wjh
 * @since 2024-12-23
 */
@Service
public class TemplateServiceImpl implements ITemplateService 
{
    @Resource
    private TemplateMapper templateMapper;
    @Resource
    private TemplateAttributeMapper templateAttributeMapper;
    @Resource
    private IConfigProtocolService configProtocolService;
    /**
     * 查询模板
     * 
     * @param tmplId 模板主键
     * @return 模板
     */
    @Override
    public Template selectTemplateByTmplId(Long tmplId)
    {
        return templateMapper.selectTemplateByTmplId(tmplId);
    }

    /**
     * 查询模板列表
     * 
     * @param template 模板
     * @return 模板
     */
    @Override
    public List<Template> selectTemplateList(Template template)
    {
        return templateMapper.selectTemplateList(template);
    }

    @Override
    public List<Dict> selectDictList() {
        return templateMapper.selectDictList();
    }

    /**
     * 新增模板
     * 
     * @param template 模板
     * @return 结果
     */
    @Override
    public int insertTemplate(Template template)
    {
        template.setCreateTime(DateUtils.getNowDate());
        template.setInnerFlag(1L);
        Long result = templateMapper.hasName(template);
        if (result > 0) {
            throw new ServiceException("模板名称已存在！");
        }
        return templateMapper.insertTemplate(template);
    }

    /**
     * 修改模板
     * 
     * @param template 模板
     * @return 结果
     */
    @Override
    public int updateTemplate(Template template)
    {
        Template templateOld = this.selectTemplateByTmplId(template.getTmplId());
        if (templateOld == null) {
            throw new ServiceException(String.format("%1$s模板不存在", template.getName()));
        }
        if (Objects.equals(templateOld.getInnerFlag(), 0L)) {
            throw new ServiceException(String.format("%1$s为内置模板,不能修改", template.getName()));
        }
        Long result = templateMapper.hasName(template);
        if (result > 0) {
            throw new ServiceException("模板名称已存在！");
        }
        return templateMapper.updateTemplate(template);
    }

    /**
     * 批量删除模板
     * 
     * @param tmplIds 需要删除的模板主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteTemplateByTmplIds(String tmplIds)
    {
        Long[] tmplIdList = Convert.toLongArray(tmplIds);
        for (Long tmplId : tmplIdList) {
            this.deleteTemplateByTmplId(tmplId);
        }
        return 1;
    }

    /**
     * 删除模板信息
     * 
     * @param tmplId 模板主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplateByTmplId(Long tmplId)
    {
        Template template = this.selectTemplateByTmplId(tmplId);
        if (Objects.equals(template.getInnerFlag(), 0L)) {
            throw new ServiceException(String.format("%1$s为内置模板,不能删除", template.getName()));
        }

        templateMapper.deleteTemplateByTmplId(tmplId);
        templateAttributeMapper.deleteTemplateAttributeByTmplId(tmplId);
        // 删除协议
        configProtocolService.deleteConfigProtocolByConfigIds(String.valueOf(tmplId));
    }

    @Override
    public List<TemplateDataVO> exportTemplate(Template template) {
        List<Template> list = templateMapper.selectTemplateList(template);
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        List<TemplateDataVO> templateDataVOList = new ArrayList<>();
        for (Template item : list) {
            TemplateDataVO templateDataVO = BeanUtil.copyProperties(item, TemplateDataVO.class);
            templateDataVO.setCreateTime(null);
            // 属性
            templateDataVO.setAttributeList(templateAttributeMapper.selectByTmplId(item.getTmplId()));
            // 协议
            templateDataVO.setProtocolList(configProtocolService.exportByConfigId(item.getTmplId()));
            templateDataVOList.add(templateDataVO);
        }
        return templateDataVOList;
    }

    @Override
    public void importTemplate(List<TemplateDataVO> templateList) {
        if (templateList == null || templateList.isEmpty()) {
            return;
        }

        List<String> errorList = new ArrayList<>();
        for (TemplateDataVO templateDataVO : templateList) {
            try {
                if (StrUtil.isBlank(templateDataVO.getName()) || templateDataVO.getTmplId() == null) {
                    errorList.add(String.format("%s 模板id或名称不可为空", templateDataVO.getName()));
                    continue;
                }
                Template template = BeanUtil.copyProperties(templateDataVO, Template.class);
                // 模板
                Template templateOld = templateMapper.selectTemplateByTmplId(template.getTmplId());
                if (templateOld != null && templateOld.getTmplId() != null) {
                    templateMapper.updateTemplate(template);
                } else {
                    templateMapper.insertTemplate(template);
                }

                // 属性
                templateAttributeMapper.deleteTemplateAttributeByTmplId(template.getTmplId());
                if (templateDataVO.getAttributeList() != null && !templateDataVO.getAttributeList().isEmpty()) {
                    templateAttributeMapper.insertList(templateDataVO.getAttributeList());
                }

                // 协议
                configProtocolService.deleteConfigProtocolByConfigIds(String.valueOf(template.getTmplId()));
                for (ConfigProtocol protocol : templateDataVO.getProtocolList()) {
                    protocol.setConfigId(template.getTmplId());
                    configProtocolService.insertConfigProtocol(protocol);
                }
            } catch (Exception e) {
                errorList.add(String.format("%s 异常数据", templateDataVO.getName()));
            }
        }

        if (!errorList.isEmpty()) {
            throw new ServiceException(String.join("；", errorList));
        }
    }

    @Override
    public void copyTemplate(TemplateCopyVO copyVO) {
        Template template = templateMapper.selectTemplateByTmplId(copyVO.getTmplId());
        if (template == null) {
            throw new ServiceException(String.format("%1$s模板不存在！", copyVO.getTmplId()));
        }
        template.setTmplId(IdUtils.getSnowflakeId());
        template.setName(copyVO.getName());
        template.setTypeCode(copyVO.getTypeCode());
        Long result = templateMapper.hasName(template);
        if (result > 0) {
            throw new ServiceException("模板名称已存在！");
        }
        templateMapper.insertTemplate(template);

        // 属性
        List<TemplateAttribute> attributeList = templateAttributeMapper.selectByTmplId(copyVO.getTmplId());
        if (attributeList != null && !attributeList.isEmpty()) {
            attributeList.forEach(item -> {
                item.setTmplId(template.getTmplId());
                item.setTmplAttrId(IdUtils.getSnowflakeId());
            });
            templateAttributeMapper.insertList(attributeList);
        }

        // 协议
        List<ConfigProtocol> protocolList = configProtocolService.exportByConfigId(copyVO.getTmplId());
        for (ConfigProtocol protocol : protocolList) {
            protocol.setConfigId(template.getTmplId());
            configProtocolService.insertConfigProtocol(protocol);
        }
    }
}
