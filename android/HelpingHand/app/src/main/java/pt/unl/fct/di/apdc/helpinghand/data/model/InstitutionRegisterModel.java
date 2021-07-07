package pt.unl.fct.di.apdc.helpinghand.data.model;

public class InstitutionRegisterModel {

    /**Variables**/

    //Username of the institution
    public String id;

    //Name of the institution
    public String name;

    //Initials of the institution
    public String initials;

    //Email of the institution
    public String email;

    //Password of the institution
    public String password;

    //Confirm password of the institution
    public String confirmation;

    /**Constructor**/

    public InstitutionRegisterModel(String name, String username, String initials, String email, String password, String confirmPassword){
        this.id = username;
        this.name = name;
        this.initials = initials;
        this.email = email;
        this.password = password;
        this.confirmation = confirmPassword;
    }

}
