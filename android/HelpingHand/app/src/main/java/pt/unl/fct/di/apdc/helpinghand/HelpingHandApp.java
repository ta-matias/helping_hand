package pt.unl.fct.di.apdc.helpinghand;

import android.app.Application;
import android.content.Context;

public class HelpingHandApp  extends Application {

    public static volatile Context applicationContext;

    public void onCreate(){
        super.onCreate();
        applicationContext = getApplicationContext();
    }
}
