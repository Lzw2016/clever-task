package org.clever.task.core.listeners;

import org.clever.task.core.TaskStore;
import org.clever.task.core.entity.JobTriggerLog;
import org.clever.task.core.entity.Scheduler;

/**
 * 定时任务触发日志
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/08/01 20:59 <br/>
 */
public class JobTriggerLogListener implements JobTriggerListener {
    @Override
    public void onTriggered(Scheduler scheduler, TaskStore taskStore, JobTriggerLog jobTriggerLog) {

    }
}
