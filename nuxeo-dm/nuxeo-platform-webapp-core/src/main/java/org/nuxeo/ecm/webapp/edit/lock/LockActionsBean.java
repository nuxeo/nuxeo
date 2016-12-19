/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.edit.lock;

import static org.jboss.seam.annotations.Install.FRAMEWORK;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.EVERYTHING;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE_PROPERTIES;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * This is the action listener that knows to decide if an user has the right to take the lock or release the lock of a
 * document.
 * <p>
 * Most of the logic of this bean should either be moved into a DocumentModel adapter or directly into the core API.
 *
 * @author <a href="mailto:bt@nuxeo.com">Bogdan Tatar</a>
 */
@Name("lockActions")
@Scope(ScopeType.EVENT)
@Install(precedence = FRAMEWORK)
public class LockActionsBean implements LockActions {
    // XXX: OG: How a remote calls could possibly work without the seam
    // injected
    // components??

    private static final long serialVersionUID = -8050964269646803077L;

    private static final Log log = LogFactory.getLog(LockActionsBean.class);

    private static final String EDIT_ACTIONS = "EDIT_ACTIONS";

    @In
    private transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    @In(create = true)
    protected transient WebActions webActions;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    // cache lock details states to reduce costly core session remote calls
    private Map<String, Serializable> lockDetails;

    private String documentId;

    @Override
    public Boolean getCanLockDoc(DocumentModel document) {
        boolean canLock;
        if (document == null) {
            log.warn("Can't evaluate lock action : currentDocument is null");
            canLock = false;
        } else if (document.isProxy()) {
            canLock = false;
        } else {
            NuxeoPrincipal userName = (NuxeoPrincipal) documentManager.getPrincipal();
            Lock lock = documentManager.getLockInfo(document.getRef());
            canLock = lock == null
                    && (userName.isAdministrator() || isManagerOnDocument(document.getRef())
                            || documentManager.hasPermission(document.getRef(), WRITE_PROPERTIES))
                    && !document.isVersion();
        }
        return canLock;
    }

    protected boolean isManagerOnDocument(DocumentRef ref) {
        return documentManager.hasPermission(ref, EVERYTHING);
    }

