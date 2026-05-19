package com.shanhe.project.device.opt.service.impl;

import com.shanhe.common.constant.Constants;
import com.shanhe.project.device.opt.domain.OptLog;
import com.shanhe.project.device.opt.mapper.OptLogMapper;
import com.shanhe.project.device.config.service.IBatteryPackService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class OptLogServiceImplTest {

    @Test
    void insertShouldWriteDefaultConfigId() {
        OptLogServiceImpl service = new OptLogServiceImpl();
        OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
        IBatteryPackService batteryPackService = Mockito.mock(IBatteryPackService.class);
        Mockito.when(optLogMapper.insert(Mockito.any(OptLog.class))).thenReturn(1L);
        ReflectionTestUtils.setField(service, "optLogMapper", optLogMapper);
        ReflectionTestUtils.setField(service, "batteryPackService", batteryPackService);

        service.insert(2, 1, 0);

        ArgumentCaptor<OptLog> captor = ArgumentCaptor.forClass(OptLog.class);
        Mockito.verify(optLogMapper).insert(captor.capture());
        Assertions.assertEquals(Constants.DEFAULT_CONFIG_ID, captor.getValue().getConfigId());
        Assertions.assertEquals(2, captor.getValue().getPackNum());
        Assertions.assertEquals(1, captor.getValue().getType());
    }
}
