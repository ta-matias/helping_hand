package pt.unl.fct.di.apdc.helpinghand.data.model;

public class UserCredentials {

    //User's username
    String username;
    //User's password (encrypted)
    String password;

    public UserCredentials(String username, String password){
        this.username = username;
        this.password = password;
    }

    public String getUsername(){
        return username;
    }

    public String getPassword(){
        return password;
    }
}
