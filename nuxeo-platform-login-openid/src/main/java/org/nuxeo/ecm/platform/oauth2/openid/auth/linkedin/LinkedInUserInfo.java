/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nelson Silva <nelson.silva@inevo.pt> - initial API and implementation
 *     Nuxeo
 */
package org.nuxeo.ecm.platform.oauth2.openid.auth.linkedin;

import java.util.Date;
import java.util.List;

import org.nuxeo.ecm.platform.oauth2.openid.auth.OpenIDUserInfo;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

public class LinkedInUserInfo extends GenericJson implements OpenIDUserInfo {

    // These fields require the r_basicprofile member permission

    @Key("id")
    protected String subject;

    @Key("formattedName")
    protected String name;

    @Key("firstName")
    protected String givenName;

    @Key("lastName")
    protected String familyName;

    @Key("publicProfileUrl")
    protected String profile;

    @Key("pictureUrl")
    protected String picture;

    // These fields require the r_emailaddress member permission

    @Key("emailAddress")
    protected String email;

    // These fields require the r_fullprofile member permission

    @Key("dateOfBirth")
    protected Date birthdate;

    // These fields require the r_contactinfo member permission

    @Key("phoneNumbers")
    protected List<String> phoneNumbers;

    @Key("mainAddress")
    protected String address;

    // These fields are not available

    @Key("middle_name")
    protected String middleName;

    @Key("nickname")
    protected String nickname;

    @Key("preferred_username")
    protected String preferredUsername;

    @Key("website")
    protected String website;

    @Key("verified_email")
    protected boolean verifiedEmail;

    @Key("gender")
    protected String gender;

    @Key("zoneinfo")
    protected String zoneInfo;

    @Key("locale")
    protected String locale;

    @Key("updated_time")
    protected Date updatedTime;

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
