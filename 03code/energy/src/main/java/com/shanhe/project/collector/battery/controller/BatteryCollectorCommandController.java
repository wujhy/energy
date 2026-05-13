package com.shanhe.project.collector.battery.controller;

import com.shanhe.framework.aspectj.lang.annotation.Log;
import com.shanhe.framework.enums.BusinessType;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.collector.battery.model.BatteryCollectorCommandResult;
import com.shanhe.project.collector.battery.protocol.BatteryAggregateCommandDefinition;
import com.shanhe.project.collector.battery.service.BatteryCollectorCommandService;
import com.shanhe.project.collector.battery.service.BatteryCollectorService;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
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

    @Resource
    private BatteryCollectorCommandService commandService;
    @Resource
    private BatteryCollectorService collectorService;

    @GetMapping("/status")
    public AjaxResult status() {
        return success(collectorService.getChannelSnapshots());
    }

    @Log(title = "蓄电池模块地址缓存重置", businessType = BusinessType.UPDATE)
    @PostMapping("/moduleAddressCache/reset")
    public AjaxResult resetModuleAddressCache(@RequestBody ResetModuleAddressCacheRequest request) {
        String channelName = request == null ? null : request.getChannelName();
        return success(collectorService.resetModuleAddressCache(channelName));
    }

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
        return success(commandService.connectResistanceTest(
                request.getChannelName(),
                request.getBatteryGroup(),
                request.getTimeoutMs()));
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
        private Long timeoutMs;
    }

}
