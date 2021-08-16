package org.clever.task.ext.job;

import lombok.extern.slf4j.Slf4j;
import org.clever.task.core.JobExecutor;
import org.clever.task.core.TaskStore;
import org.clever.task.core.entity.EnumConstant;
import org.clever.task.core.entity.Job;
import org.clever.task.core.entity.JsJob;
import org.clever.task.core.entity.Scheduler;
import org.clever.task.core.exception.JobExecutorException;

import java.util.Date;
import java.util.Objects;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/16 13:55 <br/>
 */
@Slf4j
public class JsJobExecutor implements JobExecutor {
    @Override
    public boolean support(int jobType) {
        return Objects.equals(jobType, EnumConstant.JOB_TYPE_3);
    }

    @Override
    public int order() {
        return 0;
    }

    @Override
    public void exec(Date dbNow, Job job, Scheduler scheduler, TaskStore taskStore) throws Exception {
        final JsJob jsJob = taskStore.beginReadOnlyTX(status -> taskStore.getJsJob(scheduler.getNamespace(), job.getId()));
        if (jsJob == null) {
            throw new JobExecutorException(String.format("JsJob数据不存在，JobId=%s", job.getId()));
        }
        // TODO 执行JsJob
    }
}
