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
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_SOURCE_ID_PROPERTY;
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_NAME_PROPERTY;
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.versioning.VersioningService;

/**
 * 
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * 
 */
public class RenditionCreator extends UnrestrictedSessionRunner {

    protected DocumentRef renditionRef;

    protected DocumentModel detachedDendition;

    protected String liveDocumentId;

    protected DocumentRef versionDocumentRef;

    protected Blob renditionBlob;

    protected String renditionName;

    public RenditionCreator(CoreSession session, DocumentModel liveDocument,
            DocumentModel versionDocument, Blob renditionBlob,
            String renditionName) {
        super(session);
        this.liveDocumentId = liveDocument.getId();
        this.versionDocumentRef = versionDocument.getRef();
        this.renditionBlob = renditionBlob;
        this.renditionName = renditionName;
    }

    public DocumentRef getRenditionDocumentRef() {
        return renditionRef;
    }

    public DocumentModel getDetachedDendition() {
        return detachedDendition;
    }

    @Override
    public void run() throws ClientException {
        DocumentModel versionDocument = session.getDocument(versionDocumentRef);
        DocumentModel rendition = createRenditionDocument(versionDocument);
        removeBlobs(rendition);
        updateMainBlob(rendition);

        // create a copy of the doc
        rendition = session.createDocument(rendition);
        // be sure to have the same version info
        setCorrectVersion(rendition, versionDocument);
        // set ACL
        // giveReadRightToUser(rendition);
        // do not apply default versioning to rendition
        rendition.putContextData(VersioningService.VERSIONING_OPTION,
                VersioningOption.NONE);
        rendition = session.saveDocument(rendition);

        // rendition is checkout : make it checkin
        DocumentRef renditionCheckoutRef = rendition.getRef();
        renditionRef = rendition.checkIn(VersioningOption.NONE, null);
        session.removeDocument(renditionCheckoutRef);
        rendition = session.getDocument(renditionRef);
        session.save();

        rendition.detach(true);
        detachedDendition = rendition;
    }

    protected DocumentModel createRenditionDocument(
            DocumentModel versionDocument) throws ClientException {
        DocumentModel rendition = session.createDocumentModel(null,
                versionDocument.getName(), versionDocument.getType());
        rendition.copyContent(versionDocument);
        rendition.getContextData().putScopedValue(LifeCycleConstants.INITIAL_LIFECYCLE_STATE_OPTION_NAME, versionDocument.getCurrentLifeCycleState());
        
        rendition.addFacet(RENDITION_FACET);
        rendition.setPropertyValue(RENDITION_SOURCE_ID_PROPERTY,
                versionDocument.getId());
        rendition.setPropertyValue(RENDITION_SOURCE_VERSIONABLE_ID_PROPERTY,
                liveDocumentId);
        rendition.setPropertyValue(RENDITION_NAME_PROPERTY, renditionName);
        return rendition;
    }

    protected void removeBlobs(DocumentModel rendition) throws ClientException {
        if (rendition.hasSchema(FILES_SCHEMA)) {
            rendition.setPropertyValue(FILES_FILES_PROPERTY,
                    new ArrayList<Map<String, Serializable>>());
        }
    }

    protected void updateMainBlob(DocumentModel rendition)
            throws ClientException {
        BlobHolder bh = rendition.getAdapter(BlobHolder.class);
        bh.setBlob(renditionBlob);
    }

    protected void giveReadRightToUser(DocumentModel rendition)
            throws ClientException {
        ACP acp = new ACPImpl();
        ACL acl = new ACLImpl();
        acp.addACL(acl);
        ACE ace = new ACE(getOriginatingUsername(), SecurityConstants.READ,
                true);
        acl.add(ace);
        rendition.setACP(acp, true);
    }

    protected void setCorrectVersion(DocumentModel rendition,
            DocumentModel versionDocument) throws ClientException {
        Long minorVersion = (Long) versionDocument.getPropertyValue("uid:minor_version"); // -
                                                                                          // 1L;
        rendition.setPropertyValue("uid:minor_version", minorVersion);
        rendition.setPropertyValue("uid:major_version",
                versionDocument.getPropertyValue("uid:major_version"));
    }

}
