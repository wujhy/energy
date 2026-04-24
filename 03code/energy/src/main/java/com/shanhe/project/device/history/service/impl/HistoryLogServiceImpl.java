package com.shanhe.project.device.history.service.impl;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.github.pagehelper.PageHelper;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.StringUtils;
import com.shanhe.common.utils.file.FileUtils;
import com.shanhe.common.utils.text.Convert;
import com.shanhe.common.utils.uuid.IdUtils;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.framework.enums.DataTypeEnum;
import com.shanhe.framework.enums.DeviceTypeEnum;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.domain.ConfigAttribute;
import com.shanhe.project.device.config.service.IConfigAttributeService;
import com.shanhe.project.device.config.service.IConfigService;
import com.shanhe.project.device.history.domain.HistoryLogDTO;
import com.shanhe.project.iot.data.MessageFactory;
import com.shanhe.project.sync.domain.AlarmItemLevelVo;
import org.springframework.stereotype.Service;
import com.shanhe.project.device.history.mapper.HistoryLogMapper;
import com.shanhe.project.device.history.domain.HistoryLog;
import com.shanhe.project.device.history.service.IHistoryLogService;

import javax.annotation.Resource;

/**
 * 设备历史记录Service业务层处理
 * 
 * @author wjh
 * @since 2024-12-31
 */
@Service
public class HistoryLogServiceImpl implements IHistoryLogService
{
    @Resource
    private HistoryLogMapper historyLogMapper;
    @Resource
    private IConfigAttributeService configAttributeService;
    @Resource
    private IConfigService configService;

    // 缓存枚举
    CacheKeyEnum historyCache = CacheKeyEnum.HISTORY;

    /**
     * 查询设备历史记录
     * 
     * @param historyId 设备历史记录主键
     * @return 设备历史记录
     */
    @Override
    public HistoryLog selectHistoryLogByHistoryId(Long historyId)
    {
        HistoryLog log = historyLogMapper.selectHistoryLogByHistoryId(historyId);
        this.setParam(log);
        return log;
    }

    @Override
    public List<HistoryLog> lastList(HistoryLog historyLog)
    {
        List<HistoryLog> historyLogs = historyLogMapper.lastList(historyLog);
        for (HistoryLog log : historyLogs) {
            this.setParam(log);
        }
        return historyLogs;
    }

    /**
     * 查询设备历史记录列表
     * 
     * @param query 设备历史记录
     * @return 设备历史记录
     */
    @Override
    public List<HistoryLog> selectHistoryLogList(HistoryLog query)
    {
        List<HistoryLog> historyLogs = historyLogMapper.selectHistoryLogList(query);
        Map<Long, String> configNames = new HashMap<>(0);
        for (HistoryLog log : historyLogs) {
            ConfigAttribute attribute = configAttributeService.getCacheBy(log.getConfigId(), log.getPackNum(), log.getItemCode());
            if (attribute != null) {
                // 值翻译
                log.setItemName(attribute.getName());
                log.setDataInfo(this.getDataInfo(attribute, log.getValueInfo()));
            } else {
                log.setItemName(log.getItemCode());
            }
            if (configNames.get(log.getConfigId()) == null) {
                Config config = configService.selectConfigByConfigId(log.getConfigId());
                if (config != null) {
                    configNames.put(log.getConfigId(), config.getName());
                } else {
                    configNames.put(log.getConfigId(), "");
                }
            }
            log.setConfigName(configNames.get(log.getConfigId()));
        }
        return historyLogs;
    }

    @Override
    public List<HistoryLog> simpleList(HistoryLog historyLog) {
        return historyLogMapper.simpleList(historyLog);
    }

    @Override
    public void insertHistoryLog(ConfigAttribute attribute, String value, boolean isInsert)
    {
        // 创建新纪录
        HistoryLog historyLog = new HistoryLog();
        historyLog.setHistoryId(IdUtils.getSnowflakeId());
        historyLog.setConfigId(attribute.getConfigId());
        historyLog.setItemCode(attribute.getCode());
        historyLog.setPackNum(attribute.getPackNum());
        historyLog.setValueInfo(value);
        Date date = new Date();
        historyLog.setCreateTime(date);
        historyLog.setUpdateTime(date);
        if (isInsert) {
            MessageFactory.pushData(historyLog);
        }
        // 更新缓存
        String key = String.format(historyCache.getKey(), attribute.getConfigId(), attribute.getPackNum(), attribute.getCode());
        CacheUtils.put(historyCache.getCache(), key, historyLog);

        /* 缓存取最新记录
        String key = String.format(historyCache.getKey(), attribute.getConfigId(), attribute.getPackNum(), attribute.getCode());
        HistoryLog cacheLog = (HistoryLog) CacheUtils.get(historyCache.getCache(), key);

        // 记录存在
        if (cacheLog != null) {
            // 不跟踪历史，值不同则更新
            if (Objects.equals(YesNoEnum.NO.getDictValue(), attribute.getTrack())
                    && !StringUtils.equals(cacheLog.getValueInfo(), value)) {
                cacheLog.setValueInfo(value);
                historyLogMapper.updateHistoryLog(cacheLog);
                CacheUtils.put(historyCache.getCache(), key, cacheLog);
                return;
            }

            // 更新旧记录时间
            historyLogMapper.updateTime(cacheLog.getHistoryId());
        }

        // 记录不存在 或 记录存在、需要历史记录且值不同
        if (cacheLog == null || (Objects.equals(YesNoEnum.YES.getDictValue(), attribute.getTrack())
                && !StringUtils.equals(cacheLog.getValueInfo(), value))) {
            // 创建新纪录
            HistoryLog historyLog = new HistoryLog();
            historyLog.setHistoryId(IdUtils.getSnowflakeId());
            historyLog.setConfigId(attribute.getConfigId());
            historyLog.setItemCode(attribute.getCode());
            historyLog.setPackNum(attribute.getPackNum());
            historyLog.setValueInfo(value);
            historyLogMapper.insertHistoryLog(historyLog);
            // 更新缓存
            CacheUtils.put(historyCache.getCache(), key, historyLog);
        }
         */
    }

