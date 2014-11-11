/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.trash.ejb;

import java.security.Principal;
import java.util.List;
import java.util.Set;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.trash.TrashInfo;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.runtime.api.Framework;

@Stateless
@Remote(TrashService.class)
@Local(TrashServiceLocal.class)
public class TrashServiceBean implements TrashServiceLocal {

    protected final TrashService service;

    public TrashServiceBean() {
        // use the local runtime service as the backend
        service = Framework.getLocalService(TrashService.class);
    }

    public boolean canDelete(List<DocumentModel> docs, Principal principal,
            boolean checkProxies) throws ClientException {
        return service.canDelete(docs, principal, checkProxies);
    }

    public boolean canPurgeOrUndelete(List<DocumentModel> docs,
            Principal principal) throws ClientException {
        return service.canPurgeOrUndelete(docs, principal);
    }

    public boolean checkDeletePermOnParents(List<DocumentModel> docs)
            throws ClientException {
        return service.checkDeletePermOnParents(docs);
    }

    public boolean folderAllowsDelete(DocumentModel folder)
            throws ClientException {
        return service.folderAllowsDelete(folder);
    }

    public DocumentModel getAboveDocument(DocumentModel currentDocument,
            Set<Path> rootPaths) throws ClientException {
        return service.getAboveDocument(currentDocument, rootPaths);
    }

    public TrashInfo getTrashInfo(List<DocumentModel> docs,
            Principal principal, boolean checkProxies, boolean checkDeleted)
            throws ClientException {
        return service.getTrashInfo(docs, principal, checkProxies, checkDeleted);
    }

    public void purgeDocuments(CoreSession session, List<DocumentRef> docRefs)
            throws ClientException {
        service.purgeDocuments(session, docRefs);
    }

    public void trashDocuments(List<DocumentModel> docs) throws ClientException {
        service.trashDocuments(docs);
    }

    public Set<DocumentRef> undeleteDocuments(List<DocumentModel> docs)
            throws ClientException {
        return service.undeleteDocuments(docs);
    }

}
