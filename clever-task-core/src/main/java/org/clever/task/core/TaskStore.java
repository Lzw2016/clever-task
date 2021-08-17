package org.clever.task.core;

import lombok.Getter;
import org.clever.task.core.entity.*;
import org.clever.task.core.exception.SchedulerException;
import org.clever.task.core.model.SchedulerInfo;
import org.clever.task.core.utils.SqlUtils;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 定时任务调度器数据存储
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/08/08 16:14 <br/>
 */
public class TaskStore {
    /**
     * 事务序列号
     */
    private final AtomicInteger transactionSerialNumber = new AtomicInteger(0);
    /**
     * 数据源管理器
     */
    private final DataSourceTransactionManager transactionManager;
    /**
     * 数据库操作支持 JdbcTemplate
     */
    @Getter
    private final JdbcTemplate jdbcTemplate;
    /**
     * 数据库操作支持 NamedParameterJdbcTemplate
     */
    @Getter
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public TaskStore(DataSource dataSource) {
        transactionManager = new DataSourceTransactionManager(dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
        namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    // ---------------------------------------------------------------------------------------------------------------------------------------- dao

    /**
     * 获取数据库当前时间(不需要事务)
     */
    public Date getDataSourceNow() {
        return jdbcTemplate.queryForObject(SqlConstant.DATASOURCE_NOW, Date.class);
    }

    /**
     * 更新或保存 Scheduler
     */
    public Scheduler addOrUpdateScheduler(Scheduler scheduler) {
        List<Scheduler> schedulerList = jdbcTemplate.query(
                SqlConstant.GET_SCHEDULER,
                DataClassRowMapper.newInstance(Scheduler.class),
                scheduler.getNamespace(),
                scheduler.getInstanceName()
        );
        if (schedulerList.size() > 1) {
            throw new SchedulerException(String.format(
                    "集群[namespace=%s]的调度器实例[instanceName=%s]存在多个",
                    scheduler.getNamespace(),
                    scheduler.getInstanceName()
            ));
        }
        Scheduler registered = null;
        if (!schedulerList.isEmpty()) {
            registered = schedulerList.get(0);
        }
        if (registered == null) {
            // 需要注册
            namedParameterJdbcTemplate.update(SqlConstant.ADD_SCHEDULER, new BeanPropertySqlParameterSource(scheduler));
        } else {
            // 需要更新
            namedParameterJdbcTemplate.update(SqlConstant.UPDATE_SCHEDULER, new BeanPropertySqlParameterSource(scheduler));
        }
        // 查询
        schedulerList = jdbcTemplate.query(
                SqlConstant.GET_SCHEDULER,
                DataClassRowMapper.newInstance(Scheduler.class),
                scheduler.getNamespace(),
                scheduler.getInstanceName()
        );
        if (schedulerList.isEmpty()) {
            throw new SchedulerException(String.format(
                    "调度器注册失败[namespace=%s, instanceName=%s]",
                    scheduler.getNamespace(),
                    scheduler.getInstanceName()
            ));
        }
        registered = schedulerList.get(0);
        return registered;
    }

    /**
     * 更新心跳时间
     */
    public int heartbeat(Scheduler scheduler) {
        int count = namedParameterJdbcTemplate.update(SqlConstant.HEARTBEAT_SCHEDULER, new BeanPropertySqlParameterSource(scheduler));
        if (count != 1) {
            throw new SchedulerException(String.format(
                    "心跳维持失败[namespace=%s, instanceName=%s]",
                    scheduler.getNamespace(),
                    scheduler.getInstanceName()
            ));
        }
        return count;
    }

    /**
     * 查询集群中在线的调度器列表
     */
    public List<Scheduler> queryAvailableSchedulerList(String namespace) {
        return jdbcTemplate.query(
                SqlConstant.QUERY_AVAILABLE_SCHEDULER,
                DataClassRowMapper.newInstance(Scheduler.class),
                namespace
        );
    }

    /**
     * 所有调度器
     */
    public List<SchedulerInfo> queryAllSchedulerList(String namespace) {
        return jdbcTemplate.query(
                SqlConstant.QUERY_ALL_SCHEDULER,
                DataClassRowMapper.newInstance(SchedulerInfo.class),
                namespace
        );
    }

    /**
     * 获取无效的触发器配置数量 -> type=2|3
     */
    public Integer countInvalidTrigger(String namespace) {
        return jdbcTemplate.queryForObject(SqlConstant.COUNT_INVALID_TRIGGER, Integer.class, namespace);
    }

    /**
     * 查询集群中启用的触发器列表
     */
    public List<JobTrigger> queryEnableTrigger(String namespace) {
        return jdbcTemplate.query(
                SqlConstant.QUERY_ENABLE_TRIGGER,
                DataClassRowMapper.newInstance(JobTrigger.class),
                namespace
        );
    }

    /**
     * 查询集群中启用的cron触发器列表
     */
    public List<JobTrigger> queryEnableCronTrigger(String namespace) {
        return jdbcTemplate.query(
                SqlConstant.QUERY_ENABLE_CRON_TRIGGER,
                DataClassRowMapper.newInstance(JobTrigger.class),
                namespace
        );
    }

    /**
     * 接下来N秒内需要触发的触发器列表
     */
    public List<JobTrigger> queryNextTrigger(String namespace, Double nextTime) {
        return jdbcTemplate.query(
                SqlConstant.QUERY_NEXT_TRIGGER,
                DataClassRowMapper.newInstance(JobTrigger.class),
                namespace,
                nextTime
        );
    }

    /**
     * 根据 namespace jobTriggerId 查询
     */
    public JobTrigger getTrigger(String namespace, Long jobTriggerId) {
        List<JobTrigger> jobTriggerList = jdbcTemplate.query(
                SqlConstant.GET_TRIGGER,
                DataClassRowMapper.newInstance(JobTrigger.class),
                namespace,
                jobTriggerId
        );
        if (jobTriggerList.isEmpty()) {
            return null;
        }
        return jobTriggerList.get(0);
    }

    /**
     * 获取定时任务悲观锁
     */
    public boolean getLockJob(String namespace, Long jobId, Long lockVersion) {
        return jdbcTemplate.update(SqlConstant.GET_LOCK_JOB, jobId, namespace, lockVersion) > 0;
    }

    /**
     * 根据 namespace jobId 查询
     */
    public Job getJob(String namespace, Long jobId) {
        List<Job> jobList = jdbcTemplate.query(
                SqlConstant.GET_JOB_BY_ID,
                DataClassRowMapper.newInstance(Job.class),
                namespace,
                jobId
        );
        if (jobList.isEmpty()) {
            return null;
        }
        return jobList.get(0);
    }

    /**
     * 查询当前集群所有定时任务信息
     */
    public List<Job> queryAllJob(String namespace) {
        return jdbcTemplate.query(
                SqlConstant.QUERY_ALL_JOB,
                DataClassRowMapper.newInstance(Job.class),
                namespace
        );
    }

    /**
     * 获取HttpJob
     */
    public HttpJob getHttpJob(String namespace, Long jobId) {
        List<HttpJob> jobList = jdbcTemplate.query(
                SqlConstant.HTTP_JOB_BY_JOB_ID,
                DataClassRowMapper.newInstance(HttpJob.class),
                namespace,
                jobId
        );
        if (jobList.isEmpty()) {
            return null;
        }
        return jobList.get(0);
    }

    /**
     * 获取JavaJob
     */
    public JavaJob getJavaJob(String namespace, Long jobId) {
        List<JavaJob> jobList = jdbcTemplate.query(
                SqlConstant.JAVA_JOB_BY_JOB_ID,
                DataClassRowMapper.newInstance(JavaJob.class),
                namespace,
                jobId
        );
        if (jobList.isEmpty()) {
            return null;
        }
        return jobList.get(0);
    }

    /**
     * 获取JsJob
     */
    public JsJob getJsJob(String namespace, Long jobId) {
        List<JsJob> jobList = jdbcTemplate.query(
                SqlConstant.JS_JOB_BY_JOB_ID,
                DataClassRowMapper.newInstance(JsJob.class),
                namespace,
                jobId
        );
        if (jobList.isEmpty()) {
            return null;
        }
        return jobList.get(0);
    }

    /**
     * 获取ShellJob
     */
    public ShellJob getShellJob(String namespace, Long jobId) {
        List<ShellJob> jobList = jdbcTemplate.query(
                SqlConstant.SHELL_JOB_BY_JOB_ID,
                DataClassRowMapper.newInstance(ShellJob.class),
                namespace,
                jobId
        );
        if (jobList.isEmpty()) {
            return null;
        }
        return jobList.get(0);
    }

    /**
     * 更新无效的触发器配置 -> type=2|3 更新 next_fire_time=null
     */
    public int updateInvalidTrigger(String namespace) {
        return jdbcTemplate.update(SqlConstant.UPDATE_INVALID_TRIGGER, namespace);
    }

    /**
     * 更新触发器下一次触发时间
     */
    public int updateNextFireTime(JobTrigger jobTrigger) {
        int count = namedParameterJdbcTemplate.update(SqlConstant.UPDATE_NEXT_FIRE_TIME_TRIGGER, new BeanPropertySqlParameterSource(jobTrigger));
        if (count != 1) {
            throw new SchedulerException(String.format("更新触发器下一次触发时间失败，JobTrigger(id=%s)", jobTrigger.getId()));
        }
        return count;
    }

    /**
     * 更新触发器下一次触发时间 -> type=2 更新 next_fire_time
     */
    public int updateNextFireTimeForType2(String namespace) {
        return jdbcTemplate.update(SqlConstant.UPDATE_TYPE2_NEXT_FIRE_TIME_TRIGGER, namespace);
    }

    /**
     * 更新触发器“上一次触发时间”、“下一次触发时间”
     */
    public void updateFireTime(JobTrigger jobTrigger) {
        namedParameterJdbcTemplate.update(SqlConstant.UPDATE_FIRE_TIME_TRIGGER, new BeanPropertySqlParameterSource(jobTrigger));
    }

    /**
     * 获取触发器悲观锁
     */
    public boolean getLockTrigger(String namespace, Long jobTriggerId, Long lockVersion) {
        return jdbcTemplate.update(SqlConstant.GET_LOCK_TRIGGER, jobTriggerId, namespace, lockVersion) > 0;
    }

    public int addSchedulerLog(SchedulerLog schedulerLog) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        int count = namedParameterJdbcTemplate.update(SqlConstant.ADD_SCHEDULER_LOG, new BeanPropertySqlParameterSource(schedulerLog), keyHolder);
        schedulerLog.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        return count;
    }

    public int addJobTriggerLog(JobTriggerLog jobTriggerLog) {
        return namedParameterJdbcTemplate.update(SqlConstant.ADD_JOB_TRIGGER_LOG, new BeanPropertySqlParameterSource(jobTriggerLog));
    }

    public int addJobLog(JobLog jobLog) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        int count = namedParameterJdbcTemplate.update(SqlConstant.ADD_JOB_LOG, new BeanPropertySqlParameterSource(jobLog), keyHolder);
        jobLog.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        return count;
    }

