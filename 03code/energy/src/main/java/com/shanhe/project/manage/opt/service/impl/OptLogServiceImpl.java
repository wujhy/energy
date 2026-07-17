package com.shanhe.project.manage.opt.service.impl;

import cn.hutool.core.util.ObjUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.shanhe.common.constant.Constants;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.text.Convert;
import com.shanhe.common.utils.uuid.IdUtils;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.manage.config.domain.BatteryPack;
import com.shanhe.project.manage.config.service.IBatteryPackService;
import com.shanhe.project.manage.opt.domain.OptLog;
import com.shanhe.project.manage.opt.mapper.OptLogMapper;
import com.shanhe.project.manage.opt.service.OptLogService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 设备操作日志
 *
 * @author wjh
 * @since 2025/7/9
 */
@Service
public class OptLogServiceImpl implements OptLogService {

    /** 操作日志数据访问层。 */
    @Resource
    private OptLogMapper optLogMapper;
    /** 电池组服务。 */
    @Resource
    private IBatteryPackService batteryPackService;

    CacheKeyEnum logCache = CacheKeyEnum.OPT_LOG;

    private final LogCacheAccessor cacheAccessor = new EhcacheLogCacheAccessor();

    @Override
    public Long insert(Integer packNum, Integer type, Integer result, String source) {
        OptLog optLog = new OptLog();
        optLog.setId(IdUtils.getSnowflakeId());
        optLog.setConfigId(Constants.DEFAULT_CONFIG_ID);
        optLog.setPackNum(packNum);
        optLog.setType(type);
        optLog.setResult(result);
        optLog.setSource(source);
        Date now = new Date();
        optLog.setCreateTime(now);
        optLog.setCreateTimeStr(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(now));
        optLogMapper.insert(optLog);
        if (result == null) {
            cacheAccessor.put(String.format(logCache.getKey(), optLog.getPackNum(), optLog.getType()), optLog);
        }
        return optLog.getId();
    }

