package com.example.test.base.network;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.util.Log;


import java.util.ArrayList;
import java.util.List;

public class NetStateChangeReceiver extends BroadcastReceiver {

    private static class InstanceHolder {
        private static final NetStateChangeReceiver INSTANCE = new NetStateChangeReceiver();
    }

    private List<NetStateChangeObserver> mObservers = new ArrayList<>();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            NetworkType networkType = NetWorkUtils.getNetworkType(context);
            Log.i("NetStateChangeReceiver", networkType.toString());
            notifyObservers(networkType, context);
        }
    }

    public static void registerReceiver(Context context) {
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(InstanceHolder.INSTANCE, intentFilter);
    }

    public static void unRegisterReceiver(Context context) {
        context.unregisterReceiver(InstanceHolder.INSTANCE);
    }

    public static void registerObserver(NetStateChangeObserver observer) {
        if (observer == null) {
            return;
        }
        if (!InstanceHolder.INSTANCE.mObservers.contains(observer)) {
            InstanceHolder.INSTANCE.mObservers.add(observer);
        }
    }

    public static void unRegisterObserver(NetStateChangeObserver observer) {
        if (observer == null) {
            return;
        }
        if (InstanceHolder.INSTANCE.mObservers == null) {
            return;
        }
        InstanceHolder.INSTANCE.mObservers.remove(observer);
    }

    private void notifyObservers(NetworkType networkType, Context context) {
        if (networkType == NetworkType.NETWORK_NO) {
            for (NetStateChangeObserver observer : mObservers) {
                observer.onNetDisconnected();
            }
        } else {
            for (NetStateChangeObserver observer : mObservers) {
                observer.onNetConnected(networkType, context);
            }
        }
    }
}