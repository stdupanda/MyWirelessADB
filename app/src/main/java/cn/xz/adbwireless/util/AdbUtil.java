package cn.xz.adbwireless.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

/**
 * AdbUtil 工具类
 * Created by gsx on 2016/11/8.
 */

public class AdbUtil {

    private static final String PORT = "5555";
    /**
     * 设置为空字符串""或者"/system/bin/su"
     */
    private static final String EXE_PREFIX = "";

    /**
     * shell执行ps判断指定Process是否在运行
     *
     * @param processName 进程名字
     * @return 执行进程是否在运行
     * @throws Exception
     */
    public static boolean isProcessRunning(String processName) throws Exception {
        boolean running = false;
        Process process = null;
        process = Runtime.getRuntime().exec("ps");
        BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = null;
        while ((line = in.readLine()) != null) {
            if (line.contains(processName)) {
                running = true;
                break;
            }
        }
        in.close();
        process.waitFor();
        return running;
    }

    /**
     * shell执行su判断是否有root权限
     *
     * @return 是当前app否有root权限
     */
    public static boolean hasRootPermission() {
        Process process = null;
        DataOutputStream os = null;
        boolean rooted = true;
        try {
            process = Runtime.getRuntime().exec("su");
//            process = Runtime.getRuntime().exec("echo hello");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
            if (process.exitValue() != 0) {
                rooted = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            rooted = false;
        } finally {
            if (os != null) {
                try {
                    os.close();
                    process.destroy();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return rooted;
    }

    /**
     * Root身份执行shell命令，不关注输出结果
     *
     * @param command 待执行的shell命令
     * @return 是否成功执行shell命令
     */
    public static boolean runRootCommand(String command) {
        Process process = null;
        DataOutputStream os = null;
        try {
            //Runtime.getRuntime().exec(new String[]{"/system/bin/su", "-c", "start adbd"});
            //process = Runtime.getRuntime().exec("/system/bin/su");
            // 此处若是设置为/system/bin/su则会报
            // Command: [/system/bin/su] Working Directory: null Environment: null
            process = Runtime.getRuntime().exec(EXE_PREFIX + "su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                process.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    /**
     * 设置系统属性值，这些属性值可以通过getprop获取，setprop设置
     *
     * @param property 属性名
     * @param value    属性值
     * @return 是否成功设置属性值
     */
    public static boolean setProp(String property, String value) {
        return runRootCommand("setprop " + property + " " + value);
    }

    /**
     * 启动adb服务
     *
     * @return 是否成功启动adb服务
     */
    public static boolean adbStart() {
        try {
            setProp("service.adb.tcp.port", PORT);
            if (isProcessRunning("adbd")) {
                runRootCommand("stop adbd");
            }
            runRootCommand("start adbd");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 停止adb服务
     *
     * @return 是否成功停止adb服务
     */
    public static boolean adbStop() {
        try {
            setProp("service.adb.tcp.port", "-1");
            runRootCommand("stop adbd");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;

    }

    /**
     * 检测adb服务是否正在运行
     *
     * @return 是否正在运行adb服务
     */
    public static boolean adbIfRunning() {
        try {
            return isProcessRunning("adbd");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
