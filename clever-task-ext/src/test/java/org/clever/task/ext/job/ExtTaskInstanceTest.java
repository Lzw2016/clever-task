package org.clever.task.ext.job;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.clever.task.core.TaskInstance;
import org.clever.task.core.config.SchedulerConfig;
import org.clever.task.core.entity.EnumConstant;
import org.clever.task.core.job.HttpJobExecutor;
import org.clever.task.core.job.JavaJobExecutor;
import org.clever.task.core.job.MockJobExecutor;
import org.clever.task.core.listeners.JobLogListener;
import org.clever.task.core.listeners.JobTriggerLogListener;
import org.clever.task.core.listeners.SchedulerLogListener;
import org.clever.task.core.model.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.Consumer;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/20 20:50 <br/>
 */
@Slf4j
public class ExtTaskInstanceTest {
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

    public static SchedulerConfig newSchedulerConfig(String namespace) {
        SchedulerConfig config = new SchedulerConfig();
        config.setSchedulerExecutorPoolSize(4);
        config.setJobExecutorPoolSize(8);
        config.setNamespace(namespace);
        config.setInstanceName("n01");
        config.setDescription("");
        return config;
    }

    public static void startTaskInstance(String instanceName, Consumer<TaskInstance> callback) throws InterruptedException {
        HikariDataSource dataSource = newDataSource();
        TaskInstance taskInstance = new TaskInstance(
                dataSource,
                newSchedulerConfig(instanceName),
                Arrays.asList(new MockJobExecutor(), new HttpJobExecutor(), new JavaJobExecutor(), new ShellJobExecutor()),
                Collections.singletonList(new SchedulerLogListener()),
                Collections.singletonList(new JobTriggerLogListener()),
                Collections.singletonList(new JobLogListener())
        );
        if (callback != null) {
            callback.accept(taskInstance);

        }
        taskInstance.start();
        Thread.sleep(1000 * 60 * 2);
        taskInstance.pause();
        Thread.sleep(1000 * 30);
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
        // HTTP job
        startTaskInstance("http_job_test", taskInstance -> {
            HttpJobModel jobModel = new HttpJobModel("get_baidu", "GET", "https://www.baidu.com/");
            HttpJobModel.HttpRequestData requestData = new HttpJobModel.HttpRequestData();
            requestData.addParam("aaa", "123");
            requestData.addParam("bbb", "456");
            jobModel.setRequestData(requestData);
            AbstractTrigger trigger = new CronTrigger("get_baidu_trigger", "0/1 * * * * ? *");
            taskInstance.addJob(jobModel, trigger);
        });
    }

    @Test
    public void t02() throws InterruptedException {
        // HTTP job
        startTaskInstance("http_job_test", null);
    }

    @Test
    public void t11() throws InterruptedException {
        // Java job
        startTaskInstance("java_job_test", taskInstance -> {
            JavaJobModel jobModel = new JavaJobModel("JavaJobDemo", true, "org.clever.task.ext.JavaJobDemo", "bbb");
            AbstractTrigger trigger = new CronTrigger("JavaJobDemo_trigger", "0/1 * * * * ? *");
            taskInstance.addJob(jobModel, trigger);
        });
    }

    @Test
    public void t12() throws InterruptedException {
        // Java job
        startTaskInstance("java_job_test", null);
    }

    @Test
    public void t21() throws InterruptedException {
        // HTTP job
        startTaskInstance("shell_job_test", taskInstance -> {
            ShellJobModel jobModel = new ShellJobModel("ShellJobDemo", EnumConstant.SHELL_JOB_SHELL_TYPE_NODE, "console.log(\"###----------------------\", new Date());");
            AbstractTrigger trigger = new CronTrigger("ShellJobDemo_trigger", "0/5 * * * * ? *");
            taskInstance.addJob(jobModel, trigger);
        });
    }

    @Test
    public void t22() throws InterruptedException {
        // HTTP job
        startTaskInstance("shell_job_test", null);
    }
}
