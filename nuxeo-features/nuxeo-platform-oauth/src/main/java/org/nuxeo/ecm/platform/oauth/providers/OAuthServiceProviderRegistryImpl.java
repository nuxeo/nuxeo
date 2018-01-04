/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.oauth.providers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Implementation of the {@link OAuthServiceProviderRegistry}. The main storage backend is a SQL Directory. Readonly
 * providers (contributed directly at OpenSocialService level) are managed in memory.
 *
 * @author tiry
 */
public class OAuthServiceProviderRegistryImpl extends DefaultComponent implements OAuthServiceProviderRegistry {

    protected static final Log log = LogFactory.getLog(OAuthServiceProviderRegistryImpl.class);

    public static final String DIRECTORY_NAME = "oauthServiceProviders";

    protected static final Random RANDOM = new Random();

    protected Map<String, NuxeoOAuthServiceProvider> inMemoryProviders = new HashMap<String, NuxeoOAuthServiceProvider>();

    @Override
    public NuxeoOAuthServiceProvider getProvider(String gadgetUri, String serviceName) {
        try {
            NuxeoOAuthServiceProvider provider = getEntry(gadgetUri, serviceName, null);
            return provider;
        } catch (DirectoryException e) {
            log.error("Unable to read provider from Directory backend", e);
            return null;
        }
    }

    protected String getBareGadgetUri(String gadgetUri) {
        if (gadgetUri == null) {
            return null;
        }
        String pattern = "http(s)?://(localhost|127.0.0.1)";
        return gadgetUri.replaceFirst(pattern, "");
    }

    protected String preProcessServiceName(String serviceName) {
        if (serviceName != null && serviceName.trim().isEmpty()) {
            return null;
        }
        return serviceName;
    }

    protected DocumentModel getBestEntry(DocumentModelList entries, String gadgetUri, String serviceName)
            throws PropertyException {
        if (entries.size() > 1) {
            log.warn("Found several entries for gadgetUri=" + gadgetUri + " and serviceName=" + serviceName);
        }
        if (serviceName == null || serviceName.trim().isEmpty()) {
            for (DocumentModel entry : entries) {
                if (entry.getPropertyValue("serviceName") == null
                        || ((String) entry.getPropertyValue("serviceName")).trim().isEmpty()) {
                    return entry;
                }
            }
            return null;
        } else if (gadgetUri == null || gadgetUri.trim().isEmpty()) {
            for (DocumentModel entry : entries) {
                if (entry.getPropertyValue("gadgetUrl") == null
                        || ((String) entry.getPropertyValue("gadgetUrl")).trim().isEmpty()) {
                    return entry;
                }
            }
            return null;
        }

        // XXX do better than that !
        return entries.get(0);
    }

    protected NuxeoOAuthServiceProvider getEntry(String gadgetUri, String serviceName, Set<String> ftFilter)
            {

        String id = mkStringIdx(gadgetUri, serviceName);
        if (inMemoryProviders.containsKey(id)) {
            return inMemoryProviders.get(id);
        }

        // normalize "enmpty" service name
        serviceName = preProcessServiceName(serviceName);

        if (gadgetUri == null && serviceName == null) {
            log.warn("Can not find provider with null gadgetUri and null serviceName !");
            return null;
        }

        DirectoryService ds = Framework.getService(DirectoryService.class);
        NuxeoOAuthServiceProvider provider = null;
        try (Session session = ds.open(DIRECTORY_NAME)) {
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            if (gadgetUri != null) {
                filter.put("gadgetUrl", gadgetUri);
            }
            if (serviceName != null) {
                filter.put("serviceName", serviceName);
            }
            DocumentModelList entries = session.query(filter, ftFilter);
            if (entries == null || entries.size() == 0) {
                String bareGadgetUrl = getBareGadgetUri(gadgetUri);
                if (bareGadgetUrl != null && !bareGadgetUrl.equals(gadgetUri)) {
                    Set<String> urlfilter = new HashSet<String>();
                    urlfilter.add("gadgetUrl");
                    return getEntry(bareGadgetUrl, serviceName, urlfilter);
                }
                if (serviceName != null) {
                    if (bareGadgetUrl != null) {
                        provider = getEntry(bareGadgetUrl, null, ftFilter);
                        if (provider != null) {
                            return provider;
                        }
                    }
                    if (gadgetUri != null) {
                        return getEntry(null, serviceName, ftFilter);
                    }
                }
                return null;
            }
            DocumentModel entry = getBestEntry(entries, gadgetUri, serviceName);
            if (entry == null) {
                return null;
            }
            provider = NuxeoOAuthServiceProvider.createFromDirectoryEntry(entry);
            return provider;
        }
    }

    protected String mkStringIdx(String gadgetUri, String serviceName) {
        return "k-" + gadgetUri + "-" + serviceName;
    }

    @Override
    public NuxeoOAuthServiceProvider addReadOnlyProvider(String gadgetUri, String serviceName, String consumerKey,
            String consumerSecret, String publicKey) {
        String id = mkStringIdx(gadgetUri, serviceName);
        long dummyId = RANDOM.nextLong();
        NuxeoOAuthServiceProvider sp = new NuxeoOAuthServiceProvider(dummyId, gadgetUri, serviceName, consumerKey,
                consumerSecret, publicKey);
        inMemoryProviders.put(id, sp);
        return sp;
    }

    @Override
    public void deleteProvider(String gadgetUri, String serviceName) {

        NuxeoOAuthServiceProvider provider = getProvider(gadgetUri, serviceName);
        if (provider != null) {
            deleteProvider(provider.id.toString());
        }

    }

    @Override
    public void deleteProvider(String providerId) {
        try {
            DirectoryService ds = Framework.getService(DirectoryService.class);
            try (Session session = ds.open(DIRECTORY_NAME)) {
                session.deleteEntry(providerId);
            }
        } catch (DirectoryException e) {
            log.error("Unable to delete provider " + providerId, e);
        }
    }

    @Override
    public List<NuxeoOAuthServiceProvider> listProviders() {

        List<NuxeoOAuthServiceProvider> result = new ArrayList<NuxeoOAuthServiceProvider>();
        for (NuxeoOAuthServiceProvider provider : inMemoryProviders.values()) {
            result.add(provider);
        }
        DirectoryService ds = Framework.getService(DirectoryService.class);
        Framework.doPrivileged(() -> {
            try (Session session = ds.open(DIRECTORY_NAME)) {
                DocumentModelList entries = session.query(Collections.emptyMap());
                for (DocumentModel entry : entries) {
                    result.add(NuxeoOAuthServiceProvider.createFromDirectoryEntry(entry));
                }
            } catch (DirectoryException e) {
                log.error("Error while fetching provider directory", e);
            }
        });
        return result;
    }
}
