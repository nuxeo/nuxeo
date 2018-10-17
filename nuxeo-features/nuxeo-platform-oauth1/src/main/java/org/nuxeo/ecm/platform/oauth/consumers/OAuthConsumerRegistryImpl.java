/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.oauth.consumers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Implementation of the {@link OAuthConsumerRegistry} Service. It's basically a simple Storage API on top of an SQL
 * Directory.
 *
 * @author tiry
 */
public class OAuthConsumerRegistryImpl extends DefaultComponent implements OAuthConsumerRegistry {

    protected static final Log log = LogFactory.getLog(OAuthConsumerRegistryImpl.class);

    public static final String DIRECTORY_NAME = "oauthConsumers";

    @Override
    public NuxeoOAuthConsumer getConsumer(String consumerKey, String keyType) {
        try {
            NuxeoOAuthConsumer consumer = getEntry(consumerKey, keyType);
            return consumer;
        } catch (DirectoryException e) {
            log.error("Unable to read consumer " + consumerKey + " from Directory backend", e);
            return null;
        }
    }

    @Override
    public NuxeoOAuthConsumer getConsumer(String consumerKey) {
        return getConsumer(consumerKey, null);
    }

    protected NuxeoOAuthConsumer getEntry(String consumerKey, String keyType) {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        return Framework.doPrivileged(() -> {
            try (Session session = ds.open(DIRECTORY_NAME)) {
                DocumentModel entry = session.getEntry(consumerKey);
                if (entry == null) {
                    return null;
                }
                return NuxeoOAuthConsumer.createFromDirectoryEntry(entry, keyType);
            }
        });
    }

    @Override
    public NuxeoOAuthConsumer storeConsumer(NuxeoOAuthConsumer consumer) {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        return Framework.doPrivileged(() -> {
            try (Session session = ds.open(DIRECTORY_NAME)) {
                DocumentModel entry = session.createEntry(
                        Collections.singletonMap("consumerKey", consumer.consumerKey));
                consumer.asDocumentModel(entry);
                session.updateEntry(entry);
                return NuxeoOAuthConsumer.createFromDirectoryEntry(entry, null);
            }
        });
    }

    @Override
    public void deleteConsumer(String consumerKey) {
        Framework.doPrivileged(() -> {
            try {
                DirectoryService ds = Framework.getService(DirectoryService.class);
                try (Session session = ds.open(DIRECTORY_NAME)) {
                    session.deleteEntry(consumerKey);
                }
            } catch (DirectoryException e) {
                log.error("Unable to delete consumer " + consumerKey, e);
            }
        });
    }

    @Override
    public List<NuxeoOAuthConsumer> listConsumers() {
        return Framework.doPrivileged(() -> {
            try {
                DirectoryService ds = Framework.getService(DirectoryService.class);
                try (Session session = ds.open(DIRECTORY_NAME)) {
                    DocumentModelList entries = session.query(Collections.emptyMap());
                    List<NuxeoOAuthConsumer> result = new ArrayList<NuxeoOAuthConsumer>();
                    for (DocumentModel entry : entries) {
                        result.add(NuxeoOAuthConsumer.createFromDirectoryEntry(entry, null));
                    }
                    return result;
                }
            } catch (DirectoryException e) {
                log.error("Error while fetching consumer directory", e);
                return Collections.emptyList();
            }
        });
    }

}
