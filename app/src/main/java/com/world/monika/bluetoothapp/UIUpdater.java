package com.world.monika.bluetoothapp;

import android.os.Handler;
import android.os.Looper;

/**
 * Created by pietyszukm on 13.07.2016.
 */
public class UIUpdater {
    private Handler mHandler = new Handler(Looper.getMainLooper());

    private Runnable mStatusChecker;
    private int UPDATE_INTERVAL = 3100;

    public UIUpdater(final Runnable uiUpdater) {
        mStatusChecker = new Runnable() {
            @Override
            public void run() {
                // Run the passed runnable
                uiUpdater.run();
                // Re-run it after the update interval
                mHandler.postDelayed(this, UPDATE_INTERVAL);
            }
        };
    }

    public UIUpdater(Runnable uiUpdater, int interval){
        this(uiUpdater);
        UPDATE_INTERVAL = interval;
    }

    public synchronized void startUpdates(){
        mStatusChecker.run();
    }

    public synchronized void stopUpdates(){
        mHandler.removeCallbacks(mStatusChecker);
    }
}
