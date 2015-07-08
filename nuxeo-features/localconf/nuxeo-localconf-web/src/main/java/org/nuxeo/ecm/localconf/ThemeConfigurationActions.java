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

package org.nuxeo.ecm.localconf;

import java.io.Serializable;
import java.util.List;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.theme.localconfiguration.LocalThemeConfig;
import org.nuxeo.theme.localconfiguration.LocalThemeHelper;
import org.nuxeo.theme.styling.service.ThemeStylingService;
import org.nuxeo.theme.styling.service.descriptors.Flavor;

import static org.jboss.seam.ScopeType.CONVERSATION;

@Name("themeConfigurationActions")
@Scope(CONVERSATION)
@Install(precedence = Install.FRAMEWORK)
public class ThemeConfigurationActions implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected transient ThemeStylingService themeStylingService;

    protected String theme;

    /**
     * Returns the layout to use for local configuration, to handle migration to a flavor model
     *
     * @since 5.5
     */
    public String getConfigurationLayout() {
        return "theme_configuration";
    }

    public List<Flavor> getAvailableFlavors(String themePage) {
        return themeStylingService.getFlavors(themePage);
    }

    public String getDefaultFlavorName(String themePage) {
        return themeStylingService.getDefaultFlavorName(themePage);
    }

    public Flavor getDefaultFlavor(String themePage) {
        String flavorName = themeStylingService.getDefaultFlavorName(themePage);
        if (flavorName != null) {
            return themeStylingService.getFlavor(flavorName);
        }
        return null;
    }

    public String getCurrentLocalFlavorName() {
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
