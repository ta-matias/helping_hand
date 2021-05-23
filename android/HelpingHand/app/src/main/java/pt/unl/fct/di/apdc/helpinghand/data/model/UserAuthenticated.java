package pt.unl.fct.di.apdc.helpinghand.data.model;

import java.util.Date;

public class UserAuthenticated {

    String username;
    TokenModel tokenID;
    String role;

    public void UserAuthenticated(String username, TokenModel tokenID, String role) {
        this.username = username;
        this.tokenID = tokenID;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public TokenModel getTokenID() {
        return tokenID;
    }

    public Date getCreationDate(){
        return tokenID.getCreationDate();
    }

    public String getRole() {
        return role;
    }



}