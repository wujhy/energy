package com.shanhe.project.manage.config.service;

import java.util.List;
import com.shanhe.project.manage.config.domain.ConfigAttribute;
import com.shanhe.project.manage.config.domain.ConfigAttributeListVO;
import com.shanhe.project.manage.config.domain.ConfigAttributeVO;

/**
 * 设备属性Service接口
 * 
 * @author wjh
 * @since 2024-12-23
 */
public interface IConfigAttributeService 
{
    /**
     * 查询设备属性
     * 
     * @param configAttrId 设备属性主键
     * @return 设备属性
     */
    ConfigAttribute selectConfigAttributeByConfigAttrId(Long configAttrId);

    /**
     * 查询默认设备属性列表
     *
     * @return 设备属性集合
     */
    List<ConfigAttribute> selectDefaultDeviceAttributes();

    /**
     * 查询设备属性列表
     * 
     * @param configAttribute 设备属性
     * @return 设备属性集合
     */
    List<ConfigAttribute> selectConfigAttributeList(ConfigAttribute configAttribute);

    /**
     * 查询设备属性列表
     *
     * @param configAttribute 设备属性
     * @return 设备属性集合
     */
    List<ConfigAttributeVO> viewList(ConfigAttribute configAttribute);

    /**
     * 查询设备属性下拉列表
     *
     * @param configAttribute 设备属性
     * @return 设备属性下拉列表
     */
    List<ConfigAttributeListVO> selectList(ConfigAttribute configAttribute);

    /**
     * 新增设备属性
     * 
     * @param configAttribute 设备属性
     * @param isValid 是否校验
     * @return 结果
     */
    int insertConfigAttribute(ConfigAttribute configAttribute, boolean isValid);

    /**
     * 批量插入设备属性
     *
     * @param configAttributeList 设备属性
     */
    void insertBatchConfigAttribute(List<ConfigAttribute> configAttributeList);

    /**
     * 按模板属性同步生成默认设备属性
     *
     * @param packNum 电池组编号
     * @param model 电池规格型号
     */
    void insertByTemplateAttribute(Integer packNum, Integer model);

    /**
     * 修改设备属性
     * 
     * @param configAttribute 设备属性
     * @return 结果
     */
    int updateConfigAttribute(ConfigAttribute configAttribute);

    /**
     * 修改设备属性（同步接口）
     *
     * @param configAttribute 设备属性
     */
    void updateConfigAttributeBySyn(ConfigAttribute configAttribute);

    /**
     * 修改设备属性（来自设备告警）
     *
     * @param configAttribute 设备属性
     */
    void updateConfigAttributeAlarm(ConfigAttribute configAttribute);

    /**
     * 批量删除设备属性
     * 
     * @param configAttrIds 需要删除的设备属性主键集合
     * @return 结果
     */
    int deleteConfigAttributeByConfigAttrIds(String configAttrIds);

    /**
     * 删除设备属性信息
     *
     * @param configAttribute 设备属性
     */
    void deleteConfigAttribute(ConfigAttribute configAttribute);

    /**
     * 删除默认设备所有属性
     */
    void deleteDefaultDeviceAttributes();

    /**
     * 批量删除设备属性
     *
     * @param packNums 需要删除设备包
     */
    void deleteConfigAttributeByPackNums(List<Integer> packNums);

    /**
     * 导入属性
     *
     * @param attributeList 属性列表
     */
    void importAttribute(List<ConfigAttribute> attributeList);

    /**
     * 获取默认设备属性
     *
     * @param packNum 包编码
     * @param code 属性编码
     * @return 属性信息
     */
    ConfigAttribute getBy(Integer packNum, String code);

    /**
     * 通过缓存获取默认设备属性
     *
     * @param packNum 包编码
     * @param code 属性编码
     * @return 属性信息
     */
    ConfigAttribute getCacheBy(Integer packNum, String code);

    /**
     * 通过缓存获取默认设备属性
     *
     * @param code 属性编码
     * @return 属性信息
     */
    ConfigAttribute getCacheBy(String code);

    /**
     * 通过缓存获取默认设备属性名
     *
     * @param packNum 包
     * @param code 属性编码
     * @return 名
     */
    String getNameByCache(Integer packNum, String code);

    /**
     * 更新全部缓存
     */
    void updateCache();

    /**
     * 更新默认设备属性缓存
     *
     * @param isUpdate 是否更新
     */
    void updateCache(Integer isUpdate);

    /**
     * 获取缓存属性列表
     *
     * @return 属性列表
     */
    List<ConfigAttribute> cacheAttributeList();
}
