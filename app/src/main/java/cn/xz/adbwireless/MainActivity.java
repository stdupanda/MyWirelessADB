package cn.xz.adbwireless;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.xz.adbwireless.util.AdbUtil;
import cn.xz.adbwireless.util.IConst;
import cn.xz.adbwireless.util.MLog;
import cn.xz.adbwireless.util.MNetwork;
import cn.xz.adbwireless.util.MToast;

public class MainActivity extends Activity {

    private long mExitTime = 0;
    static final double NUM = 0.6;

    static final String msgStart = "正在启动adb...";
    static final String msgStop = "正在停止adb...";

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

    private ExecutorService executorService = Executors.newCachedThreadPool();

    SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sp = getSharedPreferences(IConst.SP_FILE_NAME, MODE_PRIVATE);
        ButterKnife.bind(this);

        regReceiver();
        setScreenOnOff(sp.getBoolean(IConst.IF_SCREEN_ON, true));
        checkRoot();

        if (!MNetwork.isWifi(this)) {
            tvState.setText(getString(R.string.net_error));
            tvState.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent("android.settings.WIFI_SETTINGS"));
                }
            });
            tvSwitch.setEnabled(false);
            ivWifi.setImageResource(R.drawable.wifi_off);
        } else {
            tvState.setOnClickListener(null);
            tvSwitch.setEnabled(true);
            ivWifi.setImageResource(R.drawable.wifi_on);
        }
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        ivWifi.getLayoutParams().width = (int) (dm.widthPixels * NUM);
        ivWifi.getLayoutParams().height = (int) (dm.widthPixels * NUM);

        if (AdbUtil.adbIfRunning()) {
            tvSwitch.setText(getString(R.string.adb_stop));
            String str = getString(R.string.adb_started) +
//                    System.getProperty("line.separator", "\\n") +
                    "use adb connect " + MNetwork.getWiFiIp(MainActivity.this);
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
        if (!MNetwork.isWifi(MainActivity.this)) {
            MToast.Show(this, getString(R.string.wifi_off));
            return;
        }

        if (getString(R.string.adb_start).equals(tvSwitch.getText().toString())) {
            MLog.log("start it");
            new startAdbTask().executeOnExecutor(executorService, "null_param");
        } else if (getString(R.string.adb_stop).equals(tvSwitch.getText().toString())) {
            MLog.log("stop it");
            new stopAdbTask().executeOnExecutor(executorService, "null_param");
        }
    }

    @OnClick(R.id.iv_setting)
    void setSetting() {
        final View view = View.inflate(this, R.layout.view_config, null);
        TextView btnYes = (TextView) view.findViewById(R.id.btn_yes);
        TextView btnNo = (TextView) view.findViewById(R.id.btn_no);
        final CheckBox cbIfScreenOn = (CheckBox) view.findViewById(R.id.cb_if_screen_on);

        cbIfScreenOn.setChecked(sp.getBoolean(IConst.IF_SCREEN_ON, false));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view, 10, 0, 10, 0)
                .setTitle("App设置")
                .setCancelable(false);
        final AlertDialog configDialog = builder.create();

        btnYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sp.edit()
                        .putBoolean(IConst.IF_SCREEN_ON, cbIfScreenOn.isChecked())
                        .apply();
                MToast.Show(MainActivity.this, "app下次启动时生效");
                configDialog.dismiss();
            }
        });

        btnNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                configDialog.dismiss();
            }
        });

        configDialog.show();
    }

    @OnClick(R.id.iv_about)
    void setAbout() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(R.layout.view_about)
                .setTitle(getString(R.string.about_title))
                .setCancelable(true)
                .setPositiveButton(R.string.ok, null);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        WindowManager.LayoutParams layoutParams = alertDialog.getWindow().getAttributes();
        if (layoutParams.height > (int) (metrics.heightPixels * 0.6)) {//窗口太大了，减小高度
            layoutParams.height = (int) (metrics.heightPixels * 0.6);
        }
        layoutParams.width = (int) (metrics.widthPixels * 0.9);

        alertDialog.getWindow().setAttributes(layoutParams);
    }

    /**
     * 检测有无root权限
     *
     * @return 有true 无false
     */
    private boolean checkRoot() {
        return AdbUtil.hasRootPermission();
    }

    private String startAdb() {
        if (AdbUtil.adbStart()) {
            return "ok";
        } else {
            return "no";
        }
    }

    private String stopAdb() {
        if (AdbUtil.adbStop()) {
            return "ok";
        } else {
            return "no";
        }
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
            if (!MNetwork.isWifi(MainActivity.this)) {
                tvState.setText(getString(R.string.net_error));
                tvState.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
                tvState.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent("android.settings.WIFI_SETTINGS"));
                    }
                });
                tvSwitch.setEnabled(false);
            } else {
                if (getString(R.string.net_error).equals(tvState.getText())) {
                    tvState.setText(getString(R.string.net_ok, ""));
                    tvState.getPaint().setFlags(0);
                }
                tvState.setOnClickListener(null);
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

    private class startAdbTask extends AsyncTask<String, Integer, String> {//params,progress,result

        AlertDialog alertDialog = null;
        AlertDialog.Builder builder = null;

        @Override
        protected void onPreExecute() {
            builder = new AlertDialog.Builder(MainActivity.this);
            builder.setCancelable(false)
                    .setMessage(msgStart);
            alertDialog = builder.create();
            alertDialog.show();
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            return startAdb();
        }

        @Override
        protected void onPostExecute(String s) {
            if (null != alertDialog) {
                alertDialog.dismiss();
            }
            if ("ok".equals(s)) {
                //MToast.Show(MainActivity.this, getString(R.string.adb_started));
                String str = getString(R.string.adb_started) +
//                    System.getProperty("line.separator", "\\n") +
                        " adb connect " + "" + MNetwork.getWiFiIp(MainActivity.this);
                tvState.setText("" + str);
                tvState.refreshDrawableState();
                tvSwitch.setText(getString(R.string.adb_stop));
                ivWifi.setImageResource(R.drawable.wifi_on);
            } else {
                MToast.Show(MainActivity.this, getString(R.string.adb_start_fail));
            }
            super.onPostExecute(s);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            MLog.log(values[0]);
            super.onProgressUpdate(values);
        }
    }

    private class stopAdbTask extends AsyncTask<String, Integer, String> {//params,progress,result

        AlertDialog alertDialog = null;
        AlertDialog.Builder builder = null;

        @Override
        protected void onPreExecute() {
            builder = new AlertDialog.Builder(MainActivity.this);
            builder.setCancelable(false)
                    .setMessage(msgStop);
            alertDialog = builder.create();
            alertDialog.show();
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            return stopAdb();
        }

        @Override
        protected void onPostExecute(String s) {
            if (null != alertDialog) {
                alertDialog.dismiss();
            }
            if ("ok".equals(s)) {
                //MToast.Show(MainActivity.this, getString(R.string.adb_stopped));
                tvState.setText(getString(R.string.adb_stopped));
                tvSwitch.setText(getString(R.string.adb_start));
                ivWifi.setImageResource(R.drawable.wifi_off);
            } else {
                MToast.Show(MainActivity.this, getString(R.string.adb_stop_fail));
            }
            super.onPostExecute(s);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            MLog.log(values[0]);
            super.onProgressUpdate(values);
        }
    }
}
