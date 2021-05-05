package pt.unl.fct.di.apdc.helpinghand.data;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import java.util.HashMap;

import pt.unl.fct.di.apdc.helpinghand.ui.login.LoginActivity;

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

    //Constructor
    public SessionManager(Context context){
        this.context = context;
        pref = context.getSharedPreferences(FILE_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    /**
     * Creates a login session
     */
    public void createLoginSession(String name) {
        editor.putBoolean(IS_LOGIN, true);

        editor.putString(KEY_NAME, name);

        editor.commit();
    }

    /**
     * Check user status, if not logged in will redirect to login activity
     * otherwise won't do anything
     */
    public void checkLogin(){
        //Check Login status
        if(!this.isLoggedIn()){
            //user not logged in redirect to Login activity
            Intent i = new Intent(this.context, LoginActivity.class);

            //Close all activities
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            //Add new flag to start new activity
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            //Start activity
            this.context.startActivity(i);
        }
    }

    /**
     * Get user session details
     */
    public HashMap<String, String> getUserDetails(){
        HashMap<String, String> user = new HashMap<String, String>();

        //user name
        user.put(KEY_NAME, pref.getString(KEY_NAME, null));

        //return user
        return user;
    }

    /**
     * Remove session details
     */
    public void logout(){
        //Clear data
        editor.clear();
        editor.commit();

        //After logout redirect user to login activity
        Intent i = new Intent(this.context, LoginActivity.class);

        //Close all activities
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        //Add new flag to start new activity
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        //Start activity
        this.context.startActivity(i);
    }

    /**
     * Login check
     * @return login state
     */
    public boolean isLoggedIn() {
        return pref.getBoolean(IS_LOGIN, false);
    }
}
