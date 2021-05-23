package pt.unl.fct.di.apdc.helpinghand.data.model;

public class UserCredentials {

    String username;
    String password;

    public UserCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

}
