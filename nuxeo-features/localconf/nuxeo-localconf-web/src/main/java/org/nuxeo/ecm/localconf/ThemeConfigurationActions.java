/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.theme.styling.service.descriptors.FlavorDescriptor;

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

    public List<FlavorDescriptor> getAvailableFlavors(String themePage) {
        return themeStylingService.getFlavors(themePage);
    }

    public String getDefaultFlavorName(String themePage) {
        return themeStylingService.getDefaultFlavorName(themePage);
    }

    public FlavorDescriptor getDefaultFlavor(String themePage) {
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
