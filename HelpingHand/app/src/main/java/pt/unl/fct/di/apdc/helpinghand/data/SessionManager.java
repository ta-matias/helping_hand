package pt.unl.fct.di.apdc.helpinghand.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class SessionManager {

    //Shared preferences
    SharedPreferences pref;

    //Editor
    Editor editor;

    //Context
    Context context;

    //Shared preferences mode
    int PRIVATE_MODE = 0;

    //SharedPref file name
    private static final String FILE_NAME = "SharedPreferences";

    // All Shared Preferences Keys
    private static final String IS_LOGIN = "IsLoggedIn";

    // User name (make variable public to access from outside)
    public static final String KEY_NAME = "name";

    // Email address (make variable public to access from outside)
    public static final String KEY_EMAIL = "email";

    //Constructor
    public SessionManager(Context context){
        this.context = context;
        pref = context.getSharedPreferences(FILE_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }
}
