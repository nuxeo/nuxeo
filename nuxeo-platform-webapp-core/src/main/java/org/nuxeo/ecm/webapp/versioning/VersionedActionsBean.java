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
 * $Id$
 */

package org.nuxeo.ecm.webapp.versioning;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.ScopeType.EVENT;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.security.PermitAll;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remove;
import javax.ejb.Stateful;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.annotation.ejb.SerializedConcurrentAccess;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Context;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.ejb.EJBExceptionHandler;
import org.nuxeo.ecm.platform.ui.web.api.UserAction;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.w3c.dom.ranges.DocumentRange;

/**
 * Deals with versioning actions.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 *
 */
@Name("versionedActions")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class VersionedActionsBean extends InputController implements
        VersionedActions {

    private static final Log log = LogFactory.getLog(VersionedActionsBean.class);

    @In(create = true)
    protected transient CoreSession documentManager;

    @In
    protected transient Context sessionContext;

    @In(required = false)
    @Out(required = false)
    protected VersionModel newVersion;

    @In(required = true, create = true)
    protected transient DocumentVersioning documentVersioning;

    protected transient List<VersionModel> versionModelList;

    protected String checkedOut;

    @Create
    public void initialize() {
        newVersion = new VersionModelImpl();
        sessionContext.set("newVersion", newVersion);
    }

    @Observer(value={ EventNames.DOCUMENT_SELECTION_CHANGED,
            EventNames.DOCUMENT_CHANGED }, create=false, inject=false)
    public void resetVersions() {
        versionModelList = null;
    }

    @Factory(value = "versionList", scope = EVENT)
    public List<VersionModel> getVersionList() throws ClientException {
        if (versionModelList == null || versionModelList.isEmpty()) {
            retrieveVersions();
        }
        return versionModelList;
    }

    /**
     * Returns an empty list if a no versions are found.
     *
     * @return e5e7b4ba-0ffb-492d-8bf2-f2f2e6683ae2
     */
    public void retrieveVersions() throws ClientException {
        try {
            /**
             * in case the document is a proxy,meaning is the result of a publishing,to have the history of the document from which this proxy was
             * created,first we have to get to the version that was created when the document was publish,and to which the proxy document indicates,and
             * then from that version we have to get to the root document.
             */
            if (navigationContext.getCurrentDocument().isProxy()) {
                DocumentRef ref = navigationContext.getCurrentDocument().getRef();
                DocumentModel version = documentManager.getSourceDocument(ref);
                DocumentModel doc = documentManager.getSourceDocument(version.getRef());
                versionModelList = new ArrayList<VersionModel>(documentVersioning.getItemVersioningHistory(doc));
            } else {
                versionModelList = new ArrayList<VersionModel>(
                        documentVersioning.getCurrentItemVersioningHistory());
            }
            logDocumentWithTitle("Retrieved versions for: ",
                    navigationContext.getCurrentDocument());
        } catch (Throwable t) {
            throw EJBExceptionHandler.wrapException(t);
        }
    }

    /**
     * Restores the document to the selected version. If there is no selected
     * version it does nothing.
     *
     * @return the page that needs to be displayed next
     */
    public String restoreToVersion(VersionModel selectedVersion)
            throws ClientException {
        try {
            DocumentModel restoredDocument = documentManager.restoreToVersion(
                    navigationContext.getCurrentDocument().getRef(),
                    selectedVersion);
            //documentManager.checkOut(restoredDocument.getRef());

            logDocumentWithTitle("Restored to version: "
                    + selectedVersion.getLabel() + " the doc ",
                    restoredDocument);

            // same as edit basically
            // XXX AT: do edit events need to be sent?
            eventManager.raiseEventsOnDocumentChange(restoredDocument);
            return navigationContext.navigateToDocument(restoredDocument,
                    "after-edit");
        } catch (Throwable t) {
            throw EJBExceptionHandler.wrapException(t);
        }
    }

    public String viewArchivedVersion(VersionModel selectedVersion)
            throws ClientException {
        try {
            return navigationContext.navigateToDocument(
                    navigationContext.getCurrentDocument(), selectedVersion);
        } catch (Throwable t) {
            throw EJBExceptionHandler.wrapException(t);
        }
    }

    public boolean getCanRestore() throws ClientException {
        return documentManager.hasPermission(
                navigationContext.getCurrentDocument().getRef(),
                SecurityConstants.WRITE);
    }

    public void destroy() {
        log.debug("Removing SEAM action listener...");
    }

    /**
     * Tells if the current selected document is checked out or not.
     *
     * @return
     */
    public String getCheckedOut() throws ClientException {
        try {
            checkedOut = "Unknown";

            if (documentManager.isCheckedOut(navigationContext.getCurrentDocument().getRef())) {
                checkedOut = "Checked-out";
            } else {
                checkedOut = "Checked-in";
            }

            logDocumentWithTitle("Retrieved status " + checkedOut + " for: ",
                    navigationContext.getCurrentDocument());

            return checkedOut;
        } catch (Throwable t) {
            throw EJBExceptionHandler.wrapException(t);
        }
    }

    public void setCheckedOut(String checkedOut) {
        this.checkedOut = checkedOut;
    }

    /**
     * Checks the document out.
     *
     * @return the next page
     */
    public String checkOut() throws ClientException {
        try {
            documentManager.checkOut(navigationContext.getCurrentDocument().getRef());

            logDocumentWithTitle("Checked out ",
                    navigationContext.getCurrentDocument());

            return null;
        } catch (Throwable t) {
            throw EJBExceptionHandler.wrapException(t);
        }
    }

    /**
     * Checks the selected document in, with the selected version.
     *
     * @return
     */
    public String checkIn() throws ClientException {
        try {
            documentManager.checkIn(
                    navigationContext.getCurrentDocument().getRef(), newVersion);

            logDocumentWithTitle("Checked in ",
                    navigationContext.getCurrentDocument());

            retrieveVersions();

            // Type currentType =
            // typeManager.getType(currentDocument.getType());
            // return applicationController
            // .getPageOnEditedDocumentType(currentType);
            return navigationContext.getActionResult(
                    navigationContext.getCurrentDocument(),
                    UserAction.AFTER_EDIT);

        } catch (Throwable t) {
            throw EJBExceptionHandler.wrapException(t);
        }
    }

    @PrePassivate
    public void saveState() {
        log.debug("PrePassivate");
    }

    @PostActivate
    public void readState() {
        log.debug("PostActivate");
    }

    public DocumentModel getSourceDocument() throws ClientException {
        return documentManager.getSourceDocument(navigationContext.getCurrentDocument().getRef());
    }

}
