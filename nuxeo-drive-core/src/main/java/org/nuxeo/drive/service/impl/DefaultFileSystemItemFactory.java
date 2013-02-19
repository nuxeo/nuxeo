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

import java.security.Principal;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.impl.AbstractFileSystemItem;
import org.nuxeo.drive.adapter.impl.DocumentBackedFileItem;
import org.nuxeo.drive.adapter.impl.DocumentBackedFolderItem;
import org.nuxeo.drive.service.FileSystemItemFactory;
import org.nuxeo.drive.service.FileSystemItemManager;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.model.NoSuchDocumentException;
import org.nuxeo.runtime.api.Framework;

/**
 * Default implementation of a {@link FileSystemItemFactory}. It is
 * {@link DocumentModel} backed and is the one used by Nuxeo Drive.
 *
 * @author Antoine Taillefer
 */
public class DefaultFileSystemItemFactory implements FileSystemItemFactory {

    private static final Log log = LogFactory.getLog(DefaultFileSystemItemFactory.class);

    protected String name;

    protected Map<String, String> parameters;

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
    public Map<String, String> getParameters() {
        return parameters;
    }

    @Override
    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    @Override
    public String getParameter(String name) {
        return parameters.get(name);
    }

    @Override
    public void setParameter(String name, String value) {
        parameters.put(name, value);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc)
            throws ClientException {
        return getFileSystemItem(doc, false, null, false);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc,
            boolean includeDeleted) throws ClientException {
        return getFileSystemItem(doc, false, null, includeDeleted);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, String parentId)
            throws ClientException {
        return getFileSystemItem(doc, true, parentId, false);
    }

    @Override
    public FileSystemItem getFileSystemItem(DocumentModel doc, String parentId,
            boolean inlcudeDeleted) throws ClientException {
        return getFileSystemItem(doc, true, parentId, inlcudeDeleted);
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

    /**
     * The default factory considers that a {@link FileSystemItem} with the
     * given id exists if:
     * <ul>
     * <li>The backing {@link DocumentModel} can be fetched</li>
     * <li>It is not in the 'deleted' life cycle state</li>
     * <li>It is folderish or it can be adapted as a {@link BlobHolder} with a
     * blob</li>
     * </ul>
     *
     * @see #isFileSystemItem(DocumentModel)
     */
    @Override
    public boolean exists(String id, Principal principal)
            throws ClientException {
        try {
            DocumentModel doc = getDocumentByFileSystemId(id, principal);
            boolean exists = isFileSystemItem(doc);
            if (!exists) {
                log.debug(String.format(
                        "Document %s cannot be adapted as a FileSystemItem => returning false.",
                        doc.getId()));
            }
            return exists;
        } catch (ClientException e) {
            if (e.getCause() instanceof NoSuchDocumentException) {
                log.debug(String.format(
                        "No doc related to id %s, returning false.", id));
                return false;
            } else {
                throw e;
            }
        }
    }

    @Override
    public FileSystemItem getFileSystemItemById(String id, Principal principal)
            throws ClientException {
        try {
            DocumentModel doc = getDocumentByFileSystemId(id, principal);
            return getFileSystemItem(doc);
        } catch (ClientException e) {
            if (e.getCause() instanceof NoSuchDocumentException) {
                log.debug(String.format(
                        "No doc related to id %s, returning null.", id));
                return null;
            } else {
                throw e;
            }
        }

    }

    /*--------------------------- Protected ---------------------------------*/
    protected FileSystemItem getFileSystemItem(DocumentModel doc,
            boolean forceParentId, String parentId, boolean includeDeleted)
            throws ClientException {
        if (!includeDeleted
                && LifeCycleConstants.DELETED_STATE.equals(doc.getCurrentLifeCycleState())) {
            log.debug(String.format(
                    "Document %s is in the '%s' life cycle state, it cannot be adapted as a FileSystemItem => returning null.",
                    doc.getId(), LifeCycleConstants.DELETED_STATE));
            return null;
        }
        if (doc.isFolder()) {
            if (forceParentId) {
                return new DocumentBackedFolderItem(name, parentId, doc);
            } else {
                return new DocumentBackedFolderItem(name, doc);
            }
        }
        if (hasBlob(doc)) {
            if (forceParentId) {
                return new DocumentBackedFileItem(name, parentId, doc,
                        parameters);
            } else {
                return new DocumentBackedFileItem(name, doc, parameters);
            }
        }
        log.debug(String.format(
                "Document %s is not Folderish nor a BlobHolder with a blob, it cannot be adapted as a FileSystemItem => returning null.",
                doc.getId()));
        return null;
    }

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
        // fileSystemItemFactoryName#repositoryName#docId
        String[] idFragments = id.split(AbstractFileSystemItem.FILE_SYSTEM_ITEM_ID_SEPARATOR);
        if (idFragments.length != 3) {
            throw new ClientException(
                    String.format(
                            "FileSystemItem id %s cannot be handled by factory named %s. Should match the 'fileSystemItemFactoryName#repositoryName#docId' pattern.",
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
            Principal principal) throws ClientException {
        // Parse id, expecting
        // pattern:fileSystemItemFactoryName#repositoryName#docId
        String[] idFragments = parseFileSystemId(id);
        String repositoryName = idFragments[1];
        String docId = idFragments[2];
        CoreSession session = Framework.getLocalService(
                FileSystemItemManager.class).getSession(repositoryName,
                principal);
        return getDocumentById(docId, session);
    }

    protected DocumentModel getDocumentById(String docId, CoreSession session)
            throws ClientException {
        return session.getDocument(new IdRef(docId));
    }

    protected boolean isFileSystemItem(DocumentModel doc)
            throws ClientException {
        if (LifeCycleConstants.DELETED_STATE.equals(doc.getCurrentLifeCycleState())) {
            log.debug(String.format(
                    "Document %s is in the '%s' life cycle state.",
                    doc.getId(), LifeCycleConstants.DELETED_STATE));
            return false;
        }
        if (!doc.isFolder() && !hasBlob(doc)) {
            log.debug(String.format(
                    "Document %s is not Folderish nor a BlobHolder with a blob.",
                    doc.getId()));
            return false;
        }
        return true;
    }

}
