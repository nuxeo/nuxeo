/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.oauth.providers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Implementation of the {@link OAuthServiceProviderRegistry}.
 * The main storage backend is a SQL Directory.
 * Readonly providers (contributed directly at OpenSocialService level) are managed in memory.
 *
 * @author tiry
 *
 */
public class OAuthServiceProviderRegistryImpl extends DefaultComponent implements OAuthServiceProviderRegistry {

    protected static final Log log = LogFactory.getLog(OAuthServiceProviderRegistryImpl.class);

    public static final String DIRECTORY_NAME = "oauthServiceProviders";

    protected Map<String, NuxeoOAuthServiceProvider> inMemoryProviders = new HashMap<String, NuxeoOAuthServiceProvider>();

    public NuxeoOAuthServiceProvider getProvider(String gadgetUri, String serviceName) {
        try {
            NuxeoOAuthServiceProvider provider = getEntry(gadgetUri, serviceName, null);
            return provider;
        } catch (Exception e) {
            log.error("Unable to read provider from Directory backend", e);
            return null;
        }
    }

    protected String getBareGadgetUri(String gadgetUri) {
        if (gadgetUri==null) {
            return null;
        }
        String pattern = "http(s)?://(localhost|127.0.0.1)";
        return gadgetUri.replaceFirst(pattern, "");
    }

    protected String preProcessServiceName(String serviceName) {
        if (serviceName!=null && serviceName.trim().isEmpty()) {
            return null;
        }
        return serviceName;
    }


    protected DocumentModel getBestEntry(DocumentModelList entries, String gadgetUri, String serviceName) throws PropertyException, ClientException {
        if (entries.size()>1) {
            log.warn("Found several entries for gadgetUri=" + gadgetUri + " and serviceName=" + serviceName);
        }
        if (serviceName==null || serviceName.trim().isEmpty()){
            for (DocumentModel entry:entries) {
                if (entry.getPropertyValue("serviceName")==null || ((String) entry.getPropertyValue("serviceName")).trim().isEmpty()) {
                    return entry;
                }
            }
            return null;
        } else if (gadgetUri==null || gadgetUri.trim().isEmpty()) {
            for (DocumentModel entry:entries) {
                if (entry.getPropertyValue("gadgetUrl")==null || ((String) entry.getPropertyValue("gadgetUrl")).trim().isEmpty()) {
                    return entry;
                }
            }
            return null;
        }

        // XXX do better than that !
        return entries.get(0);
    }

    protected NuxeoOAuthServiceProvider getEntry(String gadgetUri, String serviceName, Set<String> ftFilter) throws Exception {

        String id = mkStringIdx(gadgetUri, serviceName);
        if (inMemoryProviders.containsKey(id)) {
            return inMemoryProviders.get(id);
        }

        // normalize "enmpty" service name
        serviceName = preProcessServiceName(serviceName);

        if (gadgetUri==null && serviceName==null) {
            log.warn("Can not find provider with null gadgetUri and null serviceName !");
            return null;
        }

        DirectoryService ds = Framework.getService(DirectoryService.class);
        Session session = ds.open(DIRECTORY_NAME);
        NuxeoOAuthServiceProvider provider = null;
        try {

            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            if (gadgetUri!=null) {
                filter.put("gadgetUrl", gadgetUri);
            }
            if (serviceName!=null) {
                filter.put("serviceName", serviceName);
            }
            DocumentModelList entries = session.query(filter, ftFilter);
            if (entries == null || entries.size()==0) {
                String bareGadgetUrl = getBareGadgetUri(gadgetUri);
                if (bareGadgetUrl!=null && !bareGadgetUrl.equals(gadgetUri)) {
                    Set<String> urlfilter = new HashSet<String>();
                    urlfilter.add("gadgetUrl");
                    return getEntry(bareGadgetUrl, serviceName,urlfilter);
                }
                if (serviceName!=null) {
                    if (bareGadgetUrl!=null) {
                        provider = getEntry(bareGadgetUrl, null,ftFilter);
                         if (provider!=null) {
                             return provider;
                         }
                    }
                    if (gadgetUri!=null) {
                        return getEntry(null, serviceName,ftFilter);
                    }
                }
                return null;
            }
            DocumentModel entry = getBestEntry(entries, gadgetUri, serviceName);
            if (entry==null) {
                return null;
            }
            provider = NuxeoOAuthServiceProvider.createFromDirectoryEntry(entry);
            return provider;
        } finally {
            session.close();
        }
    }

    protected String mkStringIdx(String gadgetUri, String serviceName) {
        return "k-" + gadgetUri + "-" + serviceName;
    }

    public NuxeoOAuthServiceProvider addReadOnlyProvider(String gadgetUri, String serviceName, String consumerKey, String consumerSecret, String publicKey ) {
        String id = mkStringIdx(gadgetUri, serviceName);
        Long dummyId = new Random().nextLong();
        NuxeoOAuthServiceProvider sp = new NuxeoOAuthServiceProvider(dummyId, gadgetUri, serviceName, consumerKey, consumerSecret, publicKey);
        inMemoryProviders.put(id, sp);
        return sp;
    }

    public void deleteProvider(String gadgetUri, String serviceName) {

        NuxeoOAuthServiceProvider provider = getProvider(gadgetUri, serviceName);
        if (provider!=null) {
            deleteProvider(provider.id.toString());
        }

    }

    public void deleteProvider(String providerId) {
        try {
            DirectoryService ds = Framework.getService(DirectoryService.class);
            Session session = ds.open(DIRECTORY_NAME);
            try {
                session.deleteEntry(providerId);
                session.commit();
            } finally {
                session.close();
            }
        } catch (Exception e) {
            log.error("Unable to delete provider " + providerId, e);
        }
    }

    public List<NuxeoOAuthServiceProvider> listProviders() {

        List<NuxeoOAuthServiceProvider> result = new ArrayList<NuxeoOAuthServiceProvider>();
        for (NuxeoOAuthServiceProvider provider : inMemoryProviders.values()) {
            result.add(provider);
        }
        try {
            DirectoryService ds = Framework.getService(DirectoryService.class);
            Session session = ds.open(DIRECTORY_NAME);
            try {
                DocumentModelList entries = session.getEntries();
                for (DocumentModel entry : entries) {
                    result.add(NuxeoOAuthServiceProvider.createFromDirectoryEntry(entry));
                }
            } finally {
                session.close();
            }
        } catch (Exception e) {
            log.error("Error while fetching provider directory", e);
        }
        return result;
    }

}
