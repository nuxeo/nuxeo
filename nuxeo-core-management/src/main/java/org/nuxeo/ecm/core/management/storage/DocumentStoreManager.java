/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     "Stephane Lacoin [aka matic] <slacoin at nuxeo.com>"
 */
package org.nuxeo.ecm.core.management.storage;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.management.CoreManagementComponent;
import org.nuxeo.ecm.core.repository.RepositoryInitializationHandler;

/**
 * Initialize document store by invoking registered handlers
 *
 * @author "Stephane Lacoin [aka matic] <slacoin at nuxeo.com>"
 *
 */
public class DocumentStoreManager extends RepositoryInitializationHandler {

    protected final Map<String, DocumentStoreHandlerDescriptor> handlers = new HashMap<String, DocumentStoreHandlerDescriptor>();

    public void registerHandler(DocumentStoreHandlerDescriptor desc) {
        handlers.put(desc.id, desc);
    }

    public void registerConfig(DocumentStoreConfigurationDescriptor config) {
        DocumentStoreSessionRunner.repositoryName = config.repositoryName;
    }

    public void setRepositoryName(String name) {
        DocumentStoreSessionRunner.repositoryName = name;
    }

    @Override
    public void doInitializeRepository(CoreSession session) throws ClientException {

        if (!session.getRepositoryName().equals(DocumentStoreSessionRunner.repositoryName)) {
            return;
        }

        for (DocumentStoreHandlerDescriptor desc:handlers.values()) {
            desc.handler.onStorageInitialization(session);
        }
        CoreManagementComponent.getDefault().getLocalManager().onNuxeoServerStartup(); // TODO should status manager be registered as a storage c ?
    }

}
