package com.shanhe.project.collector.battery.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;

class BatteryCollectorProtocolLogServiceTest {

    private final BatteryCollectorProtocolLogService service = new BatteryCollectorProtocolLogService();
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(BatteryCollectorProtocolLogService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void shouldSkipProtocolLogWhenDebugDisabled() {
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        properties.setDebugEnabled(false);

        service.logProtocol(properties, newState("channel-a"), "tx", "cmd=01");

        Assertions.assertTrue(appender.list.isEmpty());
    }

    @Test
    void shouldFilterProtocolLogByDebugChannel() {
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        properties.setDebugEnabled(true);
        properties.setDebugChannels(Collections.singletonList("channel-b"));

        service.logProtocol(properties, newState("channel-a"), "tx", "cmd=01");

        Assertions.assertTrue(appender.list.isEmpty());
    }

    @Test
    void shouldLogProtocolWhenDebugChannelMatches() {
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        properties.setDebugEnabled(true);
        properties.setDebugChannels(Collections.singletonList("channel-a"));

        service.logProtocol(properties, newState("channel-a"), "tx", "cmd=01");

        Assertions.assertEquals(1, appender.list.size());
        Assertions.assertTrue(appender.list.get(0).getFormattedMessage().contains("通道=channel-a"));
    }

    @Test
    void shouldSummarizeLongCompletedCommandsInPollSummary() {
        BatteryCollectorChannelState state = newState("channel-a");
        state.getActiveModuleAddresses().add(1);

        service.logPollSummary(state, false,
                Arrays.asList("01/81", "02/82"),
                Arrays.asList(
                        "01/81", "02/82", "03/83", "04/84", "05/85", "06/86", "07/87", "08/88",
                        "09/89", "0A/8A", "0B/8B", "0C/8C", "0D/8D", "0E/8E", "0F/8F", "10/90",
                        "11/91", "12/92", "13/93", "14/94", "15/95", "16/96", "17/97", "18/98",
                        "19/99", "1A/9A", "1B/9B", "1C/9C", "1D/9D", "1E/9E", "1F/9F", "20/A0",
                        "21/A1"));

        Assertions.assertEquals(1, appender.list.size());
        Assertions.assertTrue(appender.list.get(0).getFormattedMessage().contains("...+1"));
    }

    /** 构造最小可用的采集通道运行态。 */
    private BatteryCollectorChannelState newState(String channelName) {
        BatteryCollectorChannelConfig config = new BatteryCollectorChannelConfig();
        config.setName(channelName);
        config.setPortName("COM1");
        return new BatteryCollectorChannelState(config);
    }
}
