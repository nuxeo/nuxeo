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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.versioning.listeners;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.lifecycle.LifeCycleEventTypes;
import org.nuxeo.ecm.core.listener.AbstractEventListener;
import org.nuxeo.ecm.core.listener.AsynchronousEventListener;
import org.nuxeo.ecm.core.listener.DocumentModelEventListener;
import org.nuxeo.ecm.core.utils.DocumentModelUtils;
import org.nuxeo.ecm.platform.versioning.BasicVersionChangeRequest;
import org.nuxeo.ecm.platform.versioning.VersionChangeRequest;
import org.nuxeo.ecm.platform.versioning.api.VersionIncEditOptions;
import org.nuxeo.ecm.platform.versioning.api.VersioningActions;
import org.nuxeo.ecm.platform.versioning.api.VersioningException;
import org.nuxeo.ecm.platform.versioning.service.ServiceHelper;
import org.nuxeo.ecm.platform.versioning.service.VersioningService;
import org.nuxeo.ecm.platform.versioning.wfintf.WFVersioningPolicyProvider;

/**
 * Core event listener performing version increment based on rules and/or user
 * selected option. The selected option comes in options hash map.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public class DocVersioningListener extends AbstractEventListener implements
        AsynchronousEventListener, DocumentModelEventListener {

    private static final Log log = LogFactory.getLog(DocVersioningListener.class);

    /**
     * Default listener constructor registers events we are interested in.
     */
    public DocVersioningListener() {
        addEventId(LifeCycleEventTypes.LIFECYCLE_TRANSITION_EVENT);
        addEventId(DOCUMENT_CREATED);
        addEventId(BEFORE_DOC_UPDATE);
        //addEventId(DOCUMENT_UPDATED);
        addEventId(DocumentEventTypes.DOCUMENT_RESTORED);
    }

    /**
     * Core event notification.
     * <p>
     * Gets core events and updates versions if needed.
     *
     * @param coreEvent instance thrown at core layer
     */
    @Override
    public void notifyEvent(CoreEvent coreEvent) {

        final String logPrefix = "<notifyEvent> ";

        Object source = coreEvent.getSource();
        if (source instanceof DocumentModel) {

            DocumentModel doc = (DocumentModel) source;
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

                String eventId = coreEvent.getEventId();
                // log.info(logPrefix + "eventId : " + eventId);

                VersionChangeRequest req;

                if (LifeCycleEventTypes.LIFECYCLE_TRANSITION_EVENT.equals(eventId)) {

                    final Map<String, ?> evtInfoMap;
                    try {
                        evtInfoMap = coreEvent.getInfo();
                    } catch (ClassCastException e) {
                        // XXX : should define a more restrictive event info
                        log.error("BAD event info type", e);
                        return;
                    }

                    final String from;
                    final String to;
                    try {
                        from = (String) evtInfoMap.get(LifeCycleEventTypes.OPTION_NAME_FROM);
                        to = (String) evtInfoMap.get(LifeCycleEventTypes.OPTION_NAME_TO);
                    } catch (ClassCastException e) {
                        log.error("BAD option type", e);
                        return;
                    }

                    if (log.isDebugEnabled()) {
                        log.debug(logPrefix + "Lifecycle event: 1st state="
                                + from + ", 2nd state=" + to);
                    }

                    req = getChangeDocVersionsRequest(doc, from, to);

                } else if (eventId.equals(DOCUMENT_CREATED) && !doc.isProxy()) {
                    // set major version at 1
                    doc.setProperty(DocumentModelUtils.getSchemaName(majorPropName),
                            DocumentModelUtils.getFieldName(majorPropName), 1L);
                    doc.setProperty(DocumentModelUtils.getSchemaName(minorPropName),
                            DocumentModelUtils.getFieldName(minorPropName), 0L);
                    return;
                } else if (eventId.equals(BEFORE_DOC_UPDATE)) {
                    try {
                        if (!isIncOptionUserSelected(doc)) {
                             req = createAutoChangeRequest(doc);
                            return;
                        }
                        // the user has selected incrementation option
                        log.debug(logPrefix
                                + "Skip document versions auto-incrementation. "
                                + "Should be incremented by user selection.");
                    } catch (VersioningException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (ClientException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                //} else if (eventId.equals(DOCUMENT_UPDATED)) {
                    // check options
                    final Map<String, ?> options = coreEvent.getInfo();
                    if (options == null) {
                        log.error("options is null. cannot increment versions");
                        return;
                    }

                    // has to be string
                    final VersioningActions incOption = (VersioningActions) options.get(
                            VersioningActions.KEY_FOR_INC_OPTION);
                    if (incOption == null) {
                        log.warn("version increment option not available");
                        return;
                    }

                    req = createEditChangeRequest(doc, incOption);
                } else if (eventId.equals(DocumentEventTypes.DOCUMENT_RESTORED)) {
                    final Map<String, ?> options = coreEvent.getInfo();
                    if (options == null) {
                        log.warn("options is null. versions not available");
                        return;
                    }

                    // regain current versions
                    final Long majorVer = (Long) options.get(
                            VersioningDocument.CURRENT_DOCUMENT_MAJOR_VERSION_KEY);
                    final Long minorVer = (Long) options.get(
                            VersioningDocument.CURRENT_DOCUMENT_MINOR_VERSION_KEY);

                    doc.setProperty(DocumentModelUtils.getSchemaName(majorPropName),
                            DocumentModelUtils.getFieldName(majorPropName), majorVer);
                    doc.setProperty(DocumentModelUtils.getSchemaName(minorPropName),
                            DocumentModelUtils.getFieldName(minorPropName), minorVer);

                    req = createAutoChangeRequest(doc);
                } else {
                    // evt not interesting
                    return;
                }

                log.debug("<notifyEvent> req : " + req);

                try {
                    service.incrementVersions(req);
                } catch (ClientException e) {
                    log.error("Error occurred while incrementing versions for : "
                            + doc, e);
                }
            }
        }
    }

    /**
     * Doesn't return null. if the service is not available an exception is
     * thrown so the caller code won't need to check.
     *
     * @return
     * @throws VersioningException
     */
    private static VersioningService getVerService() throws VersioningException {
        VersioningService service = ServiceHelper.getVersioningService();
        if (service == null) {
            log.error("<changeDocVersions> VersioningService "
                    + "service not found ... !");
            throw new VersioningException("VersioningService service not found");
        }

        return service;
    }

    private static VersionChangeRequest getChangeDocVersionsRequest(DocumentModel doc,
            String stateFrom, String stateTo) {

        final BasicVersionChangeRequest req = new BasicVersionChangeRequest(
                VersionChangeRequest.RequestSource.WORKFLOW, doc) {

            public VersioningActions getVersioningAction() {
                // XXX : default inc if there is not rule defined?
                // normally this shouldn't be called
                log.warn("Rule for WORKFLOW not correctly defined");
                return null;
            }

        };

        req.setWfStateInitial(stateFrom);
        req.setWfStateFinal(stateTo);

        return req;
    }

    private static VersionChangeRequest createAutoChangeRequest(
            DocumentModel doc) {

        final String logPrefix = "<createAutoChangeRequest> ";

        log.debug(logPrefix);

        final VersionChangeRequest req = new BasicVersionChangeRequest(
                VersionChangeRequest.RequestSource.AUTO, doc) {

            public VersioningActions getVersioningAction() {
                // XXX : default inc if there is not rule defined?
                // normally this shouldn't be called
                log.warn("Rule for AUTO not correctly defined");
                return null; // VersioningActions.ACTION_INCREMENT_MINOR.toString();
            }

        };

        return req;
    }

    private static VersionChangeRequest createEditChangeRequest(
            DocumentModel doc, final VersioningActions incOption) {

        final String logPrefix = "<createEditChangeRequest> ";

        log.debug(logPrefix);

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
     *
     * @param doc
     * @return
     * @throws VersioningException
     * @throws ClientException
     * @throws DocumentException
     */
    private static boolean isIncOptionUserSelected(DocumentModel doc)
            throws VersioningException, ClientException {

        final String logPrefix = "<isIncOptionUserSelected> ";

        String currentLifeCycleState;
        try {
            currentLifeCycleState = doc.getCurrentLifeCycleState();
        } catch (ClientException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();

            return true;
        }
        final String documentType = doc.getType();

        log.debug(logPrefix + "currentLifeCycleState: " + currentLifeCycleState);

        if (currentLifeCycleState != null) {
            log.debug(logPrefix
                    + "checking versioning policy in component extensions");
            VersionIncEditOptions options = getVerService().getVersionIncOptions(
                    currentLifeCycleState, documentType);

            if (options.getVersioningAction() == VersioningActions.ACTION_CASE_DEPENDENT) {
                // so there are valid options to select from and versions will
                // be altered directly not through listener
                log.debug(logPrefix + "available options: " + options);
                return true;
            }
        } else {
            log.warn(logPrefix + "document lifecycle not initialized.");
        }

        // check with document Workflow
        DocumentRef docRef = doc.getRef();
        log.debug(logPrefix + "checking versioning policy in document workflow");
        final VersioningActions wfvaction = WFVersioningPolicyProvider.getVersioningPolicyFor(doc);
        if (wfvaction != null) {
            if (wfvaction == VersioningActions.ACTION_CASE_DEPENDENT) {
                log.debug(logPrefix + "WF case dependent...");
                return true;
            }
        }

        return false;
    }

}
