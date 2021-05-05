package pt.unl.fct.di.apdc.helpinghand;

import android.app.Application;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginApp extends Application {

    private ExecutorService executorService = Executors.newFixedThreadPool(4);

    public LoginApp(){}

    public ExecutorService getExecutorService(){
        return this.executorService;
    }
}
