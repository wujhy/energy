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
 * 980 聚合命令兼容入口。
 *
 * <p>当前仅保留接口形态，避免旧调用方误以为这些命令仍会直发 600 节下行模块端通道。
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

    @Log(title = "蓄电池系统状态设置", businessType = BusinessType.UPDATE)
    @PostMapping("/systemState")
    public AjaxResult systemState(@RequestBody SystemStateRequest request) {
        if (request == null) {
            return error("请求不能为空");
        }
        return success(commandService.setSystemState(
                request.getChannelName(),
                request.getBatteryGroup(),
                request.getSystemState(),
                request.getTimeoutMs()));
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

    @Log(title = "蓄电池时间同步", businessType = BusinessType.UPDATE)
    @PostMapping("/updateTimeAll")
    public AjaxResult updateTimeAll(@RequestBody UpdateTimeAllRequest request) {
        if (request == null) {
            return error("请求不能为空");
        }
        return success(commandService.updateTimeAll(
                request.getChannelName(),
                request.getDeviceType(),
                request.getYear(),
                request.getMonth(),
                request.getDay(),
                request.getHour(),
                request.getMinute(),
                request.getSecond(),
                request.getTimeoutMs()));
    }

    @Log(title = "蓄电池设备IP设置", businessType = BusinessType.UPDATE)
    @PostMapping("/setDeviceIpAddress")
    public AjaxResult setDeviceIpAddress(@RequestBody SetDeviceIpAddressRequest request) {
        if (request == null) {
            return error("请求不能为空");
        }
        return success(commandService.setDeviceIpAddress(
                request.getChannelName(),
                request.getDeviceIpBytes(),
                request.getMaskBytes(),
                request.getGatewayBytes(),
                request.getPort(),
                request.getTimeoutMs()));
    }

    @Log(title = "蓄电池服务器模式设置", businessType = BusinessType.UPDATE)
    @PostMapping("/setServerClientMode")
    public AjaxResult setServerClientMode(@RequestBody SetServerClientModeRequest request) {
        if (request == null) {
            return error("请求不能为空");
        }
        return success(commandService.setServerClientMode(
                request.getChannelName(),
                request.getMode(),
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
    public static class SystemStateRequest {
        private String channelName;
        private Integer batteryGroup;
        private Integer systemState;
        private Long timeoutMs;
    }

    @Data
    public static class SingleResistanceTestRequest {
        private String channelName;
        private Integer batteryGroup;
        private Integer batteryNumber;
        private Long timeoutMs;
    }

    @Data
    public static class UpdateTimeAllRequest {
        private String channelName;
        private Integer deviceType;
        private Integer year;
        private Integer month;
        private Integer day;
        private Integer hour;
        private Integer minute;
        private Integer second;
        private Long timeoutMs;
    }

    @Data
    public static class SetDeviceIpAddressRequest {
        private String channelName;
        private int[] deviceIpBytes;
        private int[] maskBytes;
        private int[] gatewayBytes;
        private Integer port;
        private Long timeoutMs;
    }

    @Data
    public static class SetServerClientModeRequest {
        private String channelName;
        private Integer mode;
        private Long timeoutMs;
    }
}
