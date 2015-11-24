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
package org.nuxeo.ecm.platform.oauth2.openid.auth.facebook;

import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.nuxeo.ecm.platform.oauth2.openid.auth.DefaultOpenIDUserInfo;

import com.google.api.client.util.Key;

public class FacebookUserInfo extends DefaultOpenIDUserInfo {

    @Key("id")
    protected String id;

    @Key("first_name")
    protected String firstName;

    @Key("link")
    protected String link;

    @Key("birthday")
    protected Date birthday;

    @Key("verified")
    protected boolean verified;

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

    @Override
    public Date getUpdatedTime() {
        Date date;
        try {
            DateTimeFormatter parser = ISODateTimeFormat.dateTimeParser();
            DateTime dateTime = parser.parseDateTime(updatedTime);
            date = dateTime.toDate();
        } catch (IllegalArgumentException e) {
            return null;
        }
        return date;
    }

}
