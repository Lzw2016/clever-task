package org.clever.task.core;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.clever.task.core.config.SchedulerConfig;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/08 15:55 <br/>
 */
@Slf4j
public class TaskInstanceTest {
    public static HikariConfig newHikariConfig() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariConfig.setJdbcUrl("jdbc:mysql://192.168.1.201:12000/clever_task");
        hikariConfig.setUsername("clever_task");
        hikariConfig.setPassword("Aa123456!");
        hikariConfig.setAutoCommit(false);
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setMaximumPoolSize(10);
        return hikariConfig;
    }

    public static HikariDataSource newDataSource() {
        return new HikariDataSource(newHikariConfig());
    }

    public static SchedulerConfig newSchedulerConfig() {
        SchedulerConfig config = new SchedulerConfig();
        config.setNamespace("lzw");
        config.setInstanceName("n01");
        config.setDescription("测试节点01");
        return config;
    }

    @Test
    public void t01() throws InterruptedException {
        HikariDataSource dataSource = newDataSource();
        TaskInstance taskInstance = new TaskInstance(dataSource, newSchedulerConfig());
        taskInstance.start();
        Thread.sleep(1000 * 60 * 2);
        Runtime.getRuntime().addShutdownHook(new Thread(dataSource::close));
    }

    @Test
    public void t02() throws InterruptedException {
        t01();
    }

    @Test
    public void t03() throws InterruptedException {
        t01();
    }

    @Test
    public void t04() throws InterruptedException {
        t01();
    }

    @Test
    public void t05() throws InterruptedException {
        t01();
    }

    @Test
    public void t901() throws InterruptedException {
        final AtomicInteger count = new AtomicInteger(0);
        final ScheduledExecutorService jobTriggerExecutor = Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture<?> future;
        future = jobTriggerExecutor.scheduleAtFixedRate(
                () -> log.info("--> {}", count.incrementAndGet()),
                0, 100, TimeUnit.MILLISECONDS
        );
        for (int i = 1; i < 100; i++) {
            future.cancel(true);
            future = jobTriggerExecutor.scheduleAtFixedRate(
                    () -> log.info("--> {}", count.incrementAndGet()),
                    0, 10 * i, TimeUnit.MILLISECONDS);
        }
        Thread.sleep(1000 * 10);
    }
}
