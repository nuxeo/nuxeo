/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Vladimir Pasquier <vpasquier@nuxeo.com>
 * Laurent Doguin <ldoguin@nuxeo.com>
 *
 */
package org.nuxeo.ecm.platform.thumbnail;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailAdapter;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.platform.dublincore.listener.DublinCoreListener;

/**
 * Thumbnail bean in session unrestricted to add/update thumbnail facet to a
 * document and store doc thumbnail
 *
 * @since 5.7
 */
public class AddThumbnailUnrestricted extends UnrestrictedSessionRunner {

    private static final Log log = LogFactory.getLog(AddThumbnailUnrestricted.class);

    // duplicated from Audit module to avoid circular dependency
    public static final String DISABLE_AUDIT_LOGGER = "disableAuditLogger";

    protected DocumentModel doc;

    public AddThumbnailUnrestricted(CoreSession coreSession, DocumentModel doc,
            BlobHolder blobHolder) {
        super(coreSession);
        this.doc = doc;
    }

    @Override
    public void run() throws ClientException {
        try {
            ThumbnailAdapter thumbnailAdapter = doc.getAdapter(ThumbnailAdapter.class);
            if (thumbnailAdapter != null) {
                Blob thumbnailBlob = thumbnailAdapter.computeThumbnail(session);
                if (thumbnailBlob != null) {
                    if (!doc.hasFacet(ThumbnailConstants.THUMBNAIL_FACET)) {
                        doc.addFacet(ThumbnailConstants.THUMBNAIL_FACET);
                    }
                    if (!doc.isProxy() && !doc.isVersion()) {
                        doc.setPropertyValue(
                                ThumbnailConstants.THUMBNAIL_PROPERTY_NAME,
                                (Serializable) thumbnailBlob);
                        saveDocument();
                    }
                } else {
                    if (doc.hasFacet(ThumbnailConstants.THUMBNAIL_FACET)) {
                        if (!doc.isProxy() && !doc.isVersion()) {
                            doc.setPropertyValue(
                                    ThumbnailConstants.THUMBNAIL_PROPERTY_NAME,
                                    null);
                            doc.removeFacet(ThumbnailConstants.THUMBNAIL_FACET);
                            saveDocument();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error while adding thumbnail", e);
        }
    }

    protected void saveDocument() throws ClientException {
        doc.putContextData(VersioningService.DISABLE_AUTO_CHECKOUT,
                Boolean.TRUE);
        doc.putContextData(DublinCoreListener.DISABLE_DUBLINCORE_LISTENER,
                Boolean.TRUE);
        doc.putContextData(DISABLE_AUDIT_LOGGER, Boolean.TRUE);
        session.saveDocument(doc);
        session.save();
    }
}
