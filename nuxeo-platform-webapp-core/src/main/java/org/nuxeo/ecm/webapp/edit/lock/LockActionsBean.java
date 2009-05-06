/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.webapp.edit.lock;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Remove;
import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;

/**
 * This is the action listener that knows to decide if an user has the right to
 * take the lock or release the lock of a document.
 * <p>
 * Most of the logic of this bean should either be moved into a DocumentModel
 * adapter or directly into the core API.
 *
 * @author <a href="mailto:bt@nuxeo.com">Bogdan Tatar</a>
 */
@Name("lockActions")
@Scope(ScopeType.EVENT)
public class LockActionsBean implements LockActions {
    // XXX: OG: How a remote calls could possibly work without the seam injected
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
    private Map<String, String> lockDetails;

    private Boolean canLock;

    private Boolean canUnlock;

    private Boolean isLiveEditable;


    public Boolean getCanLockDoc(DocumentModel document) {
        if (canLock == null) {
            if (document == null) {
                log.warn("Can't evaluate lock action : currentDocument is null");
                canLock = false;
            } else if (document.isProxy()) {
                canLock = false;
            } else {
                try {
                    NuxeoPrincipal userName = (NuxeoPrincipal) documentManager.getPrincipal();
                    String docLock = documentManager.getLock(document.getRef());
                    canLock = docLock == null
                            && (userName.isAdministrator() || documentManager.hasPermission(
                                    document.getRef(),
                                    SecurityConstants.WRITE_PROPERTIES))
                            && !document.isVersion();
                } catch (Exception e) {
                    log.debug("evaluation of document lock "
                            + document.getName() + " failed ("
                            + e.getMessage() + ": returning false");
                    canLock = false;
                }
            }
        }
        return canLock;
    }

