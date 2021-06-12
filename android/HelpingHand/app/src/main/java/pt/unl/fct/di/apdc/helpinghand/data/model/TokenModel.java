package pt.unl.fct.di.apdc.helpinghand.data.model;

import java.util.Date;

public class TokenModel {

    String tokenId;
    long creationDate;
    String refresh_token;

    public TokenModel(String tokenId, long creationDate, String refresh_token) {
        this.tokenId = tokenId;
        this.creationDate = creationDate;
        this.refresh_token = refresh_token;
    }

    public String getTokenId(){
        return tokenId;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public String getRefresh_token() {
        return refresh_token;
    }
}
