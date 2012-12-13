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
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;

/**
 * {@link DocumentModel} backed implementation of a {@link FileItem}.
 *
 * @author Antoine Taillefer
 */
public class DocumentBackedFileItem extends
        AbstractDocumentBackedFileSystemItem implements FileItem {

    public DocumentBackedFileItem(String factoryName, DocumentModel doc)
            throws ClientException {
        super(factoryName, doc);
    }

    /*--------------------- AbstractFileSystemItem ---------------------*/
    @Override
    public String getName() throws ClientException {
        DocumentModel doc = getDocument(getSession());
        return getFileName(doc);
    }

    @Override
    public boolean isFolder() {
        return false;
    }

    @Override
    public void rename(String name) throws ClientException {
        CoreSession session = getSession();
        DocumentModel doc = getDocument(session);
        BlobHolder bh = getBlobHolder(doc);
        Blob blob = getBlob(bh);
        // TODO: not sure about the behavior for the doc title
        String fileName = blob.getFilename();
        if (fileName.equals(doc.getPropertyValue("dc:title"))) {
            doc.setPropertyValue("dc:title", name);
        }
        blob.setFilename(name);
        bh.setBlob(blob);
        session.saveDocument(doc);
    }

    /*--------------------- FileItem -----------------*/
    @Override
    public Blob getBlob() throws ClientException {
        DocumentModel doc = getDocument(getSession());
        return getBlob(doc);
    }

    @Override
    public String getDownloadURL(String baseURL) throws ClientException {
        DocumentModel doc = getDocument(getSession());
        StringBuilder downloadURLSb = new StringBuilder();
        downloadURLSb.append(baseURL);
        downloadURLSb.append("nxbigfile/");
        downloadURLSb.append(repositoryName);
        downloadURLSb.append("/");
        downloadURLSb.append(docId);
        downloadURLSb.append("/");
        downloadURLSb.append("blobholder:0");
        downloadURLSb.append("/");
        downloadURLSb.append(URIUtils.quoteURIPathComponent(getFileName(doc),
                true));
        return downloadURLSb.toString();
    }

    @Override
    public void setBlob(Blob blob) throws ClientException {
        CoreSession session = getSession();
        DocumentModel doc = getDocument(session);
        BlobHolder bh = getBlobHolder(doc);
        // If blob's filename is empty, set it to the current blob's
        // filename
        if (StringUtils.isEmpty(blob.getFilename())) {
            blob.setFilename(getBlob(bh).getFilename());
        }
        bh.setBlob(blob);
        session.saveDocument(doc);
    }

    /*--------------------- Protected -----------------*/
    protected BlobHolder getBlobHolder(DocumentModel doc)
            throws ClientException {
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

    protected String getFileName(DocumentModel doc) throws ClientException {
        return getBlob(doc).getFilename();
    }

}
