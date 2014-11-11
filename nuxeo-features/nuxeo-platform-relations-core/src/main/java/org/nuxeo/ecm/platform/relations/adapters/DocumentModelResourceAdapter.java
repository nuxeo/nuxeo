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

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
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
 *
 */
public class DocumentModelResourceAdapter extends AbstractResourceAdapter
        implements Serializable {

    private static final long serialVersionUID = -5307418102496342779L;

    @Override
    public Object getResourceRepresentation(Resource resource) {
        Object object = null;
        if (resource.isQNameResource()) {
            CoreInstance core = CoreInstance.getInstance();
            CoreSession session = null;
            try {
                RepositoryManager mgr = Framework.getService(RepositoryManager.class);
                Repository repo;
                String uid = null;
                String localName = ((QNameResource) resource).getLocalName();
                int index = localName.indexOf("/");
                if (index == -1) {
                    // BBB for when repository name was not included in the
                    // local name
                    repo = mgr.getDefaultRepository();
                    uid = localName;
                } else {
                    String repositoryName = localName.substring(0, index);
                    repo = mgr.getRepository(repositoryName);
                    uid = localName.substring(index + 1);
                }
                DocumentRef ref = new IdRef(uid);
                session = repo.open();
                object = session.getDocument(ref);
            } catch (ClientException e) {
            } catch (Exception e) {
            } finally {
                if (session != null) {
                    try {
                        core.close(session);
                    } catch (ClientException e) {
                    }
                }
            }
        }
        return object;
    }

    @Override
    public Resource getResource(Object object) {
        DocumentModel doc = (DocumentModel) object;
        String localName = doc.getRepositoryName() + '/' + doc.getId();
        return new QNameResourceImpl(namespace, localName);
    }

    @Override
    public Class<?> getKlass() {
        return DocumentModel.class;
    }

}
