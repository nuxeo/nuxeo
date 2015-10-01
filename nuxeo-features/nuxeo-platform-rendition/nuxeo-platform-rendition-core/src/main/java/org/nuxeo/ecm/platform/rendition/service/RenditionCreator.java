/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.rendition.service;

import static org.nuxeo.ecm.platform.rendition.Constants.FILES_FILES_PROPERTY;
import static org.nuxeo.ecm.platform.rendition.Constants.FILES_SCHEMA;
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_FACET;
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_NAME_PROPERTY;
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_SOURCE_ID_PROPERTY;
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.DocumentStringBlobHolder;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
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

    protected DocumentRef renditionRef;

    protected DocumentModel detachedRendition;

    protected DocumentRef liveDocumentRef;

    protected DocumentRef versionDocumentRef;

    protected Blob renditionBlob;

    protected String renditionName;

    public RenditionCreator(CoreSession session, DocumentModel liveDocument, DocumentModel versionDocument,
            Blob renditionBlob, String renditionName) {
        super(session);
        liveDocumentRef = liveDocument.getRef();
        versionDocumentRef = (versionDocument == null) ? null : versionDocument.getRef();
        this.renditionBlob = renditionBlob;
        this.renditionName = renditionName;
    }

    public DocumentRef getRenditionDocumentRef() {
        return renditionRef;
    }

    public DocumentModel getDetachedRendition() {
        return detachedRendition;
    }

    @Deprecated
    public DocumentModel getDetachedDendition() {
        return detachedRendition;
    }

    @Override
    public void run() {
        DocumentModel liveDocument = session.getDocument(liveDocumentRef);
        DocumentModel sourceDocument = (liveDocument.isVersionable())
                ? session.getDocument(versionDocumentRef) : liveDocument;
        DocumentModel rendition = createRenditionDocument(sourceDocument);
        removeBlobs(rendition);
        updateMainBlob(rendition);
        updateIconAndSizeFields(rendition);

        // create a copy of the doc
        if (rendition.getId() == null) {
            rendition = session.createDocument(rendition);
        }
        if (sourceDocument.isVersionable()) {
            // be sure to have the same version info
            setCorrectVersion(rendition, sourceDocument);
        } else {
            // set ACL
            giveReadRightToUser(rendition);
        }
        // do not apply default versioning to rendition
        rendition.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.NONE);
        rendition = session.saveDocument(rendition);

        if (sourceDocument.isVersionable()) {
            // rendition is checkout : make it checkin
            renditionRef = rendition.checkIn(VersioningOption.NONE, null);
            rendition = session.getDocument(renditionRef);
        }
        session.save();

        rendition.detach(true);
        detachedRendition = rendition;
    }

    protected DocumentModel createRenditionDocument(DocumentModel sourceDocument) {
        String doctype = sourceDocument.getType();
        String renditionMimeType = renditionBlob.getMimeType();
        boolean isSourceFolder = sourceDocument.isFolder();
        if (isSourceFolder
                || (sourceDocument.getAdapter(BlobHolder.class) instanceof DocumentStringBlobHolder
                        && !(renditionMimeType.startsWith("text/")
                                || renditionMimeType.startsWith("application/xhtml")))) {
            // We have a Folder or
            // We have a Note or other blob holder that can only hold strings, but the rendition is not a string-related
            // MIME type.
            // In either case, we'll have to create a File to hold it.
            doctype = FILE;
        }

        DocumentModel rendition = null;
        String renditionSourcePropertyName = (isSourceFolder)
                ? RENDITION_SOURCE_ID_PROPERTY : RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY;
        String liveDocumentId = session.getDocument(liveDocumentRef).getId();
        DocumentModelList existingRenditions;
        try (CoreSession userSession = CoreInstance.openCoreSession(session.getRepositoryName(),
                getOriginatingUsername())) {
            existingRenditions = userSession.query("select * from  " + doctype
                    + " where ecm:isProxy = 0 AND ecm:mixinType ='" + RENDITION_FACET + "' AND "
                    + renditionSourcePropertyName + "='" + liveDocumentId + "' AND "
                    + RENDITION_NAME_PROPERTY + "='" + renditionName + "'");
        }
        if (existingRenditions.size() > 0) {
            rendition = existingRenditions.get(0);
            rendition = session.getDocument(rendition.getRef());
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
        if (sourceDocument.isVersionable()) {
            rendition.setPropertyValue(RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY, liveDocumentId);
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

    private void updateIconAndSizeFields(DocumentModel rendition) {
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
        rendition.setPropertyValue("common:size", renditionBlob.getLength());
    }

    protected void giveReadRightToUser(DocumentModel rendition) {
        ACP acp = new ACPImpl();
        ACL acl = new ACLImpl();
        acp.addACL(acl);
        ACE ace = new ACE(getOriginatingUsername(), SecurityConstants.READ, true);
        acl.add(ace);
        rendition.setACP(acp, true);
    }

    protected void setCorrectVersion(DocumentModel rendition, DocumentModel versionDocument) {
        Long minorVersion = (Long) versionDocument.getPropertyValue("uid:minor_version"); // -
                                                                                          // 1L;
        rendition.setPropertyValue("uid:minor_version", minorVersion);
        rendition.setPropertyValue("uid:major_version", versionDocument.getPropertyValue("uid:major_version"));
    }

}
