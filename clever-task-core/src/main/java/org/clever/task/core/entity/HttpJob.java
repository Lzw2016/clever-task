package org.clever.task.core.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Http任务(HttpJob)实体类
 *
 * @author lizw
 * @since 2021-08-01 20:33:26
 */
@Data
public class HttpJob implements Serializable {
    /**
     * 主键id
     */
    private Long id;

    /**
     * 命名空间
     */
    private String namespace;

    /**
     * 任务ID
     */
    private Long jobId;

    /**
     * http请求method，ALL GET HEAD POST PUT DELETE CONNECT OPTIONS TRACE PATCH
     */
    private String requestMethod;

    /**
     * Http请求地址
     */
    private String requestUrl;

    /**
     * Http请求数据json格式，包含：params、headers、body
     */
    private String requestData;

    /**
     * Http请求是否成功校验(js脚本)
     */
    private String successCheck;

    /**
     * 创建时间
     */
    private Date createAt;

    /**
     * 更新时间
     */
    private Date updateAt;
}
