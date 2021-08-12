package org.clever.task.core;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/09 21:21 <br/>
 */
@Slf4j
public class WorkExecutor {
    // 线程池线程保持时间
    private static final long THREAD_POOL_KEEP_ALIVE_SECONDS = 3L;

    /**
     * 守护线程名称
     */
    private final String name;
    /**
     * 调度器实例名
     */
    private final String instanceName;
    /**
     * 线程池
     */
    private final ThreadPoolExecutor executor;

    public WorkExecutor(String name, String instanceName, int poolSize, int workQueueCapacity) {
        this.name = name;
        this.instanceName = instanceName;
        executor = new ThreadPoolExecutor(
                poolSize,
                poolSize,
                THREAD_POOL_KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(workQueueCapacity),
                new BasicThreadFactory.Builder().namingPattern("task-work-%d").daemon(false).build()
        );
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                executor.shutdownNow();
                log.info("[WorkExecutor] 线程池停止成功 | {} | instanceName={}", this.name, this.instanceName);
            } catch (Exception e) {
                log.error("[WorkExecutor] 线程池停止失败 | {} | instanceName={}", this.name, this.instanceName, e);
            }
        }));
    }

    public void execute(Runnable command) {
        executor.execute(command);
    }

    public Future<?> submit(Runnable command) {
        return executor.submit(command);
    }
}