    public int updateJobLogByEnd(JobLog jobLog) {
        return namedParameterJdbcTemplate.update(SqlConstant.UPDATE_JOB_LOG_BY_END, new BeanPropertySqlParameterSource(jobLog));
    }

    public int updateJobLogByRetry(JobLog jobLog) {
        return namedParameterJdbcTemplate.update(SqlConstant.UPDATE_JOB_LOG_BY_RETRY, new BeanPropertySqlParameterSource(jobLog));
    }

    // ---------------------------------------------------------------------------------------------------------------------------------------- manage

    public int addJob(Job job) {
        final Map<String, Object> paramMap = SqlUtils.toMap(job);
        final String sql = SqlUtils.insertSql(SqlConstant.JOB_TABLE_NAME, paramMap, true);
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        int count = namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource(paramMap), keyHolder);
        job.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        return count;
    }

    public int addJobTrigger(JobTrigger jobTrigger) {
        final Map<String, Object> paramMap = SqlUtils.toMap(jobTrigger);
        final String sql = SqlUtils.insertSql(SqlConstant.JOB_TRIGGER_TABLE_NAME, paramMap, true);
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        int count = namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource(paramMap), keyHolder);
        jobTrigger.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        return count;
    }

    public int addHttpJob(HttpJob httpJob) {
        final Map<String, Object> paramMap = SqlUtils.toMap(httpJob);
        final String sql = SqlUtils.insertSql(SqlConstant.HTTP_JOB_TABLE_NAME, paramMap, true);
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        int count = namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource(paramMap), keyHolder);
        httpJob.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        return count;
    }

    public int addJavaJob(JavaJob javaJob) {
        final Map<String, Object> paramMap = SqlUtils.toMap(javaJob);
        final String sql = SqlUtils.insertSql(SqlConstant.JAVA_JOB_TABLE_NAME, paramMap, true);
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        int count = namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource(paramMap), keyHolder);
        javaJob.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        return count;
    }

    public int addFileResource(FileResource fileResource) {
        final Map<String, Object> paramMap = SqlUtils.toMap(fileResource);
        final String sql = SqlUtils.insertSql(SqlConstant.FILE_RESOURCE_TABLE_NAME, paramMap, true);
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        int count = namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource(paramMap), keyHolder);
        fileResource.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        return count;
    }

    public int addJsJob(JsJob jsJob) {
        final Map<String, Object> paramMap = SqlUtils.toMap(jsJob);
        final String sql = SqlUtils.insertSql(SqlConstant.JS_JOB_TABLE_NAME, paramMap, true);
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        int count = namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource(paramMap), keyHolder);
        jsJob.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        return count;
    }

    public int addShellJob(ShellJob shellJob) {
        final Map<String, Object> paramMap = SqlUtils.toMap(shellJob);
        final String sql = SqlUtils.insertSql(SqlConstant.SHELL_JOB_TABLE_NAME, paramMap, true);
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        int count = namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource(paramMap), keyHolder);
        shellJob.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        return count;
    }

    /**
     * 根据JobId查询脚本文件
     */
    public FileResource getFileResourceById(String namespace, Long fileResourceId) {
        List<FileResource> jobTriggerList = jdbcTemplate.query(
                SqlConstant.GET_FILE_RESOURCE_BY_ID,
                DataClassRowMapper.newInstance(FileResource.class),
                fileResourceId,
                namespace
        );
        if (jobTriggerList.isEmpty()) {
            return null;
        }
        return jobTriggerList.get(0);
    }

    // ---------------------------------------------------------------------------------------------------------------------------------------- transaction support

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link org.springframework.transaction.TransactionDefinition#PROPAGATION_REQUIRED}
     * @param timeout             设置事务超时时间，-1表示不超时(单位：秒)
     * @param isolationLevel      设置事务隔离级别 {@link org.springframework.transaction.TransactionDefinition#ISOLATION_DEFAULT}
     * @param readOnly            设置事务是否只读
     * @param <T>                 返回值类型
     * @see org.springframework.transaction.TransactionDefinition
     */
    public <T> T beginTX(TransactionCallback<T> action, int propagationBehavior, int timeout, int isolationLevel, boolean readOnly) {
        Assert.notNull(action, "数据库操作不能为空");
        TransactionTemplate transactionTemplate = createTransactionDefinition(isolationLevel, propagationBehavior, readOnly, timeout);
        return transactionTemplate.execute(action);
    }

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link org.springframework.transaction.TransactionDefinition#PROPAGATION_REQUIRED}
     * @param timeout             设置事务超时时间(单位：秒)
     * @param isolationLevel      设置事务隔离级别 {@link org.springframework.transaction.TransactionDefinition#ISOLATION_DEFAULT}
     * @param <T>                 返回值类型
     * @see org.springframework.transaction.TransactionDefinition
     */
    public <T> T beginTX(TransactionCallback<T> action, int propagationBehavior, int timeout, int isolationLevel) {
        return beginTX(action, propagationBehavior, timeout, isolationLevel, false);
    }

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link org.springframework.transaction.TransactionDefinition#PROPAGATION_REQUIRED}
     * @param timeout             设置事务超时时间(单位：秒)
     * @param <T>                 返回值类型
     * @see org.springframework.transaction.TransactionDefinition
     */
    public <T> T beginTX(TransactionCallback<T> action, int propagationBehavior, int timeout) {
        return beginTX(action, propagationBehavior, timeout, TransactionDefinition.ISOLATION_DEFAULT, false);
    }

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link org.springframework.transaction.TransactionDefinition#PROPAGATION_REQUIRED}
     * @param <T>                 返回值类型
     * @see org.springframework.transaction.TransactionDefinition
     */
    public <T> T beginTX(TransactionCallback<T> action, int propagationBehavior) {
        return beginTX(action, propagationBehavior, -1, TransactionDefinition.ISOLATION_DEFAULT, false);
    }

    /**
     * 在事务内支持操作
     *
     * @param action 事务内数据库操作
     * @param <T>    返回值类型
     * @see org.springframework.transaction.TransactionDefinition
     */
    public <T> T beginTX(TransactionCallback<T> action) {
        return beginTX(action, TransactionDefinition.PROPAGATION_REQUIRED, -1, TransactionDefinition.ISOLATION_DEFAULT, false);
    }

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link org.springframework.transaction.TransactionDefinition#PROPAGATION_REQUIRED}
     * @param timeout             设置事务超时时间，-1表示不超时(单位：秒)
     * @param isolationLevel      设置事务隔离级别 {@link org.springframework.transaction.TransactionDefinition#ISOLATION_DEFAULT}
     * @param <T>                 返回值类型
     * @see org.springframework.transaction.TransactionDefinition
     */
    public <T> T beginReadOnlyTX(TransactionCallback<T> action, int propagationBehavior, int timeout, int isolationLevel) {
        return beginTX(action, propagationBehavior, timeout, isolationLevel, true);
    }

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link org.springframework.transaction.TransactionDefinition#PROPAGATION_REQUIRED}
     * @param timeout             设置事务超时时间，-1表示不超时(单位：秒)
     * @param <T>                 返回值类型
     * @see org.springframework.transaction.TransactionDefinition
     */
    public <T> T beginReadOnlyTX(TransactionCallback<T> action, int propagationBehavior, int timeout) {
        return beginTX(action, propagationBehavior, timeout, TransactionDefinition.ISOLATION_DEFAULT, true);
    }

    /**
     * 在事务内支持操作
     *
     * @param action              事务内数据库操作
     * @param propagationBehavior 设置事务传递性 {@link org.springframework.transaction.TransactionDefinition#PROPAGATION_REQUIRED}
     * @param <T>                 返回值类型
     * @see org.springframework.transaction.TransactionDefinition
     */
    public <T> T beginReadOnlyTX(TransactionCallback<T> action, int propagationBehavior) {
        return beginTX(action, propagationBehavior, -1, TransactionDefinition.ISOLATION_DEFAULT, true);
    }

    /**
     * 在事务内支持操作
     *
     * @param action 事务内数据库操作
     * @param <T>    返回值类型
     * @see org.springframework.transaction.TransactionDefinition
     */
    public <T> T beginReadOnlyTX(TransactionCallback<T> action) {
        return beginTX(action, TransactionDefinition.PROPAGATION_REQUIRED, -1, TransactionDefinition.ISOLATION_DEFAULT, true);
    }

    /**
     * 创建事务执行模板对象
     *
     * @param isolationLevel      设置事务隔离级别 {@link org.springframework.transaction.TransactionDefinition#ISOLATION_DEFAULT}
     * @param propagationBehavior 设置事务传递性 {@link org.springframework.transaction.TransactionDefinition#PROPAGATION_REQUIRED}
     * @param readOnly            设置事务是否只读
     * @param timeout             设置事务超时时间(单位：秒)
     * @see org.springframework.transaction.TransactionDefinition
     */
    private TransactionTemplate createTransactionDefinition(int isolationLevel, int propagationBehavior, boolean readOnly, int timeout) {
        DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
        transactionDefinition.setName(getNextTransactionName());
        transactionDefinition.setPropagationBehavior(propagationBehavior);
        transactionDefinition.setTimeout(timeout);
        transactionDefinition.setIsolationLevel(isolationLevel);
        transactionDefinition.setReadOnly(readOnly);
        return new TransactionTemplate(transactionManager, transactionDefinition);
    }

    /**
     * 获取下一个事务名称
     */
    private String getNextTransactionName() {
        int nextSerialNumber = transactionSerialNumber.incrementAndGet();
        String transactionName;
        if (nextSerialNumber < 0) {
            transactionName = GlobalConstant.TRANSACTION_NAME_PREFIX + nextSerialNumber;
        } else {
            transactionName = GlobalConstant.TRANSACTION_NAME_PREFIX + "+" + nextSerialNumber;
        }
        return transactionName;
    }
}
