/*
 * (C) Copyright 2010-2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *   Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.rendition.service;

import static org.nuxeo.ecm.platform.rendition.Constants.FILES_FILES_PROPERTY;
import static org.nuxeo.ecm.platform.rendition.Constants.FILES_SCHEMA;
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_FACET;
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_NAME_PROPERTY;
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_SOURCE_ID_PROPERTY;
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_SOURCE_MODIFICATION_DATE_PROPERTY;
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY;
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_VARIANT_PROPERTY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.DocumentStringBlobHolder;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class RenditionCreator extends UnrestrictedSessionRunner {

    private static final Log log = LogFactory.getLog(RenditionCreator.class);

    public static final String FILE = "File";

    protected DocumentModel detachedRendition;

    protected String liveDocumentId;

    protected String versionDocumentId;

    protected Blob renditionBlob;

    protected String renditionName;

    /**
     * @since 8.1
     */
    protected RenditionDefinition renditionDefinition;

    /**
     * @since 8.1
     */
    protected final String renditionVariant;

    /**
     * @since 8.1
     */
    public RenditionCreator(DocumentModel liveDocument, DocumentModel versionDocument, Blob renditionBlob,
            RenditionDefinition renditionDefinition) {
        super(liveDocument.getCoreSession());
        liveDocumentId = liveDocument.getId();
        versionDocumentId = versionDocument == null ? null : versionDocument.getId();
        this.renditionBlob = renditionBlob;
        this.renditionDefinition = renditionDefinition;
        renditionName = renditionDefinition.getName();
        renditionVariant = renditionDefinition.getProvider().getVariant(liveDocument, renditionDefinition);
    }

    public DocumentModel getDetachedRendition() {
        return detachedRendition;
    }

    /**
     * @deprecated since 7.10, misspelled, use {@link #getDetachedRendition} instead.
     */
    @Deprecated
    public DocumentModel getDetachedDendition() {
        return detachedRendition;
    }

    @Override
    public void run() {
        DocumentModel liveDocument = session.getDocument(new IdRef(liveDocumentId));
        DocumentModel sourceDocument = liveDocument.isVersionable() ? session.getDocument(new IdRef(versionDocumentId))
                : liveDocument;
        DocumentModel rendition = createRenditionDocument(sourceDocument);
        removeBlobs(rendition);
        updateMainBlob(rendition);
        updateIconField(rendition);

        // create a copy of the doc
        if (rendition.getId() == null) {
            rendition = session.createDocument(rendition);
        }
        if (sourceDocument.isVersionable()) {
            // be sure to have the same version info
            setCorrectVersion(rendition, sourceDocument);
        }
        // do not apply default versioning to rendition
        rendition.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.NONE);
        rendition = session.saveDocument(rendition);

        if (sourceDocument.isVersionable()) {
            // rendition is checked out: check it in
            DocumentRef renditionRef = rendition.checkIn(VersioningOption.NONE, null);
            rendition = session.getDocument(renditionRef);
        }
        session.save();

        rendition.detach(true);
        detachedRendition = rendition;
    }

    protected DocumentModel createRenditionDocument(DocumentModel sourceDocument) {
        String doctype = sourceDocument.getType();
        String renditionMimeType = renditionBlob.getMimeType();
        BlobHolder blobHolder = sourceDocument.getAdapter(BlobHolder.class);
        if (blobHolder == null || (blobHolder instanceof DocumentStringBlobHolder
                && !(renditionMimeType.startsWith("text/") || renditionMimeType.startsWith("application/xhtml")))) {
            // We have a document type unable to hold blobs, or
            // We have a Note or other blob holder that can only hold strings, but the rendition is not a string-related
            // MIME type.
            // In either case, we'll have to create a File to hold it.
            doctype = FILE;
        }

        boolean isVersionable = sourceDocument.isVersionable();
        String liveDocProp = isVersionable ? RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY : RENDITION_SOURCE_ID_PROPERTY;
        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM Document WHERE ecm:isProxy = 0 AND ");
        query.append(RENDITION_NAME_PROPERTY);
        query.append(" = '");
        query.append(NXQL.escapeStringInner(renditionName));
        query.append("' AND ");
        if (renditionVariant != null) {
            query.append(RENDITION_VARIANT_PROPERTY);
            query.append(" = '");
            query.append(NXQL.escapeStringInner(renditionVariant));
            query.append("' AND ");
        }
        query.append(liveDocProp);
        query.append(" = '");
        query.append(liveDocumentId);
        query.append("'");
        DocumentModelList existingRenditions = session.query(query.toString());
        String modificationDatePropertyName = getSourceDocumentModificationDatePropertyName();
        Calendar sourceLastModified = (Calendar) sourceDocument.getPropertyValue(modificationDatePropertyName);
        DocumentModel rendition;
        if (existingRenditions.size() > 0) {
            rendition = session.getDocument(existingRenditions.get(0).getRef());
            if (!isVersionable) {
                Calendar renditionSourceLastModified = (Calendar) rendition.getPropertyValue(
                        RENDITION_SOURCE_MODIFICATION_DATE_PROPERTY);
                if (renditionSourceLastModified != null && !renditionSourceLastModified.before(sourceLastModified)) {
                    this.renditionBlob = (Blob) rendition.getPropertyValue("file:content");
                    return rendition;
                }
            }
            if (rendition.isVersion()) {
                String sid = rendition.getVersionSeriesId();
                rendition = session.getDocument(new IdRef(sid));
            }
        } else {
            rendition = session.createDocumentModel(null, sourceDocument.getName(), doctype);
        }

        rendition.copyContent(sourceDocument);
        rendition.getContextData().putScopedValue(LifeCycleConstants.INITIAL_LIFECYCLE_STATE_OPTION_NAME,
                sourceDocument.getCurrentLifeCycleState());

        rendition.addFacet(RENDITION_FACET);
        rendition.setPropertyValue(RENDITION_SOURCE_ID_PROPERTY, sourceDocument.getId());
        if (isVersionable) {
            rendition.setPropertyValue(RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY, liveDocumentId);
        }
        if (sourceLastModified != null) {
            rendition.setPropertyValue(RENDITION_SOURCE_MODIFICATION_DATE_PROPERTY, sourceLastModified);
        }
        if (renditionVariant != null) {
            rendition.setPropertyValue(RENDITION_VARIANT_PROPERTY, renditionVariant);
        }
        rendition.setPropertyValue(RENDITION_NAME_PROPERTY, renditionName);

        return rendition;
    }

    protected void removeBlobs(DocumentModel rendition) {
        if (rendition.hasSchema(FILES_SCHEMA)) {
            rendition.setPropertyValue(FILES_FILES_PROPERTY, new ArrayList<Map<String, Serializable>>());
        }
    }

    protected void updateMainBlob(DocumentModel rendition) {
        BlobHolder bh = rendition.getAdapter(BlobHolder.class);
        bh.setBlob(renditionBlob);
    }

    private void updateIconField(DocumentModel rendition) {
        if (!rendition.hasSchema("common")) {
            return;
        }
        MimetypeRegistry mimetypeService;
        try {
            mimetypeService = Framework.getService(MimetypeRegistry.class);
        } catch (Exception e) {
            log.error("Cannot fetch Mimetype service when updating icon and file size rendition", e);
            return;
        }
        MimetypeEntry mimetypeEntry = mimetypeService.getMimetypeEntryByMimeType(renditionBlob.getMimeType());
        if (mimetypeEntry != null && mimetypeEntry.getIconPath() != null) {
            rendition.setPropertyValue("common:icon", "/icons/" + mimetypeEntry.getIconPath());
        }
    }

    protected void setCorrectVersion(DocumentModel rendition, DocumentModel versionDocument) {
        Long minorVersion = (Long) versionDocument.getPropertyValue("uid:minor_version");
        rendition.setPropertyValue("uid:minor_version", minorVersion);
        rendition.setPropertyValue("uid:major_version", versionDocument.getPropertyValue("uid:major_version"));
    }

    protected String getSourceDocumentModificationDatePropertyName() {
        return renditionDefinition.getSourceDocumentModificationDatePropertyName();
    }

}
