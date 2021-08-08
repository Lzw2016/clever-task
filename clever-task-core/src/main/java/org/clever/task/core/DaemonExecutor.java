package org.clever.task.core;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 守护线程执行器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/08/08 21:57 <br/>
 */
@Slf4j
public class DaemonExecutor {
    private static final int INITIAL_DELAY = 0;

    /**
     * 执行器线程池
     */
    public final ScheduledExecutorService executor;
    /**
     * 线程池Future
     */
    private ScheduledFuture<?> future;
    /**
     * 任务执行锁
     */
    private final Object lock = new Object();
    /**
     * 当前是否是运行状态
     */
    @Getter
    private boolean running = false;

    public DaemonExecutor() {
        executor = Executors.newSingleThreadScheduledExecutor();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (future != null && !future.isDone() && !future.isCancelled()) {
                try {
                    future.cancel(true);
                    // log.info("[SchedulerInstance]-[{}] 守护线程停止成功", this.getSchedulerInstanceName());
                } catch (Exception e) {
                    // log.error("[SchedulerInstance]-[{}] 守护线程停止失败", this.getSchedulerInstanceName(), e);
                }
            }
            try {
                executor.shutdownNow();
                // log.info("[SchedulerInstance]-[{}] 调度器线程池停止成功", this.getSchedulerInstanceName());
            } catch (Exception e) {
                // log.error("[SchedulerInstance]-[{}] 调度器线程池停止失败", this.getSchedulerInstanceName(), e);
            }
        }));
    }

    /**
     * 以固定速率定期执行任务
     *
     * @param command 任务逻辑
     * @param period  两次执行任务的时间间隔(单位：毫秒)
     */
    public void scheduleAtFixedRate(Runnable command, long period) {
        Assert.notNull(command, "参数command不能为空");
        Assert.isTrue(period > 0, "参数period值必须大于0");
        stop();
        future = executor.scheduleAtFixedRate(() -> {
            if (running) {
                // log.debug("[SchedulerInstance]-[{}] 守护线程正在运行，等待...", this.getSchedulerInstanceName());
                return;
            }
            synchronized (lock) {
                if (running) {
                    // log.debug("[SchedulerInstance]-[{}] 守护线程正在运行，等待...", this.getSchedulerInstanceName());
                    return;
                }
                running = true;
                try {
                    command.run();
                    // } catch (Exception e) {
                } finally {
                    running = false;
                }
            }
        }, INITIAL_DELAY, period, TimeUnit.MILLISECONDS);
    }

    /**
     * 以固定延时定期执行任务
     *
     * @param command 任务逻辑
     * @param delay   固定延时时间(单位：毫秒)
     */
    public void scheduleWithFixedDelay(Runnable command, long delay) {
        Assert.notNull(command, "参数command不能为空");
        Assert.isTrue(delay > 0, "参数delay值必须大于0");
        stop();
        future = executor.scheduleWithFixedDelay(() -> {
            if (running) {
                // log.debug("[SchedulerInstance]-[{}] 守护线程正在运行，等待...", this.getSchedulerInstanceName());
                return;
            }
            synchronized (lock) {
                if (running) {
                    // log.debug("[SchedulerInstance]-[{}] 守护线程正在运行，等待...", this.getSchedulerInstanceName());
                    return;
                }
                running = true;
                try {
                    command.run();
                    // } catch (Exception e) {
                } finally {
                    running = false;
                }
            }
        }, INITIAL_DELAY, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * 停止守护线程调度执行
     */
    public void stop() {
        if (future != null && !future.isDone() && !future.isCancelled()) {
            future.cancel(true);
        }
    }
}
