package org.clever.task.ext;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/20 21:40 <br/>
 */
@Slf4j
public class JavaJobDemo {
    private static final AtomicInteger aaa = new AtomicInteger();
    private static final AtomicInteger bbb = new AtomicInteger();

    private JavaJobDemo() {
    }

    public void aaa(Map<String, Object> jobData) {
        jobData.put("count", aaa.incrementAndGet());
        log.info("--> {}", jobData);
    }

    public static void bbb(Map<String, Object> jobData) {
        jobData.put("count", bbb.incrementAndGet());
        log.info("--> {}", jobData);
    }
}
