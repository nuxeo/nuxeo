/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Dragos Mihalache
 *     Florent Guillaume
 */

package org.nuxeo.ecm.platform.versioning.listeners;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.utils.DocumentModelUtils;
import org.nuxeo.ecm.platform.versioning.BasicVersionChangeRequest;
import org.nuxeo.ecm.platform.versioning.VersionChangeRequest;
import org.nuxeo.ecm.platform.versioning.api.VersionIncEditOptions;
import org.nuxeo.ecm.platform.versioning.api.VersioningActions;
import org.nuxeo.ecm.platform.versioning.service.ServiceHelper;
import org.nuxeo.ecm.platform.versioning.service.VersioningService;

/**
 * Core event listener performing version increment based on rules and/or user
 * selected option. The selected option comes in options hash map.
 *
 * @author Dragos Mihalache
 * @author Florent Guillaume
 */
public class DocVersioningEventListener implements EventListener {

    private static final Log log = LogFactory.getLog(DocVersioningEventListener.class);

    public void handleEvent(Event event) throws ClientException {
        EventContext ctx = event.getContext();
        if (ctx instanceof DocumentEventContext) {
            doProcess(event.getName(), (DocumentEventContext) ctx);
        }
    }

    protected void doProcess(String eventId, DocumentEventContext ctx)
            throws ClientException {
        DocumentModel doc = ctx.getSourceDocument();

        // TODO SKIP_VERSIONING seems unused
        if (doc.getContextData(ScopeType.REQUEST,
                VersioningActions.SKIP_VERSIONING) == Boolean.TRUE) {
            return;
        }
        // XXX: check if the document has versioning schema (?)

        VersioningService versioningService = ServiceHelper.getVersioningService();
        if (versioningService == null) {
            log.debug("No versioning service");
            return;
        }

        String docType = doc.getType();
        String majorPropName = versioningService.getMajorVersionPropertyName(docType);
        String minorPropName = versioningService.getMinorVersionPropertyName(docType);

        Map<String, Serializable> options = ctx.getProperties();

        VersionChangeRequest req;

        if (eventId.equals(DocumentEventTypes.DOCUMENT_CREATED)
                && !doc.isProxy()) {
            // set version to 1.0
            try {
                doc.setProperty(
                        DocumentModelUtils.getSchemaName(majorPropName),
                        DocumentModelUtils.getFieldName(majorPropName),
                        Long.valueOf(1));
                doc.setProperty(
                        DocumentModelUtils.getSchemaName(minorPropName),
                        DocumentModelUtils.getFieldName(minorPropName),
                        Long.valueOf(0));
            } catch (ClientException e) {
                throw new ClientRuntimeException(e);
            }
            return;
        } else if (eventId.equals(DocumentEventTypes.DOCUMENT_CHECKEDOUT)) {
            req = getVersionChangeRequest(doc, options);
            if (req == null) {
                req = createAutoChangeRequest(doc);
            }
        } else if (eventId.equals(DocumentEventTypes.INCREMENT_BEFORE_UPDATE)) {
            if (options.get(VersioningDocument.DOCUMENT_WAS_SNAPSHOTTED) == Boolean.TRUE) {
                // document was just snapshotted, don't increment versions again
                return;
            }
            req = getVersionChangeRequest(doc, options);
        } else if (eventId.equals(DocumentEventTypes.DOCUMENT_RESTORED)) {
            if (options == null) {
                log.warn("options is null. versions not available");
                return;
            }
            // first, restore version number from before restore
            try {
                doc.setProperty(
                        DocumentModelUtils.getSchemaName(majorPropName),
                        DocumentModelUtils.getFieldName(majorPropName),
                        options.get(VersioningDocument.CURRENT_DOCUMENT_MAJOR_VERSION_KEY));
                doc.setProperty(
                        DocumentModelUtils.getSchemaName(minorPropName),
                        DocumentModelUtils.getFieldName(minorPropName),
                        options.get(VersioningDocument.CURRENT_DOCUMENT_MINOR_VERSION_KEY));
            } catch (ClientException e) {
                throw new ClientRuntimeException(e);
            }
            return;
        } else {
            // event not interesting
            return;
        }

        log.debug("req: " + req);

        if (req == null) {
            return; // missing info
        }
        try {
            versioningService.incrementVersions(req);
        } catch (ClientException e) {
            log.error("Error incrementing versions for: " + doc, e);
        }
    }

    /**
     * Depending on the options and the type of choice for the document, get a
     * change request.
     */
    protected static VersionChangeRequest getVersionChangeRequest(
            DocumentModel doc, Map<String, Serializable> options)
            throws ClientException {
        if (isIncOptionUserSelected(doc)) {
            // user had a choice
            if (options == null) {
                return null;
            }
            VersioningActions incOption = (VersioningActions) options.get(VersioningActions.KEY_FOR_INC_OPTION);
            if (incOption == null) {
                return null;
            }
            return createEditChangeRequest(doc, incOption);
        } else {
            // user didn't have a choice
            return createAutoChangeRequest(doc);
        }
    }

    protected static VersionChangeRequest createAutoChangeRequest(
            DocumentModel doc) {
        return new BasicVersionChangeRequest(
                VersionChangeRequest.RequestSource.AUTO, doc) {
            public VersioningActions getVersioningAction() {
                log.warn("Rule for AUTO not correctly defined");
                return null;
            }
        };
    }

    protected static VersionChangeRequest createEditChangeRequest(
            DocumentModel doc, final VersioningActions incOption) {
        return new BasicVersionChangeRequest(
                VersionChangeRequest.RequestSource.EDIT, doc) {
            public VersioningActions getVersioningAction() {
                return incOption;
            }
        };
    }

    /**
     * Determine for the given doc if there are multiple increment option. The
     * user will select one in this case and the automatic increment shouldn't
     * be performed.
     */
    protected static boolean isIncOptionUserSelected(DocumentModel doc)
            throws ClientException {
        VersioningService versioningService = ServiceHelper.getVersioningService();
        VersionIncEditOptions options = versioningService.getVersionIncEditOptions(doc);
        if (options.getVersioningAction() == VersioningActions.ACTION_CASE_DEPENDENT) {
            // so there are valid options to select from and versions will
            // be altered directly not through listener
            log.debug("available options: " + options);
            return true;
        }
        return false;
    }

}
