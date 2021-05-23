package pt.unl.fct.di.apdc.helpinghand.data.model;

public class InstitutionRegisterModel {

    /**Variables**/

    //Name of the institution
    public String name;

    //Username of the institution
    public String instId;

    //Initials of the institution
    public String initials;

    //Email of the institution
    public String email;

    //Password of the institution
    public String password;

    //Confirm password of the institution
    public String confPassword;

    /**Constructor**/

    public InstitutionRegisterModel(String name, String username, String initials, String email, String password, String confirmPassword){
        this.name = name;
        this.initials = initials;
        this.instId = username;
        this.email = email;
        this.password = password;
        this.confPassword = confirmPassword;
    }

}
