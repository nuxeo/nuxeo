package org.nuxeo.ecm.platform.oauth2.openid.auth.linkedin;

import java.util.Date;
import java.util.List;

import org.nuxeo.ecm.platform.oauth2.openid.auth.OpenIDUserInfo;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

public class LinkedInUserInfo extends GenericJson implements OpenIDUserInfo {

    // These fields require the r_basicprofile member permission

    @Key("id")
    public String subject;

    @Key("formattedName")
    public String name;

    @Key("firstName")
    public String givenName;

    @Key("lastName")
    public String familyName;

    @Key("publicProfileUrl")
    public String profile;

    @Key("pictureUrl")
    public String picture;

    // These fields require the r_emailaddress member permission

    @Key("emailAddress")
    public String email;

    // These fields require the r_fullprofile member permission

    @Key("dateOfBirth")
    public Date birthdate;

    // These fields require the r_contactinfo member permission

    @Key("phoneNumbers")
    public List<String> phoneNumbers;

    @Key("mainAddress")
    public String address;

    // These fields are not available

    @Key("middle_name")
    public String middleName;

    @Key("nickname")
    public String nickname;

    @Key("preferred_username")
    public String preferredUsername;

    @Key("website")
    public String website;

    @Key("verified_email")
    public boolean verifiedEmail;

    @Key("gender")
    public String gender;

    @Key("zoneinfo")
    public String zoneInfo;

    @Key("locale")
    public String locale;

    @Key("updated_time")
    public Date updatedTime;

    @Override
    public String getSubject() {
        return subject;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getGivenName() {
        return givenName;
    }

    @Override
    public String getFamilyName() {
        return familyName;
    }

    @Override
    public String getMiddleName() {
        return middleName;
    }

    @Override
    public String getNickname() {
        return nickname;
    }

    @Override
    public String getPreferredUsername() {
        return preferredUsername;
    }

    @Override
    public String getProfile() {
        return profile;
    }

    @Override
    public String getPicture() {
        return picture;
    }

    @Override
    public String getWebsite() {
        return website;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public boolean isEmailVerified() {
        return verifiedEmail;
    }

    @Override
    public String getGender() {
        return gender;
    }

    @Override
    public Date getBirthdate() {
        return birthdate;
    }

    @Override
    public String getZoneInfo() {
        return zoneInfo;
    }

    @Override
    public String getLocale() {
        return locale;
    }

    @Override
    public String getPhoneNumber() {
        if (phoneNumbers == null || phoneNumbers.isEmpty()) {
            return null;
        }
        return phoneNumbers.get(0);
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public Date getUpdatedTime() {
        return updatedTime;
    }
}
