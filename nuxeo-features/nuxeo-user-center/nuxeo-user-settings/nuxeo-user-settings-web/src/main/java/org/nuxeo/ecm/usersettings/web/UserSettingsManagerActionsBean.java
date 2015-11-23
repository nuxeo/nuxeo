/*
 * (C) Copyright 2006-2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Christophe Capon
 *
 */

package org.nuxeo.ecm.usersettings.web;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.Set;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.usersettings.UserSettingsService;
import org.nuxeo.runtime.api.Framework;

/**
 * Personal user workspace manager actions bean.
 * 
 * @author Laurent Doguin
 * @author <a href="mailto:christophe.capon@vilogia.fr">Christophe Capon</a>
 * 
 */
@Name("userSettingsActions")
@Scope(CONVERSATION)
public class UserSettingsManagerActionsBean implements Serializable {

    private static final long serialVersionUID = 1828592026739219850L;

    protected String currentCategory = "Default";

    protected DocumentModelList currentSettingsProvider;

    private transient UserSettingsService userPreferencesService;

    @In(required = true)
    private transient CoreSession documentManager;

    private UserSettingsService getUserSettingsService() {
        if (userPreferencesService != null) {
            return userPreferencesService;
        }
        try {
            userPreferencesService = Framework.getService(UserSettingsService.class);
            return userPreferencesService;
        } catch (Exception e) {
            return null;
        }
    }

    public Set<String> getAvailableCategories() {
        return getUserSettingsService().getCategories();
    }

    public String getCurrentCategory() {
        return currentCategory;
    }

    public void setCurrentCategory(String currentCategory) throws ClientException{
        this.currentCategory = currentCategory;
        currentSettingsProvider = getUserSettingsService().getCurrentSettingsByCategory(documentManager, currentCategory);
    }

    public DocumentModelList getCurrentSettings() throws ClientException{
        if (currentSettingsProvider == null) {
            currentSettingsProvider = getUserSettingsService().getCurrentSettingsByCategory(documentManager, currentCategory);
        }
        return currentSettingsProvider;
    }

    public void saveCurrentSettings() throws ClientException{
        for (DocumentModel doc : currentSettingsProvider) {
            documentManager.saveDocument(doc);
        }
    }
    
    public void resetCurrentSettingCategory() throws ClientException {
        getUserSettingsService().resetSettingsCategory(documentManager, currentCategory);
        currentSettingsProvider = getUserSettingsService().getCurrentSettingsByCategory(documentManager, currentCategory);
    }
}
