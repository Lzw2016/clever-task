package org.clever.task.core;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/03 10:59 <br/>
 */
public interface SqlConstant {
    String DATASOURCE_NOW = "select now(3) from dual";

    // ---------------------------------------------------------------------------------------------------------------------------------------- scheduler

    String GET_SCHEDULER = "select * from scheduler where namespace=? and instance_name=?";

    String ADD_SCHEDULER = "" +
            "insert into scheduler " +
            "(namespace, instance_name, heartbeat_interval, config, description) " +
            "VALUES " +
            "(:namespace, :instanceName, :heartbeatInterval, :config, :description)";

    String UPDATE_SCHEDULER = "" +
            "update scheduler " +
            "set heartbeat_interval=:heartbeatInterval, config=:config, description=:description " +
            "where namespace=:namespace and instance_name=:instanceName";

    String HEARTBEAT_SCHEDULER = "" +
            "update scheduler " +
            "set last_heartbeat_time=now(3), heartbeat_interval=:heartbeatInterval, config=:config, description=:description " +
            "where namespace=:namespace and instance_name=:instanceName";

    String QUERY_AVAILABLE_SCHEDULER = "" +
            "select * from scheduler " +
            "where namespace=? " +
            "and last_heartbeat_time is not null " +
            "and (heartbeat_interval * 2) > ((unix_timestamp(now(3)) - unix_timestamp(last_heartbeat_time)) * 1000)";

    // ---------------------------------------------------------------------------------------------------------------------------------------- job_trigger

    String QUERY_ENABLE_TRIGGER = "select * from job_trigger " +
            "where disable=0 " +
            "and namespace=?";

    String QUERY_ENABLE_CRON_TRIGGER = "select * from job_trigger " +
            "where disable=0 " +
            "and type=1 " +
            "and namespace=?";

    String QUERY_NEXT_TRIGGER = "" +
            "select * from job_trigger " +
            "where disable=0 " +
            "and start_time<=now(3) " +
            "and next_fire_time is not null " +
            "and unix_timestamp(next_fire_time)-unix_timestamp(now(3))<=? " +
            "and namespace=? " +
            "order by next_fire_time";

    String GET_TRIGGER = "" +
            "select * from job_trigger " +
            "where disable=0 " +
            "and namespace=? " +
            "and id=?";

    // 获取无效的触发器配置数量 -> type=2|3
    String COUNT_INVALID_TRIGGER = "" +
            "select count(1) from job_trigger " +
            "where disable=0 " +
            "and ((type=2 and fixed_interval<=0) or (type=3 and delay_time<=0)) " +
            "and namespace=?";

    // 更新无效的触发器配置 -> type=2|3 更新 next_fire_time=null
    String UPDATE_INVALID_TRIGGER = "" +
            "update job_trigger set next_fire_time=null " +
            "where disable=0 " +
            "and next_fire_time is not null " +
            "and ((type=2 and fixed_interval<=0) or (type=3 and delay_time<=0)) " +
            "and namespace=?";

    // 更新触发器下一次触发时间 -> type=2 更新 next_fire_time
    String UPDATE_TYPE2_NEXT_FIRE_TIME_TRIGGER = "" +
            "update job_trigger " +
            "set next_fire_time= " +
            "  case " +
            "    when isnull(last_fire_time) then date_add(start_time, interval fixed_interval second) " +
            "    when timestampdiff(microsecond, last_fire_time, start_time)>=0 then date_add(start_time, interval fixed_interval second) " +
            "    when timestampdiff(microsecond, last_fire_time, start_time)<0 then date_add(last_fire_time, interval fixed_interval second) " +
            "    else date_add(now(), interval fixed_interval second) " +
            "  end " +
            "where disable=0 " +
            "  and type=2 " +
            "  and fixed_interval>0 " +
            "  and (next_fire_time is null or next_fire_time!= " +
            "    case " +
            "        when isnull(last_fire_time) then date_add(start_time, interval fixed_interval second) " +
            "        when timestampdiff(microsecond, last_fire_time, start_time)>=0 then date_add(start_time, interval fixed_interval second) " +
            "        when timestampdiff(microsecond, last_fire_time, start_time)<0 then date_add(last_fire_time, interval fixed_interval second) " +
            "        else date_add(now(), interval fixed_interval second) " +
            "    end) " +
            "  and namespace=?";

    String UPDATE_NEXT_FIRE_TIME_TRIGGER = "" +
            "update job_trigger set next_fire_time=:nextFireTime " +
            "where id=:id " +
            // "and (end_time is null or end_time>=now(3)) " +
            "and namespace=:namespace";

    String UPDATE_FIRE_TIME_TRIGGER = "" +
            "update job_trigger set last_fire_time=next_fire_time, next_fire_time=:nextFireTime " +
            "where id=:id " +
            "and namespace=:namespace";

    String LOCK_TRIGGER_ROW = "select id from job_trigger where namespace=? and id=? for update";

    // ---------------------------------------------------------------------------------------------------------------------------------------- job

    String QUERY_ALL_JOB = "select * from job where namespace=?";

    // ---------------------------------------------------------------------------------------------------------------------------------------- file_resource

    String GET_FILE_RESOURCE_BY_JOB_ID = "" +
            "select " +
            "  b.* " +
            "from js_job a left join file_resource b on (a.file_resource_id=b.id and a.namespace=b.namespace) " +
            "where a.job_id=? " +
            "  and a.namespace=? " +
            "limit 1";
}
