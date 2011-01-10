package org.nuxeo.ecm.platform.oauth.consumers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

public class OAuthConsumerRegistryImpl extends DefaultComponent implements OAuthConsumerRegistry {

    protected static final Log log = LogFactory.getLog(OAuthConsumerRegistryImpl.class);

    protected static final String DIRECTORY_NAME = "oauthConsumers";

    @Override
    public NuxeoOAuthConsumer getConsumer(String consumerKey) {
        try {
            NuxeoOAuthConsumer consumer = getEntry(consumerKey);
            if (consumer==null && consumerKey.equals("confluence")) {
                consumer = new NuxeoOAuthConsumer(null,consumerKey,"testoauthsharedsecret", null );
            }
            return consumer;
        } catch (Exception e) {
            log.error("Unable to read consumer " + consumerKey + " from Directory backend", e);
            return null;
        }
    }

    protected NuxeoOAuthConsumer getEntry(String consumerKey) throws Exception {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        Session session = ds.open(DIRECTORY_NAME);
        try {
            DocumentModel entry = session.getEntry(consumerKey);
            if (entry==null) {
                return null;
            }
            NuxeoOAuthConsumer consumer =  NuxeoOAuthConsumer.createFromDirectoryEntry(entry);
            return consumer;
        }
        finally {
            session.close();
        }
    }

    public NuxeoOAuthConsumer storeConsumer(NuxeoOAuthConsumer consumer) throws Exception {

        DirectoryService ds = Framework.getService(DirectoryService.class);
        Session session = ds.open(DIRECTORY_NAME);
        try {

            Map<String, Object> init = new HashMap<String, Object>();
            init.put("consumerKey", consumer.consumerKey);
            DocumentModel entry = session.createEntry(init);
            consumer.asDocumentModel(entry);
            session.updateEntry(entry);
            session.commit();
            if (entry==null) {
                return null;
            }
            consumer =  NuxeoOAuthConsumer.createFromDirectoryEntry(entry);
            return consumer;
        }
        finally {
            session.close();
        }
    }


    @Override
    public void deleteConsumer(String consumerKey) {
        try {
            DirectoryService ds = Framework.getService(DirectoryService.class);
            Session session = ds.open(DIRECTORY_NAME);
            try {
                session.deleteEntry(consumerKey);
                session.commit();
            }
            finally {
                session.close();
            }
        }catch (Exception e) {
            log.error("Unable to delete consumer " + consumerKey, e);
        }
    }

    @Override
    public List<NuxeoOAuthConsumer> listConsumers() {

        List<NuxeoOAuthConsumer> result = new ArrayList<NuxeoOAuthConsumer>();
        try {
            DirectoryService ds = Framework.getService(DirectoryService.class);
            Session session = ds.open(DIRECTORY_NAME);
            try {
                DocumentModelList entries = session.getEntries();
                for (DocumentModel entry : entries) {
                    result.add(NuxeoOAuthConsumer.createFromDirectoryEntry(entry));
                }
            }
            finally {
                session.close();
            }
        }
        catch (Exception e) {
            log.error("Error while fetching consumer directory", e);
        }
        return result;
    }

}
