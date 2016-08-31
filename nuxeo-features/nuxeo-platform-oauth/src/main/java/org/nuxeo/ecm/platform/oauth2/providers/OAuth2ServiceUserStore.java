/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *      Nelson Silva
 */
package org.nuxeo.ecm.platform.oauth2.providers;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.oauth2.tokens.NuxeoOAuth2Token;
import org.nuxeo.runtime.api.Framework;

/**
 * Directory backed storage for mapping between users and services The current implementation reuses the existing token
 * directory as storage.
 *
 * @since 7.3
 */
public class OAuth2ServiceUserStore {

    protected static final Log log = LogFactory.getLog(OAuth2ServiceUserStore.class);

    public static final String DIRECTORY_NAME = "oauth2Tokens";

    public static final String ENTRY_ID = "id";

    private String serviceName;

    public OAuth2ServiceUserStore(String serviceName) {
        this.serviceName = serviceName;
    }

    public String store(String nuxeoLogin) {
        return store(nuxeoLogin, new HashMap<>());
    }

    public String store(String nuxeoLogin, Map<String, Object> fields) {
        DirectoryService ds = Framework.getLocalService(DirectoryService.class);
        return Framework.doPrivileged(() -> {
            try (Session session = ds.open(DIRECTORY_NAME)) {
                fields.put("nuxeoLogin", nuxeoLogin);
                fields.put("serviceName", serviceName);
                DocumentModel entry = session.createEntry(fields);
                Long id = (Long) entry.getProperty(NuxeoOAuth2Token.SCHEMA, ENTRY_ID);
                return id.toString();
            }
        });
    }

    public String find(Map<String, Serializable> filter) {
        filter.put("serviceName", serviceName);
        DocumentModelList entries = query(filter);
        if (entries == null || entries.size() == 0) {
            return null;
        }
        if (entries.size() > 1) {
            log.error("Found several tokens");
        }
        Long id = (Long) entries.get(0).getProperty(NuxeoOAuth2Token.SCHEMA, ENTRY_ID);
        return id.toString();
    }

    protected DocumentModelList query(Map<String, Serializable> filter) {
        DirectoryService ds = Framework.getLocalService(DirectoryService.class);
        return Framework.doPrivileged(() -> {
            try (Session session = ds.open(DIRECTORY_NAME)) {
                return session.query(filter);
            }
        });
    }
}
