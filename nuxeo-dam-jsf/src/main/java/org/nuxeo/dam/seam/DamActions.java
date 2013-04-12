/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.dam.AssetLibrary;
import org.nuxeo.dam.DamService;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.7
 */
@Name("damActions")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class DamActions implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String MAIN_TABS_DAM = "MAIN_TABS:dam";

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient WebActions webActions;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    protected String selectedNewAssetType;

    public String getSelectedDocumentId() {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        return currentDocument != null ? currentDocument.getId() : null;
    }

    public void setSelectedDocumentId(String selectedDocumentId)
            throws ClientException {
        DocumentModel selectedDocument = documentManager.getDocument(new IdRef(
                selectedDocumentId));
        navigationContext.setCurrentDocument(selectedDocument);
    }

    public void selectDocument(DocumentModel doc) throws ClientException {
        navigationContext.setCurrentDocument(doc);
    }

    public String getDamMainTab() {
        return MAIN_TABS_DAM;
    }

    public void setDamMainTab(String tabs) {
        webActions.setCurrentTabIds(!StringUtils.isBlank(tabs) ? tabs
                : MAIN_TABS_DAM);
    }

    /*
     * ----- Asset creation -----
     */

    public boolean getCanCreateInAssetLibrary() throws ClientException {
        AssetLibrary assetLibrary = getAssetLibrary();
        DocumentRef assetLibraryRef = new PathRef(assetLibrary.getPath());
        return documentManager.hasPermission(assetLibraryRef,
                SecurityConstants.ADD_CHILDREN);
    }

    public boolean getCanCreateNewAsset() throws ClientException {
        return !getAllowedAssetTypes().isEmpty()
                && getCanCreateInAssetLibrary();
    }

    public AssetLibrary getAssetLibrary() {
        return Framework.getLocalService(DamService.class).getAssetLibrary();
    }

    public List<Type> getAllowedAssetTypes() {
        return Framework.getLocalService(DamService.class).getAllowedAssetTypes();
    }

    /**
     * Gets the selected asset new asset type.
     * <p>
     * If selected type is null, initialize it to the first one, and initialize
     * the changeable document with this document type.
     */
    public String getSelectedNewAssetType() throws ClientException {
        if (selectedNewAssetType == null) {
            List<Type> allowedAssetTypes = getAllowedAssetTypes();
            if (!allowedAssetTypes.isEmpty()) {
                selectedNewAssetType = allowedAssetTypes.get(0).getId();
            }
            if (selectedNewAssetType != null) {
                selectNewAssetType();
            }
        }
        return selectedNewAssetType;
    }

    public void setSelectedNewAssetType(String selectedNewAssetType) {
        this.selectedNewAssetType = selectedNewAssetType;
    }

    public void selectNewAssetType() throws ClientException {
        String selectedType = getSelectedNewAssetType();
        if (selectedType == null) {
            // ignore
            return;
        }
        Map<String, Object> context = new HashMap<String, Object>();
        context.put(CoreEventConstants.PARENT_PATH,
                navigationContext.getCurrentDocument().getPathAsString());
        DocumentModel changeableDocument = documentManager.createDocumentModel(
                selectedType, context);
        navigationContext.setChangeableDocument(changeableDocument);
    }

    public void saveNewAsset() throws ClientException {
        DocumentModel changeableDocument = navigationContext.getChangeableDocument();
        if (changeableDocument.getId() != null) {
            return;
        }
        PathSegmentService pss = Framework.getLocalService(PathSegmentService.class);

        changeableDocument.setPathInfo(getAssetLibrary().getPath(),
                pss.generatePathSegment(changeableDocument));

        changeableDocument = documentManager.createDocument(changeableDocument);
        documentManager.save();

        // reset changeable document and selected type
        cancelNewAsset();

        facesMessages.add(StatusMessage.Severity.INFO,
                messages.get("document_saved"),
                messages.get(changeableDocument.getType()));
    }

    public void cancelNewAsset() {
        navigationContext.setChangeableDocument(null);
        setSelectedNewAssetType(null);
    }

    public String viewInDM() throws ClientException {
        webActions.setCurrentTabIds("MAIN_TABS:documents");
        return navigationContext.navigateToDocument(navigationContext.getCurrentDocument());
    }

}
