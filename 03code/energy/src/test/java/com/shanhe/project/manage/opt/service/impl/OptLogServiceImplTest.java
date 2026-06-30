package com.shanhe.project.manage.opt.service.impl;

import com.shanhe.common.constant.Constants;
import com.shanhe.project.manage.opt.domain.OptLog;
import com.shanhe.project.manage.opt.mapper.OptLogMapper;
import com.shanhe.project.manage.config.service.IBatteryPackService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

    @Test
    void selectRunningListShouldDelegateToMapper() {
        OptLogServiceImpl service = new OptLogServiceImpl();
        OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
        ReflectionTestUtils.setField(service, "optLogMapper", optLogMapper);
        OptLog runningLog = new OptLog();
        runningLog.setPackNum(1);
        runningLog.setResult(null);
        Mockito.when(optLogMapper.selectRunningList(1)).thenReturn(Collections.singletonList(runningLog));

        List<OptLog> result = service.selectRunningList(1);

        Assertions.assertEquals(1, result.size());
        Assertions.assertNull(result.get(0).getResult());
        Mockito.verify(optLogMapper).selectRunningList(1);
    }

    @Test
    void selectRunningListShouldReturnEmptyListWhenMapperReturnsNull() {
        OptLogServiceImpl service = new OptLogServiceImpl();
        OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
        ReflectionTestUtils.setField(service, "optLogMapper", optLogMapper);
        Mockito.when(optLogMapper.selectRunningList(Mockito.any())).thenReturn(null);

        List<OptLog> result = service.selectRunningList(1);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void selectRunningListShouldReturnEmptyListWhenNoRunningLogs() {
        OptLogServiceImpl service = new OptLogServiceImpl();
        OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
        ReflectionTestUtils.setField(service, "optLogMapper", optLogMapper);
        Mockito.when(optLogMapper.selectRunningList(1)).thenReturn(Collections.emptyList());

        List<OptLog> result = service.selectRunningList(1);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void selectRunningListShouldReturnMultipleRunningLogs() {
        OptLogServiceImpl service = new OptLogServiceImpl();
        OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
        ReflectionTestUtils.setField(service, "optLogMapper", optLogMapper);
        OptLog log1 = new OptLog();
        log1.setPackNum(1);
        log1.setResult(null);
        OptLog log2 = new OptLog();
        log2.setPackNum(1);
        log2.setResult(null);
        Mockito.when(optLogMapper.selectRunningList(1)).thenReturn(Arrays.asList(log1, log2));

        List<OptLog> result = service.selectRunningList(1);

        Assertions.assertEquals(2, result.size());
    }

    @Test
    void getRunningOptLogShouldDelegateToMapper() {
        OptLogServiceImpl service = new OptLogServiceImpl();
        OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
        ReflectionTestUtils.setField(service, "optLogMapper", optLogMapper);
        OptLog runningLog = new OptLog();
        runningLog.setPackNum(1);
        runningLog.setType(2);
        runningLog.setResult(null);
        Mockito.when(optLogMapper.getRunningOptLog(1, 2)).thenReturn(runningLog);

        OptLog result = service.getRunningOptLog(1, 2);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getPackNum());
        Assertions.assertEquals(2, result.getType());
        Assertions.assertNull(result.getResult());
        Mockito.verify(optLogMapper).getRunningOptLog(1, 2);
    }

    @Test
    void getRunningOptLogShouldReturnNullWhenNoRunningLog() {
        OptLogServiceImpl service = new OptLogServiceImpl();
        OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
        ReflectionTestUtils.setField(service, "optLogMapper", optLogMapper);
        Mockito.when(optLogMapper.getRunningOptLog(1, 2)).thenReturn(null);

        OptLog result = service.getRunningOptLog(1, 2);

        Assertions.assertNull(result);
    }

    @Test
    void selectRunningListWithNullPackNumShouldQueryAll() {
        OptLogServiceImpl service = new OptLogServiceImpl();
        OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
        ReflectionTestUtils.setField(service, "optLogMapper", optLogMapper);
        Mockito.when(optLogMapper.selectRunningList(null)).thenReturn(Collections.emptyList());

        List<OptLog> result = service.selectRunningList(null);

        Assertions.assertNotNull(result);
        Mockito.verify(optLogMapper).selectRunningList(null);
    }
}