    @Factory(value="currentDocumentCanBeLocked", scope= ScopeType.EVENT)
    public Boolean getCanLockCurrentDoc() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        return getCanLockDoc(currentDocument);
    }


    protected void resetEventContext() {
        Context evtCtx = Contexts.getEventContext();
        if (evtCtx!=null) {
            evtCtx.remove("currentDocumentCanBeLocked");
            evtCtx.remove("currentDocumentLockDetails");
            evtCtx.remove("currentDocumentCanBeUnlocked");
        }
    }

    public Boolean getCanUnlockDoc(DocumentModel document) {
        if (canUnlock == null) {
            if (document == null) {
                canUnlock = false;
            } else {
                try {
                    NuxeoPrincipal userName = (NuxeoPrincipal) documentManager.getPrincipal();
                    Map<String, String> lockDetails = getLockDetails(document);
                    if (lockDetails.isEmpty() || document.isProxy()) {
                        canUnlock = false;
                    } else {
                        canUnlock = ((userName.isAdministrator() || documentManager.hasPermission(
                                document.getRef(),
                                SecurityConstants.EVERYTHING)) ? true
                                : (userName.getName().equals(
                                        lockDetails.get(LOCKER)) && documentManager.hasPermission(
                                                document.getRef(),
                                        SecurityConstants.WRITE_PROPERTIES)))
                                && !document.isVersion();
                    }
                } catch (Exception e) {
                    log.debug("evaluation of document lock "
                            + document.getName() + " failed ("
                            + e.getMessage() + ": returning false");
                    canUnlock = false;
                }
            }
        }
        return canUnlock;
    }

    @Factory(value="currentDocumentCanBeUnlocked", scope= ScopeType.EVENT)
    public Boolean getCanUnlockCurrentDoc() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        return getCanUnlockDoc(currentDocument);
    }

    public String lockCurrentDocument() throws ClientException {
        return lockDocument(navigationContext.getCurrentDocument());
    }

    public String lockDocument(DocumentModel document) throws ClientException {
        log.debug("Lock a document ...");
        resetEventContext();
        String message = "document.lock.failed";
        DocumentRef ref = document.getRef();
        if (documentManager.hasPermission(ref, SecurityConstants.WRITE_PROPERTIES)
                && documentManager.getLock(ref) == null) {
            documentManager.setLock(ref, getDocumentLockKey());
            documentManager.save();
            message = "document.lock";
        }
        facesMessages.add(FacesMessage.SEVERITY_INFO,
                resourcesAccessor.getMessages().get(message));
        resetLockState();
        webActions.resetTabList();
        return "document_view";
    }

    public String unlockCurrentDocument() throws ClientException {
        return unlockDocument(navigationContext.getCurrentDocument());
    }

    // helper inner class to do the unrestricted unlock
    protected class unrestrictedUnlocker extends UnrestrictedSessionRunner {

        private final DocumentRef docRefToUnlock;

        protected unrestrictedUnlocker(DocumentRef docRef) {
            super(documentManager);
            docRefToUnlock = docRef;
        }

        /*
         * Use an unrestricted session to unlock the document.
         */
        @Override
        public void run() throws ClientException {
            session.unlock(docRefToUnlock);
            session.save();
        }
    }


    public String unlockDocument(DocumentModel document) throws ClientException {
        log.debug("Unlock a document ...");
        resetEventContext();
        String message;
        Map<String, String> lockDetails = getLockDetails(document);
        if (lockDetails == null) {
            message = "document.unlock.done";
        } else {
            NuxeoPrincipal userName = (NuxeoPrincipal) documentManager.getPrincipal();
            if (userName.isAdministrator()
                    || documentManager.hasPermission(document.getRef(),
                            SecurityConstants.EVERYTHING)
                    || userName.getName().equals(lockDetails.get(LOCKER))) {

                if (!documentManager.hasPermission(document.getRef(),
                        SecurityConstants.WRITE_PROPERTIES)) {

                    try {
                        // Here administrator should always be able to unlock so
                        // we need to grant him this possibility even if it
                        // doesn't have the write permission.

                        new unrestrictedUnlocker(document.getRef()).runUnrestricted();

                        message = "document.unlock";
                    } catch (Exception e) {
                        throw new ClientException(e.getMessage());
                    }
                } else {
                    documentManager.unlock(document.getRef());
                    documentManager.save();
                    message = "document.unlock";
                }
            } else {
                message = "document.unlock.not.permitted";
            }
        }
        facesMessages.add(FacesMessage.SEVERITY_INFO,
                resourcesAccessor.getMessages().get(message));
        resetLockState();
        webActions.resetTabList();
        return "document_view";
    }

    public void lockDocuments(List<DocumentModel> documents) {
        // TODO Auto-generated method stub
    }

    public void unlockDocuments(List<DocumentModel> documents) {
        // TODO Auto-generated method stub
    }

    public Action getLockOrUnlockAction() {
        log.debug("Get lock or unlock action ...");
        Action lockOrUnlockAction = null;
        List<Action> actionsList = webActions.getActionsList(EDIT_ACTIONS);
        if (actionsList != null && !actionsList.isEmpty()) {
            lockOrUnlockAction = actionsList.get(0);
        }
        return lockOrUnlockAction;
    }

    @Factory(value="currentDocumentLockDetails", scope = ScopeType.EVENT)
    public Map<String, String> getCurrentDocLockDetails()
            throws ClientException {
        Map<String, String> details = null;
        if (navigationContext.getCurrentDocument() != null) {
            details = getLockDetails(navigationContext.getCurrentDocument());
        }
        return details;
    }

    public Map<String, String> getLockDetails(DocumentModel document)
            throws ClientException {
        if (lockDetails == null) {
            lockDetails = new HashMap<String, String>();
            String documentKey = documentManager.getLock(document.getRef());
            if (documentKey == null) {
                return lockDetails;
            }
            String[] values = documentKey.split(":");
            lockDetails.put(LOCKER, values[0]);
            lockDetails.put(LOCK_TIME, values[1]);
        }
        return lockDetails;
    }

    private String getDocumentLockKey() {
        StringBuilder result = new StringBuilder();
        result.append(documentManager.getPrincipal().getName()).append(':').append(
                DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date()));
        return result.toString();
    }

    /**
     * @deprecated use LiveEditBootstrapHelper.isCurrentDocumentLiveEditable() instead
     */
    @Deprecated
    public Boolean isCurrentDocumentLiveEditable() {
        if (isLiveEditable == null) {
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            try {
                NuxeoPrincipal userName = (NuxeoPrincipal) documentManager.getPrincipal();
                String docLock = documentManager.getLock(currentDocument.getRef());
                if (docLock == null) {
                    isLiveEditable = (userName.isAdministrator() || documentManager.hasPermission(
                            currentDocument.getRef(), SecurityConstants.WRITE_PROPERTIES))
                            && !currentDocument.isVersion();
                } else {
                    isLiveEditable = (userName.isAdministrator() ? true
                            : (userName.getName().equals(
                                    getLockDetails(currentDocument).get(LOCKER)) && documentManager.hasPermission(
                                    currentDocument.getRef(),
                                    SecurityConstants.WRITE_PROPERTIES)))
                            && !currentDocument.isVersion();
                }
            } catch (Exception e) {
                log.debug("evaluation of edit on line option for document "
                        + currentDocument.getName() + " failed ("
                        + e.getMessage() + ": returning false");
                isLiveEditable = false;
            }
        }
        return isLiveEditable;
    }

    //@Observer( value={ EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED }, create=false, inject=false)
    public void resetLockState() {
        lockDetails = null;
        canLock = null;
        canUnlock = null;
        isLiveEditable = null;
    }

    @Destroy
    @Remove
    public void destroy() {
    }

}
