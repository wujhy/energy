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
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        optLog.setCreateTimeStr(sdf.format(new Date()));
        optLogMapper.insert(optLog);

        // 当前状态运行中，需要把旧记录运行中的置为超时
        if (result == null) {
            Object object = CacheUtils.get(logCache.getCache(),
                    String.format(logCache.getKey(), optLog.getPackNum(), optLog.getType()));
            if (object != null) {
                update(((OptLog) object).getId(), 2, null);
            }
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
        CacheUtils.remove(logCache.getCache(), cacheKey);

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
    }

    /**
     * 查询操作日志列表
     *
     * @param optLog 查询条件
     * @return 操作日志列表
     */
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
        // 旧缓存
        List<String> startKeys = new ArrayList<>();
        Set<String> oldKeys = CacheUtils.getCacheKeys(logCache.getCache());

        // 所有未完日志
        List<OptLog> list = optLogMapper.findRunningList();
        for (OptLog log : list) {
            // 电池测试
            int type = 1;
            if (BatteryTestEnum._1.getDictValue().equals(log.getType())) {
                // 内阻测试
                type = 0;
            }
            // 缓存
            String key = String.format(logCache.getKey(), log.getPackNum(), type);
            // 存在重复数据，时间排序靠后的完成掉（脏数据）
            Object object = CacheUtils.get(logCache.getCache(), key);
            if (object != null) {
                OptLog old = (OptLog) object;
                if (!Objects.equals(old.getId(), log.getId())) {
                    update(log.getId(), YesNoEnum.YES.getDictValue(), null);
                    continue;
                }
                log.setSave(old.isSave());
                log.setCount(old.getCount());
            } else {
                log.setSave(true);
                log.setCount(100);
            }
            startKeys.add(key);
            CacheUtils.put(logCache.getCache(), key, log);
        }

        // 删除
        for (String key : oldKeys) {
            if (!startKeys.contains(key)) {
                CacheUtils.remove(logCache.getCache(), key);
            }
        }
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
        Object object = CacheUtils.get(logCache.getCache(), cacheKey);
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
        Set<String> oldKeys = CacheUtils.getCacheKeys(logCache.getCache());
        for (String key : oldKeys) {
            OptLog log = (OptLog) CacheUtils.get(logCache.getCache(), key);
            if (log != null && ObjUtil.equals(log.getConfigId(), Constants.DEFAULT_CONFIG_ID) && ObjUtil.equals(log.getPackNum(), packNum)) {
                CacheUtils.remove(logCache.getCache(), key);
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
        // 缓存记录
        int keyType = 1;
        if (BatteryTestEnum._1.getDictValue().equals(type)) {
            // 内阻测试
            keyType = 0;
        }
        String cacheKey = String.format(logCache.getKey(), packNum, keyType);
        OptLog log = (OptLog) CacheUtils.get(logCache.getCache(), cacheKey);
        if (log == null) {
            log = optLogMapper.getRunningOptLog(packNum, type);
            if (log == null) {
                return;
            }
        }
        // 类型一致
        if (!log.getType().equals(type)) {
            return;
        }
        sotOptLog(log, cacheKey, (Date) null);
    }
}
