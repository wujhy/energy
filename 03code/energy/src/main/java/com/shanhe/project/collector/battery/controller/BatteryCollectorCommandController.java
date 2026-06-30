package com.shanhe.project.collector.battery.controller;

import com.shanhe.framework.aspectj.lang.annotation.Log;
import com.shanhe.framework.enums.BusinessType;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.collector.battery.model.BatteryCollectorCommandResult;
import com.shanhe.project.collector.battery.model.BatteryDeviceState;
import com.shanhe.project.collector.battery.protocol.BatteryAggregateCommandDefinition;
import com.shanhe.project.collector.battery.service.BatteryCollectorCommandService;
import com.shanhe.project.collector.battery.service.BatteryCollectorService;
import com.shanhe.project.collector.battery.service.BatteryCurrentStateService;
import com.shanhe.project.collector.battery.service.BatteryDeviceStateService;
import lombok.Data;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 蓄电池采集调试与兼容命令入口。
 *
 * @author wjh
 * @since 2026-04-28
 */
@RestController
@RequestMapping("/collector/battery")
public class BatteryCollectorCommandController extends BaseController {

    /** 帧日志默认查询条数上限。 */
    private static final int DEFAULT_FRAME_LOG_QUERY_LIMIT = 500;
    /** 帧日志最大查询条数上限。 */
    private static final int MAX_FRAME_LOG_QUERY_LIMIT = 2000;


    @Resource
    private BatteryCollectorCommandService commandService;
    @Resource
    private BatteryCollectorService collectorService;
    @Resource
    private BatteryCurrentStateService batteryCurrentStateService;
    @Resource
    private BatteryDeviceStateService batteryDeviceStateService;
    @Resource
    private com.shanhe.project.collector.battery.mapper.BatteryModuleFrameLogMapper frameLogMapper;
    @Resource
    private com.shanhe.project.collector.battery.config.BatteryCollectorProperties batteryCollectorProperties;

    /** 查询采集通道运行状态快照。 */
    @GetMapping("/status")
    public AjaxResult status() {
        return success(collectorService.getChannelSnapshots());
    }

    /** 查询采集运行指标（通道、队列、轮询、超时、快照）。 */
    @GetMapping("/metrics")
    public AjaxResult metrics() {
        return success(collectorService.getMetrics());
    }

    /** 查询指定电池组的当前状态（实时数据、状态、告警摘要）。 */
    @GetMapping("/currentState")
    public AjaxResult currentState(Integer packNum) {
        return success(batteryCurrentStateService.getCurrentState(packNum));
    }

    /** 重置模块地址缓存，强制下一轮全量发现。 */
    @Log(title = "蓄电池模块地址缓存重置", businessType = BusinessType.UPDATE)
    @PostMapping("/moduleAddressCache/reset")
    public AjaxResult resetModuleAddressCache(@RequestBody ResetModuleAddressCacheRequest request) {
        String channelName = request == null ? null : request.getChannelName();
        return success(collectorService.resetModuleAddressCache(channelName));
    }

    /** 980 聚合命令兼容入口，映射到 600 模块端控制命令。 */
    @Log(title = "蓄电池980聚合命令兼容入口", businessType = BusinessType.UPDATE)
    @PostMapping("/execute")
    public AjaxResult execute(@RequestBody ExecuteRequest request) {
        if (request == null || request.getCommandDefinition() == null) {
            return error("commandDefinition不能为空");
        }
        if (request.getChannelName() == null || request.getChannelName().trim().isEmpty()) {
            return error("channelName不能为空");
        }
        BatteryCollectorCommandResult result = commandService.execute(
                request.getCommandDefinition(),
                request.getChannelName(),
                request.getTimeoutMs(),
                toIntArray(request.getPayloadBytes()));
        return success(result);
    }

    /** 单体内阻测试。 */
    @Log(title = "蓄电池单体内阻测试", businessType = BusinessType.UPDATE)
    @PostMapping("/singleResistanceTest")
    public AjaxResult singleResistanceTest(@RequestBody SingleResistanceTestRequest request) {
        if (request == null) {
            return error("请求不能为空");
        }
        return success(commandService.singleInternalResistanceTest(
                request.getChannelName(),
                request.getBatteryGroup(),
                request.getBatteryNumber(),
                request.getTimeoutMs()));
    }

