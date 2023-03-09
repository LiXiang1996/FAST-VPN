package com.example.test.base.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.test.App;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class NetworkUtil {

    private final static String DEFAULT_IP_ADDRESS = "0.0.0.0";

    private final Set<onNetworkStatusChangedListener> mListenerSet = new HashSet<>();

    private boolean isNetworkAvailable = false;

    private String mIpAddress = DEFAULT_IP_ADDRESS;

    private final ConnectivityManager.NetworkCallback mNetworkCallback =
            new ConnectivityManager.NetworkCallback() {

                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    Log.d("TAG", "onAvailable: 网络已连接");

                    isNetworkAvailable = true;
                    refreshIpAddress();
                    for (onNetworkStatusChangedListener onNetworkStatusChangedListener : mListenerSet) {
                        onNetworkStatusChangedListener.networkStatusChanged(true);
                    }
                }

                @Override
                public void onLost(@NonNull Network network) {
                    super.onLost(network);

                    if (isNetworkConnected()) {
                        Log.d("TAG", "===>onLost: 回调是网络断开实际上只是使用wifi状态断开手机网络 ");
                        refreshIpAddress();
                    } else {
                        Log.d("TAG", "===>onLost: 网络已断开");
                        isNetworkAvailable = false;
                        mIpAddress = DEFAULT_IP_ADDRESS;
                        for (onNetworkStatusChangedListener onNetworkStatusChangedListener : mListenerSet) {
                            onNetworkStatusChangedListener.networkStatusChanged(false);
                        }
                    }
                }
            };

    private NetworkUtil() {

    }

    private static class InstanceHolder {
        private static final NetworkUtil instance = new NetworkUtil();
    }

    public interface onNetworkStatusChangedListener {
        void networkStatusChanged(boolean available);
    }

    public static NetworkUtil get() {
        return InstanceHolder.instance;
    }

    public void addNetworkStatusChangedListener(@NonNull onNetworkStatusChangedListener callback) {
        mListenerSet.add(callback);
        callback.networkStatusChanged(isNetworkAvailable);
    }

    public void removeNetworkStatusChangedListener(@NonNull onNetworkStatusChangedListener callback) {
        mListenerSet.remove(callback);
    }

    public void registerNetworkCallback() {
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        NetworkRequest request = builder.build();
        ConnectivityManager connMgr = (ConnectivityManager) App.Companion.getContext()
                .getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMgr != null) {
            connMgr.registerNetworkCallback(request, mNetworkCallback);
        }
        refreshIpAddress();
    }

    public void unRegisterNetworkCallback() {
        ConnectivityManager connMgr = (ConnectivityManager) App.Companion.getContext()
                .getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMgr != null) {
            connMgr.unregisterNetworkCallback(mNetworkCallback);
        }
        mListenerSet.clear();
    }

    /**
     * @return IPV4地址
     */
    public String getLocalIpAddress() {
        return mIpAddress;
    }

    /**
     * 刷新网络的ip地址
     */
    private void refreshIpAddress() {
        NetworkInfo info = ((ConnectivityManager) Objects.requireNonNull(App.Companion.getContext())
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
            // 3/4g网络
            if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
                try {
                    for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                        NetworkInterface intf = en.nextElement();
                        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                            InetAddress inetAddress = enumIpAddr.nextElement();
                            if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                                mIpAddress = inetAddress.getHostAddress();
                            }
                        }
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                }

            } else if (info.getType() == ConnectivityManager.TYPE_WIFI) {
                //  wifi网络
                WifiManager wifiManager = (WifiManager) App.Companion.getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                mIpAddress = intIP2StringIP(wifiInfo.getIpAddress());
            } else if (info.getType() == ConnectivityManager.TYPE_ETHERNET) {
                // 有限网络
                mIpAddress = getLocalIp();
            }
        }
        Log.d("TAG", "real ip is " + mIpAddress);
    }

    private String intIP2StringIP(int ip) {
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                (ip >> 24 & 0xFF);
    }

    // 获取有限网IP
    private String getLocalIp() {
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
        } catch (SocketException ex) {

        }
        return DEFAULT_IP_ADDRESS;
    }

    /**
     * 判断是否联网
     */
    public boolean isNetworkAvailable() {
        return isNetworkAvailable;
    }

    private boolean isNetworkConnected() {
        ConnectivityManager connMgr = (ConnectivityManager) Objects.requireNonNull(App.Companion.getContext()).getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connMgr.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isAvailable();
    }


    /**
     * 判断当前网络是否可用(6.0以上版本)
     * 实时
     * @param context
     * @return
     */
    public static boolean isNetSystemUsable(Context context) {
        boolean isNetUsable = false;
        ConnectivityManager manager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            NetworkCapabilities networkCapabilities =
                    manager.getNetworkCapabilities(manager.getActiveNetwork());
            if (networkCapabilities != null) {
                isNetUsable = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            }
        }
        return isNetUsable;
    }
}