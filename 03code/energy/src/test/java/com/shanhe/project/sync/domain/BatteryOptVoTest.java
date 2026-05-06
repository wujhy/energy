package com.shanhe.project.sync.domain;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSONObject;
import com.shanhe.project.device.config.domain.DevBatteryOpt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BatteryOptVoTest {

    @Test
    void shouldKeepModelNumWhenParsingSingleBatteryCommand() {
        BatteryOptVo optVo = JSONObject.parseObject("{\"devId\":10,\"packNum\":1,\"testType\":6,\"modelNum\":8}",
                BatteryOptVo.class);

        DevBatteryOpt batteryOpt = BeanUtil.copyProperties(optVo, DevBatteryOpt.class);

        Assertions.assertEquals(8, batteryOpt.getModelNum());
    }
}
