package org.clever.task.core.entity;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/01 20:36 <br/>
 */
public interface EnumConstant {
    /**
     * 所属模块：0-自定义扩展，1-资源文件，2-初始化脚本，3-HTTP API，4-定时任务
     */
    int FILE_RESOURCE_MODULE_4 = 4;
    /**
     * 数据类型：0-文件夹，1-文件
     */
    int FILE_RESOURCE_IS_FILE_1 = 1;
    /**
     * 读写权限：0-可读可写，1-只读
     */
    int FILE_RESOURCE_READ_ONLY_0 = 0;

    /**
     * 是否是手动触发，0：系统自动触发，1：用户手动触发
     */
    int JOB_TRIGGER_IS_MANUAL_0 = 0;
    /**
     * 是否是手动触发，0：系统自动触发，1：用户手动触发
     */
    int JOB_TRIGGER_IS_MANUAL_1 = 1;

    /**
     * 是否错过了触发，0：否，1：是
     */
    int JOB_TRIGGER_MIS_FIRED_0 = 0;
    /**
     * 是否错过了触发，0：否，1：是
     */
    int JOB_TRIGGER_MIS_FIRED_1 = 1;

    /**
     * 是否允许多节点并行触发，使用悲观锁实现，0：禁止，1：允许
     */
    int JOB_TRIGGER_ALLOW_CONCURRENT_0 = 0;
    /**
     * 是否允许多节点并行触发，使用悲观锁实现，0：禁止，1：允许
     */
    int JOB_TRIGGER_ALLOW_CONCURRENT_1 = 1;

    /**
     * 任务类型，1：cron触发，2：固定速率触发
     */
    int JOB_TRIGGER_TYPE_1 = 1;
    /**
     * 任务类型，1：cron触发，2：固定速率触发
     */
    int JOB_TRIGGER_TYPE_2 = 2;

    /**
     * 错过触发策略，1：忽略，2：立即补偿触发一次
     */
    int JOB_TRIGGER_MISFIRE_STRATEGY_1 = 1;
    /**
     * 错过触发策略，1：忽略，2：立即补偿触发一次
     */
    int JOB_TRIGGER_MISFIRE_STRATEGY_2 = 2;

    /**
     * 是否允许多节点并发执行，使用悲观锁实现，0：禁止，1：允许
     */
    int JOB_ALLOW_CONCURRENT_0 = 0;
    /**
     * 是否允许多节点并发执行，使用悲观锁实现，0：禁止，1：允许
     */
    int JOB_ALLOW_CONCURRENT_1 = 1;

    /**
     * 路由策略，0：不启用，1：指定节点优先，2：固定节点白名单，3：固定节点黑名单
     */
    int JOB_ROUTE_STRATEGY_0 = 0;
    /**
     * 路由策略，0：不启用，1：指定节点优先，2：固定节点白名单，3：固定节点黑名单
     */
    int JOB_ROUTE_STRATEGY_1 = 1;
    /**
     * 路由策略，0：不启用，1：指定节点优先，2：固定节点白名单，3：固定节点黑名单
     */
    int JOB_ROUTE_STRATEGY_2 = 2;
    /**
     * 路由策略，0：不启用，1：指定节点优先，2：固定节点白名单，3：固定节点黑名单
     */
    int JOB_ROUTE_STRATEGY_3 = 3;

    /**
     * 负载均衡策略，1：抢占，2：随机，3：轮询，4：一致性HASH
     */
    int JOB_LOAD_BALANCE_1 = 1;
    /**
     * 负载均衡策略，1：抢占，2：随机，3：轮询，4：一致性HASH
     */
    int JOB_LOAD_BALANCE_2 = 2;
    /**
     * 负载均衡策略，1：抢占，2：随机，3：轮询，4：一致性HASH
     */
    int JOB_LOAD_BALANCE_3 = 3;
    /**
     * 负载均衡策略，1：抢占，2：随机，3：轮询，4：一致性HASH
     */
    int JOB_LOAD_BALANCE_4 = 4;

    /**
     * 是否禁用：0-启用，1-禁用
     */
    int JOB_DISABLE_0 = 0;
    /**
     * 是否禁用：0-启用，1-禁用
     */
    int JOB_DISABLE_1 = 1;

    /**
     * 任务类型，1：http调用，2：java调用，3：js脚本，4：shell脚本
     */
    int JOB_TYPE_1 = 1;
    /**
     * 任务类型，1：http调用，2：java调用，3：js脚本，4：shell脚本
     */
    int JOB_TYPE_2 = 2;
    /**
     * 任务类型，1：http调用，2：java调用，3：js脚本，4：shell脚本
     */
    int JOB_TYPE_3 = 3;
    /**
     * 任务类型，1：http调用，2：java调用，3：js脚本，4：shell脚本
     */
    int JOB_TYPE_4 = 4;

    /**
     * 是否更新任务数据，0：不更新，1：更新
     */
    int JOB_IS_UPDATE_DATA_0 = 0;
    /**
     * 是否更新任务数据，0：不更新，1：更新
     */
    int JOB_IS_UPDATE_DATA_1 = 1;

    /**
     * 任务执行结果，0：成功，1：失败，2：取消
     */
    int JOB_LOG_STATUS_0 = 0;
    /**
     * 任务执行结果，0：成功，1：失败，2：取消
     */
    int JOB_LOG_STATUS_1 = 1;
    /**
     * 任务执行结果，0：成功，1：失败，2：取消
     */
    int JOB_LOG_STATUS_2 = 2;

    /**
     * 是否是静态方法(函数)，0：非静态，1：静态
     */
    int JAVA_JOB_IS_STATIC_0 = 0;
    /**
     * 是否是静态方法(函数)，0：非静态，1：静态
     */
    int JAVA_JOB_IS_STATIC_1 = 1;
}
