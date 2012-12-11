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
import org.nuxeo.drive.adapter.AbstractDocumentBackedFileSystemItem;
import org.nuxeo.drive.adapter.FileItem;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;

/**
 * {@link DocumentModel} backed implementation of a {@link FileItem}.
 *
 * @author Antoine Taillefer
 */
public class DocumentBackedFileItem extends
        AbstractDocumentBackedFileSystemItem implements FileItem {

    public DocumentBackedFileItem(DocumentModel doc) {
        super(doc);
    }

    /*--------------------- AbstractDocumentBackedFileSystemItem ---------------------*/
    @Override
    public String getName() throws ClientException {
        return getFileName();
    }

    @Override
    public boolean isFolder() {
        return false;
    }

    @Override
    public void rename(String name) throws ClientException {
        // TODO: not sure about the behavior for the doc title
        Blob blob = getBlob();
        if (doc.getTitle().equals(blob.getFilename())) {
            doc.setPropertyValue("dc:title", name);
        }
        blob.setFilename(name);
        getBlobHolder().setBlob(blob);
        getCoreSession().saveDocument(doc);
    }

    /*--------------------- FileItem -----------------*/
    @Override
    public Blob getBlob() throws ClientException {
        Blob blob = getBlobHolder().getBlob();
        if (blob == null) {
            throw new ClientException(
                    "Document has no blob, it is not adaptable as a FileItem and therefore it cannot not be part of the items to synchronize.");
        }
        return blob;
    }

    @Override
    public String getDownloadURL(String baseURL) throws ClientException {
        StringBuilder downloadURLSb = new StringBuilder();
        downloadURLSb.append(baseURL);
        downloadURLSb.append("nxbigfile/");
        downloadURLSb.append(doc.getRepositoryName());
        downloadURLSb.append("/");
        downloadURLSb.append(doc.getRef().toString());
        downloadURLSb.append("/");
        downloadURLSb.append("blobholder:0");
        downloadURLSb.append("/");
        downloadURLSb.append(URIUtils.quoteURIPathComponent(getFileName(), true));
        return downloadURLSb.toString();
    }

    @Override
    public void setBlob(Blob blob) throws ClientException {
        // If blob's filename is empty, set it to the current blob's filename
        if (StringUtils.isEmpty(blob.getFilename())) {
            blob.setFilename(getBlob().getFilename());
        }
        getBlobHolder().setBlob(blob);
        getCoreSession().saveDocument(doc);
    }

    /*--------------------- Protected -----------------*/
    protected String getFileName() throws ClientException {
        return getBlob().getFilename();
    }

    protected BlobHolder getBlobHolder() throws ClientException {
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        if (bh == null) {
            throw new ClientException(
                    String.format(
                            "Document %s is not a BlobHolder, it is not adaptable as a FileItem and therefore it cannot not be part of the items to synchronize.",
                            doc.getId()));
        }
        return bh;
    }

}
