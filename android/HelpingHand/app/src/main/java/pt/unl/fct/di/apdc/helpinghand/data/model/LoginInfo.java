package pt.unl.fct.di.apdc.helpinghand.data.model;

public class LoginInfo {

    public String name;
    public String role;
    public String token;
    public String expires;

    public LoginInfo(String name, String role, String token,String expires) {
        this.name = name;
        this.role = role;
        this.token = token;
        this.expires = expires;
    }
}
