package org.nuxeo.ecm.platform.oauth2.openid.auth.facebook;

import java.util.Date;

import org.nuxeo.ecm.platform.oauth2.openid.auth.DefaultOpenIDUserInfo;
import com.google.api.client.util.Key;

public class FacebookUserInfo extends DefaultOpenIDUserInfo {

    @Key("id")
    public String id;

    @Key("first_name")
    public String firstName;

    @Key("link")
    public String link;

    @Key("birthday")
    public Date birthday;

    @Key("verified")
    public boolean verified;

    @Override
    public String getSubject() {
        return id;
    }

    @Override
    public String getGivenName() {
        return firstName;
    }

    @Override
    public String getProfile() {
        return link;
    }

    @Override
    public Date getBirthdate() {
        return birthday;
    }

    @Override
    public boolean isEmailVerified() {
        return verified;
    }


}
