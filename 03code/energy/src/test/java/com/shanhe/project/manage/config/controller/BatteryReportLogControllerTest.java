package com.shanhe.project.manage.config.controller;

import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.manage.config.domain.BatteryReportLog;
import com.shanhe.project.manage.config.service.BatteryReportLogService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class BatteryReportLogControllerTest {

    @Test
    void shouldUseHistoricalLastReportForDetail() {
        BatteryReportLogService reportLogService = Mockito.mock(BatteryReportLogService.class);
        BatteryReportLog historicalLog = new BatteryReportLog();
        historicalLog.setPackData("{}");
        historicalLog.setMonitorData("[]");
        Mockito.when(reportLogService.selectLastHasAlarm(1)).thenReturn(historicalLog);
        BatteryReportLogController controller = new BatteryReportLogController();
        ReflectionTestUtils.setField(controller, "batteryReportLogService", reportLogService);

        AjaxResult result = controller.detailList(1L, 1);

        BatteryReportLog data = (BatteryReportLog) result.get(AjaxResult.DATA_TAG);
        Assertions.assertSame(historicalLog, data);
        Assertions.assertNull(data.getPackData());
        Assertions.assertNull(data.getMonitorData());
        Mockito.verify(reportLogService).selectLastHasAlarm(1);
    }
}
