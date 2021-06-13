package pt.unl.fct.di.apdc.helpinghand.utility;


import android.content.Context;
import android.content.SharedPreferences;

import pt.unl.fct.di.apdc.helpinghand.R;
import pt.unl.fct.di.apdc.helpinghand.data.model.UserAuthenticated;

/**
 * For management of the preferences
 */
public class AppPreferenceTools {

    private SharedPreferences mPreferences;
    private Context mContext;
    private static final String STRING_PREF_UNAVAILABLE = "string preference unavailable";

    public AppPreferenceTools(Context context) {
        this.mContext = context;
        this.mPreferences = this.mContext.getSharedPreferences("app_preference", Context.MODE_PRIVATE);
    }

    /**
     * saves the user authenticated at sign up || sign in
     *
     * @param userAuthenticated
     */
    public void saveAuthenticatedInfo(UserAuthenticated userAuthenticated){
        mPreferences.edit()
                .putString(this.mContext.getString(R.string.user_username), userAuthenticated.getUsername())
                .putString(this.mContext.getString(R.string.token_id), userAuthenticated.getTokenID().getTokenId())
                .putString(this.mContext.getString(R.string.role), userAuthenticated.getRole())
                .putLong(this.mContext.getString(R.string.creation_date), userAuthenticated.getCreationDate())
                .putString(this.mContext.getString(R.string.refresh_token), userAuthenticated.getTokenID().getRefresh_token())
                .commit();
    }

    public void saveUserInfo(UserAuthenticated userAuthenticated){
        mPreferences.edit()
                .putString(this.mContext.getString(R.string.user_username), userAuthenticated.getUsername())
                .putString(this.mContext.getString(R.string.role), userAuthenticated.getRole())
                .apply();
    }

    /**
     * get acess token
     */
    public String getAccesToken(){
        return mPreferences.getString(this.mContext.getString(R.string.token_id), STRING_PREF_UNAVAILABLE);
    }

    public boolean isAuthorized(){
        return !getAccesToken().equals(STRING_PREF_UNAVAILABLE);
    }

    /**
     * TODO: GETTERS and logout
     */

    /**
     * getter for username
     *
     * @return string with username
     */
    public String getUsername(){
        return mPreferences.getString(this.mContext.getString((R.string.user_username)), STRING_PREF_UNAVAILABLE);
    }

    /**
     * getter for role
     *
     * @return string with role
     */
    public String getRole(){
        return mPreferences.getString(this.mContext.getString(R.string.role), STRING_PREF_UNAVAILABLE);
    }

    /**
     * getter for creation date of the acess token
     *
     * @return string with creation date ( TODO: possibly change to Date format )
     */
    public String getCreationDate(){
        return mPreferences.getString(this.mContext.getString(R.string.creation_date), STRING_PREF_UNAVAILABLE);
    }

    public String getRefreshToken(){
        return mPreferences.getString(this.mContext.getString(R.string.refresh_token), STRING_PREF_UNAVAILABLE);
    }

    /**
     * removes prefs in logout
     */
    public void removeAllPrefs(){
        mPreferences.edit().clear().commit();
    }
}
