package org.nuxeo.ecm.platform.oauth.providers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

public class OAuthServiceProviderRegistryImpl extends DefaultComponent implements OAuthServiceProviderRegistry {

    protected static final Log log = LogFactory.getLog(OAuthServiceProviderRegistryImpl.class);

    public static final String DIRECTORY_NAME = "oauthServiceProviders";

    protected Map<String, NuxeoOAuthServiceProvider> inMemoryProviders = new HashMap<String, NuxeoOAuthServiceProvider>();

    public NuxeoOAuthServiceProvider getProvider(String gadgetUri, String serviceName) {
        try {
            NuxeoOAuthServiceProvider provider = getEntry(gadgetUri, serviceName);
            return provider;
        } catch (Exception e) {
            log.error("Unable to read provider from Directory backend", e);
            return null;
        }
    }

    protected NuxeoOAuthServiceProvider getEntry(String gadgetUri, String serviceName) throws Exception {

        String id = mkStringIdx(gadgetUri, serviceName);
        if (inMemoryProviders.containsKey(id)) {
            return inMemoryProviders.get(id);
        }

        DirectoryService ds = Framework.getService(DirectoryService.class);
        Session session = ds.open(DIRECTORY_NAME);
        try {

            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            filter.put("gadgetUrl", gadgetUri);
            filter.put("serviceName", serviceName);
            DocumentModelList entries = session.query(filter);
            if (entries == null || entries.size()==0) {
                return null;
            }
            if (entries.size()>1) {
                log.warn("Found several entries for gadgetUri=" + gadgetUri + " and serviceName=" + serviceName);

            }
            NuxeoOAuthServiceProvider provider = NuxeoOAuthServiceProvider.createFromDirectoryEntry(entries.get(0));
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
