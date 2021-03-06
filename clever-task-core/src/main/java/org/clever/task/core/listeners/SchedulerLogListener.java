package org.clever.task.core.listeners;

import lombok.extern.slf4j.Slf4j;
import org.clever.task.core.TaskStore;
import org.clever.task.core.entity.Scheduler;
import org.clever.task.core.entity.SchedulerLog;

/**
 * 调度器事件日志
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/08/01 21:01 <br/>
 */
@Slf4j
public class SchedulerLogListener implements SchedulerListener {
    @Override
    public void onStarted(Scheduler scheduler, TaskStore taskStore, SchedulerLog schedulerLog) {
        saveSchedulerLog(taskStore, schedulerLog);
    }

    @Override
    public void onPaused(Scheduler scheduler, TaskStore taskStore, SchedulerLog schedulerLog) {
        saveSchedulerLog(taskStore, schedulerLog);
    }

    @Override
    public void onErrorEvent(Scheduler scheduler, TaskStore taskStore, SchedulerLog schedulerLog, Exception error) {
        saveSchedulerLog(taskStore, schedulerLog);
    }

    private void saveSchedulerLog(TaskStore taskStore, SchedulerLog schedulerLog) {
        int count = taskStore.beginTX(status -> taskStore.addSchedulerLog(schedulerLog));
        if (count <= 0) {
            log.error("调度器日志保存失败，schedulerLog={}", schedulerLog);
        }
    }
}