    @Override
    @Factory(value = "currentDocumentCanBeLocked", scope = ScopeType.EVENT)
    public Boolean getCanLockCurrentDoc() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        return getCanLockDoc(currentDocument);
    }

    @Observer(value = { EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED }, create = false)
    @BypassInterceptors
    public void resetEventContext() {
        Context evtCtx = Contexts.getEventContext();
        if (evtCtx != null) {
            evtCtx.remove("currentDocumentCanBeLocked");
            evtCtx.remove("currentDocumentLockDetails");
            evtCtx.remove("currentDocumentCanBeUnlocked");
        }
    }

    @Override
    public Boolean getCanUnlockDoc(DocumentModel document) {
        boolean canUnlock = false;
        if (document == null) {
            canUnlock = false;
        } else {
            NuxeoPrincipal userName = (NuxeoPrincipal) documentManager.getPrincipal();
            Map<String, Serializable> lockDetails = getLockDetails(document);
            if (lockDetails.isEmpty() || document.isProxy()) {
                canUnlock = false;
            } else {
                canUnlock = ((userName.isAdministrator()
                        || documentManager.hasPermission(document.getRef(), EVERYTHING))
                                ? true
                                : (userName.getName().equals(lockDetails.get(LOCKER))
                                        && documentManager.hasPermission(document.getRef(), WRITE_PROPERTIES)))
                        && !document.isVersion();
            }
        }
        return canUnlock;
    }

    @Override
    @Factory(value = "currentDocumentCanBeUnlocked", scope = ScopeType.EVENT)
    public Boolean getCanUnlockCurrentDoc() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        return getCanUnlockDoc(currentDocument);
    }

    @Override
    public String lockCurrentDocument() {
        String view = lockDocument(navigationContext.getCurrentDocument());
        navigationContext.invalidateCurrentDocument();
        return view;
    }

    @Override
    public String lockDocument(DocumentModel document) {
        log.debug("Lock a document ...");
        resetEventContext();
        String message = "document.lock.failed";
        DocumentRef ref = document.getRef();
        if (documentManager.hasPermission(ref, WRITE_PROPERTIES) && documentManager.getLockInfo(ref) == null) {
            documentManager.setLock(ref);
            documentManager.save();
            message = "document.lock";
            Events.instance().raiseEvent(EventNames.DOCUMENT_LOCKED, document);
            Events.instance().raiseEvent(EventNames.DOCUMENT_CHANGED, document);
        }
        facesMessages.add(StatusMessage.Severity.INFO, resourcesAccessor.getMessages().get(message));
        resetLockState();
        webActions.resetTabList();
        return null;
    }

    @Override
    public String unlockCurrentDocument() {
        String view = unlockDocument(navigationContext.getCurrentDocument());
        navigationContext.invalidateCurrentDocument();
        return view;
    }

    // helper inner class to do the unrestricted unlock
    protected class UnrestrictedUnlocker extends UnrestrictedSessionRunner {

        private final DocumentRef docRefToUnlock;

        protected UnrestrictedUnlocker(DocumentRef docRef) {
            super(documentManager);
            docRefToUnlock = docRef;
        }

        /*
         * Use an unrestricted session to unlock the document.
         */
        @Override
        public void run() {
            session.removeLock(docRefToUnlock);
            session.save();
        }
    }

    @Override
    public String unlockDocument(DocumentModel document) {
        log.debug("Unlock a document ...");
        resetEventContext();
        String message;
        Map<String, Serializable> lockDetails = getLockDetails(document);
        if (lockDetails == null) {
            message = "document.unlock.done";
        } else {
            NuxeoPrincipal userName = (NuxeoPrincipal) documentManager.getPrincipal();
            if (userName.isAdministrator() || documentManager.hasPermission(document.getRef(), EVERYTHING)
                    || userName.getName().equals(lockDetails.get(LOCKER))) {

                if (!documentManager.hasPermission(document.getRef(), WRITE_PROPERTIES)) {
                    // Here administrator should always be able to unlock so
                    // we need to grant him this possibility even if it
                    // doesn't have the write permission.

                    new UnrestrictedUnlocker(document.getRef()).runUnrestricted();

                    documentManager.save(); // process invalidations from unrestricted session

                    message = "document.unlock";
                } else {
                    documentManager.removeLock(document.getRef());
                    documentManager.save();
                    message = "document.unlock";
                }
                Events.instance().raiseEvent(EventNames.DOCUMENT_UNLOCKED, document);
                Events.instance().raiseEvent(EventNames.DOCUMENT_CHANGED, document);
            } else {
                message = "document.unlock.not.permitted";
            }
        }
        facesMessages.add(StatusMessage.Severity.INFO, resourcesAccessor.getMessages().get(message));
        resetLockState();
        webActions.resetTabList();
        return null;
    }

    @Override
    public Action getLockOrUnlockAction() {
        log.debug("Get lock or unlock action ...");
        Action lockOrUnlockAction = null;
        List<Action> actionsList = webActions.getActionsList(EDIT_ACTIONS);
        if (actionsList != null && !actionsList.isEmpty()) {
            lockOrUnlockAction = actionsList.get(0);
        }
        return lockOrUnlockAction;
    }

    @Override
    @Factory(value = "currentDocumentLockDetails", scope = ScopeType.EVENT)
    public Map<String, Serializable> getCurrentDocLockDetails() {
        Map<String, Serializable> details = null;
        if (navigationContext.getCurrentDocument() != null) {
            details = getLockDetails(navigationContext.getCurrentDocument());
        }
        return details;
    }

    @Override
    public Map<String, Serializable> getLockDetails(DocumentModel document) {
        if (lockDetails == null || !StringUtils.equals(documentId, document.getId())) {
            lockDetails = new HashMap<String, Serializable>();
            documentId = document.getId();
            Lock lock = documentManager.getLockInfo(document.getRef());
            if (lock == null) {
                return lockDetails;
            }
            lockDetails.put(LOCKER, lock.getOwner());
            lockDetails.put(LOCK_CREATED, lock.getCreated());
        }
        return lockDetails;
    }

    @Override
    @BypassInterceptors
    public void resetLockState() {
        lockDetails = null;
        documentId = null;
    }

}
