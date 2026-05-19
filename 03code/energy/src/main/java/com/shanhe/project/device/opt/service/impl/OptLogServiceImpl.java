package com.shanhe.project.device.opt.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.shanhe.common.constant.Constants;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.text.Convert;
import com.shanhe.common.utils.uuid.IdUtils;
import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.domain.BatteryReportLog;
import com.shanhe.project.device.config.service.IBatteryPackService;
import com.shanhe.project.device.opt.domain.OptLog;
import com.shanhe.project.device.opt.mapper.OptLogMapper;
import com.shanhe.project.device.opt.service.OptLogService;
import org.springframework.scheduling.annotation.Async;
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

    @Resource
    private OptLogMapper optLogMapper;
    @Resource
    private IBatteryPackService batteryPackService;

    CacheKeyEnum logCache = CacheKeyEnum.OPT_LOG;

    @Override
    public Long insert(Integer packNum, Integer type, Integer result) {
        OptLog optLog = new OptLog();
        optLog.setId(IdUtils.getSnowflakeId());
        optLog.setConfigId(Constants.DEFAULT_CONFIG_ID);
        optLog.setPackNum(packNum);
        optLog.setType(type);
        optLog.setResult(result);
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

    @Async
    @Override
    public void insertBattery(Integer packNum, Map<String, Object> packMap, BatteryReportLog oldInfo) {
        // 备电测试记录
        this.batteryTest(packNum, packMap, oldInfo);

        // 内阻测试记录
        this.resistanceTest(packNum, packMap, oldInfo);
    }

    private void batteryTest(Integer packNum, Map<String, Object> packMap, BatteryReportLog oldInfo) {
        // 电池状态0：监控1：充电2：停电3：核容4：未连接5：备电6：空闲
        String batteryPackStatus = (String) packMap.get("batteryPackStatus");
        Integer type = getTestType(batteryPackStatus);

        // 缓存记录
        String cacheKey = String.format(logCache.getKey(), packNum, 1);
        Object object = CacheUtils.get(logCache.getCache(), cacheKey);

        // type == null 不需要记录，只需要结束
        if (null == type) {
            this.sotOptLog(object, cacheKey, oldInfo);
        } else {

            if (object == null) {
                // 创建新纪录
                this.create(packNum, type, cacheKey);
            } else {
                // 缓存记录
                OptLog oldOptLog = (OptLog) object;
                if (!type.equals(oldOptLog.getType())) {
                    this.sotOptLog(object, cacheKey, oldInfo);

                    this.create(packNum, type, cacheKey);
                } else {
                    insert(oldOptLog, cacheKey);
                }
            }
        }

    }

    private void insert(OptLog oldOptLog, String cacheKey) {
        // 已保存
        if (oldOptLog.isSave()) {
            return;
        }

        // 达到次数
        if (oldOptLog.getCount() >= 1) {
            oldOptLog.setSave(true);

            Date createTime = oldOptLog.getCreateTime();
            if (createTime == null) {
                createTime = new Date();
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            oldOptLog.setCreateTimeStr(sdf.format(createTime));
            optLogMapper.insert(oldOptLog);
        } else {
            oldOptLog.setCount(oldOptLog.getCount() + 1);
        }
        CacheUtils.put(logCache.getCache(), cacheKey, oldOptLog);
    }

    private void create(Integer packNum, Integer type, String cacheKey) {
        // 创建新纪录
        OptLog optLog = new OptLog();
        optLog.setId(IdUtils.getSnowflakeId());
        optLog.setConfigId(Constants.DEFAULT_CONFIG_ID);
        optLog.setPackNum(packNum);
        optLog.setType(type);

        optLog.setCreateTime(new Date());
        optLog.setCount(1);
        optLog.setSave(false);
        // 缓存数据
        CacheUtils.put(logCache.getCache(), cacheKey, optLog);
    }

    private void sotOptLog(Object object, String cacheKey, BatteryReportLog oldInfo) {
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
        if (oldInfo == null || oldInfo.getCreateTime() == null) {
            endTime = new Date(System.currentTimeMillis() - 5000);
        } else {
            endTime = oldInfo.getCreateTime();
        }
        update(optLog.getId(), YesNoEnum.YES.getDictValue(), endTime);
    }

    /**
     * 状态转换
     */
    private Integer getTestType(String batteryPackStatus) {
        // 电池状态0：监控1：充电2：停电3：核容4：未连接5：备电6：空闲
        Integer testType = null;

        if (StrUtil.equals("1", batteryPackStatus)) {
            testType = BatteryTestEnum._7.getDictValue();

        } else if (StrUtil.equals("3", batteryPackStatus)) {
            testType = BatteryTestEnum._3.getDictValue();

        } else if (StrUtil.equals("5", batteryPackStatus)) {
            testType = BatteryTestEnum._5.getDictValue();

        } else if (StrUtil.equals("6", batteryPackStatus)) {
            testType = BatteryTestEnum._4.getDictValue();
        }
        return testType;
    }


    /**
     * 运行类型日志
     *
     * @param packNum 电池包序号
     * @param packMap 电池组数据
     */
    private void resistanceTest(Integer packNum, Map<String, Object> packMap, BatteryReportLog oldInfo) {
        // 0表示不在内阻测试、6表示正在内阻测试、7表示内阻测试正常结束、8表示内阻测试异常结束
        String resistanceTestStatus = (String) packMap.get("resistanceTestStatus");

        // 缓存记录
        String cacheKey = String.format(logCache.getKey(), packNum, 0);
        Object object = CacheUtils.get(logCache.getCache(), cacheKey);
        // 是否运行
        if (StrUtil.equals("6", resistanceTestStatus)) {
            // 记录不存在创建
            if (object == null) {
                // 创建新纪录
                this.create(packNum, BatteryTestEnum._1.getDictValue(), cacheKey);
            } else {
                // 缓存记录
                OptLog oldOptLog = (OptLog) object;
                insert(oldOptLog, cacheKey);
            }
        } else {
            this.sotOptLog(object, cacheKey, oldInfo);
        }
    }

    @Override
    public void update(Long id, Integer result, Date updateTime) {
        if (updateTime == null) {
            updateTime = new Date();
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String updateTimeStr = sdf.format(updateTime);
        optLogMapper.update(id, result, updateTimeStr);
    }

    @Override
    public List<OptLog> select(OptLog optLog) {
        List<OptLog> optLogList = optLogMapper.select(optLog);
        Map<Integer, Double> batCapacityMap = new HashMap<>();
        if (optLogList != null && !optLogList.isEmpty()) {
            for (OptLog log : optLogList) {
                if (log.getContent() != null) {
                    log.setParams(JSON.parseObject(log.getContent(), Map.class));
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
    public int deleteByIds(String ids) {
        return optLogMapper.deleteByIds(Convert.toStrArray(ids));
    }

    @Override
    public void deleteDefaultDeviceLogs() {
        optLogMapper.deleteByConfigIds(Convert.toStrArray(String.valueOf(Constants.DEFAULT_CONFIG_ID)));
    }

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

    @Override
    public OptLog getRunningOptLog(Integer packNum, Integer type) {
        return optLogMapper.getRunningOptLog(Constants.DEFAULT_CONFIG_ID, packNum, type);
    }

    @Override
    public Integer count(Integer packNum, List<Integer> types) {
        Integer count = optLogMapper.count(Constants.DEFAULT_CONFIG_ID, packNum, types);
        if (count != null) {
            return count;
        }
        return 0;
    }

    @Override
    public void updateBatteryBcapacity(Long optId, Double dischargeCapacity, Double bcapacity, Double current, Date endTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String endTimeStr = sdf.format(endTime);
        // 查询最后一次放电记录
        optLogMapper.updateBattery(optId, dischargeCapacity, bcapacity, current, endTimeStr);
    }

    @Override
    public OptLog lastType(Integer packNum, int type) {
        return optLogMapper.lastByType(Constants.DEFAULT_CONFIG_ID, packNum, type);
    }

    @Override
    public void deleteByPackNum(Integer packNum) {
        optLogMapper.deleteByConfigIdPackNum(Constants.DEFAULT_CONFIG_ID, packNum);
    }

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
            log = optLogMapper.getRunningOptLog(Constants.DEFAULT_CONFIG_ID, packNum, type);
            if (log == null) {
                return;
            }
        }
        // 类型一致
        if (!log.getType().equals(type)) {
            return;
        }
        sotOptLog(log, cacheKey, null);
    }
}
