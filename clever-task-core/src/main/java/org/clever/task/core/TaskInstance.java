package org.clever.task.core;

import lombok.extern.slf4j.Slf4j;
import org.clever.task.core.config.SchedulerConfig;
import org.clever.task.core.cron.CronExpressionUtil;
import org.clever.task.core.entity.EnumConstant;
import org.clever.task.core.entity.Job;
import org.clever.task.core.entity.JobTrigger;
import org.clever.task.core.entity.Scheduler;
import org.clever.task.core.exception.SchedulerException;
import org.clever.task.core.utils.DateTimeUtils;
import org.clever.task.core.utils.JacksonMapper;
import org.clever.task.core.utils.JobTriggerUtils;

import javax.sql.DataSource;
import java.util.*;

/**
 * 定时任务调度器实例
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/08/01 20:38 <br/>
 */
@Slf4j
public class TaskInstance {
    private static final String DATA_CHECK_DAEMON_NAME = "定时任务数据校验";
    private static final String REGISTER_SCHEDULER_DAEMON_NAME = "调度器节点注册";
    private static final String CALC_NEXT_FIRE_TIME_DAEMON_NAME = "校准触发器触发时间";
    private static final String HEARTBEAT_DAEMON_NAME = "心跳保持";
    private static final String RELOAD_SCHEDULER_DAEMON_NAME = "加载调度器";
    private static final String RELOAD_NEXT_TRIGGER_DAEMON_NAME = "加载将要触发的触发器";
    private static final String TRIGGER_JOB_EXEC_DAEMON_NAME = "调度器轮询任务";

    private static final String SCHEDULER_EXECUTOR_NAME = "调度线程池";
    private static final String JOB_EXECUTOR_NAME = "定时任务执行线程池";

    // 数据完整性校验(一致性校验)的时间间隔(单位：毫秒)
    private static final int DATA_CHECK_INTERVAL = 300_000;
    // 调度器节点注册的时间间隔(单位：毫秒)
    private static final int REGISTER_SCHEDULER_INTERVAL = 60_000;
    // 初始化触发器下一次触发时间(校准触发器触发时间)的时间间隔(单位：毫秒)
    private static final int CALC_NEXT_FIRE_TIME_INTERVAL = 300_000;
    // 维护当前集群可用的调度器列表的时间间隔(单位：毫秒)
    private static final int RELOAD_SCHEDULER_INTERVAL = 5_000;
    // 维护接下来N秒内需要触发的触发器列表的时间间隔(单位：毫秒)
    private static final int RELOAD_NEXT_TRIGGER_INTERVAL = 3_000;
    // 接下来N秒内需要触发的触发器列表(N = heartbeatInterval * NEXT_TRIGGER_N)
    private static final int NEXT_TRIGGER_N = 2;
    // 调度器轮询任务的时间间隔(单位：毫秒)
    private static final int TRIGGER_JOB_EXEC_INTERVAL = 30;

    /**
     * 调度器数据存储对象
     */
    private final TaskStore taskStore;
    /**
     * 调度器上下文
     */
    private final TaskContext taskContext;
    /**
     * 调度器状态
     */
    private volatile TaskState taskState = TaskState.None;
    /**
     * 调度器锁
     */
    private final Object schedulerLock = new Object();

    /**
     * 数据完整性校验、一致性校验 (守护线程)
     */
    private final DaemonExecutor dataCheckDaemon;
    /**
     * 调度器节点注册 (守护线程)
     */
    private final DaemonExecutor registerSchedulerDaemon;
    /**
     * 初始化触发器下一次触发时间(校准触发器触发时间) (守护线程)
     */
    private final DaemonExecutor calcNextFireTimeDaemon;
    /**
     * 心跳保持 (守护线程)
     */
    private final DaemonExecutor heartbeatDaemon;
    /**
     * 维护当前集群可用的调度器列表 (守护线程)
     */
    private final DaemonExecutor reloadSchedulerDaemon;
    /**
     * 维护接下来N秒内需要触发的触发器列表 (守护线程)
     */
    private final DaemonExecutor reloadNextTriggerDaemon;
    /**
     * 调度器轮询任务 (守护线程)
     */
    private final DaemonExecutor triggerJobExecDaemon;
    /**
     * 调度工作线程池
     */
    private final WorkExecutor schedulerWorker;
    /**
     * 定时任务执行工作线程池
     */
    private final WorkExecutor jobWorker;
    /**
     * 定时任务执行器实现列表
     */
    private final List<JobExecutor> jobExecutors;

