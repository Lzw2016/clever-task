package org.clever.task.core.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/13 21:41 <br/>
 */
@Slf4j
public class SnowFlakeTest {

    @Test
    public void t01() {
        log.info("-----------------------------------------------------------------");
        SnowFlake snowFlake = new SnowFlake(1, 0);
        for (int i = 0; i < 10; i++) {
            log.info("-> {}", snowFlake.nextId());
        }
        log.info("-----------------------------------------------------------------");
        snowFlake = new SnowFlake(1023, 0);
        for (int i = 0; i < 10; i++) {
            log.info("-> {}", snowFlake.nextId());
        }
    }
}
