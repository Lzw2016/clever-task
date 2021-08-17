package org.clever.task.ext.job;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.clever.task.core.JobExecutor;
import org.clever.task.core.TaskStore;
import org.clever.task.core.entity.*;
import org.clever.task.core.exception.JobExecutorException;
import org.clever.task.core.utils.DateTimeUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Objects;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/16 13:56 <br/>
 */
@Slf4j
public class ShellJobExecutor implements JobExecutor {
    private static final int COMMAND_TIMEOUT = 1000 * 60 * 10;
    private static final String WORKING_DIR;

    static {
        final String dir = "shell_job";
        String workingDir;
        try {
            workingDir = new File("./").getAbsolutePath();
            workingDir = FilenameUtils.concat(workingDir, dir);
            FileUtils.forceMkdir(new File(workingDir));
        } catch (Exception e) {
            log.warn("创建ShellJob工作目录失败", e);
            try {
                workingDir = FilenameUtils.concat(System.getProperty("user.home"), dir);
                FileUtils.forceMkdir(new File(workingDir));
            } catch (Exception ioException) {
                log.warn("创建ShellJob工作目录失败", e);
                workingDir = null;
                System.exit(-1);
            }
        }
        workingDir = FilenameUtils.normalize(workingDir);
        WORKING_DIR = workingDir;
        if (!(new File(WORKING_DIR).isDirectory())) {
            log.warn("创建ShellJob工作目录失败: {}", WORKING_DIR);
            System.exit(-1);
        }
        log.info("ShellJob工作目录: {}", workingDir);
    }

    @Override
    public boolean support(int jobType) {
        return Objects.equals(jobType, EnumConstant.JOB_TYPE_4);
    }

    @Override
    public int order() {
        return 0;
    }

    @Override
    public void exec(Date dbNow, Job job, Scheduler scheduler, TaskStore taskStore) throws Exception {
        final ShellJob shellJob = taskStore.beginReadOnlyTX(status -> taskStore.getShellJob(scheduler.getNamespace(), job.getId()));
        if (shellJob == null) {
            throw new JobExecutorException(String.format("ShellJob数据不存在，JobId=%s", job.getId()));
        }
        final FileResource fileResource = taskStore.beginReadOnlyTX(status -> taskStore.getFileResourceById(scheduler.getNamespace(), shellJob.getFileResourceId()));
        if (fileResource == null) {
            throw new JobExecutorException(String.format("FileResource数据不存在，JobId=%s | FileResourceId=%s", job.getId(), shellJob.getFileResourceId()));
        }
        final String dir = FilenameUtils.concat(WORKING_DIR, String.format("%s_%s", shellJob.getId(), job.getName()));
        final String timeLine = String.format(
                "\n### %s ###---------------------------------------------------------------------------------------------------------------------------\n",
                DateTimeUtils.formatToString(dbNow)
        );
        final String command = FilenameUtils.concat(dir, EnumConstant.SHELL_TYPE_COMMAND_MAPPING.getOrDefault(shellJob.getShellType(), "sh"));
        final String scriptFile = FilenameUtils.concat(dir, "shell" + EnumConstant.SHELL_TYPE_FILE_SUFFIX_MAPPING.getOrDefault(shellJob.getShellType(), ".txt"));
        final String outLogFile = FilenameUtils.concat(dir, "out.log");
        final String errLogFile = FilenameUtils.concat(dir, "err.log");
        // 生成脚本文件
        FileUtils.forceMkdir(new File(dir));
        FileUtils.writeStringToFile(new File(scriptFile), fileResource.getContent(), StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(new File(outLogFile), timeLine, StandardCharsets.UTF_8, true);
        FileUtils.writeStringToFile(new File(errLogFile), timeLine, StandardCharsets.UTF_8, true);
        int exitValue = execShell(command, scriptFile, outLogFile, errLogFile);
        log.info("ShellJob执行完成，进程退出状态码:{}， shellType={} | shellJobId={} | fileResourceId={}", exitValue, shellJob.getShellType(), shellJob.getId(), fileResource.getId());
    }

    private int execShell(String command, String scriptFile, String outLogFile, String errLogFile, String... params) throws Exception {
        FileOutputStream out = null;
        FileOutputStream err = null;
        // exit code: 0=success, 1=error
        int exitValue;
        try {
            out = new FileOutputStream(outLogFile, true);
            err = new FileOutputStream(errLogFile, true);
            PumpStreamHandler streamHandler = new PumpStreamHandler(out, err);
            // command
            CommandLine commandline = new CommandLine(command);
            commandline.addArgument(scriptFile);
            if (params != null && params.length > 0) {
                commandline.addArguments(params);
            }
            // set timeout
            ExecuteWatchdog watchdog = new ExecuteWatchdog(COMMAND_TIMEOUT);
            // exec
            DefaultExecutor exec = new DefaultExecutor();
            exec.setWatchdog(watchdog);
            exec.setExitValues(null);
            exec.setWorkingDirectory(new File(WORKING_DIR));
            exec.setStreamHandler(streamHandler);
            exitValue = exec.execute(commandline);
            watchdog.checkException();
        } finally {
            try {
                IOUtils.close(out);
            } catch (IOException ignored) {
            }
            try {
                IOUtils.close(err);
            } catch (IOException ignored) {
            }
        }
        return exitValue;
    }
}
