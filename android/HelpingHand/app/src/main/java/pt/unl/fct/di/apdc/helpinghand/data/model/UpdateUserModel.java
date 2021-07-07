package pt.unl.fct.di.apdc.helpinghand.data.model;

public class UpdateUserModel {

    /**
     * Model to update the user info
     */
    public String phone;
    public String address1;
    public String address2;
    public String city;
    public String zipcode;

    public UpdateUserModel(){}

    public UpdateUserModel(String phone, String address1, String address2, String city, String zip){
        this.phone = phone;
        this.address1 = address1;
        this.address2 = address2;
        this.city = city;
        this.zipcode = zip;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress1() {
        return address1;
    }

    public String getAddress2() {
        return address2;
    }

    public String getCity() {
        return city;
    }

    public String getZip() {
        return zipcode;
    }
}
