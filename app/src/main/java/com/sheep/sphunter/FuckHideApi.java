package com.sheep.sphunter;

import me.weishu.reflection.Reflection;
import android.app.Application;
import android.content.Context;


public class FuckHideApi extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Reflection.unseal(base);
    }
}
