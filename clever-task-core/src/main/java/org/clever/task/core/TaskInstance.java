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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * 定时任务调度器实例
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/08/01 20:38 <br/>
 */
@Slf4j
public class TaskInstance {
    // 线程池线程保持时间
    private static final long THREAD_POOL_KEEP_ALIVE_SECONDS = 60L;
    // 定时任务触发线程轮询的时间间隔(单位：毫秒)
    private static final int JOB_TRIGGER_INTERVAL = 500;
    // 接下来N秒内需要触发的触发器列表(N = heartbeatInterval * NEXT_TRIGGER_INTERVAL)
    private static final int NEXT_TRIGGER_INTERVAL = 2;
    // SchedulerContext allJobMap 数据自动刷新时间间隔
    private static final int JOB_RELOAD_INTERVAL = 3_000;

    /*
        1.数据完整性校验、一致性校验
        2.调度器节点注册
        3.初始化触发器下一次触发时间(校准触发器触发时间)
        ------------------------------------------------------
        1.心跳保持
        2.维护当前集群可用的调度器列表
        3.维护定时任务列表
        4.维护接下来N秒内需要触发的触发器列表
        5.调度器轮询任务
    */
    /**
     * 心跳保持-守护线程
     */
    private final DaemonExecutor heartbeatDaemon = new DaemonExecutor();


    /**
     * 调度线程池
     */
    private final ThreadPoolExecutor schedulerExecutor;
    /**
     * 定时任务执行线程池
     */
    private final ThreadPoolExecutor jobExecutor;
    /**
     * 守护线程池
     */
    public final ScheduledExecutorService daemonExecutor = Executors.newSingleThreadScheduledExecutor();
    /**
     * 守护线程池Future
     */
    private ScheduledFuture<?> daemonFuture;
    /**
     * 守护线程运行标识
     */
    private boolean daemonRunning = false;
    /**
     * 守护线程锁
     */
    private final Object daemonLock = new Object();
    /**
     * 定时任务触发线程池
     */
    public final ScheduledExecutorService jobTriggerExecutor = Executors.newSingleThreadScheduledExecutor();
    /**
     * 定时任务触发线程池Future
     */
    private ScheduledFuture<?> jobTriggerFuture;
    /**
     * 定时任务触发线程运行标识
     */
    private boolean jobTriggerRunning = false;
    /**
     * 定时任务触发线程锁
     */
    private final Object jobTriggerLock = new Object();
    /**
     * 调度器数据存储对象
     */
    private final TaskStore schedulerStore;
    /**
     * 调度器上下文
     */
    private final TaskContext schedulerContext;
    /**
     * 调度器状态
     */
    private volatile TaskState schedulerState = TaskState.None;
    /**
     * 调度器锁
     */
    private final Object schedulerLock = new Object();


