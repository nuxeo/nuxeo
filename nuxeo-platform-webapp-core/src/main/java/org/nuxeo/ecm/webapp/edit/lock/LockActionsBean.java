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

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Remove;
import javax.faces.application.FacesMessage;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.api.ECM;
import org.nuxeo.ecm.platform.api.login.SystemSession;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * This is the action listener that knows to decide if an user has the right to
 * take the lock or release the lock of a document.
 *
 * Most of the logics of this bean should either be moved into a DocumentModel
 * adapter or directly into the core API
 *
 * @author <a href="mailto:bt@nuxeo.com">Bogdan Tatar</a>
 *
 */
@Name("lockActions")
@Scope(CONVERSATION)
public class LockActionsBean implements LockActions {
    // XXX: OG: How a remote calls could possibly work without the seam injected
    // components??

    private static final long serialVersionUID = -8050964269646803077L;

    private static final Log log = LogFactory.getLog(LockActionsBean.class);

    private static final String EDIT_ACTIONS = "EDIT_ACTIONS";

    @In(required = true)
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

    public Boolean getCanLockCurrentDoc() {
        if (canLock == null) {
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            if (currentDocument == null) {
                log.warn("Can't evaluate lock action : currentDocument is null");
                canLock = false;
            } else if (currentDocument.isProxy()) {
                canLock = false;
            } else {
                try {
                    NuxeoPrincipal userName = (NuxeoPrincipal) documentManager.getPrincipal();
                    String docLock = documentManager.getLock(currentDocument.getRef());
                    canLock = docLock == null
                            && (userName.isAdministrator() || documentManager.hasPermission(
                                    currentDocument.getRef(),
                                    SecurityConstants.WRITE))
                            && !currentDocument.isVersion();
                } catch (Exception e) {
                    log.info("evaluation of document lock "
                            + currentDocument.getName() + " failed ("
                            + e.getMessage() + ": returning false");
                    canLock = false;
                }
            }
        }
        return canLock;
    }

    public Boolean getCanUnlockCurrentDoc() {
        if (canUnlock == null) {
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            if (currentDocument == null) {
                canUnlock = false;
            } else {
                try {
                    NuxeoPrincipal userName = (NuxeoPrincipal) documentManager.getPrincipal();
                    Map<String, String> lockDetails = getLockDetails(currentDocument);
                    if (lockDetails.isEmpty() || currentDocument.isProxy()) {
                        canUnlock = false;
                    } else {
                        canUnlock = ((userName.isAdministrator() || documentManager.hasPermission(
                                currentDocument.getRef(),
                                SecurityConstants.EVERYTHING)) ? true
                                : (userName.getName().equals(
                                        lockDetails.get(LOCKER)) && documentManager.hasPermission(
                                        currentDocument.getRef(),
                                        SecurityConstants.WRITE)))
                                && !currentDocument.isVersion();
                    }
                } catch (Exception e) {
                    log.info("evaluation of document lock "
                            + currentDocument.getName() + " failed ("
                            + e.getMessage() + ": returning false");
                    canUnlock = false;
                }
            }
        }
        return canUnlock;
    }

    public String lockCurrentDocument() throws ClientException {
        return lockDocument(navigationContext.getCurrentDocument());
    }

    public String lockDocument(DocumentModel document) throws ClientException {
        log.debug("Lock a document ...");
        String message = "document.lock.failed";
        DocumentRef ref = document.getRef();
        if (documentManager.hasPermission(ref, SecurityConstants.WRITE)
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

    public String unlockDocument(DocumentModel document) throws ClientException {
        log.debug("Unlock a document ...");
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
                        SecurityConstants.WRITE)) {

                    try {
                        // Here administrator should always be able to unlock so
                        // we need to grant him this possibility even if it
                        // doesn't have the write permission.
                        SystemSession session = new SystemSession();
                        session.login();

                        // Open a new repository session which will be
                        // unrestricted. We need to do this here since the
                        // document manager in Seam context has been initialized
                        // with caller principal rights.
                        CoreSession unrestrictedSession = ECM.getPlatform().openRepository(
                                navigationContext.getCurrentServerLocation().getName());

                        // Publish the document using the new session.
                        unrestrictedSession.unlock(document.getRef());
                        unrestrictedSession.save();

                        // Close the unrestricted session.
                        CoreInstance.getInstance().close(unrestrictedSession);

                        // Logout the system session.
                        // Note, this is not necessary to take further actions
                        // here
                        // regarding the user session.
                        session.logout();

                        message = "document.unlock";
                    } catch (LoginException e) {
                        throw new ClientException(e.getMessage());
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
                            currentDocument.getRef(), SecurityConstants.WRITE))
                            && !currentDocument.isVersion();
                } else {
                    isLiveEditable = (userName.isAdministrator() ? true
                            : (userName.getName().equals(
                                    getLockDetails(currentDocument).get(LOCKER)) && documentManager.hasPermission(
                                    currentDocument.getRef(),
                                    SecurityConstants.WRITE)))
                            && !currentDocument.isVersion();
                }
            } catch (Exception e) {
                log.info("evaluation of edit on line option for document "
                        + currentDocument.getName() + " failed ("
                        + e.getMessage() + ": returning false");
                isLiveEditable = false;
            }
        }
        return isLiveEditable;
    }

    @Observer( value={ EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED }, create=false, inject=false)
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
