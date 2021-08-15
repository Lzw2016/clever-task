package org.clever.task.core.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.clever.task.core.entity.EnumConstant;
import org.clever.task.core.entity.HttpJob;
import org.clever.task.core.utils.JacksonMapper;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.LinkedHashMap;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/15 12:06 <br/>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class HttpJobModel extends AbstractJob {
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
    private HttpRequestData requestData;
    /**
     * Http请求是否成功校验(js脚本)
     */
    private String successCheck;

    public HttpJobModel(String name, String requestMethod, String requestUrl) {
        Assert.hasText(name, "参数name不能为空");
        Assert.hasText(requestMethod, "参数requestMethod不能为空");
        Assert.hasText(requestUrl, "参数requestUrl不能为空");
        this.name = name;
        this.requestMethod = requestMethod;
        this.requestUrl = requestUrl;
    }

    @Override
    public Integer getType() {
        return EnumConstant.JOB_TYPE_1;
    }

    public HttpJob toJobEntity() {
        HttpJob httpJob = new HttpJob();
        httpJob.setRequestMethod(getRequestMethod());
        httpJob.setRequestUrl(getRequestUrl());
        if (getRequestData() != null) {
            httpJob.setRequestData(getRequestData().toString());
        }
        httpJob.setSuccessCheck(getSuccessCheck());
        return httpJob;
    }

    @Data
    public static final class HttpRequestData implements Serializable {
        private LinkedHashMap<String, String> params;
        private LinkedHashMap<String, String> headers;
        private LinkedHashMap<String, String> body;
        // private LinkedHashMap<String, HttpCookie> cookies;

        @Override
        public String toString() {
            return JacksonMapper.getInstance().toJson(this);
        }
    }
}