    public TaskInstance(DataSource dataSource, SchedulerConfig schedulerConfig) {
        // 初始化数据源
        schedulerStore = new TaskStore(dataSource);
        // 注册调度器
        Scheduler scheduler = toScheduler(schedulerConfig);
        scheduler = registerScheduler(scheduler);
        schedulerContext = new TaskContext(this, schedulerConfig, scheduler);
        // 初始化线程池
        schedulerExecutor = new ThreadPoolExecutor(
                schedulerConfig.getSchedulerExecutorPoolSize(),
                schedulerConfig.getSchedulerExecutorPoolSize(),
                THREAD_POOL_KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(schedulerConfig.getMaxConcurrent())
        );
        jobExecutor = new ThreadPoolExecutor(
                schedulerConfig.getJobExecutorPoolSize(),
                schedulerConfig.getJobExecutorPoolSize(),
                THREAD_POOL_KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(schedulerConfig.getMaxConcurrent())
        );
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (daemonFuture != null && !daemonFuture.isDone() && !daemonFuture.isCancelled()) {
                try {
                    daemonFuture.cancel(true);
                    log.info("[SchedulerInstance]-[{}] 守护线程停止成功", this.getSchedulerInstanceName());
                } catch (Exception e) {
                    log.error("[SchedulerInstance]-[{}] 守护线程停止失败", this.getSchedulerInstanceName(), e);
                }
            }
            if (jobTriggerFuture != null && !jobTriggerFuture.isDone() && !jobTriggerFuture.isCancelled()) {
                try {
                    jobTriggerFuture.cancel(true);
                    log.info("[SchedulerInstance]-[{}] 定时任务触发线程停止成功", this.getSchedulerInstanceName());
                } catch (Exception e) {
                    log.error("[SchedulerInstance]-[{}] 定时任务触发线程停止失败", this.getSchedulerInstanceName(), e);
                }
            }
            try {
                schedulerExecutor.shutdownNow();
                log.info("[SchedulerInstance]-[{}] 调度器线程池停止成功", this.getSchedulerInstanceName());
            } catch (Exception e) {
                log.error("[SchedulerInstance]-[{}] 调度器线程池停止失败", this.getSchedulerInstanceName(), e);
            }
            try {
                jobExecutor.shutdownNow();
                log.info("[SchedulerInstance]-[{}] 任务执行线程池停止成功", this.getSchedulerInstanceName());
            } catch (Exception e) {
                log.error("[SchedulerInstance]-[{}] 任务执行线程池停止失败", this.getSchedulerInstanceName(), e);
            }
            try {
                daemonExecutor.shutdownNow();
                log.info("[SchedulerInstance]-[{}] 守护线程池停止成功", this.getSchedulerInstanceName());
            } catch (Exception e) {
                log.error("[SchedulerInstance]-[{}] 守护线程池停止失败", this.getSchedulerInstanceName(), e);
            }
            try {
                jobTriggerExecutor.shutdownNow();
                log.info("[SchedulerInstance]-[{}] 定时任务触发线程池停止成功", this.getSchedulerInstanceName());
            } catch (Exception e) {
                log.error("[SchedulerInstance]-[{}] 定时任务触发线程池停止失败", this.getSchedulerInstanceName(), e);
            }
        }));
    }

    // ---------------------------------------------------------------------------------------------------------------------------------------- api

    /**
     * 当前集群 namespace
     */
    public String getNamespace() {
        return schedulerContext.getCurrentScheduler().getNamespace();
    }

    /**
     * 当前调度器实例名
     */
    public String getSchedulerInstanceName() {
        return schedulerContext.getCurrentScheduler().getInstanceName();
    }

    /**
     * 调度器上下文
     */
    public TaskContext getContext() {
        return schedulerContext;
    }

    /**
     * 当前调度器状态
     */
    public TaskState getSchedulerState() {
        return schedulerState;
    }

    /**
     * 同步启动调度器
     */
    public void start() {
        doStart();
    }

    /**
     * 异步延时启动调度器
     *
     * @param seconds 延时时间(单位：秒)
     */
    public void startDelayed(int seconds) {
        schedulerExecutor.execute(() -> {
            if (seconds > 0) {
                try {
                    Thread.sleep(seconds * 1000L);
                } catch (InterruptedException e) {
                    log.warn("[SchedulerInstance]-[{}] 异步延时启动，延时失败", this.getSchedulerInstanceName(), e);
                }
            }
            try {
                doStart();
            } catch (Exception e) {
                log.error("[SchedulerInstance]-[{}] 异步延时启动失败", this.getSchedulerInstanceName(), e);
            }
        });
    }

