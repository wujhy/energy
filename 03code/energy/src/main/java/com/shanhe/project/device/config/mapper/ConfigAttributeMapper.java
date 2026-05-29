package com.shanhe.project.device.config.mapper;

import com.shanhe.project.device.config.domain.ConfigAttribute;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 设备属性Mapper接口
 * 
 * @author wjh
 * @since 2024-12-23
 */
public interface ConfigAttributeMapper 
{
    /**
     * 查询设备属性
     * 
     * @param configAttrId 设备属性主键
     * @return 设备属性
     */
    ConfigAttribute selectConfigAttributeByConfigAttrId(Long configAttrId);

    /**
     * 校验名称是否存在
     *
     * @param configAttribute 属性
     * @return 数量
     */
    Long hasName(ConfigAttribute configAttribute);

    /**
     * 查询设备属性列表
     * 
     * @param configAttribute 设备属性
     * @return 设备属性集合
     */
    List<ConfigAttribute> selectConfigAttributeList(ConfigAttribute configAttribute);

    /**
     * 查询设备属性列表（缓存）
     *
     * @return 设备属性集合
     */
    List<ConfigAttribute> configAttributeList();

    /**
     * 查询默认设备属性列表
     *
     * @return 设备属性集合
     */
    List<ConfigAttribute> selectDefaultDeviceAttributes();

    /**
     * 新增设备属性
     * 
     * @param configAttribute 设备属性
     * @return 结果
     */
    int insertConfigAttribute(ConfigAttribute configAttribute);

    /**
     * 批量插入设备属性
     *
     * @param configAttributeList 设备属性
     * @return 结果
     */
    int insertBatchConfigAttribute(List<ConfigAttribute> configAttributeList);

    /**
     * 按模板属性同步生成设备属性
     *
     * @param packNum 电池组编号
     * @param model 电池规格型号
     * @return 结果
     */
    int insertByTemplateAttribute(@Param("packNum") Integer packNum, @Param("model") Integer model);

    /**
     * 修改设备属性
     * 
     * @param configAttribute 设备属性
     * @return 结果
     */
    int updateConfigAttribute(ConfigAttribute configAttribute);

    /**
     * 修改设备属性值
     *
     * @param configAttrId 设备属性id
     * @param value 设备属性值
     * @param alarm 是否告警
     * @return 结果
     */
    int updateBy(@Param("configAttrId") Long configAttrId, @Param("value") String value, @Param("alarm") Integer alarm);

    /**
     * 删除设备属性
     * 
     * @param configAttrId 设备属性主键
     * @return 结果
     */
    int deleteConfigAttributeByConfigAttrId(Long configAttrId);

    /**
     * 批量删除设备属性
     * 
     * @param configAttrIds 需要删除的数据主键集合
     * @return 结果
     */
    int deleteConfigAttributeByConfigAttrIds(String[] configAttrIds);

    /**
     * 删除默认设备属性
     * @return 结果
     */
    int deleteDefaultDeviceAttributes();

    /**
     * 批量删除设备属性
     *
     * @param packNums 需要删除设备包
     * @return 结果
     */
    int deleteConfigAttributeByPackNums(@Param("packNums") List<Integer> packNums);

    /**
     * 导入属性
     *
     * @param attributeList 属性列表
     */
    void importAttribute(List<ConfigAttribute> attributeList);

    /**
     * 根据电池组编号和属性编码查询
     *
     * @param packNum 电池组编号
     * @param code 属性编码
     * @return 配置属性
     */
    ConfigAttribute getBy(@Param("packNum") Integer packNum, @Param("code") String code);
}
