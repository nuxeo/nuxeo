/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.adapter.impl;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.service.FileSystemItemManager;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.runtime.api.Framework;

/**
 * {@link DocumentModel} backed implementation of a {@link FileSystemItem}.
 *
 * @author Antoine Taillefer
 * @see DocumentBackedFileItem
 * @see DocumentBackedFolderItem
 */
public abstract class AbstractDocumentBackedFileSystemItem extends
        AbstractFileSystemItem {

    protected final String repositoryName;

    protected final Principal principal;

    protected final String docId;

    protected final String docPath;

    protected final String creator;

    protected final Calendar created;

    protected final Calendar lastModificationDate;

    protected AbstractDocumentBackedFileSystemItem(String factoryName,
            DocumentModel doc) throws ClientException {
        super(factoryName);
        repositoryName = doc.getRepositoryName();
        principal = doc.getCoreSession().getPrincipal();
        docId = doc.getId();
        docPath = doc.getPathAsString();
        creator = (String) doc.getPropertyValue("dc:creator");
        created = (Calendar) doc.getPropertyValue("dc:created");
        lastModificationDate = (Calendar) doc.getPropertyValue("dc:modified");
    }

    /*--------------------- FileSystemItem ---------------------*/
    public String getId() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.getId());
        sb.append("/");
        sb.append(repositoryName);
        sb.append("/");
        sb.append(docId);
        return sb.toString();
    }

    public String getCreator() {
        return creator;
    }

    public Calendar getCreationDate() {
        return created;
    }

    public Calendar getLastModificationDate() {
        return lastModificationDate;
    }

    public void delete() throws ClientException {
        List<DocumentModel> docs = new ArrayList<DocumentModel>();
        DocumentModel doc = getDocument(getSession());
        docs.add(doc);
        getTrashService().trashDocuments(docs);
    }

    /*--------------------- AbstractDocumentBackedFileSystemItem -----------------*/
    public CoreSession getSession() throws ClientException {
        return Framework.getLocalService(FileSystemItemManager.class).getSession(
                repositoryName, principal);
    }

    /*--------------------- Protected -------------------------*/
    protected DocumentModel getDocument(CoreSession session)
            throws ClientException {
        return session.getDocument(new IdRef(docId));
    }

    protected TrashService getTrashService() {
        return Framework.getLocalService(TrashService.class);
    }

}
