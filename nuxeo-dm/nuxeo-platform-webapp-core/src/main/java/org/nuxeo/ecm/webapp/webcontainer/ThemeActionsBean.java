/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
            currentFavicons = new ArrayList<IconDescriptor>();
            if (f != null) {
                List<IconDescriptor> icons = f.getFavicons();
                currentFavicons.addAll(icons);
            }
        }
        return currentFavicons;
    }

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