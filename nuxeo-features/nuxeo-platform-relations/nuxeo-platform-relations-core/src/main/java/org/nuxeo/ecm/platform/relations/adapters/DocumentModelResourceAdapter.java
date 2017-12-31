/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: DocumentModelResourceAdapter.java 28924 2008-01-10 14:04:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations.adapters;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.impl.AbstractResourceAdapter;
import org.nuxeo.ecm.platform.relations.api.impl.QNameResourceImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Resource adapter using the document model id.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class DocumentModelResourceAdapter extends AbstractResourceAdapter implements Serializable {

    private static final Log log = LogFactory.getLog(DocumentModelResourceAdapter.class);

    private static final long serialVersionUID = -5307418102496342779L;

    @Override
    public Serializable getResourceRepresentation(Resource resource, Map<String, Object> context) {
        Serializable object = null;
        if (resource.isQNameResource()) {
            CoreSession session = null;
            boolean sessionOpened = false;
            try {
                String repoName;
                String uid;
                String localName = ((QNameResource) resource).getLocalName();
                int index = localName.indexOf('/');
                if (index == -1) {
                    // BBB for when repository name was not included in the
                    // local name
                    RepositoryManager mgr = Framework.getService(RepositoryManager.class);
                    repoName = mgr.getDefaultRepositoryName();
                    uid = localName;
                } else {
                    repoName = localName.substring(0, index);
                    uid = localName.substring(index + 1);
                }
                DocumentRef ref = new IdRef(uid);

                if (context != null) {
                    session = (CoreSession) context.get(CORE_SESSION_CONTEXT_KEY);
                    if (!session.getRepositoryName().equals(repoName)) {
                        // let's open one
                        session = null;
                    }
                }
                if (session == null) {
                    // open one
                    session = CoreInstance.openCoreSession(repoName);
                    sessionOpened = true;
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Opened a new session '%s' with id %s", repoName,
                                session.getSessionId()));
                    }
                }
                if (!session.exists(ref)) {
                    return null;
                }
                object = session.getDocument(ref);
            } catch (DocumentNotFoundException e) {
                log.warn("Cannot get resource: " + resource, e);
            } finally {
                if (sessionOpened) {
                    ((CloseableCoreSession) session).close();
                }
            }
        }
        return object;
    }

    @Override
    public Resource getResource(Serializable object, Map<String, Object> context) {
        if (object instanceof DocumentModel) {
            DocumentModel doc = (DocumentModel) object;
            String localName = doc.getRepositoryName() + '/' + doc.getId();
            return new QNameResourceImpl(namespace, localName);
        } else if (object instanceof DocumentLocation) {
            DocumentLocation docLoc = (DocumentLocation) object;
            String localName = docLoc.getServerName() + '/' + docLoc.getIdRef().toString();
            return new QNameResourceImpl(namespace, localName);
        } else {
            throw new IllegalArgumentException(String.format("cannot build resource for '%s'", object));
        }
    }

    @Override
    public Class<?> getKlass() {
        return DocumentModel.class;
    }

}
