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

import org.apache.commons.lang.StringUtils;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.drive.adapter.FileItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.VersioningFileSystemItemFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.runtime.api.Framework;

/**
 * {@link DocumentModel} backed implementation of a {@link FileItem}.
 *
 * @author Antoine Taillefer
 */
public class DocumentBackedFileItem extends AbstractDocumentBackedFileSystemItem implements FileItem {

    private static final long serialVersionUID = 1L;

    protected String downloadURL;

    protected String digestAlgorithm;

    protected String digest;

    protected boolean canUpdate;

    protected VersioningFileSystemItemFactory factory;

    public DocumentBackedFileItem(VersioningFileSystemItemFactory factory, DocumentModel doc) throws ClientException {
        this(factory, doc, false);
    }

    public DocumentBackedFileItem(VersioningFileSystemItemFactory factory, DocumentModel doc,
            boolean relaxSyncRootConstraint) throws ClientException {
        this(factory, doc, relaxSyncRootConstraint, true);
    }

    public DocumentBackedFileItem(VersioningFileSystemItemFactory factory, DocumentModel doc,
            boolean relaxSyncRootConstraint, boolean getLockInfo) throws ClientException {
        super(factory.getName(), doc, relaxSyncRootConstraint, getLockInfo);
        initialize(factory, doc);
    }

    public DocumentBackedFileItem(VersioningFileSystemItemFactory factory, FolderItem parentItem, DocumentModel doc)
            throws ClientException {
        this(factory, parentItem, doc, false);
    }

    public DocumentBackedFileItem(VersioningFileSystemItemFactory factory, FolderItem parentItem, DocumentModel doc,
            boolean relaxSyncRootConstraint) throws ClientException {
        this(factory, parentItem, doc, relaxSyncRootConstraint, true);
    }

    public DocumentBackedFileItem(VersioningFileSystemItemFactory factory, FolderItem parentItem, DocumentModel doc,
            boolean relaxSyncRootConstraint, boolean getLockInfo) throws ClientException {
        super(factory.getName(), parentItem, doc, relaxSyncRootConstraint, getLockInfo);
        initialize(factory, doc);
    }

    protected DocumentBackedFileItem() {
        // Needed for JSON deserialization
    }

    /*--------------------- FileSystemItem ---------------------*/
    @Override
    public void rename(String name) throws ClientException {
        /* Update doc properties */
        CoreSession session = getSession();
        DocumentModel doc = getDocument(session);
        // Handle versioning
        FileSystemItemHelper.versionIfNeeded(factory, doc, session);
        BlobHolder bh = getBlobHolder(doc);
        Blob blob = getBlob(bh);
        blob.setFilename(name);
        bh.setBlob(blob);
        updateDocTitleIfNeeded(doc, name);
        doc = session.saveDocument(doc);
        session.save();
        /* Update FileSystemItem attributes */
        this.name = name;
        updateDownloadURL();
        updateLastModificationDate(doc);
    }

    /*--------------------- FileItem -----------------*/
    @Override
    public Blob getBlob() throws ClientException {
        DocumentModel doc = getDocument(getSession());
        return getBlob(doc);
    }

    @Override
    public String getDownloadURL() throws ClientException {
        return downloadURL;
    }

    @Override
    public String getDigestAlgorithm() {
        return digestAlgorithm;
    }

    @Override
    public String getDigest() {
        return digest;
    }

    @Override
    public boolean getCanUpdate() {
        return canUpdate;
    }

    @Override
    public void setBlob(Blob blob) throws ClientException {
        /* Update doc properties */
        CoreSession session = getSession();
        DocumentModel doc = getDocument(session);
        // Handle versioning
        FileSystemItemHelper.versionIfNeeded(factory, doc, session);
        // If blob's filename is empty, set it to the current name
        String blobFileName = blob.getFilename();
        if (StringUtils.isEmpty(blobFileName)) {
            blob.setFilename(name);
        } else {
            updateDocTitleIfNeeded(doc, blobFileName);
            name = blobFileName;
            updateDownloadURL();
        }
        BlobHolder bh = getBlobHolder(doc);
        bh.setBlob(blob);
        doc = session.saveDocument(doc);
        session.save();
        /* Update FileSystemItem attributes */
        updateLastModificationDate(doc);
        updateDigest(getBlob(doc));
    }

