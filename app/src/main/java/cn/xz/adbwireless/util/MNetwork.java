package cn.xz.adbwireless.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class MNetwork {
    private MNetwork() {
    }

    /**
     * 检测网络状态是否能联网
     *
     * @param context context
     */
    public static boolean isNetAvailable(Context context) {
        ConnectivityManager manager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        return (info != null && info.isAvailable());
    }

    public static boolean isWifi(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkINfo = cm.getActiveNetworkInfo();
        return networkINfo != null
                && networkINfo.getType() == ConnectivityManager.TYPE_WIFI;
    }

    public static String getWiFiSSID(Context context) {
        WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        int wifiState = wifiMgr.getWifiState();
        WifiInfo info = wifiMgr.getConnectionInfo();
        String wifiId = info != null ? info.getSSID() : null;
        return wifiId;
    }

    public static String getWiFiIp(Context context) {
        WifiManager wifimanage = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);//获取WifiManager
        //检查wifi是否开启
        if (!wifimanage.isWifiEnabled()) {
            //wifimanage.setWifiEnabled(true);
            MLog.log("wifi尚未打开！");
            return null;
        }

        WifiInfo wifiinfo = wifimanage.getConnectionInfo();
        return intToIp(wifiinfo.getIpAddress());
    }

    public static String getWifiMask(Context context){
        WifiManager wifimanage = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);//获取WifiManager
        //检查wifi是否开启
        if (!wifimanage.isWifiEnabled()) {
            //wifimanage.setWifiEnabled(true);
            MLog.log("wifi尚未打开！");
            return null;
        }

        DhcpInfo dhcpInfo = wifimanage.getDhcpInfo();
        return intToIp(dhcpInfo.netmask);

    }

    public static String getWifiGateway(Context context){
        WifiManager wifimanage = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);//获取WifiManager
        //检查wifi是否开启
        if (!wifimanage.isWifiEnabled()) {
            //wifimanage.setWifiEnabled(true);
            MLog.log("wifi尚未打开！");
            return null;
        }

        DhcpInfo dhcpInfo = wifimanage.getDhcpInfo();
        return intToIp(dhcpInfo.gateway);

    }

    private static String intToIp(int ipAddress) {
        return ((ipAddress & 0xff) + "." + (ipAddress >> 8 & 0xff) + "."
                + (ipAddress >> 16 & 0xff) + "." + (ipAddress >> 24 & 0xff));
    }
}
