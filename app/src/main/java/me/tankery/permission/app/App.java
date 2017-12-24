package me.tankery.permission.app;

import android.app.Application;

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

        Timber.uprootAll();
        Timber.plant(new DebugTree());
        Timber.i("App created");
    }
}
