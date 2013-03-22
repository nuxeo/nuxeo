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
            DocumentModel entry = session.createEntry(userInfo);
            session.updateEntry(entry);
        } catch (Exception e) {
            log.error("Error during token storage", e);
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (DirectoryException e) {}
            }
        }
    }

    public String getNuxeoLogin(OpenIDUserInfo userInfo) {

        Session session = null;
        try {
            DirectoryService ds = Framework.getService(DirectoryService.class);
            session = ds.open(DIRECTORY_NAME);
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            filter.put("provider", providerName);
            filter.put("subject", userInfo.getSubject());
            DocumentModelList entries = session.query(filter);
            if (entries.size() == 0) {
                return null;
            }
            DocumentModel entry = entries.get(0);
            return (String) entry.getPropertyValue(SCHEMA_NAME + ":" + "subject");
        } catch (Exception e) {
            log.error("Error retrieving OpenID user info", e);
            return null;
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (DirectoryException e) {}
            }
        }
    }

    public OpenIDUserInfo getUserInfo(String nuxeoLogin) {

        Session session = null;
        try {
            DirectoryService ds = Framework.getService(DirectoryService.class);
            session = ds.open(DIRECTORY_NAME);
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            filter.put("provider", providerName);
            filter.put("nuxeoLogin", nuxeoLogin);
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
                } catch (DirectoryException e) {}
            }
        }
    }

}
