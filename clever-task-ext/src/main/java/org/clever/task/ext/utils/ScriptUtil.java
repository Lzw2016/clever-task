package org.clever.task.ext.utils;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 1、内嵌编译器如"PythonInterpreter"无法引用扩展包，因此推荐使用java调用控制台进程方式"Runtime.getRuntime().exec()"来运行脚本(shell或python)；
 * 2、因为通过java调用控制台进程方式实现，需要保证目标机器PATH路径正确配置对应编译器；
 * 3、暂时脚本执行日志只能在脚本执行结束后一次性获取，无法保证实时性；因此为确保日志实时性，可改为将脚本打印的日志存储在指定的日志文件上；
 * 4、python 异常输出优先级高于标准输出，体现在Log文件中，因此推荐通过logging方式打日志保持和异常信息一致；否则用prinf日志顺序会错乱
 */
public class ScriptUtil {

    /**
     * make script file
     *
     * @param scriptFileName
     * @param content
     * @throws IOException
     */
    public static void markScriptFile(String scriptFileName, String content) throws IOException {
        // make file,   filePath/gluesource/666-123456789.py
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(scriptFileName);
            fileOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
            fileOutputStream.close();
        } catch (Exception e) {
            throw e;
        } finally {
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
        }
    }

    /**
     * 日志文件输出方式
     * <p>
     * 优点：支持将目标数据实时输出到指定日志文件中去
     * 缺点：
     * 标准输出和错误输出优先级固定，可能和脚本中顺序不一致
     * Java无法实时获取
     *
     * @param command
     * @param scriptFile
     * @param logFile
     * @param params
     * @return
     * @throws IOException
     */
    public static Integer execToFile(String command, String scriptFile, String logFile, String... params) {
        // TODO https://blog.csdn.net/fd_mas/article/details/50147701
        // 标准输出：print （null if watchdog timeout）
        // 错误输出：logging + 异常 （still exists if watchdog timeout）
        // 标准输入
        FileOutputStream out = null;
        FileOutputStream err = null;
        // exit code: 0=success, 1=error
        Integer exitValue = null;
        try {
            out = new FileOutputStream(logFile, true);
            err = new FileOutputStream(logFile, true);
            PumpStreamHandler streamHandler = new PumpStreamHandler(out, err);
            // command
            CommandLine commandline = new CommandLine(command);
            commandline.addArgument(scriptFile);
            if (params != null && params.length > 0) {
                commandline.addArguments(params);
            }
            ExecuteWatchdog watchdog = new ExecuteWatchdog(1000 * 60 * 5);
            // exec
            DefaultExecutor exec = new DefaultExecutor();
            exec.setWatchdog(watchdog);
            exec.setExitValues(null);
            exec.setStreamHandler(streamHandler);
            exitValue = exec.execute(commandline);
            if (watchdog.killedProcess()) {
                // 进程被 kill
            }
            watchdog.checkException();
        } catch (Exception e) {
            // TODO XxlJobLogger.log(e);
            exitValue = -1;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // TODO XxlJobLogger.log(e);
                }
            }
            if (err != null) {
                try {
                    err.close();
                } catch (IOException e) {
                    // TODO XxlJobLogger.log(e);
                }
            }
        }
        return exitValue;
    }

}
