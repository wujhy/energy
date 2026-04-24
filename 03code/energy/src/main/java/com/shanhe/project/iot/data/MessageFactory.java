package com.shanhe.project.iot.data;

import com.alibaba.fastjson.JSON;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.shanhe.project.device.config.domain.BatteryReportLog;
import com.shanhe.project.device.config.domain.MonitorData;
import com.shanhe.project.device.config.mapper.BatteryReportLogMapper;
import com.shanhe.project.device.history.domain.HistoryLog;
import com.shanhe.project.device.history.mapper.HistoryLogMapper;
import com.shanhe.project.energy.stat.domain.StatBatteryBat;
import com.shanhe.project.energy.stat.domain.StatBatteryPack;
import com.shanhe.project.energy.stat.mapper.StatBatteryBatMapper;
import com.shanhe.project.energy.stat.mapper.StatBatteryPackMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 数据处理工程量
 *
 * @author zhoubin
 * @date 2024/11/18
 */
public class MessageFactory {

    private final static Logger logger = LoggerFactory.getLogger(MessageFactory.class);

    /**
     * 当下异常的时候，状态缓存时间，缓存时间为30秒
     */
    private final static Cache<String, Boolean> cache = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).build();

    /**
     * 数据处理队列
     */
    public static BlockingQueue<MonitorData> dataQueue;

    public static int queueSize = 10000;
    /**
     * 最近数据放入的时间戳
     */
    public static AtomicLong lastPushTime = new AtomicLong(0);
    private static Boolean logOutPut = true;

    private static BatteryReportLogMapper batteryReportLogMapper;
    private static HistoryLogMapper historyLogMapper;
    private static StatBatteryPackMapper statBatteryPackMapper;
    private static StatBatteryBatMapper statBatteryBatMapper;

    /**
     * 初始化队列
     *
     * @param logQueueSize 队列大小
     */
    public static void initQueue(int logQueueSize, BatteryReportLogMapper initBatteryReportLogMapper,
                                 HistoryLogMapper initHistoryLogMapper, StatBatteryPackMapper initStatBatteryPackMapper,
                                 StatBatteryBatMapper initStatBatteryBatMapper) {
        queueSize = logQueueSize;

        batteryReportLogMapper = initBatteryReportLogMapper;
        historyLogMapper = initHistoryLogMapper;
        statBatteryBatMapper = initStatBatteryBatMapper;
        statBatteryPackMapper = initStatBatteryPackMapper;

        if (null == dataQueue) {
            dataQueue = new LinkedBlockingQueue<>(logQueueSize);
        }
    }

    /**
     * 放入数据
     *
     * @param monitor 数据
     */
    public static void pushData(MonitorData monitor) {
        if (null == monitor) {
            return;
        }
        if (null == dataQueue) {
            throw new RuntimeException("需要通过 StartupRunner 开启队列");
        }
        if (dataQueue.size() < queueSize) {
            dataQueue.add(monitor);
        } else {
            logger.error("数据处理队列已满，请稍后再试");
        }
    }

    /**
     * 放入数据
     *
     * @param monitors 数据
     */
    public static void pushData(List<? extends MonitorData> monitors) {
        if (CollectionUtils.isEmpty(monitors)) {
            return;
        }
        if (null == dataQueue) {
            throw new RuntimeException("需要通过 StartupRunner 开启队列");
        }
        if (dataQueue.size() < queueSize) {
            dataQueue.addAll(monitors);
        } else {
            logger.error("数据处理队列已满，请稍后再试");
        }
    }

    public static void starGainData(int maxCount) {
        List<MonitorData> datas = new ArrayList<>();
        while (true) {
            try {
                //开始推送日志
                // monitor 为上锁标志
                doStartLog(maxCount, dataQueue, lastPushTime,datas);
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException ex) {
//                }
            } catch (Exception e) {
                String exceptionMsg = e.getMessage();
                if (e instanceof InvocationTargetException) {
                    if (exceptionMsg == null) {
                        exceptionMsg = ((InvocationTargetException) e).getTargetException().getMessage();
                    }
                }
                logger.error("data error:--------starGainData--------" + exceptionMsg + "-------------------");
                //睡眠1秒
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                }
            } catch (Throwable e){
                try {
                    Thread.sleep(5000); // 等待5秒
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                logger.error("data system error:--------starGainData--------" + e + "-------------------");
            }
        }
    }

    private static void doStartLog(int maxCount, BlockingQueue<MonitorData> queue, AtomicLong pushTime,List<MonitorData> datas) throws InterruptedException {
        int size = queue.size();
        // 当前时间戳
        long currentTimeMillis = System.currentTimeMillis();
        // 时间差
        long time = currentTimeMillis - pushTime.get();

        if (size >= maxCount || time > 500) {
            if(datas==null) {
                datas = new ArrayList<>();
            }else{
                datas.clear();
            }
            // 批量获取
            queue.drainTo(datas, maxCount);

            // 推送日志信息
            push(datas);
            pushTime.set(currentTimeMillis);

//        } else if (size == 0) {
//            Monitor data = queue.take();
//            datas.add(data);
//            // 推送日志信息
//            push(datas);
        } else {
            // 等待100毫秒
            Thread.sleep(100);
        }
    }

    private static void push(List<MonitorData> datas) {

        // 从现有的缓存中获取,如果缓存中有key,则返回value，如果没有则返回null
        logOutPut = cache.getIfPresent("monitor");

        if (logOutPut == null || logOutPut) {
            if (datas != null && !datas.isEmpty()) {
                List<BatteryReportLog> batteryPackInfos = new ArrayList<>();
                List<HistoryLog> otherMonitors = new ArrayList<>();
                List<StatBatteryPack> statBatteryPacks = new ArrayList<>();
                List<StatBatteryBat> statBatteryBats = new ArrayList<>();

                for (MonitorData data : datas) {
                    if (data instanceof BatteryReportLog) {

                        BatteryReportLog batteryReportLog = (BatteryReportLog) data;
                        if (batteryReportLog.getPackData() == null) {
                            batteryReportLog.setPackData(JSON.toJSONString(batteryReportLog.getPackParam()));
                        }
                        if (batteryReportLog.getMonitorData() == null) {
                            batteryReportLog.setMonitorData(JSON.toJSONString(batteryReportLog.getBatteryList()));
                        }
                        batteryPackInfos.add(batteryReportLog);
                    } else if (data instanceof HistoryLog) {
                        otherMonitors.add((HistoryLog) data);
                    } else if (data instanceof StatBatteryPack) {
                        statBatteryPacks.add((StatBatteryPack) data);
                    } else if (data instanceof StatBatteryBat) {
                        statBatteryBats.add((StatBatteryBat) data);
                    }
                }
                if (!batteryPackInfos.isEmpty()) {
                    try {
                        batteryReportLogMapper.insertList(batteryPackInfos);
                    } catch (Exception e) {
                        // 处理或记录异常
                        logger.error("插入电池数据异常",e);
                    }
                }
                if (!otherMonitors.isEmpty()) {
                    try {
                        historyLogMapper.insertList(otherMonitors);
                    } catch (Exception e) {
                        // 处理或记录异常
                        logger.error("插入其他数据异常",e);
                    }
                }
                if (!statBatteryPacks.isEmpty()) {
                    try {
                        statBatteryPackMapper.insertList(statBatteryPacks);
                    } catch (Exception e) {
                        // 处理或记录异常
                        logger.error("插入统计电池数据异常",e);
                    }
                }
                if (!statBatteryBats.isEmpty()) {
                    try {
                        statBatteryBatMapper.insertList(statBatteryBats);
                    } catch (Exception e) {
                        // 处理或记录异常
                        logger.error("插入统计电池单体数据异常",e);
                    }
                }

            }
            cache.put("monitor", true);
        }
    }
}
