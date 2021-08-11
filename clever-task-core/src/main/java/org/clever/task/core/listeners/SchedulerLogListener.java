package org.clever.task.core.listeners;

import org.clever.task.core.TaskStore;
import org.clever.task.core.entity.Scheduler;
import org.clever.task.core.entity.SchedulerLog;

/**
 * 调度器事件日志
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/08/01 21:01 <br/>
 */
public class SchedulerLogListener implements SchedulerListener {
    @Override
    public void onStarted(Scheduler scheduler, TaskStore taskStore, SchedulerLog schedulerLog) {

    }

    @Override
    public void onPaused(Scheduler scheduler, TaskStore taskStore, SchedulerLog schedulerLog) {

    }

    @Override
    public void onErrorEvent(Scheduler scheduler, TaskStore taskStore, SchedulerLog schedulerLog) {

    }
}
