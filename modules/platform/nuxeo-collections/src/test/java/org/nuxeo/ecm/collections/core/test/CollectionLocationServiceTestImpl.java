/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard <grenard@nuxeo.com>
 */
package org.nuxeo.ecm.collections.core.test;

import org.nuxeo.ecm.collections.api.CollectionLocationService;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.runtime.model.DefaultComponent;

public class CollectionLocationServiceTestImpl extends DefaultComponent implements CollectionLocationService {

    @Override
    public DocumentModel getUserDefaultCollectionsRoot(CoreSession session) {
        DocumentModel us = getUserSpace(session);
        DocumentModel collectionRoot = session.createDocumentModel(us.getPathAsString(), "Collections", "Collections");
        return session.getOrCreateDocument(collectionRoot);
    }

    @Override
    public DocumentModel getUserFavorites(CoreSession session) {
        DocumentModel us = getUserSpace(session);
        DocumentModel favorites = session.createDocumentModel(us.getPathAsString(), "Favorites", "Favorites");
        return session.getOrCreateDocument(favorites);
    }

    protected DocumentModel getUserSpace(CoreSession session) {
        DocumentModel userSpace = session.createDocumentModel("/", session.getPrincipal().getName(), "Folder");
        return CoreInstance.doPrivileged(session, s -> {
            return session.getOrCreateDocument(userSpace, doc -> initUserSpaceRoot(session, doc));
        });
    }

    protected DocumentModel initUserSpaceRoot(CoreSession session, DocumentModel doc) {
        ACP acp = new ACPImpl();
        ACE denyEverything = new ACE(SecurityConstants.EVERYONE, SecurityConstants.EVERYTHING, false);
        ACE allowEverything = new ACE(session.getPrincipal().getName(), SecurityConstants.EVERYTHING, true);
        ACL acl = new ACLImpl();
        acl.setACEs(new ACE[] { allowEverything, denyEverything });
        acp.addACL(acl);
        doc.setACP(acp, true);
        return doc;
    }

}
