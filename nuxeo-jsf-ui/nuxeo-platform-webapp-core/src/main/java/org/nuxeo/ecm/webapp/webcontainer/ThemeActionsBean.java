/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     stan
 */

package org.nuxeo.ecm.webapp.webcontainer;

import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.theme.styling.service.ThemeStylingService;
import org.nuxeo.theme.styling.service.descriptors.FlavorDescriptor;
import org.nuxeo.theme.styling.service.descriptors.IconDescriptor;
import org.nuxeo.theme.styling.service.descriptors.LogoDescriptor;

@Name("themeActions")
@Scope(ScopeType.PAGE)
public class ThemeActionsBean implements ThemeActions {

    private static final long serialVersionUID = 1L;

    protected String defaultPage;

    protected String currentPage;

    protected String currentFlavor;

    protected LogoDescriptor currentLogo;

    protected List<IconDescriptor> currentFavicons;

    @In(create = true, required = false)
    protected transient ThemeStylingService themeStylingService;

    @Override
    public String getDefaultTheme() {
        if (defaultPage == null) {
            defaultPage = themeStylingService.negotiate("jsfDefaultPage", FacesContext.getCurrentInstance());
        }
        return defaultPage;
    }

    @Override
    public LogoDescriptor getLogo() {
        if (currentLogo == null) {
            String flavor = getCurrentFlavor();
            currentLogo = getLogo(flavor);
        }
        return currentLogo;
    }

    /**
     * Returns favicons for current flavor.
     *
     * @since 7.4
     */
    public List<IconDescriptor> getFavicons() {
        if (currentFavicons == null) {
            String flavor = getCurrentFlavor();
            FlavorDescriptor f = themeStylingService.getFlavor(flavor);
            currentFavicons = new ArrayList<>();
            if (f != null) {
                List<IconDescriptor> icons = f.getFavicons();
                currentFavicons.addAll(icons);
            }
        }
        return currentFavicons;
    }

    @Override
    public String getCurrentFlavor() {
        if (currentFlavor == null) {
            // put current page name to request for flavor negotiation
            FacesContext faces = FacesContext.getCurrentInstance();
            HttpServletRequest request = (HttpServletRequest) faces.getExternalContext().getRequest();
            request.setAttribute("jsfPage", getCurrentPage());
            currentFlavor = themeStylingService.negotiate("jsfFlavor", FacesContext.getCurrentInstance());
        }
        return currentFlavor;
    }

    @Override
    public String getCurrentFlavor(String pageName) {
        if (currentPage == null || !currentPage.equals(pageName)) {
            setCurrentPage(pageName);
            // reset current flavor to resolve it again
            currentFlavor = null;
        }
        return getCurrentFlavor();
    }

    @Override
    public String getCurrentPage() {
        if (currentPage == null) {
            currentPage = themeStylingService.negotiate("jsfPage", FacesContext.getCurrentInstance());
            if (currentPage == null) {
                // BBB
                currentPage = getDefaultTheme();
            }
        }
        return currentPage;
    }

    @Override
    public void setCurrentPage(String pageName) {
        currentPage = pageName;
    }

    @Override
    public LogoDescriptor getLogo(String flavorName) {
        if (flavorName == null) {
            return null;
        }
        return themeStylingService.getLogo(flavorName);
    }

    @Override
    public FlavorDescriptor getFlavor(String flavorName) {
        if (flavorName == null) {
            return null;
        }
        return themeStylingService.getFlavor(flavorName);
    }

}
