package pt.unl.fct.di.apdc.helpinghand.data.model;

public class UserAuthenticated {

    //User's username
    String username;
    //User's token
    String tokenID;
    //User's role
    String role;
    //Token's creation time
    long creationData;
    //Token's Expiration time
    long expirationData;

    public UserAuthenticated (String username, String tokenID, String role, long creationData, long expirationData){
        this.username = username;
        this.tokenID = tokenID;
        this.role = role;
        this.creationData = creationData;
        this.expirationData = expirationData;
    }

    public String getUsername() {
        return username;
    }

    public String getTokenID() {
        return tokenID;
    }

    public String getRole() {
        return role;
    }

    public long getCreationData() {
        return creationData;
    }

    public long getExpirationData() {
        return expirationData;
    }
}
