package org.clever.task.core.listeners;

import org.clever.task.core.TaskStore;
import org.clever.task.core.entity.JobTriggerLog;
import org.clever.task.core.entity.Scheduler;

/**
 * 触发器事件监听器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/08/01 20:45 <br/>
 */
public interface JobTriggerListener {
    /**
     * 触发成功
     */
    void onTriggered(Scheduler scheduler, TaskStore taskStore, JobTriggerLog jobTriggerLog);
}