//    /**
//     * 暂停调度器
//     */
//    public void pause() {
//    }
//
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

    // 注册调度器，返回注册后的调度器对象
    private Scheduler registerScheduler(Scheduler scheduler) {
        return schedulerStore.beginTX(status -> schedulerStore.addOrUpdateScheduler(scheduler));
    }

    // 启动调度器
    private void doStart() {
        if (schedulerState != TaskState.None && schedulerState != TaskState.Pause) {
            throw new SchedulerException(String.format("无效的操作，当前调度器状态：%s，", schedulerState));
        }
        synchronized (schedulerLock) {
            if (schedulerState != TaskState.None && schedulerState != TaskState.Pause) {
                throw new SchedulerException(String.format("无效的操作，当前调度器状态：%s，", schedulerState));
            }
            // 备份之前的状态
            final TaskState oldState = schedulerState;
            try {
                // 开始初始化
                schedulerState = TaskState.Initializing;
                init();
                // 初始化完成就是运行中
                schedulerState = TaskState.Running;
            } catch (Exception e) {
                // 异常就还原之前的状态
                schedulerState = oldState;
                log.error("[SchedulerInstance]-[{}] 启动失败", this.getSchedulerInstanceName(), e);
            }
        }
    }

    // 初始化调度器
    private void init() {
        final Scheduler scheduler = schedulerContext.getCurrentScheduler();
        // TODO 数据完整性校验、一致性校验
        // 初始化守护线程
        initDaemon(scheduler);
        // 初始化触发器
        initJobTrigger(scheduler);
        // 开始轮询任务
        initJobTriggerExecutor();
    }

    // 初始化守护线程
    private void initDaemon(final Scheduler scheduler) {
        if (daemonFuture != null && !daemonFuture.isDone() && !daemonFuture.isCancelled()) {
            daemonFuture.cancel(true);
        }
        daemonFuture = daemonExecutor.scheduleAtFixedRate(() -> {
            if (daemonRunning) {
                log.debug("[SchedulerInstance]-[{}] 守护线程正在运行，等待...", this.getSchedulerInstanceName());
                return;
            }
            synchronized (daemonLock) {
                if (daemonRunning) {
                    log.debug("[SchedulerInstance]-[{}] 守护线程正在运行，等待...", this.getSchedulerInstanceName());
                    return;
                }
                daemonRunning = true;
                try {
                    final long startTime = System.currentTimeMillis();
                    // 心跳保持
                    schedulerStore.beginTX(status -> schedulerStore.heartbeat(scheduler));
                    // 当前集群可用的调度器列表
                    final List<Scheduler> availableSchedulerList = schedulerStore.beginReadOnlyTX(status -> schedulerStore.queryAvailableSchedulerList(scheduler.getNamespace()));
                    schedulerContext.setAvailableSchedulerList(availableSchedulerList);
                    // 接下来N秒内需要触发的触发器列表
                    final long nextTime = scheduler.getHeartbeatInterval() * NEXT_TRIGGER_INTERVAL;
                    final List<JobTrigger> nextJobTriggerList = schedulerStore.beginReadOnlyTX(status -> schedulerStore.queryNextTrigger(nextTime, scheduler.getNamespace()));
                    schedulerContext.setNextJobTriggerMap(nextJobTriggerList);
                    // 当前所有启用的定时任务信息
                    if (startTime - schedulerContext.getJobLastLoadTime() > JOB_RELOAD_INTERVAL) {
                        final List<Job> allJobList = schedulerStore.beginReadOnlyTX(status -> schedulerStore.queryAllJob(scheduler.getNamespace()));
                        schedulerContext.setAllJobMap(allJobList, startTime);
                    }
                    final long endTime = System.currentTimeMillis();
                    log.debug("[SchedulerInstance]-[{}] 守护线程完成 | 耗时: {}ms", this.getSchedulerInstanceName(), (endTime - startTime));
                } catch (Exception e) {
                    log.error("[SchedulerInstance]-[{}] 守护线程异常", this.getSchedulerInstanceName(), e);
                } finally {
                    daemonRunning = false;
                }
            }
        }, 0, scheduler.getHeartbeatInterval(), TimeUnit.MILLISECONDS);
    }

    // 初始化触发器
    private void initJobTrigger(final Scheduler scheduler) {
        // 1.更新无效的Trigger配置
        int invalidCount = schedulerStore.beginReadOnlyTX(status -> schedulerStore.countInvalidTrigger(scheduler.getNamespace()));
        int updateCount = schedulerStore.beginTX(status -> schedulerStore.updateInvalidTrigger(scheduler.getNamespace()));
        if (updateCount > 0) {
            log.info("[SchedulerInstance]-[{}] 更新异常触发器nextFireTime=null | 更新数量：{}", this.getSchedulerInstanceName(), updateCount);
        }
        // 全部cron触发器列表
        List<JobTrigger> cronTriggerList = schedulerStore.beginReadOnlyTX(status -> schedulerStore.queryEnableCronTrigger(scheduler.getNamespace()));
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
                    schedulerStore.beginTX(status -> schedulerStore.updateNextFireTime(cronTrigger));
                }
            }
        }
        // 2.计算触发器下一次触发时间
        // 更新触发器下一次触发时间 -> type=2 更新 next_fire_time
        updateCount = schedulerStore.beginTX(status -> schedulerStore.updateNextFireTimeForType2(scheduler.getNamespace()));
        // 更新cron触发器下一次触发时间 -> type=1
        for (JobTrigger cronTrigger : effectiveCronTriggerList) {
            try {
                final Date nextFireTime = JobTriggerUtils.getNextFireTime(cronTrigger);
                if (cronTrigger.getNextFireTime() == null || cronTrigger.getNextFireTime().compareTo(nextFireTime) != 0) {
                    updateCount++;
                    cronTrigger.setNextFireTime(nextFireTime);
                    schedulerStore.beginTX(status -> schedulerStore.updateNextFireTime(cronTrigger));
                }
            } catch (Exception e) {
                // TODO 记录调度器日志(异步)
                log.error("[SchedulerInstance]-[{}] 计算触发器下一次触发时间失败 | JobTrigger(id={})", this.getSchedulerInstanceName(), cronTrigger.getId(), e);
            }
        }
        log.info("[SchedulerInstance]-[{}] 更新触发器下一次触发时间nextFireTime字段 | 更新数量：{}", this.getSchedulerInstanceName(), updateCount);
        if (invalidCount > 0) {
            log.warn("[SchedulerInstance]-[{}] 触发器配置检查完成，异常的触发器数量：{}", this.getSchedulerInstanceName(), invalidCount);
        } else {
            log.info("[SchedulerInstance]-[{}] 触发器配置检查完成，无异常触发器", this.getSchedulerInstanceName());
        }
        // 更新触发器下一次触发时间 -> type=3 TODO 暂不支持固定延时触发 type=3
    }

    // 初始化轮询任务
    private void initJobTriggerExecutor() {
        if (jobTriggerFuture != null && !jobTriggerFuture.isDone() && !jobTriggerFuture.isCancelled()) {
            jobTriggerFuture.cancel(true);
        }
        jobTriggerFuture = jobTriggerExecutor.scheduleAtFixedRate(() -> {
            if (jobTriggerRunning) {
                log.warn("[SchedulerInstance]-[{}] 定时任务触发线程正在运行，等待...", this.getSchedulerInstanceName());
                return;
            }
            if (schedulerContext.getAllJobSize() <= 0) {
                log.debug("[SchedulerInstance]-[{}] 未找到定时任务数据", this.getSchedulerInstanceName());
                return;
            }
            synchronized (jobTriggerLock) {
                if (jobTriggerRunning) {
                    log.warn("[SchedulerInstance]-[{}] 定时任务触发线程正在运行，等待...", this.getSchedulerInstanceName());
                    return;
                }
                jobTriggerRunning = true;
                try {
                    final long startTime = System.currentTimeMillis();
                    // 轮询触发 job
                    final List<JobTrigger> nextJobTriggerList = schedulerContext.getNextJobTriggerList();
                    final Date dbNow = schedulerStore.getDataSourceNow();
                    for (JobTrigger jobTrigger : nextJobTriggerList) {
                        // 判断触发时间是否已到
                        if (dbNow.compareTo(jobTrigger.getNextFireTime()) < 0) {
                            continue;
                        }
                        try {
                            schedulerExecutor.execute(() -> executeJobTrigger(dbNow, jobTrigger));
                            log.debug(
                                    "[SchedulerInstance]-[{}] JobTrigger触发完成 | id={} name={}",
                                    this.getSchedulerInstanceName(),
                                    jobTrigger.getId(),
                                    jobTrigger.getName()
                            );
                        } catch (Exception e) {
                            // TODO 记录调度器日志(异步)
                            log.error(
                                    "[SchedulerInstance]-[{}] JobTrigger触发失败 | id={} name={}",
                                    this.getSchedulerInstanceName(),
                                    jobTrigger.getId(),
                                    jobTrigger.getName(),
                                    e
                            );
                        }
                    }
                    final long endTime = System.currentTimeMillis();
                    log.debug("[SchedulerInstance]-[{}] 定时任务触发线程完成 | 耗时: {}ms", this.getSchedulerInstanceName(), (endTime - startTime));
                } finally {
                    jobTriggerRunning = false;
                }
            }
        }, 0, JOB_TRIGGER_INTERVAL, TimeUnit.MILLISECONDS);
    }

    // 执行任务触发器
    private void executeJobTrigger(final Date dbNow, final JobTrigger jobTrigger) {
        try {
            final Job job = schedulerContext.getJob(jobTrigger.getJobId());
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
            final int jobRunCount = schedulerContext.getJobRunCount(jobTrigger.getJobId());
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
            schedulerStore.beginTX(status -> {
                JobTrigger currentJobTrigger = jobTrigger;
                if (!allowConcurrent) {
                    // 锁住JobTrigger
                    schedulerStore.lockTriggerRow(jobTrigger.getNamespace(), jobTrigger.getId());
                    // 获取最新的JobTrigger
                    currentJobTrigger = schedulerStore.getTrigger(jobTrigger.getNamespace(), jobTrigger.getId());
                }
                // 定时任务数据不存在了
                if (currentJobTrigger == null || currentJobTrigger.getNextFireTime() == null) {
                    schedulerContext.removeNextJobTrigger(jobTrigger.getId());
                    return null;
                }
                // 判断是否被其他节点执行了
                if (dbNow.compareTo(currentJobTrigger.getNextFireTime()) < 0) {
                    // TODO 被其它节点执行了
                    schedulerContext.putNextJobTrigger(currentJobTrigger);
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
                    jobExecutor.execute(() -> executeJob(dbNow, job));
                }
                // 计算下一次触发时间
                final Date newNextFireTime = JobTriggerUtils.getNextFireTime(dbNow, currentJobTrigger);
                currentJobTrigger.setNextFireTime(newNextFireTime);
                schedulerStore.updateFireTime(currentJobTrigger);
                // 获取最新的JobTrigger
                currentJobTrigger = schedulerStore.getTrigger(currentJobTrigger.getNamespace(), currentJobTrigger.getId());
                if (currentJobTrigger == null) {
                    schedulerContext.removeNextJobTrigger(jobTrigger.getId());
                } else {
                    schedulerContext.putNextJobTrigger(currentJobTrigger);
                }
                return null;
            });
        } catch (Exception e) {
            // TODO 记录触发器日志(异步)
            log.error(
                    "[SchedulerInstance]-[{}] JobTrigger触发失败 | id={} name={}",
                    this.getSchedulerInstanceName(),
                    jobTrigger.getId(),
                    jobTrigger.getName(),
                    e
            );
        } finally {
            // TODO 触发器事件处理
        }
    }

    private void executeJob(final Date dbNow, final Job job) {
        try {
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
                            "[SchedulerInstance]-[{}] Job执行失败，重试次数：{} | id={} name={}",
                            this.getSchedulerInstanceName(),
                            retryCount,
                            job.getId(),
                            job.getName(),
                            e
                    );
                }
            }
        } catch (Exception e) {
            // TODO 记录任务执行日志(异步)
            log.error(
                    "[SchedulerInstance]-[{}] Job执行失败 | id={} name={}",
                    this.getSchedulerInstanceName(),
                    job.getId(),
                    job.getName(),
                    e
            );
        } finally {
            // TODO 任务执行事件处理
        }
    }

    private void runJob(final Date dbNow, final Job job) {
        log.info("#### ---> 模拟执行定时任务 | name={} | time={}", job.getName(), DateTimeUtils.formatToString(dbNow, "HH:mm:ss.SSS"));
//        switch (job.getType()) {
//            case EnumConstant.JOB_TYPE_1:
//                // http调用 TODO 暂不支持http任务
//                throw new SchedulerException(String.format("暂不支持http任务，Job(id=%s)", job.getId()));
//                // break;
//            case EnumConstant.JOB_TYPE_2:
//                // java调用 TODO 暂不支持java任务
//                throw new SchedulerException(String.format("暂不支持java任务，Job(id=%s)", job.getId()));
//                // break;
//            case EnumConstant.JOB_TYPE_3:
//                // js脚本
//                FileResource fileResource = getFileResourceByJobId(job.getNamespace(), job.getId());
//                if (fileResource == null) {
//                    throw new SchedulerException(String.format("js脚本任务FileResource不存在，Job(id=%s)", job.getId()));
//                }
//                if (StringUtils.isBlank(fileResource.getContent())) {
//                    throw new SchedulerException(String.format("js脚本任务脚本内容为空，Job(id=%s)", job.getId()));
//                }
//                ScriptEngineInstanceUtils.scriptEngineInstance.wrapFunctionAndEval(fileResource.getContent(), (Consumer<ScriptObject>) ScriptObject::execute);
//                break;
//            case EnumConstant.JOB_TYPE_4:
//                // shell脚本 TODO 暂不支持shell脚本任务
//                throw new SchedulerException(String.format("暂不支持shell脚本任务，Job(id=%s)", job.getId()));
//                // break;
//        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------------------- support

    // SchedulerConfig 转换成 Scheduler
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