    /*--------------------- Protected -----------------*/
    protected final void initialize(VersioningFileSystemItemFactory factory, DocumentModel doc) throws ClientException {
        this.factory = factory;
        Blob blob = getBlob(doc);
        name = getFileName(blob, doc.getTitle());
        folder = false;
        updateDownloadURL();
        updateDigest(blob);
        if (digest == null) {
            digestAlgorithm = null;
        }
        canUpdate = canRename;
    }

    protected BlobHolder getBlobHolder(DocumentModel doc) throws ClientException {
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        if (bh == null) {
            throw new ClientException(
                    String.format(
                            "Document %s is not a BlobHolder, it is not adaptable as a FileItem and therefore it cannot not be part of the items to synchronize.",
                            doc.getId()));
        }
        return bh;
    }

    protected Blob getBlob(BlobHolder blobHolder) throws ClientException {
        Blob blob = blobHolder.getBlob();
        if (blob == null) {
            throw new ClientException(
                    "Document has no blob, it is not adaptable as a FileItem and therefore it cannot not be part of the items to synchronize.");
        }
        return blob;
    }

    protected Blob getBlob(DocumentModel doc) throws ClientException {
        BlobHolder bh = getBlobHolder(doc);
        return getBlob(bh);
    }

    protected String getFileName(Blob blob, String docTitle) throws ClientException {
        String filename = blob.getFilename();
        return filename != null ? filename : docTitle;
    }

    protected void updateDocTitleIfNeeded(DocumentModel doc, String name) throws ClientException {
        // TODO: not sure about the behavior for the doc title
        if (this.name.equals(docTitle)) {
            doc.setPropertyValue("dc:title", name);
            docTitle = name;
        }
    }

    protected void updateDownloadURL() throws ClientException {
        StringBuilder downloadURLSb = new StringBuilder();
        downloadURLSb.append("nxbigfile/");
        downloadURLSb.append(repositoryName);
        downloadURLSb.append("/");
        downloadURLSb.append(docId);
        downloadURLSb.append("/");
        downloadURLSb.append("blobholder:0");
        downloadURLSb.append("/");
        // Remove chars that are invalid in filesystem names
        String escapedFilename = name.replaceAll("(/|\\\\|\\*|<|>|\\?|\"|:|\\|)", "-");
        downloadURLSb.append(URIUtils.quoteURIPathComponent(escapedFilename, true));
        downloadURL = downloadURLSb.toString();
    }

    protected void updateDigest(Blob blob) throws ClientException {
        String blobDigest = blob.getDigest();
        if (StringUtils.isEmpty(blobDigest)) {
            // Force md5 digest algorithm and digest computation for a StringBlob,
            // typically the note:note property of a Note document
            digestAlgorithm = FileSystemItemHelper.MD5_DIGEST_ALGORITHM;
            digest = FileSystemItemHelper.getMD5Digest(blob);
        } else {
            digestAlgorithm = blob.getDigestAlgorithm();
            digest = blobDigest;
        }
    }

    protected NuxeoDriveManager getNuxeoDriveManager() {
        return Framework.getLocalService(NuxeoDriveManager.class);
    }

    /*---------- Needed for JSON deserialization ----------*/
    protected void setDownloadURL(String downloadURL) {
        this.downloadURL = downloadURL;
    }

    protected void setDigestAlgorithm(String digestAlgorithm) {
        this.digestAlgorithm = digestAlgorithm;
    }

    protected void setDigest(String digest) {
        this.digest = digest;
    }

    protected void setCanUpdate(boolean canUpdate) {
        this.canUpdate = canUpdate;
    }

}
