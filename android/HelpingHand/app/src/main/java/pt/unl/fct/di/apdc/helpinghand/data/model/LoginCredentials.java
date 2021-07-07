package pt.unl.fct.di.apdc.helpinghand.data.model;

import java.util.regex.Pattern;

public class LoginCredentials {

    String id;
    String password;

    private static final Pattern PASSWORD_REGEX = Pattern.compile("^"
            + "(?=.*[0-9])"
            + "(?=.*[a-z])"
            + "(?=.*[A-Z])"
            + "(?=.*[!@#&()–{}:;',?/*~$^+=<>])"
            + ".{8,20}"
            + "$");

    //Empty constructor
    public LoginCredentials(){};

    //Constructor with credentials
    public LoginCredentials(String clientId, String password) {
        this.id = clientId;
        this.password = password;
    }

    /*GETTERS*/
    public String getClientId() {
        return id;
    }

    public String getPassword() {
        return password;
    }

    /*SETTERS*/

    public void setClientId(String clientId){
        this.id = clientId;
    }

    public void setPassword(String password){
        this.password = password;
    }

    /*Validating data*/

    public boolean validate(){
        if(id == null)
            return false;
        else if(!PASSWORD_REGEX.matcher(password).matches())
            return false;
        return true;
    }

}
