/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *      Nelson Silva
 */
package org.nuxeo.ecm.platform.oauth2.providers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.oauth2.tokens.NuxeoOAuth2Token;
import org.nuxeo.runtime.api.Framework;

import java.io.Serializable;
import java.util.Map;

/**
 * Directory backed storage for mapping between users and services
 * The current implementation reuses the existing token directory as storage.
 *
 * @since 7.3
 */
public class OAuth2ServiceUserStore {

    protected static final Log log = LogFactory.getLog(OAuth2ServiceUserStore.class);

    public static final String DIRECTORY_NAME = "oauth2Tokens";

    private String serviceName;

    public OAuth2ServiceUserStore(String serviceName) {
        this.serviceName = serviceName;
    }

    public String store(String nuxeoLogin, Map<String, Object> fields) {
        DirectoryService ds = Framework.getLocalService(DirectoryService.class);
        Session session = null;
        try {
            session = ds.open(DIRECTORY_NAME);
            fields.put("nuxeoLogin", nuxeoLogin);
            fields.put("serviceName", serviceName);
            DocumentModel entry = session.createEntry(fields);
            Long id = (Long) entry.getProperty(NuxeoOAuth2Token.SCHEMA, "id");
            return id.toString();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public String find(Map<String, Serializable> filter) {
        filter.put("serviceName", serviceName);
        DocumentModelList entries = query(filter);
        if (entries.size() == 0) {
            return null;
        }
        if (entries.size() > 1) {
            log.error("Found several tokens");
        }
        DocumentModel entry =  entries.get(0);
        return entry != null ? (String) entry.getProperty(NuxeoOAuth2Token.SCHEMA, "id") : null;
    }

    protected DocumentModelList query(Map<String, Serializable> filter) {
        DirectoryService ds = Framework.getLocalService(DirectoryService.class);
        Session session = null;
        try {
            session = ds.open(DIRECTORY_NAME);
            return session.query(filter);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
}
