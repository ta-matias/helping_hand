package pt.unl.fct.di.apdc.helpinghand.data.model;

public class UpdateInstModel {


    public String name;
    public String initials;
    public String phone;
    public String address;
    public String addressComp;
    public String location;


    public UpdateInstModel() {}

    public UpdateInstModel(String name, String initials, String address, String addressComp, String location, String phone) {
        this.name = name;
        this.initials = initials;
        this.address = address;
        this.addressComp = addressComp;
        this.location = location;
        this.phone = phone;
    }
}
