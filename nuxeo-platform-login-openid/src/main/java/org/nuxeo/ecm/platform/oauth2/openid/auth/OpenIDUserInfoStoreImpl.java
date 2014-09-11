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
 *     Nelson Silva <nelson.silva@inevo.pt>
 */
package org.nuxeo.ecm.platform.oauth2.openid.auth;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

public class OpenIDUserInfoStoreImpl implements OpenIDUserInfoStore {

    protected static final Log log = LogFactory.getLog(OpenIDUserInfoStoreImpl.class);

    public static final String DIRECTORY_NAME = "openIdUserInfos";

    public static final String SCHEMA_NAME = "openIdUserInfo";

    public static final String NUXEO_LOGIN_KEY = "nuxeoLogin";

    public static final String OPENID_SUBJECT_KEY = "subject";

    public static final String OPENID_PROVIDER_KEY = "provider";

    public static final String ID = "id";

    private String providerName;

    public OpenIDUserInfoStoreImpl(String providerName) {
        this.providerName = providerName;
    }

    @Override
    public void storeUserInfo(String userId, OpenIDUserInfo userInfo) {
        Session session = null;
        try {
            DirectoryService ds = Framework.getService(DirectoryService.class);
            session = ds.open(DIRECTORY_NAME);
            Map<String, Object> data = new HashMap<String, Object>();

            // Generate an ID
            String userInfoId = getID(providerName, userInfo.getSubject());

            data.put(NUXEO_LOGIN_KEY, userId);
            data.put(OPENID_PROVIDER_KEY, providerName);

            // Copy the standard fields
            data.put(OPENID_SUBJECT_KEY, userInfo.getSubject());
            data.put("name", userInfo.getName());
            data.put("given_name", userInfo.getGivenName());
            data.put("family_name", userInfo.getFamilyName());
            data.put("middle_name", userInfo.getMiddleName());
            data.put("nickname", userInfo.getNickname());
            data.put("preferred_username", userInfo.getPreferredUsername());
            data.put("profile", userInfo.getProfile());
            data.put("picture", userInfo.getPicture());
            data.put("website", userInfo.getWebsite());
            data.put("email", userInfo.getEmail());
            data.put("email_verified", userInfo.isEmailVerified());
            data.put("gender", userInfo.getGender());
            data.put("birthdate", userInfo.getBirthdate());
            data.put("zoneinfo", userInfo.getZoneInfo());
            data.put("locale", userInfo.getLocale());
            data.put("phone_number", userInfo.getPhoneNumber());
            data.put("address", userInfo.getAddress());
            data.put("updated_time", userInfo.getUpdatedTime());

            if (session.hasEntry(userInfoId)) {
                DocumentModel userInfoDoc = session.getEntry(userInfoId);
                userInfoDoc.setProperties(SCHEMA_NAME, data);
                session.updateEntry(userInfoDoc);
            } else {
                data.put(ID, userInfoId);
                session.createEntry(data);
            }

        } catch (DirectoryException e) {
            log.error("Error during token storage", e);
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (DirectoryException e) {
                    log.debug(e);
                }
            }
        }
    }

    @Override
    public String getNuxeoLogin(OpenIDUserInfo userInfo) {

        Session session = null;
        try {
            DirectoryService ds = Framework.getService(DirectoryService.class);
            session = ds.open(DIRECTORY_NAME);
            DocumentModel entry = session.getEntry(getID(providerName,
                    userInfo.getSubject()));
            if (entry == null) {
                return null;
            }
            return (String) entry.getPropertyValue(SCHEMA_NAME + ":"
                    + NUXEO_LOGIN_KEY);
        } catch (Exception e) {
            log.error("Error retrieving OpenID user info", e);
            return null;
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (DirectoryException e) {
                }
            }
        }
    }

    @Override
    public OpenIDUserInfo getUserInfo(String nuxeoLogin) {

        Session session = null;
        try {
            DirectoryService ds = Framework.getService(DirectoryService.class);
            session = ds.open(DIRECTORY_NAME);
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            filter.put(OPENID_PROVIDER_KEY, providerName);
            filter.put(NUXEO_LOGIN_KEY, nuxeoLogin);
            DocumentModelList entries = session.query(filter);
            if (entries.size() == 0) {
                return null;
            }
            DocumentModel entry = entries.get(0);
            DefaultOpenIDUserInfo userInfo = new DefaultOpenIDUserInfo();
            userInfo.putAll(entry.getProperties(SCHEMA_NAME));
            return userInfo;
        } catch (Exception e) {
            log.error("Error retrieving OpenID user info", e);
            return null;
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (DirectoryException e) {
                }
            }
        }
    }

    protected String getID(String provider, String subject) {
        return subject + "@" + provider;
    }

}
