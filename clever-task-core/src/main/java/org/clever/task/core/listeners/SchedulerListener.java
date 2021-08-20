package org.clever.task.core.listeners;

import org.clever.task.core.TaskStore;
import org.clever.task.core.entity.Scheduler;
import org.clever.task.core.entity.SchedulerLog;

/**
 * 调度器事件监听器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/08/01 20:45 <br/>
 */
public interface SchedulerListener {
    /**
     * 调度器启动完成
     */
    void onStarted(Scheduler scheduler, TaskStore taskStore, SchedulerLog schedulerLog);

    /**
     * 调度器已停止
     */
    void onPaused(Scheduler scheduler, TaskStore taskStore, SchedulerLog schedulerLog);

    /**
     * 调度器出现错误
     */
    void onErrorEvent(Scheduler scheduler, TaskStore taskStore, SchedulerLog schedulerLog, Exception error);
}