    public TaskInstance(DataSource dataSource, SchedulerConfig schedulerConfig, List<JobExecutor> jobExecutors) {
        // 初始化数据源
        taskStore = new TaskStore(dataSource);
        // 注册调度器
        Scheduler scheduler = registerScheduler(toScheduler(schedulerConfig));
        taskContext = new TaskContext(schedulerConfig, scheduler);
        // 初始化守护线程池
        dataCheckDaemon = new DaemonExecutor(DATA_CHECK_DAEMON_NAME, schedulerConfig.getInstanceName());
        registerSchedulerDaemon = new DaemonExecutor(REGISTER_SCHEDULER_DAEMON_NAME, schedulerConfig.getInstanceName());
        calcNextFireTimeDaemon = new DaemonExecutor(CALC_NEXT_FIRE_TIME_DAEMON_NAME, schedulerConfig.getInstanceName());
        heartbeatDaemon = new DaemonExecutor(HEARTBEAT_DAEMON_NAME, schedulerConfig.getInstanceName());
        reloadSchedulerDaemon = new DaemonExecutor(RELOAD_SCHEDULER_DAEMON_NAME, schedulerConfig.getInstanceName());
        reloadNextTriggerDaemon = new DaemonExecutor(RELOAD_NEXT_TRIGGER_DAEMON_NAME, schedulerConfig.getInstanceName());
        triggerJobExecDaemon = new DaemonExecutor(TRIGGER_JOB_EXEC_DAEMON_NAME, schedulerConfig.getInstanceName());
        // 初始化工作线程池
        schedulerWorker = new WorkExecutor(
                SCHEDULER_EXECUTOR_NAME,
                schedulerConfig.getInstanceName(),
                schedulerConfig.getSchedulerExecutorPoolSize(),
                schedulerConfig.getMaxConcurrent()
        );
        jobWorker = new WorkExecutor(
                JOB_EXECUTOR_NAME,
                schedulerConfig.getInstanceName(),
                schedulerConfig.getJobExecutorPoolSize(),
                schedulerConfig.getMaxConcurrent()
        );
        // 初始化定时任务执行器实现列表
        jobExecutors.sort(Comparator.comparingInt(JobExecutor::order));
        this.jobExecutors = jobExecutors;
        if (this.jobExecutors.isEmpty()) {
            log.error("[TaskInstance] 定时任务执行器实现列表为空 | instanceName={}", schedulerConfig.getInstanceName());
        }
        if (log.isInfoEnabled()) {
            StringBuilder sb = new StringBuilder();
            this.jobExecutors.forEach(jobExecutor -> sb.append("\n").append(jobExecutor.getClass().getName()));
            log.info("[TaskInstance] 定时任务执行器实现列表顺序如下 | instanceName={} {}", schedulerConfig.getInstanceName(), sb);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------------------- api

    /**
     * 当前集群 namespace
     */
    public String getNamespace() {
        return taskContext.getCurrentScheduler().getNamespace();
    }

    /**
     * 当前调度器实例名
     */
    public String getInstanceName() {
        return taskContext.getCurrentScheduler().getInstanceName();
    }

    /**
     * 调度器上下文
     */
    public TaskContext getContext() {
        return taskContext;
    }

    /**
     * 当前调度器状态
     */
    public TaskState getTaskState() {
        return taskState;
    }

    /**
     * 同步启动调度器
     */
    public void start() {
        startCheck();
        synchronized (schedulerLock) {
            startCheck();
            final Scheduler scheduler = taskContext.getCurrentScheduler();
            // 备份之前的状态
            final TaskState oldState = taskState;
            try {
                // 开始初始化
                taskState = TaskState.Initializing;
                // 1.数据完整性校验、一致性校验
                dataCheckDaemon.scheduleAtFixedRate(this::dataCheck, DATA_CHECK_INTERVAL);
                // 2.调度器节点注册
                registerSchedulerDaemon.scheduleAtFixedRate(() -> registerScheduler(taskContext.getCurrentScheduler()), REGISTER_SCHEDULER_INTERVAL);
                // 3.初始化触发器下一次触发时间(校准触发器触发时间)
                calcNextFireTimeDaemon.scheduleAtFixedRate(this::calcNextFireTime, CALC_NEXT_FIRE_TIME_INTERVAL);
                // 1.心跳保持
                heartbeatDaemon.scheduleAtFixedRate(this::heartbeat, scheduler.getHeartbeatInterval());
                // 2.维护当前集群可用的调度器列表
                reloadSchedulerDaemon.scheduleAtFixedRate(this::reloadScheduler, RELOAD_SCHEDULER_INTERVAL);
                // 3.维护接下来N秒内需要触发的触发器列表
                reloadNextTriggerDaemon.scheduleAtFixedRate(this::reloadNextTrigger, RELOAD_NEXT_TRIGGER_INTERVAL);
                // 4.调度器轮询任务
                triggerJobExecDaemon.scheduleAtFixedRate(this::triggerJobExec, TRIGGER_JOB_EXEC_INTERVAL);
                // 5.维护定时任务列表 TODO 维护定时任务列表?
                // if (startTime - taskContext.getJobLastLoadTime() > JOB_RELOAD_INTERVAL) {
                //     final List<Job> allJobList = taskStore.beginReadOnlyTX(status -> taskStore.queryAllJob(scheduler.getNamespace()));
                //     taskContext.setAllJobMap(allJobList, startTime);
                // }
                // 初始化完成就是运行中
                taskState = TaskState.Running;
            } catch (Exception e) {
                // 异常就还原之前的状态
                taskState = oldState;
                log.error("[TaskInstance]-[{}] 启动失败", this.getInstanceName(), e);
            }
        }
    }

    /**
     * 异步延时启动调度器
     *
     * @param seconds 延时时间(单位：秒)
     */
    public void startDelayed(int seconds) {
        schedulerWorker.execute(() -> {
            if (seconds > 0) {
                try {
                    Thread.sleep(seconds * 1000L);
                } catch (InterruptedException e) {
                    log.warn("[TaskInstance] 异步延时启动，延时失败 | instanceName={}", this.getInstanceName(), e);
                }
            }
            try {
                start();
            } catch (Exception e) {
                log.error("[TaskInstance] 异步延时启动失败 | instanceName={}", this.getInstanceName(), e);
            }
        });
    }

    /**
     * 暂停调度器
     */
    public void pause() {
        dataCheckDaemon.stop();
        registerSchedulerDaemon.stop();
        calcNextFireTimeDaemon.stop();
        heartbeatDaemon.stop();
        reloadSchedulerDaemon.stop();
        reloadNextTriggerDaemon.stop();
        triggerJobExecDaemon.stop();
    }

//    /**
//     * 增加定时任务
//     */
//    public void addJob() {
//    }
//
//    /**
//     *
//     */
//    public void addJobs() {
//    }
//
//    /**
//     *
//     */
//    public void disableJob() {
//    }
//
//    /**
//     *
//     */
//    public void disableJobs() {
//    }
//
//    /**
//     *
//     */
//    public void enableJob() {
//    }
//
//    /**
//     *
//     */
//    public void enableJobs() {
//    }
//
//    /**
//     *
//     */
//    public void deleteJob() {
//    }
//
//    /**
//     *
//     */
//    public void deleteJobs() {
//    }
//
//    /**
//     *
//     */
//    public void triggerJob() {
//    }
//
//    /**
//     *
//     */
//    public void triggerJobs() {
//    }
//
//    /**
//     *
//     */
//    public void updateJob() {
//    }
//
//    /**
//     *
//     */
//    public void updateJobs() {
//    }
//
//    /**
//     *
//     */
//    public void interruptJob() {
//    }
//
//    /**
//     *
//     */
//    public void interruptJobs() {
//    }
//
//    /**
//     *
//     */
//    public void queryJobs() {
//    }
//
//    /**
//     *
//     */
//    public void allSchedulers() {
//    }


    // ---------------------------------------------------------------------------------------------------------------------------------------- service

    // ---------------------------------------------------------------------------------------------------------------------------------------- service

    /**
     * 启动调度器前的校验
     */
    private void startCheck() {
        if (taskState != TaskState.None && taskState != TaskState.Pause) {
            throw new SchedulerException(String.format("无效的操作，当前调度器状态：%s，", taskState));
        }
    }

    /**
     * 调度器节点注册，返回注册后的调度器对象
     */
    private Scheduler registerScheduler(Scheduler scheduler) {
        return taskStore.beginTX(status -> taskStore.addOrUpdateScheduler(scheduler));
    }

    /**
     * 数据完整性校验、一致性校验
     */
    private void dataCheck() {
        // TODO 数据完整性校验、一致性校验
    }

    /**
     * 初始化触发器下一次触发时间(校准触发器触发时间)
     */
    private void calcNextFireTime() {
        final Scheduler scheduler = taskContext.getCurrentScheduler();
        // 1.更新无效的Trigger配置
        int invalidCount = taskStore.beginReadOnlyTX(status -> taskStore.countInvalidTrigger(scheduler.getNamespace()));
        int updateCount = taskStore.beginTX(status -> taskStore.updateInvalidTrigger(scheduler.getNamespace()));
        if (updateCount > 0) {
            log.info("[TaskInstance] 更新异常触发器nextFireTime=null | 更新数量：{} | instanceName={}", updateCount, this.getInstanceName());
        }
        // 全部cron触发器列表
        List<JobTrigger> cronTriggerList = taskStore.beginReadOnlyTX(status -> taskStore.queryEnableCronTrigger(scheduler.getNamespace()));
        // 有效的cron触发器列表
        List<JobTrigger> effectiveCronTriggerList = new ArrayList<>(cronTriggerList.size());
        // 检查cron格式有效性
        for (JobTrigger cronTrigger : cronTriggerList) {
            boolean effective = CronExpressionUtil.isValidExpression(cronTrigger.getCron());
            if (effective) {
                effectiveCronTriggerList.add(cronTrigger);
            } else {
                invalidCount++;
                if (cronTrigger.getNextFireTime() != null) {
                    cronTrigger.setNextFireTime(null);
                    taskStore.beginTX(status -> taskStore.updateNextFireTime(cronTrigger));
                }
            }
        }
        // 2.计算触发器下一次触发时间
        // 更新触发器下一次触发时间 -> type=2 更新 next_fire_time
        updateCount = taskStore.beginTX(status -> taskStore.updateNextFireTimeForType2(scheduler.getNamespace()));
        // 更新cron触发器下一次触发时间 -> type=1
        for (JobTrigger cronTrigger : effectiveCronTriggerList) {
            try {
                final Date nextFireTime = JobTriggerUtils.getNextFireTime(cronTrigger);
                if (cronTrigger.getNextFireTime() == null || cronTrigger.getNextFireTime().compareTo(nextFireTime) != 0) {
                    updateCount++;
                    cronTrigger.setNextFireTime(nextFireTime);
                    taskStore.beginTX(status -> taskStore.updateNextFireTime(cronTrigger));
                }
            } catch (Exception e) {
                // TODO 记录调度器日志(异步)
                log.error("[TaskInstance] 计算触发器下一次触发时间失败 | JobTrigger(id={}) | instanceName={}", cronTrigger.getId(), this.getInstanceName(), e);
            }
        }
        log.info("[TaskInstance] 更新触发器下一次触发时间nextFireTime字段 | 更新数量：{} | instanceName={}", updateCount, this.getInstanceName());
        if (invalidCount > 0) {
            log.warn("[TaskInstance] 触发器配置检查完成，异常的触发器数量：{} | instanceName={}", invalidCount, this.getInstanceName());
        } else {
            log.info("[TaskInstance] 触发器配置检查完成，无异常触发器 | instanceName={}", this.getInstanceName());
        }
    }

    /**
     * 心跳保持
     */
    private void heartbeat() {
        final Scheduler scheduler = taskContext.getCurrentScheduler();
        taskStore.beginTX(status -> taskStore.heartbeat(scheduler));
    }

    /**
     * 维护当前集群可用的调度器列表
     */
    private void reloadScheduler() {
        final Scheduler scheduler = taskContext.getCurrentScheduler();
        final List<Scheduler> availableSchedulerList = taskStore.beginReadOnlyTX(status -> taskStore.queryAvailableSchedulerList(scheduler.getNamespace()));
        taskContext.setAvailableSchedulerList(availableSchedulerList);
    }

    /**
     * 维护接下来N秒内需要触发的触发器列表
     */
    private void reloadNextTrigger() {
        final Scheduler scheduler = taskContext.getCurrentScheduler();
        final long nextTime = RELOAD_NEXT_TRIGGER_INTERVAL * NEXT_TRIGGER_N;
        final List<JobTrigger> nextJobTriggerList = taskStore.beginReadOnlyTX(status -> taskStore.queryNextTrigger(scheduler.getNamespace(), nextTime));
        taskContext.setNextJobTriggerMap(nextJobTriggerList);
    }

    /**
     * 调度器轮询任务
     */
    private void triggerJobExec() {
        final long startTime = System.currentTimeMillis();
        // 轮询触发 job
        final List<JobTrigger> nextJobTriggerList = taskContext.getNextJobTriggerList();
        final Date dbNow = taskStore.getDataSourceNow();
        for (JobTrigger jobTrigger : nextJobTriggerList) {
            // 判断触发时间是否已到
            if (dbNow.compareTo(jobTrigger.getNextFireTime()) < 0) {
                continue;
            }
            try {
                schedulerWorker.execute(() -> doTriggerJobExec(dbNow, jobTrigger));
                log.debug(
                        "[TaskInstance] JobTrigger触发完成 | id={} name={} | instanceName={}",
                        jobTrigger.getId(),
                        jobTrigger.getName(),
                        this.getInstanceName()
                );
            } catch (Exception e) {
                log.error(
                        "[TaskInstance] JobTrigger触发失败 | id={} name={} | instanceName={}",
                        jobTrigger.getId(),
                        jobTrigger.getName(),
                        this.getInstanceName(),
                        e
                );
            } finally {
                // TODO 记录调度器日志(异步)
            }
        }
        final long endTime = System.currentTimeMillis();
        log.debug("[TaskInstance] 定时任务触发线程完成 | 耗时：{}ms | instanceName={}", (endTime - startTime), this.getInstanceName());
    }

    /**
     * 触发定时任务逻辑
     */
    private void doTriggerJobExec(final Date dbNow, final JobTrigger jobTrigger) {
        try {
            final Job job = taskStore.beginReadOnlyTX(status -> taskStore.getJob(jobTrigger.getNamespace(), jobTrigger.getJobId()));
            if (job == null) {
                throw new SchedulerException(String.format(
                        "JobTrigger对应的Job数据不存在，JobTrigger(id=%s|jobId=%s)",
                        jobTrigger.getId(),
                        jobTrigger.getJobId()
                ));
            }
            // 当前任务是否禁用
            if (!Objects.equals(job.getDisable(), EnumConstant.JOB_DISABLE_0)) {
                // TODO 当前任务被禁用
                return;
            }
            final int jobRunCount = taskContext.getJobRunCount(jobTrigger.getJobId());
            // 1.控制重入执行
            if (jobRunCount > Math.max(job.getMaxReentry(), 0)) {
                // TODO 最大重入执行数量
                return;
            }
            // 2.控制并发执行 是否允许多节点并发执行
            final boolean allowConcurrent = Objects.equals(EnumConstant.JOB_ALLOW_CONCURRENT_1, job.getAllowConcurrent());
            // 3.控制任务执行节点 // TODO 暂不支持控制任务执行节点
            switch (job.getRouteStrategy()) {
                case EnumConstant.JOB_ROUTE_STRATEGY_1:
                    // 指定节点优先
                    break;
                case EnumConstant.JOB_ROUTE_STRATEGY_2:
                    // 固定节点白名单
                    break;
                case EnumConstant.JOB_ROUTE_STRATEGY_3:
                    // 固定节点黑名单
                    break;
            }
            // 4.负载均衡策略 // TODO 暂不支持负载均衡策略
            switch (job.getLoadBalance()) {
                case EnumConstant.JOB_LOAD_BALANCE_1:
                    // 抢占
                    break;
                case EnumConstant.JOB_LOAD_BALANCE_2:
                    // 随机
                    break;
                case EnumConstant.JOB_LOAD_BALANCE_3:
                    // 轮询
                    break;
                case EnumConstant.JOB_LOAD_BALANCE_4:
                    // 一致性HASH
                    break;
            }
            taskStore.beginTX(status -> {
                JobTrigger currentJobTrigger = jobTrigger;
                if (!allowConcurrent) {
                    // 锁住JobTrigger
                    taskStore.lockTriggerRow(jobTrigger.getNamespace(), jobTrigger.getId());
                    // 获取最新的JobTrigger
                    currentJobTrigger = taskStore.getTrigger(jobTrigger.getNamespace(), jobTrigger.getId());
                }
                // 定时任务数据不存在了
                if (currentJobTrigger == null || currentJobTrigger.getNextFireTime() == null) {
                    taskContext.removeNextJobTrigger(jobTrigger.getId());
                    return null;
                }
                // 判断是否被其他节点执行了
                if (dbNow.compareTo(currentJobTrigger.getNextFireTime()) < 0) {
                    // TODO 被其它节点执行了
                    taskContext.putNextJobTrigger(currentJobTrigger);
                    return null;
                }
                // 触发定时任务
                final Date endTime = jobTrigger.getEndTime();
                final Date lastFireTime = jobTrigger.getLastFireTime();
                final Date nextFireTime = jobTrigger.getNextFireTime();
                final Integer misfireStrategy = jobTrigger.getMisfireStrategy();
                boolean needRunJob = true;
                // 判断是否错过了触发
                if (lastFireTime != null && endTime != null && nextFireTime.compareTo(endTime) > 0) {
                    needRunJob = false;
                    // 需要补偿触发
                    switch (misfireStrategy) {
                        case EnumConstant.JOB_TRIGGER_MISFIRE_STRATEGY_1:
                            // 忽略补偿触发 TODO 记录触发器日志(异步)
                            break;
                        case EnumConstant.JOB_TRIGGER_MISFIRE_STRATEGY_2:
                            // TODO 立即补偿触发一次(异步)
                            needRunJob = true;
                            break;
                        default:
                            throw new SchedulerException(String.format("任务触发器misfireStrategy字段值错误，JobTrigger(id=%s)", jobTrigger.getId()));
                    }
                }
                // 执行定时任务
                if (needRunJob) {
                    // 执行任务
                    jobWorker.execute(() -> executeJob(dbNow, job));
                }
                // 计算下一次触发时间
                final Date newNextFireTime = JobTriggerUtils.getNextFireTime(dbNow, currentJobTrigger);
                currentJobTrigger.setNextFireTime(newNextFireTime);
                taskStore.updateFireTime(currentJobTrigger);
                // 获取最新的JobTrigger
                currentJobTrigger = taskStore.getTrigger(currentJobTrigger.getNamespace(), currentJobTrigger.getId());
                if (currentJobTrigger == null) {
                    taskContext.removeNextJobTrigger(jobTrigger.getId());
                } else {
                    taskContext.putNextJobTrigger(currentJobTrigger);
                }
                return null;
            });
        } catch (Exception e) {
            // TODO 记录触发器日志(异步)
            log.error(
                    "[TaskInstance] JobTrigger触发失败 | id={} name={} | instanceName={}",
                    jobTrigger.getId(),
                    jobTrigger.getName(),
                    this.getInstanceName(),
                    e
            );
        } finally {
            // TODO 触发器事件处理
        }
    }

    /**
     * 执行定时任务逻辑
     */
    private void executeJob(final Date dbNow, final Job job) {
        try {
            final int jobRunCount = taskContext.getAndIncrementJobRunCount(job.getId());
            // 控制重入执行
            if (jobRunCount > Math.max(job.getMaxReentry(), 0)) {
                // TODO 最大重入执行数量
                return;
            }
            // 支持重试执行任务
            final int maxRetryCount = Math.max(job.getMaxRetryCount(), 1);
            int retryCount = 0;
            while (retryCount < maxRetryCount) {
                retryCount++;
                try {
                    runJob(dbNow, job);
                    break;
                } catch (Exception e) {
                    // TODO 记录任务执行日志(异步)
                    log.error(
                            "[TaskInstance] Job执行失败，重试次数：{} | id={} name={} | instanceName={}",
                            retryCount,
                            job.getId(),
                            job.getName(),
                            this.getInstanceName(),
                            e
                    );
                }
            }
        } catch (Exception e) {
            // TODO 记录任务执行日志(异步)
            log.error(
                    "[TaskInstance] Job执行失败 | id={} name={} | instanceName={}",
                    job.getId(),
                    job.getName(),
                    this.getInstanceName(),
                    e
            );
        } finally {
            // TODO 任务执行事件处理
            final int jobRunCount = taskContext.decrementAndGetJobRunCount(job.getId());
        }
    }

    /**
     * 运行定时任务
     */
    private void runJob(final Date dbNow, final Job job) {
        log.info("#### ---> 模拟执行定时任务 | name={} | time={}", job.getName(), DateTimeUtils.formatToString(dbNow, "HH:mm:ss.SSS"));
        JobExecutor jobExecutor = null;
        for (JobExecutor executor : jobExecutors) {
            if (executor.support(job.getType())) {
                jobExecutor = executor;
                break;
            }
        }
//        if (jobExecutor == null) {
//            throw new SchedulerException(String.format("暂不支持的任务类型，Job(id=%s)", job.getId()));
//        }
//        jobExecutor.exec(dbNow, job);
    }

    // ---------------------------------------------------------------------------------------------------------------------------------------- support

    /**
     * SchedulerConfig 转换成 Scheduler
     */
    private Scheduler toScheduler(SchedulerConfig schedulerConfig) {
        Scheduler.Config config = new Scheduler.Config();
        config.setSchedulerExecutorPoolSize(schedulerConfig.getSchedulerExecutorPoolSize());
        config.setJobExecutorPoolSize(schedulerConfig.getJobExecutorPoolSize());
        config.setLoadWeight(schedulerConfig.getLoadWeight());
        config.setMaxConcurrent(schedulerConfig.getMaxConcurrent());
        Scheduler scheduler = new Scheduler();
        scheduler.setNamespace(schedulerConfig.getNamespace());
        scheduler.setInstanceName(schedulerConfig.getInstanceName());
        scheduler.setHeartbeatInterval(schedulerConfig.getHeartbeatInterval());
        scheduler.setConfig(JacksonMapper.getInstance().toJson(config));
        scheduler.setDescription(schedulerConfig.getDescription());
        return scheduler;
    }
}
