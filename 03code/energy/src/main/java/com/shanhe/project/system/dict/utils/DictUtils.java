package com.shanhe.project.system.dict.utils;

import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.StringUtils;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.project.system.dict.domain.SysDictData;

import java.util.List;

/**
 * 字典工具类
 * 
 * @author ruoyi
 */
public class DictUtils
{
    /**
     * 分隔符
     */
    public static final String SEPARATOR = ",";

    static CacheKeyEnum dictCache = CacheKeyEnum.DICT;

    /**
     * 设置字典缓存
     * 
     * @param key 参数键
     * @param dictDataList 字典数据列表
     */
    public static void setDictCache(String key, List<SysDictData> dictDataList)
    {
        CacheUtils.put(getCacheName(), getCacheKey(key), dictDataList);
    }

    /**
     * 获取字典缓存
     * 
     * @param key 参数键
     */
    public static List<SysDictData> getDictCache(String key)
    {
        Object cacheObj = CacheUtils.get(getCacheName(), getCacheKey(key));
        if (StringUtils.isNotNull(cacheObj)) {
            return StringUtils.cast(cacheObj);
        }
        return null;
    }

    /**
     * 根据字典类型和字典值获取字典标签
     * 
     * @param dictType 字典类型
     * @param dictValue 字典值
     * @return 字典标签
     */
    public static String getDictLabel(String dictType, String dictValue)
    {
        return getDictLabel(dictType, dictValue, SEPARATOR);
    }

    /**
     * 根据字典类型和字典标签获取字典值
     * 
     * @param dictType 字典类型
     * @param dictLabel 字典标签
     * @return 字典值
     */
    public static String getDictValue(String dictType, String dictLabel)
    {
        return getDictValue(dictType, dictLabel, SEPARATOR);
    }

    /**
     * 根据字典类型和字典值获取字典标签
     * 
     * @param dictType 字典类型
     * @param dictValue 字典值
     * @param separator 分隔符
     * @return 字典标签
     */
    public static String getDictLabel(String dictType, String dictValue, String separator)
    {
        StringBuilder propertyString = new StringBuilder();
        List<SysDictData> dictDataList = getDictCache(dictType);
        if (dictDataList == null) {
            return null;
        }

        if (StringUtils.containsAny(separator, dictValue) && StringUtils.isNotEmpty(dictDataList)) {
            for (SysDictData dict : dictDataList) {
                for (String value : dictValue.split(separator)) {
                    if (value.equals(dict.getDictValue())) {
                        propertyString.append(dict.getDictLabel()).append(separator);
                        break;
                    }
                }
            }
        } else {
            for (SysDictData dict : dictDataList) {
                if (dictValue.equals(dict.getDictValue())) {
                    return dict.getDictLabel();
                }
            }
        }
        return StringUtils.stripEnd(propertyString.toString(), separator);
    }

    /**
     * 删除指定字典缓存
     * 
     * @param key 字典键
     */
    public static void removeDictCache(String key)
    {
        CacheUtils.remove(getCacheName(), getCacheKey(key));
    }

    /**
     * 根据字典类型和字典标签获取字典值
     * 
     * @param dictType 字典类型
     * @param dictLabel 字典标签
     * @param separator 分隔符
     * @return 字典值
     */
    public static String getDictValue(String dictType, String dictLabel, String separator)
    {
        StringBuilder propertyString = new StringBuilder();
        List<SysDictData> dictDataList = getDictCache(dictType);
        if (dictDataList == null) {
            return null;
        }
        if (StringUtils.containsAny(separator, dictLabel) && StringUtils.isNotEmpty(dictDataList)) {
            for (SysDictData dict : dictDataList) {
                for (String label : dictLabel.split(separator)) {
                    if (label.equals(dict.getDictLabel())) {
                        propertyString.append(dict.getDictValue()).append(separator);
                        break;
                    }
                }
            }
        } else {
            for (SysDictData dict : dictDataList) {
                if (dictLabel.equals(dict.getDictLabel())) {
                    return dict.getDictValue();
                }
            }
        }
        return StringUtils.stripEnd(propertyString.toString(), separator);
    }

    /**
     * 清空字典缓存
     */
    public static void clearDictCache()
    {
        CacheUtils.removeAll(getCacheName());
    }

    /**
     * 获取cache name
     * 
     * @return 缓存名
     */
    public static String getCacheName()
    {
        return dictCache.getCache();
    }

    /**
     * 设置cache key
     * 
     * @param configKey 参数键
     * @return 缓存键key
     */
    public static String getCacheKey(String configKey)
    {
        return dictCache.getKey() + configKey;
    }
}
