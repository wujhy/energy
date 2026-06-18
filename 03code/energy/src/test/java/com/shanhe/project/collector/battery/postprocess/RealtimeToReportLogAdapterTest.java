package com.shanhe.project.collector.battery.postprocess;

import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.device.config.domain.BatteryReportLog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class RealtimeToReportLogAdapterTest {

    @Test
    void shouldPassThroughStandardStatusFields() {
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setBatteryPackStatus(6);
        group.setResistanceTestStatus(2);
        group.setDeviceWorkStatus(3);
        group.setDeviceWorkIoStatus(4);

        BatteryReportLog reportLog = RealtimeToReportLogAdapter.adapt(2, group, null);

        Map<String, Object> packParam = reportLog.getPackParam();
        Assertions.assertEquals(2, reportLog.getPackNum());
        Assertions.assertEquals(6, packParam.get("batteryPackStatus"));
        Assertions.assertEquals(2, packParam.get("resistanceTestStatus"));
        Assertions.assertEquals(3, packParam.get("deviceWorkStatus"));
        Assertions.assertEquals(4, packParam.get("deviceWorkIOStatus"));
    }

    @Test
    void shouldOmitMissingStatusFields() {
        BatteryReportLog reportLog = RealtimeToReportLogAdapter.adapt(2, new BatteryModuleGroupRealtime(), null);

        Map<String, Object> packParam = reportLog.getPackParam();
        Assertions.assertFalse(packParam.containsKey("batteryPackStatus"));
        Assertions.assertFalse(packParam.containsKey("resistanceTestStatus"));
        Assertions.assertFalse(packParam.containsKey("deviceWorkStatus"));
        Assertions.assertFalse(packParam.containsKey("deviceWorkIOStatus"));
    }
}
