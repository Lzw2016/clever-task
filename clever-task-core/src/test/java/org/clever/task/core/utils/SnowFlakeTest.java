package org.clever.task.core.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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

    @Test
    public void t02() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("1", "1a");
        map.put("3", "3c");
        map.put("4", "4d");
        map.put("2", "2b");
        log.info("-> {}", map.values());
        map.remove("4");
        log.info("-> {}", map.values());
        map.put("3", "3ccc");
        log.info("-> {}", map.values());
    }

    @Test
    public void t03() {
        Map<String, String> map = new HashMap<>();
        map.put("1", "1a");
        map.put("3", "3c");
        map.put("4", "4d");
        map.put("2", "2b");
        log.info("-> {}", map.values());
        map.remove("4");
        log.info("-> {}", map.values());
    }
}
