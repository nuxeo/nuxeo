/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.platform.versioning.listeners;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;

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
import org.nuxeo.ecm.core.lifecycle.LifeCycleEventTypes;
import org.nuxeo.ecm.core.utils.DocumentModelUtils;
import org.nuxeo.ecm.platform.versioning.BasicVersionChangeRequest;
import org.nuxeo.ecm.platform.versioning.VersionChangeRequest;
import org.nuxeo.ecm.platform.versioning.api.VersionIncEditOptions;
import org.nuxeo.ecm.platform.versioning.api.VersioningActions;
import org.nuxeo.ecm.platform.versioning.api.VersioningException;
import org.nuxeo.ecm.platform.versioning.service.ServiceHelper;
import org.nuxeo.ecm.platform.versioning.service.VersioningService;

/**
 * Core event listener performing version increment based on rules and/or user
 * selected option. The selected option comes in options hash map.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public class DocVersioningEventListener implements EventListener {

    private static final Log log = LogFactory
            .getLog(DocVersioningEventListener.class);

    public void handleEvent(Event event) throws ClientException {

        EventContext ctx = event.getContext();
        if (ctx instanceof DocumentEventContext) {
            DocumentEventContext docCtx = (DocumentEventContext) ctx;
            doProcess(event.getName(),docCtx);
        }
    }

    protected void doProcess(String eventId, DocumentEventContext docCtx) {
        DocumentModel doc = docCtx.getSourceDocument();
        Map<String, Serializable> options = docCtx.getProperties();

        Object ctxSkipVersioning = doc.getContextData(ScopeType.REQUEST,
                VersioningActions.SKIP_VERSIONING);
        boolean skipVersioningFlag;
        if (ctxSkipVersioning == null) {
            // not set, will return default
            skipVersioningFlag = false;
        } else {
            skipVersioningFlag = (Boolean) ctxSkipVersioning;
        }

        if (!skipVersioningFlag) {
            VersioningService service;
            try {
                service = getVerService();
            } catch (VersioningException e) {
                return;
            }

            String docType = doc.getType();
            String majorPropName = service.getMajorVersionPropertyName(docType);
            String minorPropName = service.getMinorVersionPropertyName(docType);

            // XXX: check if the document has versioning schema (?)
            VersionChangeRequest req;

            if (LifeCycleEventTypes.LIFECYCLE_TRANSITION_EVENT.equals(eventId)) {
                // XXX Used to check the workflow rules, do we need
                // version changes on lifecycle change?
                return;
            } else if (eventId.equals(DOCUMENT_CREATED) && !doc.isProxy()) {
                // set major version at 1
                try {
                    doc.setProperty(DocumentModelUtils
                            .getSchemaName(majorPropName), DocumentModelUtils
                            .getFieldName(majorPropName), 1L);
                    doc.setProperty(DocumentModelUtils
                            .getSchemaName(minorPropName), DocumentModelUtils
                            .getFieldName(minorPropName), 0L);
                } catch (ClientException e) {
                    throw new ClientRuntimeException(e);
                }
                return;
            } else if (eventId.equals(BEFORE_DOC_UPDATE)) {
                try {
                    if (!isIncOptionUserSelected(doc)) {
                        req = createAutoChangeRequest(doc);
                        return;
                    }
                    // the user has selected incrementation option
                    log.debug("Skip document versions auto-incrementation. "
                            + "Should be incremented by user selection.");
                } catch (VersioningException e) {
                    log.error(e);
                }

                if (options == null) {
                    log.error("options is null. cannot increment versions");
                    return;
                }

                // has to be string
                final VersioningActions incOption = (VersioningActions) options
                        .get(VersioningActions.KEY_FOR_INC_OPTION);
                if (incOption == null) {
                    log.debug("version increment option not available");
                    return;
                }

                req = createEditChangeRequest(doc, incOption);
            } else if (eventId.equals(DocumentEventTypes.DOCUMENT_RESTORED)) {
                if (options == null) {
                    log.warn("options is null. versions not available");
                    return;
                }

                // regain current versions
                final Long majorVer = (Long) options
                        .get(VersioningDocument.CURRENT_DOCUMENT_MAJOR_VERSION_KEY);
                final Long minorVer = (Long) options
                        .get(VersioningDocument.CURRENT_DOCUMENT_MINOR_VERSION_KEY);

                try {
                    doc.setProperty(DocumentModelUtils
                            .getSchemaName(majorPropName), DocumentModelUtils
                            .getFieldName(majorPropName), majorVer);
                    doc.setProperty(DocumentModelUtils
                            .getSchemaName(minorPropName), DocumentModelUtils
                            .getFieldName(minorPropName), minorVer);
                } catch (ClientException e) {
                    throw new ClientRuntimeException(e);
                }

                req = createAutoChangeRequest(doc);
            } else {
                // event not interesting
                return;
            }

            log.debug("<notifyEvent> req: " + req);

            try {
                service.incrementVersions(req);
            } catch (ClientException e) {
                log.error("Error incrementing versions for: " + doc, e);
            }
        }
    }

    /**
     * Doesn't return null. If the service is not available, an exception is
     * thrown so the caller code won't need to check.
     *
     * @return the versioning service
     * @throws VersioningException
     *             if the versioning service was not found
     */
    private static VersioningService getVerService() throws VersioningException {
        VersioningService service = ServiceHelper.getVersioningService();
        if (service == null) {
            throw new VersioningException("VersioningService service not found");
        }
        return service;
    }

    private static VersionChangeRequest createAutoChangeRequest(
            DocumentModel doc) {

        log.debug("<createAutoChangeRequest> ");

        final VersionChangeRequest req = new BasicVersionChangeRequest(
                VersionChangeRequest.RequestSource.AUTO, doc) {

            public VersioningActions getVersioningAction() {
                log.warn("Rule for AUTO not correctly defined");
                return null;
            }

        };

        return req;
    }

    private static VersionChangeRequest createEditChangeRequest(
            DocumentModel doc, final VersioningActions incOption) {

        log.debug("<createEditChangeRequest> ");

        final VersionChangeRequest req = new BasicVersionChangeRequest(
                VersionChangeRequest.RequestSource.EDIT, doc) {

            public VersioningActions getVersioningAction() {
                return incOption;
            }
        };

        return req;
    }

    /**
     * Determine for the given doc if there are multiple increment option. The
     * user will select one in this case and the automatic increment shouldn't
     * be performed.
     */
    private static boolean isIncOptionUserSelected(DocumentModel doc)
            throws VersioningException {

        final String logPrefix = "<isIncOptionUserSelected> ";

        String currentLifeCycleState;
        try {
            currentLifeCycleState = doc.getCurrentLifeCycleState();
        } catch (ClientException e) {
            log.error(e);
            return true;
        }
        final String documentType = doc.getType();

        log.debug(logPrefix + "currentLifeCycleState: " + currentLifeCycleState);

        if (currentLifeCycleState != null) {
            log.debug(logPrefix
                    + "checking versioning policy in component extensions");
            VersionIncEditOptions options = getVerService()
                    .getVersionIncOptions(currentLifeCycleState, documentType);

            if (options.getVersioningAction() == VersioningActions.ACTION_CASE_DEPENDENT) {
                // so there are valid options to select from and versions will
                // be altered directly not through listener
                log.debug(logPrefix + "available options: " + options);
                return true;
            }
        } else {
            log.warn(logPrefix + "document lifecycle not initialized.");
        }

        return false;
    }

}
