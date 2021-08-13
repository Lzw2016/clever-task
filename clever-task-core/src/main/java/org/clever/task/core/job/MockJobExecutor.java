package org.clever.task.core.job;

import lombok.extern.slf4j.Slf4j;
import org.clever.task.core.JobExecutor;
import org.clever.task.core.TaskStore;
import org.clever.task.core.entity.Job;
import org.clever.task.core.entity.Scheduler;
import org.clever.task.core.utils.DateTimeUtils;

import java.util.Date;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/12 12:14 <br/>
 */
@Slf4j
public class MockJobExecutor implements JobExecutor {
    @Override
    public boolean support(int jobType) {
        return true;
    }

    @Override
    public int order() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void exec(Date dbNow, Job job, Scheduler scheduler, TaskStore taskStore) throws Exception {
        Thread.sleep(3_000);
        log.info("#### ---> 模拟执行定时任务 | name={} | time={}", job.getName(), DateTimeUtils.formatToString(dbNow, "HH:mm:ss.SSS"));
    }
}
