/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     "Stephane Lacoin [aka matic] <slacoin at nuxeo.com>"
 */
package org.nuxeo.ecm.core.management.storage;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.management.CoreManagementComponent;
import org.nuxeo.ecm.core.repository.RepositoryInitializationHandler;
import org.nuxeo.runtime.api.Framework;

/**
 * Initialize document store by invoking registered handlers
 *
 * @author "Stephane Lacoin [aka matic] <slacoin at nuxeo.com>"
 */
public class DocumentStoreManager extends RepositoryInitializationHandler {

    public static final String MANAGEMENT_ROOT_TYPE = "ManagementRoot";

    public static final String MANAGEMENT_ROOT_NAME = "management";

    public static final String MANAGEMENT_ROOT_PATH = "/" + MANAGEMENT_ROOT_NAME;

    public static PathRef newPath(String... components) {
        StringBuilder sb = new StringBuilder();
        sb.append(MANAGEMENT_ROOT_PATH);
        for (String component : components) {
            sb.append("/").append(component);
        }
        return new PathRef(sb.toString());
    }

    protected final Map<String, DocumentStoreHandlerDescriptor> handlers = new HashMap<String, DocumentStoreHandlerDescriptor>();

    public void registerHandler(DocumentStoreHandlerDescriptor desc) {
        if (desc.handler == null) {
            throw new RuntimeException("Class wasn't resolved or new instance failed, check logs");
        }
        handlers.put(desc.id, desc);
    }

    protected DocumentStoreConfigurationDescriptor config = new DocumentStoreConfigurationDescriptor();

    public void registerConfig(DocumentStoreConfigurationDescriptor config) {
        this.config = config;
        DocumentStoreSessionRunner.repositoryName = config.repositoryName;
    }

    protected String defaultRepositoryName;

    protected boolean mgmtInitialized;

    protected boolean defaultInitialized;

    protected DocumentRef rootletRef;

    @Override
    public void doInitializeRepository(CoreSession session) {
        if (defaultRepositoryName == null) {
            RepositoryManager mgr = Framework.getService(RepositoryManager.class);
            defaultRepositoryName = mgr.getDefaultRepositoryName();
            if (DocumentStoreSessionRunner.repositoryName == null) {
                DocumentStoreSessionRunner.repositoryName = defaultRepositoryName;
            }
        }
        String repositoryName = session.getRepositoryName();

        if (repositoryName.equals(DocumentStoreSessionRunner.repositoryName)) {
            mgmtInitialized = true;
            rootletRef = setupRootlet(session);
            for (DocumentStoreHandlerDescriptor desc : handlers.values()) {
                desc.handler.onStorageInitialization(session, rootletRef);
            }
        }

        if (repositoryName.equals(defaultRepositoryName)) {
            defaultInitialized = true;
        }

        if (defaultInitialized && mgmtInitialized) {
            CoreManagementComponent.getDefault().onNuxeoServerStartup();
        }
    }

    protected DocumentModel createRootlet(CoreSession session) {
        DocumentModel rootlet = session.createDocumentModel("/", MANAGEMENT_ROOT_NAME, MANAGEMENT_ROOT_TYPE);
        rootlet = session.createDocument(rootlet);

        ACP acp = rootlet.getACP();
        ACL acl = acp.getOrCreateACL();

        for (ACE ace : acl.getACEs()) {
            acl.remove(ace);
        }

        acl.add(new ACE(config.groupName, SecurityConstants.EVERYTHING, true));
        acl.add(new ACE(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, false));
        rootlet.setACP(acp, true);

        session.save();

        return rootlet;
    }

    protected DocumentRef setupRootlet(CoreSession session) {
        DocumentModel rootlet;
        if (!session.exists(new PathRef(MANAGEMENT_ROOT_PATH))) {
            rootlet = createRootlet(session);
        } else {
            rootlet = session.getDocument(new PathRef(MANAGEMENT_ROOT_PATH));
        }
        return rootlet.getRef();
    }

}