    /** 手动设置模块地址。 */
    @Log(title = "蓄电池模块手动编号", businessType = BusinessType.UPDATE)
    @PostMapping("/manualModuleAddress")
    public AjaxResult manualModuleAddress(@RequestBody ManualModuleAddressRequest request) {
        if (request == null) {
            return error("请求不能为空");
        }
        if (request.getChannelName() == null || request.getChannelName().trim().isEmpty()
                || request.getBatteryGroup() == null
                || request.getModuleAddress() == null
                || request.getNewModuleAddress() == null) {
            return error("channelName、batteryGroup、moduleAddress、newModuleAddress不能为空");
        }
        return success(commandService.manualSetSubmoduleAddress(
                request.getChannelName(),
                request.getBatteryGroup(),
                request.getModuleAddress(),
                request.getNewModuleAddress(),
                request.getTimeoutMs()));
    }

    /** 连接条电阻测试。 */
    @Log(title = "蓄电池连接条电阻测试", businessType = BusinessType.UPDATE)
    @PostMapping("/connectResistanceTest")
    public AjaxResult connectResistanceTest(@RequestBody ConnectResistanceTestRequest request) {
        if (request == null) {
            return error("请求不能为空");
        }
        if (request.getChannelName() == null || request.getChannelName().trim().isEmpty()
                || request.getBatteryGroup() == null) {
            return error("channelName、batteryGroup不能为空");
        }
        int batteryCount = request.getBatteryCount() == null || request.getBatteryCount() <= 0
                ? 245 : Math.min(request.getBatteryCount(), 245);
        return success(commandService.connectResistanceTest(
                request.getChannelName(),
                request.getBatteryGroup(),
                batteryCount,
                request.getTimeoutMs()));
    }

    /** 查询指定电池组的设备状态。 */
    @GetMapping("/deviceState")
    public AjaxResult deviceState(Integer packNum) {
        return success(batteryDeviceStateService.selectByPackNum(packNum));
    }

    /** 查询原始帧日志。 */
    @GetMapping("/frameLog")
    public AjaxResult frameLog(String channelName, Integer batteryGroup, String commandCode) {
        int limit = resolveFrameLogQueryLimit();
        return success(frameLogMapper.selectList(channelName, batteryGroup, commandCode, limit));
    }

    private int resolveFrameLogQueryLimit() {
        Integer configuredLimit = batteryCollectorProperties.getRawFrameLogQueryLimit();
        if (configuredLimit == null || configuredLimit <= 0) {
            return DEFAULT_FRAME_LOG_QUERY_LIMIT;
        }
        return Math.min(configuredLimit, MAX_FRAME_LOG_QUERY_LIMIT);
    }

    /** 条件查询设备状态列表。 */
    @GetMapping("/deviceState/list")
    public AjaxResult deviceStateList(BatteryDeviceState query) {
        return success(batteryDeviceStateService.selectList(query));
    }

    /** 删除指定设备状态记录。 */
    @Log(title = "删除设备状态记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/deviceState/{stateId}")
    public AjaxResult deleteDeviceState(@PathVariable Long stateId) {
        batteryDeviceStateService.deleteByStateId(stateId);
        return success();
    }

    private int[] toIntArray(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return new int[0];
        }
        int[] payload = new int[values.size()];
        for (int i = 0; i < values.size(); i++) {
            Integer value = values.get(i);
            payload[i] = value == null ? 0 : (value & 0xFF);
        }
        return payload;
    }

    @Data
    public static class ExecuteRequest {
        private BatteryAggregateCommandDefinition commandDefinition;
        private String channelName;
        private Long timeoutMs;
        private List<Integer> payloadBytes;
    }

    @Data
    public static class ResetModuleAddressCacheRequest {
        private String channelName;
    }

    @Data
    public static class SingleResistanceTestRequest {
        private String channelName;
        private Integer batteryGroup;
        private Integer batteryNumber;
        private Long timeoutMs;
    }

    @Data
    public static class ManualModuleAddressRequest {
        private String channelName;
        private Integer batteryGroup;
        private Integer moduleAddress;
        private Integer newModuleAddress;
        private Long timeoutMs;
    }

    @Data
    public static class ConnectResistanceTestRequest {
        private String channelName;
        private Integer batteryGroup;
        private Integer batteryCount;
        private Long timeoutMs;
    }

}
