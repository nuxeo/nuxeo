/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Razvan Caraghin
 *     Florent Guillaume
 */

package org.nuxeo.ecm.webapp.versioning;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.ScopeType.EVENT;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.VersionModelImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.UserAction;
import org.nuxeo.ecm.webapp.helpers.EventManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * Deals with versioning actions.
 *
 * @author Razvan Caraghin
 * @author Florent Guillaume
 * @author Thierry Martins
 */
@Name("versionedActions")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class VersionedActionsBean implements VersionedActions, Serializable {

    private static final long serialVersionUID = 4472648747609642493L;

    private static final Log log = LogFactory.getLog(VersionedActionsBean.class);

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected transient FacesMessages facesMessages;

    @In(create = true, required = false)
    protected transient ResourcesAccessor resourcesAccessor;

    @In
    protected transient Context sessionContext;

    @In(required = false)
    @Out(required = false)
    protected VersionModel newVersion;

    @In(create = true)
    protected transient DocumentVersioning documentVersioning;

    protected transient List<VersionModel> versionModelList;

    protected String checkedOut;

    @Create
    public void initialize() {
        newVersion = new VersionModelImpl();
        sessionContext.set("newVersion", newVersion);
    }

    @Observer(value = { EventNames.DOCUMENT_SELECTION_CHANGED,
            EventNames.DOCUMENT_CHANGED,
            EventNames.DOCUMENT_SUBMITED_FOR_PUBLICATION,
            EventNames.DOCUMENT_PUBLISHED }, create = false, inject = false)
    @BypassInterceptors
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

    public void retrieveVersions() throws ClientException {
        /**
         * in case the document is a proxy,meaning is the result of a
         * publishing,to have the history of the document from which this proxy
         * was created,first we have to get to the version that was created when
         * the document was publish,and to which the proxy document
         * indicates,and then from that version we have to get to the root
         * document.
         */
        if (navigationContext.getCurrentDocument().isProxy()) {
            DocumentRef ref = navigationContext.getCurrentDocument().getRef();
            DocumentModel version = documentManager.getSourceDocument(ref);
            DocumentModel doc = documentManager.getSourceDocument(version.getRef());
            versionModelList = new ArrayList<VersionModel>(
                    documentVersioning.getItemVersioningHistory(doc));
        } else {
            versionModelList = new ArrayList<VersionModel>(
                    documentVersioning.getCurrentItemVersioningHistory());
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
        DocumentModel restoredDocument = documentManager.restoreToVersion(
                navigationContext.getCurrentDocument().getRef(),
                selectedVersion);
        documentManager.save();

        // same as edit basically
        // XXX AT: do edit events need to be sent?
        EventManager.raiseEventsOnDocumentChange(restoredDocument);
        return navigationContext.navigateToDocument(restoredDocument,
                "after-edit");
    }

    public String viewArchivedVersion(VersionModel selectedVersion)
            throws ClientException {
        return navigationContext.navigateToDocument(
                navigationContext.getCurrentDocument(), selectedVersion);
    }

    public boolean getCanRestore() throws ClientException {
        // TODO: should check for a specific RESTORE permission instead
        return documentManager.hasPermission(
                navigationContext.getCurrentDocument().getRef(),
                SecurityConstants.WRITE_PROPERTIES);
    }

    /**
     * Tells if the current selected document is checked out or not.
     *
     * @return
     */
    public String getCheckedOut() throws ClientException {
        if (documentManager.isCheckedOut(navigationContext.getCurrentDocument().getRef())) {
            checkedOut = "Checked-out";
        } else {
            checkedOut = "Checked-in";
        }
        return checkedOut;
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
        documentManager.checkOut(navigationContext.getCurrentDocument().getRef());
        return null;
    }

    /**
     * Checks the selected document in, with the selected version.
     *
     * @return
     */
    public String checkIn() throws ClientException {
        documentManager.checkIn(
                navigationContext.getCurrentDocument().getRef(), newVersion);
        retrieveVersions();
        return navigationContext.getActionResult(
                navigationContext.getCurrentDocument(), UserAction.AFTER_EDIT);
    }

    public DocumentModel getSourceDocument() throws ClientException {
        return documentManager.getSourceDocument(navigationContext.getCurrentDocument().getRef());
    }

    public boolean canRemoveArchivedVersion(VersionModel selectedVersion) {
        try {
            DocumentRef docRef = navigationContext.getCurrentDocument().getRef();
            DocumentModel docVersion = documentManager.getDocumentWithVersion(
                    docRef, selectedVersion);
            return documentManager.canRemoveDocument(docVersion.getRef());
        } catch (ClientException e) {
            log.debug("ClientException in canRemoveArchivedVersion: "
                    + e.getMessage());
            return false;
        }
    }

    public String removeArchivedVersion(VersionModel selectedVersion)
            throws ClientException {
        DocumentRef docRef = navigationContext.getCurrentDocument().getRef();
        DocumentModel docVersion = documentManager.getDocumentWithVersion(
                docRef, selectedVersion);
        documentManager.removeDocument(docVersion.getRef());
        documentManager.save();
        resetVersions();
        facesMessages.add(FacesMessage.SEVERITY_INFO,
                resourcesAccessor.getMessages().get(
                        "feedback.versioning.versionRemoved"));
        return null;
    }

}
