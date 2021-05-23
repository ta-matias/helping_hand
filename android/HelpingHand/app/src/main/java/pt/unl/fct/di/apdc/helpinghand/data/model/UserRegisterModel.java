package pt.unl.fct.di.apdc.helpinghand.data.model;

public class UserRegisterModel {

    private String name;
    //User's username
    public String userId;

    //User's email
    public String email;

    //User's password
    public String password;

    //User's confirmation password
    public String confPassword;

    //Constructor
    public UserRegisterModel(String username, String email, String password, String confirmPassword) {
        this.userId = username;
        this.password = password;
        this.email = email;
        this.confPassword = confirmPassword;
    }
}
