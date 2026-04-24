package com.shanhe.project.device.template.service.impl;

import java.util.List;
import java.util.Objects;

import com.shanhe.common.exception.ServiceException;
import com.shanhe.common.utils.LinearCalculator;
import com.shanhe.framework.enums.DataTypeEnum;
import com.shanhe.framework.enums.YesNoEnum;
import org.springframework.stereotype.Service;
import com.shanhe.project.device.template.mapper.TemplateAttributeMapper;
import com.shanhe.project.device.template.domain.TemplateAttribute;
import com.shanhe.project.device.template.service.ITemplateAttributeService;
import com.shanhe.common.utils.text.Convert;

import javax.annotation.Resource;

/**
 * 模板属性Service业务层处理
 * 
 * @author wjh
 * @since 2024-12-23
 */
@Service
public class TemplateAttributeServiceImpl implements ITemplateAttributeService 
{
    @Resource
    private TemplateAttributeMapper templateAttributeMapper;

    /**
     * 查询模板属性
     * 
     * @param tmplAttrId 模板属性主键
     * @return 模板属性
     */
    @Override
    public TemplateAttribute selectTemplateAttributeByTmplAttrId(Long tmplAttrId)
    {
        return templateAttributeMapper.selectTemplateAttributeByTmplAttrId(tmplAttrId);
    }

    /**
     * 查询模板属性列表
     * 
     * @param templateAttribute 模板属性
     * @return 模板属性
     */
    @Override
    public List<TemplateAttribute> selectTemplateAttributeList(TemplateAttribute templateAttribute)
    {
        return templateAttributeMapper.selectTemplateAttributeList(templateAttribute);
    }

    /**
     * 新增模板属性
     * 
     * @param templateAttribute 模板属性
     * @return 结果
     */
    @Override
    public int insertTemplateAttribute(TemplateAttribute templateAttribute)
    {
        this.check(templateAttribute);
        return templateAttributeMapper.insertTemplateAttribute(templateAttribute);
    }

    /**
     * 修改模板属性
     * 
     * @param templateAttribute 模板属性
     * @return 结果
     */
    @Override
    public int updateTemplateAttribute(TemplateAttribute templateAttribute)
    {
        this.check(templateAttribute);
        templateAttribute.setUnit(templateAttribute.getUnit() == null ? "" : templateAttribute.getUnit());
        templateAttribute.setPoint(templateAttribute.getPoint() == null ? 0 : templateAttribute.getPoint());
        return templateAttributeMapper.updateTemplateAttribute(templateAttribute);
    }

    private void check(TemplateAttribute templateAttribute){
        Long result = templateAttributeMapper.hasName(templateAttribute);
        if (result > 0) {
            throw new ServiceException("模板属性名或编码已存在！");
        }
        // 模拟量、补充线性值
        if (Objects.equals(templateAttribute.getType(), DataTypeEnum._2.getDictValue())
                && Objects.equals(templateAttribute.getIsLinear(), YesNoEnum.YES.getDictValue())) {
            double[] section = LinearCalculator.findIntersection(
                    templateAttribute.getMinOrigRange(),templateAttribute.getMinTargetRange(),
                    templateAttribute.getMaxOrigRange(),templateAttribute.getMaxTargetRange());
            if(section != null){
                templateAttribute.setSpk(section[0]);
                templateAttribute.setSpb(section[1]);
            }
        }
    }

    /**
     * 批量删除模板属性
     * 
     * @param tmplAttrIds 需要删除的模板属性主键
     * @return 结果
     */
    @Override
    public int deleteTemplateAttributeByTmplAttrIds(String tmplAttrIds)
    {
        return templateAttributeMapper.deleteTemplateAttributeByTmplAttrIds(Convert.toStrArray(tmplAttrIds));
    }

    /**
     * 删除模板属性信息
     * 
     * @param tmplAttrId 模板属性主键
     * @return 结果
     */
    @Override
    public int deleteTemplateAttributeByTmplAttrId(Long tmplAttrId)
    {
        return templateAttributeMapper.deleteTemplateAttributeByTmplAttrId(tmplAttrId);
    }
}
