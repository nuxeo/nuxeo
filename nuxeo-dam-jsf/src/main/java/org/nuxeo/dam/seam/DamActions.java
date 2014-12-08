/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.dam.seam;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;
import static org.nuxeo.dam.DamConstants.ASSETS_VIEW_ID;
import static org.nuxeo.dam.DamConstants.DAM_MAIN_TAB_ACTION;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;

import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.dam.AssetLibrary;
import org.nuxeo.dam.DamService;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;
import org.nuxeo.ecm.webapp.action.MainTabsActions;
import org.nuxeo.ecm.webapp.contentbrowser.DocumentActions;
import org.nuxeo.runtime.api.Framework;

/**
 * Handles global DAM actions (selected asset, asset creation, ...).
 *
 * @since 5.7
 */
@Name("damActions")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class DamActions implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String MAIN_TABS_DAM = "MAIN_TABS:dam";

    public static final String MAIN_TABS_DOCUMENT_MANAGEMENT = "MAIN_TABS:documents";

    public static final String DAM_ID_PATTERN = "damid";

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient WebActions webActions;

    @In(create = true)
    protected transient DocumentActions documentActions;

    @In(create = true)
    protected transient MainTabsActions mainTabsActions;

    public String getSelectedDocumentId() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        return currentDocument != null ? currentDocument.getId() : null;
    }

    public void setSelectedDocumentId(String selectedDocumentId) throws ClientException {
        DocumentModel selectedDocument = documentManager.getDocument(new IdRef(selectedDocumentId));
        selectDocument(selectedDocument);
    }

    public void selectDocument(DocumentModel doc) throws ClientException {
        navigationContext.setCurrentDocument(doc);
        mainTabsActions.setDocumentFor(DAM_MAIN_TAB_ACTION, doc);
    }

    public String getDamMainTab() {
        return MAIN_TABS_DAM;
    }

    public void setDamMainTab(String tabs) {
        webActions.setCurrentTabIds(!StringUtils.isBlank(tabs) ? tabs : MAIN_TABS_DAM);
    }

    public String viewInDM() throws ClientException {
        webActions.setCurrentTabIds(MAIN_TABS_DOCUMENT_MANAGEMENT);
        return navigationContext.navigateToDocument(navigationContext.getCurrentDocument());
    }

    /**
     * @deprecated since 5.9.5. Use {@code #getAssetPermanentLinkUrl}.
     */
    public String viewInDAM() throws ClientException, UnsupportedEncodingException {
        return getAssetPermanentLinkUrl(false);
    }

    public String updateCurrentDocument() throws ClientException {
        documentActions.updateCurrentDocument();
        return null;
    }

    public boolean getCanCreateInAssetLibrary() throws ClientException {
        AssetLibrary assetLibrary = getAssetLibrary();
        DocumentRef assetLibraryRef = new PathRef(assetLibrary.getPath());
        return documentManager.hasPermission(assetLibraryRef, SecurityConstants.ADD_CHILDREN);
    }

    public AssetLibrary getAssetLibrary() {
        return Framework.getLocalService(DamService.class).getAssetLibrary();
    }

    /**
     * Returns true if the user is viewing DAM.
     */
    public boolean isOnDamView() {
        if (FacesContext.getCurrentInstance() == null) {
            return false;
        }

        UIViewRoot viewRoot = FacesContext.getCurrentInstance().getViewRoot();
        if (viewRoot != null) {
            String viewId = viewRoot.getViewId();
            // FIXME find a better way to update the current document only
            // if we are on DAM
            if (ASSETS_VIEW_ID.equals(viewId)) {
                return true;
            }
        }
        return false;
    }

    public boolean isOnAssetsView() {
        if (FacesContext.getCurrentInstance() == null) {
            return false;
        }

        UIViewRoot viewRoot = FacesContext.getCurrentInstance().getViewRoot();
        if (viewRoot != null) {
            String viewId = viewRoot.getViewId();
            // FIXME find a better way to know if we are on the assets view
            if (ASSETS_VIEW_ID.equals(viewId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the download URL for the current document.
     *
     * @since 5.9.4
     */
    public String getDownloadURL() {
        return DocumentModelFunctions.bigFileUrl(navigationContext.getCurrentDocument(), "blobholder:0", "");
    }

    /**
     * Returns the permanent link of an asset.
     *
     * @since 5.9.5
     */
    public String getAssetPermanentLinkUrl(boolean newConversation) throws ClientException,
            UnsupportedEncodingException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        return DocumentModelFunctions.documentUrl(DAM_ID_PATTERN, currentDocument, "asset", null, newConversation);
    }
}
