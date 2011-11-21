/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.localconfiguration.theme;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.ValueHolder;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.localconfiguration.LocalThemeConfig;
import org.nuxeo.theme.localconfiguration.LocalThemeHelper;
import org.nuxeo.theme.perspectives.PerspectiveManager;
import org.nuxeo.theme.perspectives.PerspectiveType;
import org.nuxeo.theme.styling.service.ThemeStylingService;
import org.nuxeo.theme.styling.service.descriptors.Flavor;
import org.nuxeo.theme.themes.ThemeManager;

@Name("themeConfigurationActions")
@Scope(CONVERSATION)
@Install(precedence = Install.FRAMEWORK)
public class ThemeConfigurationActions implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(ThemeConfigurationActions.class);

    @In(create = true)
    protected transient NavigationContext navigationContext;

    protected String theme;

    /**
     * @deprecated since 5.5: local theme configuration now only handles
     *             flavors.
     */
    @Deprecated
    public List<SelectItem> getAvailableThemes() {
        List<SelectItem> themes = new ArrayList<SelectItem>();
        for (String theme : ThemeManager.getThemeNames("jsf-facelets")) {
            themes.add(new SelectItem(theme, theme));
        }
        return themes;
    }

    /**
     * @deprecated since 5.5: local theme configuration now only handles
     *             flavors.
     */
    @Deprecated
    public List<SelectItem> getAvailablePages() {
        List<SelectItem> pages = new ArrayList<SelectItem>();
        if (theme != null && !theme.equals("")) {
            for (String pageName : Manager.getThemeManager().getPageNames(theme)) {
                pages.add(new SelectItem(pageName, pageName));
            }
        }
        return pages;
    }

    /**
     * @deprecated since 5.5: local theme configuration now only handles
     *             flavors.
     */
    @Deprecated
    public List<SelectItem> getAvailablePerspectives() {
        List<SelectItem> selectItemList = new ArrayList<SelectItem>();
        for (PerspectiveType perspectiveType : PerspectiveManager.listPerspectives()) {
            selectItemList.add(new SelectItem(perspectiveType.name,
                    perspectiveType.title));
        }
        return selectItemList;
    }

    /**
     * @deprecated since 5.5: local theme configuration now only handles
     *             flavors.
     */
    @Deprecated
    public void themeChange(ActionEvent event) {
        UIComponent select = event.getComponent().getParent();
        if (select instanceof ValueHolder) {
            theme = (String) ((ValueHolder) select).getValue();
        } else {
            log.error("Bad component returned " + select);
            throw new AbortProcessingException("Bad component returned "
                    + select);
        }
    }

    /**
     * Returns the layout to use for local configuration, to handle migration
     * to a flavor model
     *
     * @since 5.5
     * @return
     */
    public String getConfigurationLayout() {
        Boolean useOldThemeConf = Boolean.valueOf(Framework.getProperty(LocalThemeConfig.OLD_THEME_CONFIGURATION_PROPERTY));
        if (Boolean.TRUE.equals(useOldThemeConf)) {
            return "old_theme_configuration";
        }
        return "theme_configuration";
    }

    public List<Flavor> getAvailableFlavors(String themePage) {
        ThemeStylingService service = getStylingService();
        if (service == null) {
            return null;
        }
        return service.getFlavors(themePage);
    }

    public String getDefaultFlavorName(String themePage) {
        ThemeStylingService service = getStylingService();
        if (service == null) {
            return null;
        }
        return service.getDefaultFlavorName(themePage);
    }

    public Flavor getDefaultFlavor(String themePage) {
        ThemeStylingService service = getStylingService();
        if (service == null) {
            return null;
        }
        String flavorName = service.getDefaultFlavorName(themePage);
        if (flavorName != null) {
            return service.getFlavor(flavorName);
        }
        return null;
    }

    protected ThemeStylingService getStylingService() {
        try {
            ThemeStylingService service = Framework.getService(ThemeStylingService.class);
            if (service == null) {
                log.error("Missing ThemeStylingService");
                return null;
            }
            return service;
        } catch (Exception e) {
            log.error(e);
            return null;
        }
    }

    public String getCurrentLocalFlavorName() throws ClientException {
        DocumentModel currentSuperSpace = navigationContext.getCurrentSuperSpace();
        if (currentSuperSpace != null) {
            LocalThemeConfig localThemeConfig = LocalThemeHelper.getLocalThemeConfig(currentSuperSpace);
            if (localThemeConfig != null) {
                // extract the flavor
                String flavor = localThemeConfig.getFlavor();
                return flavor;
            }
        }
        return null;
    }

}
