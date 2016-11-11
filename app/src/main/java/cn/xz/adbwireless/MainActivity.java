package cn.xz.adbwireless;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.xz.adbwireless.util.AdbUtil;
import cn.xz.adbwireless.util.MLog;
import cn.xz.adbwireless.util.MToast;

public class MainActivity extends Activity {

    private long mExitTime = 0;
    static final double NUM = 0.6;

    private static final int MSG_DIALOG_SHOW = 1;
    private static final int MSG_DIALOG_HIDE = 0;
    private static final int MSG_ADB_START = 2;
    private static final int MSG_ADB_STOP = 3;

    AlertDialog alertDialog = null;

    @BindView(R.id.iv_wifi)
    ImageView ivWifi;
    @BindView(R.id.tv_switch)
    TextView tvSwitch;
    @BindView(R.id.tv_state)
    TextView tvState;
    @BindView(R.id.iv_setting)
    ImageView ivSetting;
    @BindView(R.id.iv_about)
    ImageView ivAbout;

    private BroadcastReceiver myNetReceiver;

    Handler handler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_DIALOG_SHOW: {
                    MLog.log("MSG_DIALOG_SHOW");
                    //show dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setCancelable(false)
                            .setMessage("正在启动adb...");
                    alertDialog = builder.create();
                    alertDialog.show();
                    break;
                }
                case MSG_DIALOG_HIDE: {
                    MLog.log("MSG_DIALOG_HIDE");
                    //hide dialog
                    if (null != alertDialog) {
                        alertDialog.dismiss();
                    }
                    break;
                }
                case MSG_ADB_START: {
                    startAdb();
                    break;
                }
                case MSG_ADB_STOP: {
                    stopAdb();
                    break;
                }
                default: {
                    //
                }
            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        regReceiver();
        setScreenOnOff(true);
        checkRoot();


        if (!judgeNetwork()) {
            tvState.setText(getString(R.string.net_error));
            tvSwitch.setEnabled(false);
            ivWifi.setImageResource(R.drawable.wifi_off);
        } else {
            tvSwitch.setEnabled(true);
            ivWifi.setImageResource(R.drawable.wifi_on);
        }
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        ivWifi.getLayoutParams().width = (int) (dm.widthPixels * NUM);
        ivWifi.getLayoutParams().height = (int) (dm.widthPixels * NUM);

        if (AdbUtil.adbIfRunning()) {
            tvSwitch.setText(getString(R.string.adb_stop));
            String str = //getString(R.string.adb_started) +
//                    System.getProperty("line.separator", "\\n") +
                    "use adb connect " + getLocalIpAddress();
            tvState.setText(str);
            ivWifi.setImageResource(R.drawable.wifi_on);
        } else {
            tvSwitch.setText(getString(R.string.adb_start));
            tvState.setText(getString(R.string.adb_stopped));
            ivWifi.setImageResource(R.drawable.wifi_off);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unRegReceiver();
    }

