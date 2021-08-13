package org.clever.task.core;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.clever.task.core.config.SchedulerConfig;
import org.clever.task.core.cron.CronExpression;
import org.clever.task.core.cron.CronExpressionUtil;
import org.clever.task.core.job.HttpJobExecutor;
import org.clever.task.core.job.MockJobExecutor;
import org.clever.task.core.listeners.JobLogListener;
import org.clever.task.core.listeners.JobTriggerLogListener;
import org.clever.task.core.listeners.SchedulerLogListener;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
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

    public static SchedulerConfig newSchedulerConfig(String instanceName) {
        SchedulerConfig config = new SchedulerConfig();
        config.setNamespace("lzw");
        config.setInstanceName(instanceName);
        config.setDescription("测试节点01");
        return config;
    }

    public static void startTaskInstance(String instanceName) throws InterruptedException {
        HikariDataSource dataSource = newDataSource();
        TaskInstance taskInstance = new TaskInstance(
                dataSource,
                newSchedulerConfig(instanceName),
                Arrays.asList(new MockJobExecutor(), new HttpJobExecutor()),
                Collections.singletonList(new SchedulerLogListener()),
                Collections.singletonList(new JobTriggerLogListener()),
                Collections.singletonList(new JobLogListener())
        );
        taskInstance.start();
        Thread.sleep(1000 * 60 * 10);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Thread.sleep(8_000);
            } catch (InterruptedException ignored) {
            }
            dataSource.close();
        }));
    }

    @Test
    public void t01() throws InterruptedException {
        startTaskInstance("n01");
    }

    @Test
    public void t02() throws InterruptedException {
        startTaskInstance("n02");
    }

    @Test
    public void t03() throws InterruptedException {
        startTaskInstance("n03");
    }

    @Test
    public void t04() throws InterruptedException {
        startTaskInstance("n04");
    }

    @Test
    public void t05() throws InterruptedException {
        startTaskInstance("n05");
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

    @Test
    public void t903() {
        final String cronExpression = "0 0/5 * * * ? *";
        final Date now = new Date();
        final long startTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            CronExpressionUtil.getNextTime(cronExpression, now);
        }
        final long endTime = System.currentTimeMillis();
        log.info("--> {}", (endTime - startTime));
    }

    @Test
    public void t904() throws ParseException {
        final String cron = "0 0/5 * * * ? *";
        final Date now = new Date();
        CronExpression cronExpression = new CronExpression(cron);
        final long startTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            cronExpression.getNextValidTimeAfter(now);
        }
        final long endTime = System.currentTimeMillis();
        log.info("--> {}", (endTime - startTime));
    }
}