    /**
     * 批量删除设备历史记录
     * 
     * @param historyIds 需要删除的设备历史记录主键
     * @return 结果
     */
    @Override
    public int deleteHistoryLogByHistoryIds(String historyIds)
    {
        String[] historyIdArr = Convert.toStrArray(historyIds);
        for (String id : historyIdArr) {
            HistoryLog log = historyLogMapper.selectHistoryLogByHistoryId(Long.valueOf(id));
            if (log != null) {
                // 记录相同，删除
                String key = String.format(historyCache.getKey(), log.getConfigId(), log.getPackNum(), log.getItemCode());
                HistoryLog logCache = (HistoryLog) CacheUtils.get(historyCache.getCache(), key);
                if (logCache != null && Objects.equals(logCache.getHistoryId(), log.getHistoryId())) {
                    CacheUtils.remove(historyCache.getCache(), key);
                }
                historyLogMapper.deleteHistoryLogByHistoryId(log.getHistoryId());
            }
        }
        return 1;
    }

    @Override
    public void updateCache() {
        // 查询设备历史
        List<HistoryLog> historyLogOldList = historyLogMapper.cacheHistoryLog();
        if (historyLogOldList == null || historyLogOldList.isEmpty()) {
            return;
        }
        for (HistoryLog historyLog : historyLogOldList) {
            /* alarm.配置id.包编号.模块编号.属性编码 */
            String key = String.format(historyCache.getKey(), historyLog.getConfigId(), historyLog.getPackNum(), historyLog.getItemCode());
            CacheUtils.put(historyCache.getCache(), key, historyLog);
        }
    }

    @Override
    public HistoryLog lastValue(ConfigAttribute attribute) {
        HistoryLog historyLog;
        // 先从缓存获取值
        String key = String.format(historyCache.getKey(), attribute.getConfigId(), attribute.getPackNum(), attribute.getCode());
        Object object = CacheUtils.get(historyCache.getCache(), key);
        if (object == null) {
            return null;
        }

        historyLog = (HistoryLog)object;
        // 数据处理
        this.setParam(historyLog);

        return historyLog;
    }

    @Override
    public String getCacheBy(Long configId, Integer packNum, String code) {
        String key = String.format(historyCache.getKey(), configId, packNum, code);
        Object object = CacheUtils.get(historyCache.getCache(), key);
        if (object != null) {
            return ((HistoryLog)object).getValueInfo();
        }
        return null;
    }

    @Override
    public void deleteHistoryLog(Integer dayNum) {
        historyLogMapper.deleteHistoryLog(dayNum);
    }

    @Override
    public void deleteByConfigId(Long configId) {
        historyLogMapper.deleteByConfigId(configId);
    }

    @Override
    public void export(HistoryLog params) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");

        String fileName = FileUtils.getUsbPath(params.getExportPath()) + "历史数据_" + sdf.format(new Date()) + ".xlsx";
        // 确保目录存在
        File file = new File(fileName);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        Long count = historyLogMapper.selectCount(params);

        try (ExcelWriter excelWriter = EasyExcel.write(fileName, HistoryLogDTO.class).build()) {
            // 这里注意 如果同一个sheet只要创建一次
            WriteSheet writeSheet = EasyExcel.writerSheet("模板").build();
            for (int i = 0; i < count; i += 1000) {
                PageHelper.startPage(i / 1000 + 1, 1000, true, false, true);
                List<HistoryLog> list = selectHistoryLogList(params);

                /*数据组装*/
                List<HistoryLogDTO> excelDataList = list.stream().map(HistoryLogDTO::of).collect(Collectors.toList());

                excelWriter.write(excelDataList, writeSheet);
            }
        }
    }

    /**
     * 值处理
     */
    private void setParam(HistoryLog historyLog) {
        if (historyLog == null) {
            return;
        }
        ConfigAttribute attribute = configAttributeService.getCacheBy(historyLog.getConfigId(), historyLog.getPackNum(), historyLog.getItemCode());
        // 值翻译
        historyLog.setDataInfo(this.getDataInfo(attribute, historyLog.getValueInfo()));
    }

    /**
     * 值处理
     */
    private String getDataInfo(ConfigAttribute configAttribute, String value) {
        if (StrUtil.isBlank(value) || configAttribute == null) {
            return null;
        }
        // 数据处理类型
        DataTypeEnum dataTypeEnum = DataTypeEnum.find(configAttribute.getType());
        switch (dataTypeEnum) {
            case _2:
                // 模拟量（值加单位）
                if (StrUtil.isNotBlank(configAttribute.getUnit())) {
                    return value + configAttribute.getUnit();
                }
                break;
            case _1:
                // 开关量
            case _3:
                // 枚举量
                for (AlarmItemLevelVo levelVo : configAttribute.getListLevel()) {
                    if (StrUtil.equals(value, levelVo.getDictId()) && StrUtil.isNotBlank(levelVo.getDictName())) {
                        return levelVo.getDictName();
                    }
                }
                break;
            default:
                break;
        }
        return value;
    }
}
