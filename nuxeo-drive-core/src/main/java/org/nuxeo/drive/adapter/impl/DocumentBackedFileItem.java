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

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.drive.adapter.FileItem;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.impl.DefaultFileSystemItemFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.runtime.api.Framework;

/**
 * {@link DocumentModel} backed implementation of a {@link FileItem}.
 *
 * @author Antoine Taillefer
 */
public class DocumentBackedFileItem extends
        AbstractDocumentBackedFileSystemItem implements FileItem {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DocumentBackedFileItem.class);

    protected String downloadURL;

    protected String digestAlgorithm;

    protected String digest;

    protected boolean canUpdate;

    // Versioning delay in seconds, default value: 1 hour
    protected double versioningDelay = 3600;

    // Versioning option, default value: MINOR
    protected VersioningOption versioningOption = VersioningOption.MINOR;

    public DocumentBackedFileItem(String factoryName, DocumentModel doc,
            Map<String, String> params) throws ClientException {
        super(factoryName, doc);
        initialize(doc, params);
    }

    public DocumentBackedFileItem(String factoryName, String parentId,
            DocumentModel doc, Map<String, String> params)
            throws ClientException {
        super(factoryName, parentId, doc);
        initialize(doc, params);
    }

    protected DocumentBackedFileItem() {
        // Needed for JSON deserialization
    }

    /*--------------------- FileSystemItem ---------------------*/
    @Override
    public void rename(String name) throws ClientException {
        // Update doc properties
        CoreSession session = getSession();
        DocumentModel doc = getDocument(session);
        BlobHolder bh = getBlobHolder(doc);
        Blob blob = getBlob(bh);
        blob.setFilename(name);
        bh.setBlob(blob);
        updateDocTitleIfNeeded(doc, name);
        doc = session.saveDocument(doc);
        session.save();
        // Update FileSystemItem attributes
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
        versionIfNeeded(doc, session);
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
        updateDigest(doc);
    }

    /*--------------------- DocumentBackedFileItem -----------------*/
    @JsonIgnore
    public double getVersioningDelay() {
        return versioningDelay;
    }

    @JsonIgnore
    public VersioningOption getVersioningOption() {
        return versioningOption;
    }

    /*--------------------- Protected -----------------*/
    protected void initialize(DocumentModel doc, Map<String, String> params)
            throws ClientException {
        this.name = getFileName(doc);
        this.folder = false;
        updateDownloadURL();
        // TODO: should get the digest algorithm from the binary store
        // configuration, but it is not exposed as a public API for now
        this.digestAlgorithm = FileSystemItemHelper.MD5_DIGEST_ALGORITHM;
        updateDigest(doc);
        if (this.digest == null) {
            this.digestAlgorithm = null;
        }
        this.canUpdate = this.canRename;
        if (params != null) {
            String versioningDelayParam = params.get(DefaultFileSystemItemFactory.VERSIONING_DELAY_PARAM);
            if (!StringUtils.isEmpty(versioningDelayParam)) {
                versioningDelay = Double.parseDouble(versioningDelayParam);
            }
            String versioningOptionParam = params.get(DefaultFileSystemItemFactory.VERSIONING_OPTION_PARAM);
            if (!StringUtils.isEmpty(versioningOptionParam)) {
                versioningOption = VersioningOption.valueOf(versioningOptionParam);
            }
        }
    }

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
        String filename = getBlob(doc).getFilename();
        return filename != null ? filename : doc.getTitle();
    }

    protected void updateDocTitleIfNeeded(DocumentModel doc, String name)
            throws ClientException {
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
        downloadURLSb.append(URIUtils.quoteURIPathComponent(name, true));
        downloadURL = downloadURLSb.toString();
    }

    protected void updateDigest(DocumentModel doc) throws ClientException {
        Blob blob = getBlob(doc);
        // Force digest computation for a StringBlob,
        // typically the note:note property of a Note document
        digest = FileSystemItemHelper.getDigest(blob, digestAlgorithm);
    }

    protected void versionIfNeeded(DocumentModel doc, CoreSession session)
            throws ClientException {
        if (needsVersioning(doc)) {
            doc.putContextData(VersioningService.VERSIONING_OPTION,
                    versioningOption);
            session.saveDocument(doc);
        }
    }

    /**
     * Need to version the doc if the current contributor is different from the
     * last contributor or if the last modification was done more than
     * {@link #versioningDelay} seconds ago.
     */
    protected boolean needsVersioning(DocumentModel doc)
            throws PropertyException, ClientException {
        //
        String lastContributor = (String) doc.getPropertyValue("dc:lastContributor");
        boolean contributorChanged = !principal.getName().equals(
                lastContributor);
        if (contributorChanged) {
            log.debug(String.format(
                    "Contributor %s is different from the last contributor %s => will create a version of the document.",
                    principal.getName(), lastContributor));
            return true;
        }
        if (getLastModificationDate() == null) {
            log.debug("Last modification date is null => will not create a version of the document.");
            return true;
        }
        long lastModified = System.currentTimeMillis()
                - getLastModificationDate().getTimeInMillis();
        long versioningDelayMillis = (long) versioningDelay * 1000;
        if (lastModified > versioningDelayMillis) {
            log.debug(String.format(
                    "Last modification was done %d milliseconds ago, this is more than the versioning delay %d milliseconds => will create a version of the document.",
                    lastModified, versioningDelayMillis));
            return true;
        }
        log.debug(String.format(
                "Contributor %s is the last contributor and last modification was done %d milliseconds ago, this is less than the versioning delay %d milliseconds => will not create a version of the document.",
                principal.getName(), lastModified, versioningDelayMillis));
        return false;
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
