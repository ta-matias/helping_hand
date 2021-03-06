package pt.unl.fct.di.apdc.helpinghand.data.model;

public class AccountInfo {

    public String phone;
    public String address1;
    public String address2;
    public String zipcode;
    public String city;

    public AccountInfo(String phone, String address1, String address2, String zipcode, String city) {
        this.phone = phone;
        this.address1 = address1;
        this.address2 = address2;
        this.zipcode = zipcode;
        this.city = city;
    }

    public boolean badData() {
        if(phone == null) return true;
        if(address1 == null) return true;
        if(address2 == null) return true;
        if(zipcode == null) return true;
        if(city == null) return true;
        return false;
    }
}
