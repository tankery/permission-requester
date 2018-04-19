package me.tankery.permission.app;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;

import timber.log.Timber;
import timber.log.Timber.DebugTree;

/**
 * App
 * Created by tankery on 24/12/2017.
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }

        LeakCanary.install(this);

        Timber.uprootAll();
        Timber.plant(new DebugTree());
        Timber.i("App created");
    }
}
