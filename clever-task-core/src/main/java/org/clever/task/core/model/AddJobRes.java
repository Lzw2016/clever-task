package org.clever.task.core.model;

import lombok.Data;
import org.clever.task.core.entity.*;

import java.io.Serializable;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/22 20:52 <br/>
 */
@Data
public class AddJobRes implements Serializable {
    private Job job;

    private JobTrigger jobTrigger;

    private HttpJob httpJob ;

    private JavaJob javaJob;

    private JsJob jsJob;

    private ShellJob shellJob;

    private FileResource fileResource;
}