    @Override
    public void update(Long id, Integer result, Date updateTime) {
        if (updateTime == null) {
            updateTime = new Date();
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String updateTimeStr = sdf.format(updateTime);
        optLogMapper.update(id, result, updateTimeStr);
        if (result != null) {
            evictRunningCache(id);
        }
    }

    @Override
    public void updateRuntime(Long id, String status, Integer result) {
        String endedAt = result == null ? null : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        optLogMapper.updateRuntime(id, status, result, endedAt);
        if (result != null) {
            evictRunningCache(id);
        }
    }

    @Override
    public void touchProgress(Long id) {
        if (id == null) {
            return;
        }
        optLogMapper.touchProgress(id);
    }

    @Override
    public List<OptLog> select(OptLog optLog) {
        List<OptLog> optLogList = optLogMapper.select(optLog);
        Map<Integer, Double> batCapacityMap = new HashMap<>(16);
        if (optLogList != null && !optLogList.isEmpty()) {
            for (OptLog log : optLogList) {
                if (log.getContent() != null) {
                    log.setParams(JSON.parseObject(log.getContent(), new TypeReference<Map<String, Object>>() {}));
                }
                Integer packNum = log.getPackNum();
                if (packNum != null && batCapacityMap.containsKey(packNum)) {
                    log.setBatCapacity(batCapacityMap.get(packNum));
                } else {
                    BatteryPack batteryPack = batteryPackService.selectBatteryInfoByPackNum(packNum);
                    if (batteryPack != null) {
                        log.setBatCapacity(batteryPack.getBatCapacity());
                        if (packNum != null) {
                            batCapacityMap.put(packNum, batteryPack.getBatCapacity());
                        }
                    }
                }
            }
        }
        return optLogList;
    }

    @Override
    public List<OptLog> selectRunningList(Integer packNum) {
        List<OptLog> list = optLogMapper.selectRunningList(packNum);
        return list == null ? Collections.emptyList() : list;
    }

    @Override
    public int deleteByIds(String ids) {
        return optLogMapper.deleteByIds(Convert.toStrArray(ids));
    }

    /** 删除默认设备操作日志 */
    @Override
    public void deleteDefaultDeviceLogs() {
        optLogMapper.deleteDefaultDeviceLogs();
        updateCache();
    }

    @Override
    public void updateCache() {
        Set<String> oldKeys = cacheAccessor.keys();
        List<OptLog> list = optLogMapper.findRunningList();
        List<String> keys = new ArrayList<>();
        for (OptLog log : list) {
            String key = String.format(logCache.getKey(), log.getPackNum(), log.getType());
            keys.add(key);
            cacheAccessor.put(key, log);
        }
        for (String key : oldKeys) {
            if (!keys.contains(key)) {
                cacheAccessor.remove(key);
            }
        }
    }

    @Override
    public OptLog getRunningOptLog(Integer packNum, Integer type) {
        return optLogMapper.getRunningOptLog(packNum, type);
    }

    @Override
    public Integer count(Integer packNum, List<Integer> types) {
        Integer count = optLogMapper.count(packNum, types);
        return count != null ? count : 0;
    }

    @Override
    public void updateBatteryCapacity(Long optId, Double dischargeCapacity, Double capacity, Double current, Date endTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String endTimeStr = sdf.format(endTime);
        // 查询最后一次放电记录
        optLogMapper.updateBattery(optId, dischargeCapacity, capacity, current, endTimeStr);
    }

    @Override
    public OptLog lastType(Integer packNum, int type) {
        return optLogMapper.lastByType(packNum, type);
    }

    @Override
    public void deleteByPackNum(Integer packNum) {
        optLogMapper.deleteByPackNum(packNum);
        updateCache();
    }

    @Override
    public void closeOptLog(Integer packNum) {
        Set<String> oldKeys = cacheAccessor.keys();
        for (String key : oldKeys) {
            OptLog log = (OptLog) cacheAccessor.get(key);
            if (log != null && ObjUtil.equals(log.getPackNum(), packNum)) {
                update(log.getId(), YesNoEnum.YES.getDictValue(), null);
            }
        }
    }

    @Override
    public OptLog selectRunningCacheLog(Integer packNum) {
        if (packNum == null) {
            return null;
        }
        OptLog latest = null;
        for (String key : cacheAccessor.keys()) {
            Object value = cacheAccessor.get(key);
            if (!(value instanceof OptLog)) {
                continue;
            }
            OptLog log = (OptLog) value;
            if (!ObjUtil.equals(log.getPackNum(), packNum)) {
                continue;
            }
            if (latest == null || latest.getCreateTime() == null
                    || (log.getCreateTime() != null && log.getCreateTime().after(latest.getCreateTime()))) {
                latest = log;
            }
        }
        return latest;
    }

    @Override
    public void doStopTest(Integer packNum, Integer type) {
        OptLog log = (OptLog) cacheAccessor.get(String.format(logCache.getKey(), packNum, type));
        if (log == null) {
            // 缓存未命中时回查数据库，避免重启或缓存丢失后残留运行日志无法关闭
            log = optLogMapper.getRunningOptLog(packNum, type);
        }
        if (log != null) {
            update(log.getId(), YesNoEnum.YES.getDictValue(), null);
        }
    }

    private void evictRunningCache(Long id) {
        if (id == null) {
            return;
        }
        for (String key : cacheAccessor.keys()) {
            Object value = cacheAccessor.get(key);
            if (value instanceof OptLog && Objects.equals(id, ((OptLog) value).getId())) {
                cacheAccessor.remove(key);
            }
        }
    }

    interface LogCacheAccessor {
        Object get(String key);
        void put(String key, Object value);
        void remove(String key);
        Set<String> keys();
    }

    private class EhcacheLogCacheAccessor implements LogCacheAccessor {
        @Override
        public Object get(String key) {
            return CacheUtils.get(logCache.getCache(), key);
        }

        @Override
        public void put(String key, Object value) {
            CacheUtils.put(logCache.getCache(), key, value);
        }

        @Override
        public void remove(String key) {
            CacheUtils.remove(logCache.getCache(), key);
        }

        @Override
        public Set<String> keys() {
            return CacheUtils.getCacheKeys(logCache.getCache());
        }
    }
}
