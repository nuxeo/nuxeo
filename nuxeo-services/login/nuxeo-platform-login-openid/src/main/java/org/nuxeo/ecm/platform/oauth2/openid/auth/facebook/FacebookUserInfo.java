/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nelson Silva <nelson.silva@inevo.pt> - initial API and implementation
 *     Nuxeo
 */
package org.nuxeo.ecm.platform.oauth2.openid.auth.facebook;

import static org.nuxeo.common.utils.DateUtils.parseISODateTime;

import java.util.Date;

import org.nuxeo.common.utils.DateUtils;
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
            date = DateUtils.toDate(parseISODateTime(updatedTime));
        } catch (IllegalArgumentException e) {
            return null;
        }
        return date;
    }

}
