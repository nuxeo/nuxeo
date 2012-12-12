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

import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
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

    protected final DocumentModel doc;

    protected AbstractDocumentBackedFileSystemItem(String factoryName,
            DocumentModel doc) {
        super(factoryName);
        this.doc = doc;
    }

    /*--------------------- FileSystemItem ---------------------*/
    public String getId() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.getId());
        sb.append("/");
        sb.append(doc.getRepositoryName());
        sb.append("/");
        sb.append(doc.getId());
        return sb.toString();
    }

    public String getCreator() throws ClientException {
        return (String) doc.getPropertyValue("dc:creator");
    }

    public Calendar getCreationDate() throws ClientException {
        return (Calendar) doc.getPropertyValue("dc:created");
    }

    public Calendar getLastModificationDate() throws ClientException {
        return (Calendar) doc.getPropertyValue("dc:modified");
    }

    public void delete() throws ClientException {
        List<DocumentModel> docs = new ArrayList<DocumentModel>();
        // docs.add(getDocumentByFileSystemId(id, session));
        docs.add(doc);
        getTrashService().trashDocuments(docs);
    }

    public DocumentModel getDocument() {
        return doc;
    }

    /*--------------------- Protected -----------------*/
    protected CoreSession getCoreSession() {
        return doc.getCoreSession();
    }

    protected FileManager getFileManager() {
        return Framework.getLocalService(FileManager.class);
    }

    protected TrashService getTrashService() {
        return Framework.getLocalService(TrashService.class);
    }

}
