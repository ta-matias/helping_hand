package pt.unl.fct.di.apdc.helpinghand.data.model;

public class TokenModel {

    String tokenId;
    String expires;
    String refresh_token;

    public TokenModel(String tokenId, String creationDate, String refresh_token) {
        this.tokenId = tokenId;
        this.expires = creationDate;
        this.refresh_token = refresh_token;
    }

    public String getTokenId(){
        return tokenId;
    }

    public String getExpires() {
        return expires;
    }

    public String getRefresh_token() {
        return refresh_token;
    }
}
