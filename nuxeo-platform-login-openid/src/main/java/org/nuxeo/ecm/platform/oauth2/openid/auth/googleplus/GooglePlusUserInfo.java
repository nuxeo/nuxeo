package org.nuxeo.ecm.platform.oauth2.openid.auth.googleplus;

import java.util.Date;
import java.util.List;

import org.nuxeo.ecm.platform.oauth2.openid.auth.OpenIDUserInfo;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.ArrayMap;
import com.google.api.client.util.Key;

/**
 * GooglePlus user info
 *
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.9.5
 */
public class GooglePlusUserInfo extends GenericJson implements OpenIDUserInfo {
    @Key("id")
    protected String id;

    @Key("displayName")
    protected String name;

    @Key("verified")
    protected boolean verified;

    @Key("gender")
    protected String gender;

    @Override
    public String getSubject() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getGivenName() {
        ArrayMap<String, Object> nameObject = getNameObject();
        return nameObject == null ? ""
                : String.valueOf(nameObject.get("givenName"));
    }

    @Override
    public String getFamilyName() {
        ArrayMap<String, Object> nameObject = getNameObject();
        return nameObject == null ? ""
                : String.valueOf(nameObject.get("familyName"));
    }

    @Override
    public String getMiddleName() {
        return null;
    }

    @Override
    public String getNickname() {
        return null;
    }

    @Override
    public String getPreferredUsername() {
        return getGivenName();
    }

    @Override
    public String getProfile() {
        return null;
    }

    @Override
    public String getPicture() {
        return null;
    }

    @Override
    public String getWebsite() {
        return null;
    }

    @Override
    public String getEmail() {
        return String.valueOf(getEmailsObject().get(0).get("value"));
    }

    @Override
    public boolean isEmailVerified() {
        return verified;
    }

    @Override
    public String getGender() {
        return gender;
    }

    @Override
    public Date getBirthdate() {
        return null;
    }

    @Override
    public String getZoneInfo() {
        return null;
    }

    @Override
    public String getLocale() {
        return null;
    }

    @Override
    public String getPhoneNumber() {
        return null;
    }

    @Override
    public String getAddress() {
        return null;
    }

    @Override
    public Date getUpdatedTime() {
        return null;
    }

    protected ArrayMap<String, Object> getNameObject() {
        return (ArrayMap<String, Object>) get("name");
    }

    protected List<ArrayMap<String, Object>> getEmailsObject() {
        return (List<ArrayMap<String, Object>>) get("emails");
    }
}
