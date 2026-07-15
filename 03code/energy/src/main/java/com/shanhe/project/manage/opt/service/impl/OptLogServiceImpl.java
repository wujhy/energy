package com.shanhe.project.manage.opt.service.impl;

import cn.hutool.core.util.ObjUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.shanhe.common.constant.Constants;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.text.Convert;
import com.shanhe.common.utils.uuid.IdUtils;
import com.shanhe.framework.enums.BatteryTestEnum;
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
    private LogCacheAccessor cacheAccessor = new EhcacheLogCacheAccessor();

    private static final int INTERNAL_RESISTANCE_SLOT = 0;
    private static final int GENERAL_TEST_SLOT = 1;

    /**
     * 插入操作日志
     *
     * @param packNum 电池组编号
     * @param type 操作类型
     * @param result 操作结果
     * @return 日志ID
     */
    @Override
    public Long insert(Integer packNum, Integer type, Integer result) {
        return insert(packNum, type, result, null);
    }

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
            cacheRunning(optLog);
        }
        return optLog.getId();
    }

    /**
     * 插入操作日志
     *
     * @param params 操作参数
     * @param result 操作结果
     * @return 日志ID
     */
    @Override
    public Long insert(Map<String, Object> params, Integer result) {
        OptLog optLog = new OptLog();
        optLog.setId(IdUtils.getSnowflakeId());
        optLog.setConfigId(Constants.DEFAULT_CONFIG_ID);
        optLog.setContent(JSON.toJSONString(params));
        optLog.setResult(result);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        optLog.setCreateTimeStr(sdf.format(new Date()));
        optLogMapper.insert(optLog);
        return optLog.getId();
    }







    /** 判断是否需要插入操作日志。 */
    private void sotOptLog(Object object, String cacheKey, Date endTimeSource) {
        if (object == null) {
            return;
        }
        OptLog optLog = (OptLog) object;
        // 未运行，缓存记录未结束则更新并清除缓存

        // 没保存，则不更新
        cacheAccessor.remove(cacheKey);

        if (!optLog.isSave()) {
            return;
        }

        // 当前时间减 5 秒
        Date endTime = null;
        if (endTimeSource == null) {
            endTime = new Date(System.currentTimeMillis() - 5000);
        } else {
            endTime = endTimeSource;
        }
        update(optLog.getId(), YesNoEnum.YES.getDictValue(), endTime);
    }



    /**
     * 更新操作日志
     *
     * @param id 日志ID
     * @param result 操作结果
     * @param updateTime 更新时间
     */
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

    /**
     * 查询操作日志列表
     *
     * @param optLog 查询条件
     * @return 操作日志列表
     */
    @Override
    public void updateRuntime(Long id, String status, Integer result) {
        String endedAt = result == null ? null : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        optLogMapper.updateRuntime(id, status, result, endedAt);
        if (result != null) {
            evictRunningCache(id);
        }
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


    /**
     * 查询运行中的操作日志。
     *
     * @param packNum 电池组编号；为空时查询全部
     * @return 运行中日志列表
     */
    @Override
    public List<OptLog> selectRunningList(Integer packNum) {
        List<OptLog> list = optLogMapper.selectRunningList(packNum);
        return list == null ? Collections.emptyList() : list;
    }
    /**
     * 根据ID批量删除操作日志
     *
     * @param ids 日志ID字符串，逗号分隔
     * @return 结果
     */
    @Override
    public int deleteByIds(String ids) {
        return optLogMapper.deleteByIds(Convert.toStrArray(ids));
    }

    /** 删除默认设备操作日志 */
    @Override
    public void deleteDefaultDeviceLogs() {
        optLogMapper.deleteDefaultDeviceLogs();
    }

    /** 更新操作日志缓存 */
    @Override
    public void updateCache() {
        List<OptLog> list = optLogMapper.findRunningList();
        if (list == null) {
            return;
        }
        Set<String> oldKeys = cacheAccessor.keys();
        for (String key : oldKeys) {
            cacheAccessor.remove(key);
        }
        for (OptLog log : list) {
            String key = cacheKey(log.getPackNum(), log.getType());
            if (cacheAccessor.get(key) == null) {
                cacheRunning(log);
            }
        }
    }

    private void cacheRunning(OptLog log) {
        if (log == null || log.getPackNum() == null || log.getType() == null) {
            return;
        }
        log.setSave(true);
        log.setCount(100);
        cacheAccessor.put(cacheKey(log.getPackNum(), log.getType()), log);
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

    private String cacheKey(Integer packNum, Integer type) {
        int slot = BatteryTestEnum._1.getDictValue().equals(type)
                ? INTERNAL_RESISTANCE_SLOT : GENERAL_TEST_SLOT;
        return String.format(logCache.getKey(), packNum, slot);
    }
    /**
     * 从缓存获取未完成的操作日志
     *
     * @param packNum 电池组编号
     * @param type 操作类型
     * @return 操作日志
     */
    @Override
    public OptLog selectNotFinishedCacheLog(Integer packNum, Integer type) {
        // 缓存记录
        String cacheKey = String.format(logCache.getKey(), packNum, type);
        Object object = cacheAccessor.get(cacheKey);
        if (object == null) {
            return null;
        }
        return (OptLog) object;
    }

    /**
     * 获取运行中的操作日志
     *
     * @param packNum 电池组编号
     * @param type 操作类型
     * @return 操作日志
     */
    @Override
    public OptLog getRunningOptLog(Integer packNum, Integer type) {
        return optLogMapper.getRunningOptLog(packNum, type);
    }

    /**
     * 统计操作日志数量
     *
     * @param packNum 电池组编号
     * @param types 操作类型列表
     * @return 日志数量
     */
    @Override
    public Integer count(Integer packNum, List<Integer> types) {
        Integer count = optLogMapper.count(packNum, types);
        if (count != null) {
            return count;
        }
        return 0;
    }

    /**
     * 更新电池容量信息
     *
     * @param optId 操作日志ID
     * @param dischargeCapacity 放电容量
     * @param bcapacity 电池容量
     * @param current 电流
     * @param endTime 结束时间
     */
    @Override
    public void updateBatteryBcapacity(Long optId, Double dischargeCapacity, Double bcapacity, Double current, Date endTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String endTimeStr = sdf.format(endTime);
        // 查询最后一次放电记录
        optLogMapper.updateBattery(optId, dischargeCapacity, bcapacity, current, endTimeStr);
    }

    /**
     * 获取指定类型的最新操作日志
     *
     * @param packNum 电池组编号
     * @param type 操作类型
     * @return 操作日志
     */
    @Override
    public OptLog lastType(Integer packNum, int type) {
        return optLogMapper.lastByType(packNum, type);
    }

    /**
     * 删除指定电池组的操作日志
     *
     * @param packNum 电池组编号
     */
    @Override
    public void deleteByPackNum(Integer packNum) {
        optLogMapper.deleteByPackNum(packNum);
    }

    /**
     * 关闭指定电池组的操作日志
     *
     * @param packNum 电池组编号
     */
    @Override
    public void closeOptLog(Integer packNum) {
        Set<String> oldKeys = cacheAccessor.keys();
        for (String key : oldKeys) {
            OptLog log = (OptLog) cacheAccessor.get(key);
            if (log != null && ObjUtil.equals(log.getConfigId(), Constants.DEFAULT_CONFIG_ID) && ObjUtil.equals(log.getPackNum(), packNum)) {
                cacheAccessor.remove(key);
                update(log.getId(), YesNoEnum.YES.getDictValue(), null);
            }
        }
    }

    /**
     * 停止测试
     *
     * @param packNum 电池组编号
     * @param type 操作类型
     */
    @Override
    public void doStopTest(Integer packNum, Integer type) {
        String cacheKey = cacheKey(packNum, type);
        OptLog log = (OptLog) cacheAccessor.get(cacheKey);
        if (log == null || !Objects.equals(log.getType(), type)) {
            log = optLogMapper.getRunningOptLog(packNum, type);
            if (log == null) {
                return;
            }
            log.setSave(true);
        }
        sotOptLog(log, cacheKey, (Date) null);
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
