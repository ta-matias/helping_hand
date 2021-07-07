package pt.unl.fct.di.apdc.helpinghand.data.model;

public class UserRegisterModel {

    private String name;
    //User's username
    public String id;

    //User's email
    public String email;

    //User's password
    public String password;

    //User's confirmation password
    public String confirmation;

    //Constructor
    public UserRegisterModel(String username, String email, String password, String confirmPassword) {
        this.id = username;
        this.password = password;
        this.email = email;
        this.confirmation = confirmPassword;
    }
}