    //连续按两次退出系统
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if ((System.currentTimeMillis() - mExitTime) > 2000) {
                MToast.Show(this, "再按一次退出程序！");
                mExitTime = System.currentTimeMillis();
            } else {
                stopAdb();
                finish();
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @OnClick(R.id.tv_switch)
    void setSwitch() {
        if (!checkRoot()) {
            MToast.Show(this, getString(R.string.acquire_root_failed));
            return;
        }
        if (!judgeNetwork()) {
            MToast.Show(this, getString(R.string.wifi_off));
            return;
        }

        handler.sendEmptyMessage(MSG_DIALOG_SHOW);
        SystemClock.sleep(1000);

        if (getString(R.string.adb_start).equals(tvSwitch.getText().toString())) {
            MLog.log("start it");
            //startAdb();
            handler.sendEmptyMessage(MSG_ADB_START);
        } else if (getString(R.string.adb_stop).equals(tvSwitch.getText().toString())) {
            MLog.log("stop it");
            //stopAdb();
            handler.sendEmptyMessage(MSG_ADB_STOP);
        }
    }

    @OnClick(R.id.iv_setting)
    void setSetting() {
        //
    }

    /**
     * 检测有无root权限
     *
     * @return 有true 无false
     */
    private boolean checkRoot() {
        if (!AdbUtil.hasRootPermission()) {
            return false;
        } else {
            return true;
        }
    }

    private void startAdb() {
        if (AdbUtil.adbStart()) {
            MToast.Show(this, getString(R.string.adb_started));
            String str = getString(R.string.adb_started) +
//                    System.getProperty("line.separator", "\\n") +
                    " adb connect " + getLocalIpAddress();
            tvState.setText(str);
            tvSwitch.setText(getString(R.string.adb_stop));
            ivWifi.setImageResource(R.drawable.wifi_on);
        } else {
            MToast.Show(this, getString(R.string.adb_start_fail));
        }

        Message msg2 = Message.obtain();
        msg2.what = MSG_DIALOG_HIDE;
        handler.sendMessage(msg2);
    }

    private void stopAdb() {
        if (AdbUtil.adbStop()) {
            MToast.Show(this, getString(R.string.adb_stopped));
            tvState.setText(getString(R.string.adb_stopped));
            tvSwitch.setText(getString(R.string.adb_start));
            ivWifi.setImageResource(R.drawable.wifi_off);
        } else {
            MToast.Show(this, getString(R.string.adb_stop_fail));
        }

        Message msg2 = Message.obtain();
        msg2.what = MSG_DIALOG_HIDE;
        handler.sendMessage(msg2);
    }

    /**
     * 设置打开应用时屏幕常量（退出后不起作用）
     *
     * @param ifOn true保持亮 false不保持亮
     */
    private void setScreenOnOff(boolean ifOn) {
        if (ifOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                ConnectivityManager mConnectivityManager =
                        (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo netInfo = mConnectivityManager.getActiveNetworkInfo();
                if (netInfo != null && netInfo.isAvailable()) {//网络连接
                    String name = netInfo.getTypeName();
                    MLog.log(name);
                    if (netInfo.getType() == ConnectivityManager.TYPE_WIFI) {//WiFi网络
                        MLog.log("当前连接WiFi网络");
                    } else if (netInfo.getType() == ConnectivityManager.TYPE_ETHERNET) {//有线网络
                        MLog.log("当前连接有线网络");
                    } else if (netInfo.getType() == ConnectivityManager.TYPE_MOBILE) {//3g网络
                        MLog.log("当前连接移动网络");
                    }
                } else {//网络断开
                    MLog.log("当前网络已断开");
                    MToast.Show(MainActivity.this, "网络已断开！请检查网络连接！");
                }
            }
            if (!judgeNetwork()) {
                tvState.setText(getString(R.string.net_error));
                tvSwitch.setEnabled(false);
            } else {
                tvSwitch.setEnabled(true);
            }
        }
    }

    /**
     * 注册广播
     */
    private void regReceiver() {
        if (null == myNetReceiver) {
            myNetReceiver = new MyBroadcastReceiver();
        }
        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(myNetReceiver, mFilter);
    }

    /**
     * 解除注册广播
     */
    private void unRegReceiver() {
        if (myNetReceiver != null) {
            unregisterReceiver(myNetReceiver);
        }
    }

    /**
     * 检测网络是否连接
     *
     * @return 是true否 false
     */
    private boolean checkNetworkState() {
        boolean flag = false;
        //得到网络连接信息
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        //去进行判断网络是否连接
        if (manager.getActiveNetworkInfo() != null) {
            flag = manager.getActiveNetworkInfo().isAvailable();
        }
        return flag;
    }

    /**
     * 获取当前网络类型，wifi、mobile、null
     *
     * @return
     */
    private String checkNetworkType() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) { // connected to the internet
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                return "wifi";
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                return "mobile";
            } else {
                return "other" + activeNetwork.getType();
            }
        }
        return null;
    }

    /**
     * 获取当前网络类型是否为wifi或者Ethernet
     *
     * @return 是true 否false
     */
    private boolean judgeNetwork() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) {// connected to the internet
            MLog.log("当前网络环境：" + activeNetwork.getType());
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI
                    || activeNetwork.getType() == ConnectivityManager.TYPE_ETHERNET) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return 获取ip地址
     */
    public String getIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()
                            && inetAddress instanceof Inet4Address) {

                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @return 获取局域网的ip地址形式（32位整型IP地址转成本地ip）
     */
    private String getLocalIpAddress() {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        // 获取32位整型IP地址
        int ipAddress = wifiInfo.getIpAddress();
        // 返回整型地址转换成“*.*.*.*”地址
        return String.format(Locale.ENGLISH, "%d.%d.%d.%d", (ipAddress & 0xff),
                (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff),
                (ipAddress >> 24 & 0xff));
    }


}
