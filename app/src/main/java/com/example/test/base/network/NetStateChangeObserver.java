package com.example.test.base.network;

import android.content.Context;

public interface NetStateChangeObserver {

    public void onNetDisconnected();
    public void onNetConnected(NetworkType networkType, Context context);
}
