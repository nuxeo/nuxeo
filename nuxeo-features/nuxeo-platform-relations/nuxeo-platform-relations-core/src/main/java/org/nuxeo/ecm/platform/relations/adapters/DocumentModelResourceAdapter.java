/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id: DocumentModelResourceAdapter.java 28924 2008-01-10 14:04:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations.adapters;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.repository.Repository;
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
public class DocumentModelResourceAdapter extends AbstractResourceAdapter
        implements Serializable {

    private static final Log log = LogFactory.getLog(DocumentModelResourceAdapter.class);

    private static final long serialVersionUID = -5307418102496342779L;

    @Override
    public Serializable getResourceRepresentation(Resource resource,
            Map<String, Serializable> context) {
        Serializable object = null;
        if (resource.isQNameResource()) {
            CoreSession session = null;
            boolean sessionOpened = false;
            try {
                RepositoryManager mgr = Framework.getService(RepositoryManager.class);
                String repoName;
                String uid;
                String localName = ((QNameResource) resource).getLocalName();
                int index = localName.indexOf('/');
                if (index == -1) {
                    // BBB for when repository name was not included in the
                    // local name
                    repoName = mgr.getDefaultRepository().getName();
                    uid = localName;
                } else {
                    repoName = localName.substring(0, index);
                    uid = localName.substring(index + 1);
                }
                DocumentRef ref = new IdRef(uid);

                if (context != null) {
                    Serializable givenSessionId = context.get(CORE_SESSION_ID_CONTEXT_KEY);
                    if (givenSessionId instanceof String) {
                        session = CoreInstance.getInstance().getSession(
                                (String) givenSessionId);
                        if (!session.getRepositoryName().equals(repoName)) {
                            // let's open one
                            session = null;
                        }
                    }
                }
                if (session == null) {
                    // open one
                    sessionOpened = true;
                    Repository repo = mgr.getRepository(repoName);
                    session = repo.open();
                    if (log.isDebugEnabled()) {
                        log.debug(String.format(
                                "Opened a new session '%s' with id %s",
                                repoName, session.getSessionId()));
                    }
                }
                if (!session.exists(ref)) {
                    return null;
                }
                object = session.getDocument(ref);
            } catch (Exception e) {
                log.warn("Cannot get resource: " + resource, e);
            } finally {
                if (session != null && sessionOpened) {
                    CoreInstance core = CoreInstance.getInstance();
                    core.close(session);
                }
            }
        }
        return object;
    }

    @Override
    public Resource getResource(Serializable object,
            Map<String, Serializable> context) {
        if (object instanceof DocumentModel) {
            DocumentModel doc = (DocumentModel) object;
            String localName = doc.getRepositoryName() + '/' + doc.getId();
            return new QNameResourceImpl(namespace, localName);
        } else if (object instanceof DocumentLocation) {
            DocumentLocation docLoc = (DocumentLocation) object;
            String localName = docLoc.getServerName() + '/'
                    + docLoc.getIdRef().toString();
            return new QNameResourceImpl(namespace, localName);
        } else {
            throw new IllegalArgumentException(String.format(
                    "cannot build resource for '%s'", object));
        }
    }

    @Override
    public Class<?> getKlass() {
        return DocumentModel.class;
    }

}
