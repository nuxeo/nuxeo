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

package org.nuxeo.ecm.platform.oauth2.providers;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

public class OAuth2ServiceProviderRegistryImpl extends DefaultComponent
        implements OAuth2ServiceProviderRegistry {

    protected static final Log log = LogFactory.getLog(OAuth2ServiceProviderRegistryImpl.class);

    public static final String DIRECTORY_NAME = "oauth2ServiceProviders";

    public NuxeoOAuth2ServiceProvider getProvider(String serviceName) {
        try {
            NuxeoOAuth2ServiceProvider provider = getEntry(serviceName, null);
            return provider;
        } catch (Exception e) {
            log.error("Unable to read provider from Directory backend", e);
            return null;
        }
    }

    public NuxeoOAuth2ServiceProvider addProvider(String serviceName,
            String tokenServerURL, String authorizationServerURL,
            String clientId, String clientSecret, List<String> scopes)
            throws Exception {

        NuxeoOAuth2ServiceProvider provider = new NuxeoOAuth2ServiceProvider(
                null, serviceName, tokenServerURL, authorizationServerURL,
                clientId, clientSecret, scopes);
        DirectoryService ds = Framework.getService(DirectoryService.class);
        Session session = null;

        try {
            session = ds.open(DIRECTORY_NAME);
            DocumentModel creationEntry = BaseSession.createEntryModel(null,
                    NuxeoOAuth2ServiceProvider.SCHEMA, null, null);
            DocumentModel entry = session.createEntry(creationEntry);
            provider.asDocumentModel(entry);
            session.updateEntry(entry);

            return getProvider(serviceName);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    protected String preProcessServiceName(String serviceName) {
        if (serviceName != null && serviceName.trim().isEmpty()) {
            return null;
        }
        return serviceName;
    }

    protected NuxeoOAuth2ServiceProvider getEntry(String serviceName,
            Set<String> ftFilter) throws Exception {

        // normalize "empty" service name
        serviceName = preProcessServiceName(serviceName);

        if (serviceName == null) {
            log.warn("Can not find provider with null serviceName !");
            return null;
        }

        DirectoryService ds = Framework.getService(DirectoryService.class);
        Session session = null;
        NuxeoOAuth2ServiceProvider provider = null;
        try {
            session = ds.open(DIRECTORY_NAME);
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            if (serviceName != null) {
                filter.put("serviceName", serviceName);
            }
            DocumentModelList entries = session.query(filter, ftFilter);
            if (entries == null || entries.size() == 0) {
                return null;
            }
            if (entries.size() > 1) {
                log.warn("Found several entries for  serviceName="
                        + serviceName);
            }
            // XXX do better than that !
            DocumentModel entry = entries.get(0);
            provider = NuxeoOAuth2ServiceProvider.createFromDirectoryEntry(entry);
            return provider;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

}
