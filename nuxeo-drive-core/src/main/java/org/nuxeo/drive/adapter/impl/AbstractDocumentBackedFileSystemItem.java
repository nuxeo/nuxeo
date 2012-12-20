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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.nuxeo.drive.adapter.FileSystemItem;
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

    /** Backing {@link DocumentModel} attributes */
    protected final String repositoryName;

    protected final String docId;

    protected final String docPath;

    protected String docTitle;

    protected AbstractDocumentBackedFileSystemItem(String factoryName,
            DocumentModel doc) throws ClientException {
        this(factoryName, null, doc);
        CoreSession docSession = doc.getCoreSession();
        DocumentModel parentDoc = docSession.getParentDocument(doc.getRef());
        if (parentDoc == null) {
            throw new UnsupportedOperationException(
                    String.format(
                            "Doc %s has no parent document, please provide a FileSystemItem parentId to the constructor.",
                            doc.getPathAsString()));
        } else {
            // Pass a mock parent id when fetching the parent file system item
            // to avoid recursive calls
            this.parentId = getFileSystemItemAdapterService().getFileSystemItem(
                    parentDoc, "mockParentId").getId();
        }
    }

    protected AbstractDocumentBackedFileSystemItem(String factoryName,
            String parentId, DocumentModel doc) throws ClientException {

        super(factoryName, doc.getCoreSession().getPrincipal());

        // Backing DocumentModel attributes
        repositoryName = doc.getRepositoryName();
        docId = doc.getId();
        docPath = doc.getPathAsString();
        docTitle = doc.getTitle();

        // FileSystemItem attributes
        id = computeId(docId);
        this.parentId = parentId;
        creator = (String) doc.getPropertyValue("dc:creator");
        creationDate = (Calendar) doc.getPropertyValue("dc:created");
        lastModificationDate = (Calendar) doc.getPropertyValue("dc:modified");
    }

    /*--------------------- FileSystemItem ---------------------*/
    public void delete() throws ClientException {
        List<DocumentModel> docs = new ArrayList<DocumentModel>();
        DocumentModel doc = getDocument(getSession());
        docs.add(doc);
        getTrashService().trashDocuments(docs);
    }

    /*--------------------- AbstractDocumentBackedFileSystemItem -----------------*/
    @JsonIgnore
    public CoreSession getSession() throws ClientException {
        return getSession(repositoryName);
    }

    /*--------------------- Protected -------------------------*/
    protected String computeId(String docId) {
        StringBuilder sb = new StringBuilder();
        sb.append(super.getId());
        sb.append(repositoryName);
        sb.append("/");
        sb.append(docId);
        return sb.toString();
    }

    protected DocumentModel getDocument(CoreSession session)
            throws ClientException {
        return session.getDocument(new IdRef(docId));
    }

    protected TrashService getTrashService() {
        return Framework.getLocalService(TrashService.class);
    }

}
