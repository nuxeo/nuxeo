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
package org.nuxeo.drive.service.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.impl.DocumentBackedFileItem;
import org.nuxeo.drive.adapter.impl.DocumentBackedFolderItem;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.model.NoSuchDocumentException;

/**
 * Default implementation of a {@link FileSystemItemFactory}. It is
 * {@link DocumentModel} backed and is the one used by Nuxeo Drive.
 *
 * @author Antoine Taillefer
 */
public class DefaultFileSystemItemFactory implements FileSystemItemFactory {

    private static final Log log = LogFactory.getLog(DefaultFileSystemItemFactory.class);

    protected String name;

    /**
     * Prevent from instantiating class as it should only be done by
     * {@link FileSystemItemFactoryDescriptor#getFactory()}.
     */
    protected DefaultFileSystemItemFactory() {
    }

    /*--------------------------- FileSystemItemFactory ---------------------------------*/
    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc)
            throws ClientException {
        if (doc.isFolder()) {
            return new DocumentBackedFolderItem(getName(), doc);
        }
        if (hasBlob(doc)) {
            return new DocumentBackedFileItem(getName(), doc);
        }
        log.debug(String.format(
                "Document %s is not Folderish nor a BlobHolder with a blob, it cannot be adapted as a FileSystemItem => returning null.",
                doc.getId()));
        return null;
    }

    @Override
    public boolean canHandleFileSystemItemId(String id) {
        try {
            parseFileSystemId(id);
        } catch (ClientException e) {
            log.debug(e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public boolean exists(String id, CoreSession session)
            throws ClientException {
        try {
            DocumentModel doc = getDocumentByFileSystemId(id, session);
            return doc.isFolder() || hasBlob(doc);
        } catch (ClientException e) {
            if (e.getCause() instanceof NoSuchDocumentException) {
                return false;
            } else {
                throw e;
            }
        }
    }

    @Override
    public FileSystemItem getFileSystemItemById(String id, CoreSession session)
            throws ClientException {
        DocumentModel doc = getDocumentByFileSystemId(id, session);
        return getFileSystemItem(doc);
    }

    /*--------------------------- Protected ---------------------------------*/
    protected boolean hasBlob(DocumentModel doc) throws ClientException {
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        if (bh == null) {
            log.debug(String.format("Document %s is not a BlobHolder.",
                    doc.getId()));
            return false;
        }
        Blob blob = bh.getBlob();
        if (blob == null) {
            log.debug(String.format(
                    "Document %s is a BlobHolder without a blob.", doc.getId()));
            return false;
        }
        return true;
    }

    protected String[] parseFileSystemId(String id) throws ClientException {

        // Parse id, expecting pattern:
        // fileSystemItemFactoryName/repositoryName/docId
        String[] idFragments = id.split("/");
        if (idFragments.length != 3) {
            throw new ClientException(
                    String.format(
                            "FileSystemItem id %s cannot be handled by factory named %s. Should match the 'fileSystemItemFactoryName/repositoryName/docId' pattern.",
                            id, name));
        }

        // Check if factory name matches
        String factoryName = idFragments[0];
        if (!name.equals(factoryName)) {
            throw new ClientException(
                    String.format(
                            "Factoy name [%s] parsed from id %s does not match the actual factory name [%s].",
                            factoryName, id, name));
        }
        return idFragments;
    }

    protected DocumentModel getDocumentByFileSystemId(String id,
            CoreSession session) throws ClientException {

        // Parse id, expecting
        // pattern:fileSystemItemFactoryName/repositoryName/docId
        String[] idFragments = parseFileSystemId(id);
        String repositoryName = idFragments[1];
        String docId = idFragments[2];

        // Fetch document using the appropriate session
        CoreSession repoSession = session;
        if (!repositoryName.equals(session.getRepositoryName())) {
            Map<String, Serializable> context = new HashMap<String, Serializable>();
            context.put("principal", (Serializable) session.getPrincipal());
            repoSession = CoreInstance.getInstance().open(repositoryName,
                    context);
            try {
                return getDocumentById(docId, repoSession);
            } finally {
                CoreInstance.getInstance().close(repoSession);
            }
        } else {
            return getDocumentById(docId, repoSession);
        }
    }

    protected DocumentModel getDocumentById(String docId, CoreSession session)
            throws ClientException {
        return session.getDocument(new IdRef(docId));
    }

}
