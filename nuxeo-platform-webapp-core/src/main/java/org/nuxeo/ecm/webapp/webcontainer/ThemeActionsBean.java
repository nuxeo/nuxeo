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

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.theme.ApplicationType;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.NegotiationDef;
import org.nuxeo.theme.negotiation.Negotiator;
import org.nuxeo.theme.styling.service.ThemeStylingService;
import org.nuxeo.theme.styling.service.descriptors.Flavor;
import org.nuxeo.theme.styling.service.descriptors.Logo;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.types.TypeRegistry;

@Name("themeActions")
@Scope(ScopeType.PAGE)
public class ThemeActionsBean implements ThemeActions {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(ThemeActionsBean.class);

    public String getDefaultTheme() {
        FacesContext faces = FacesContext.getCurrentInstance();
        return getDefaultTheme(faces.getExternalContext());
    }

    protected String getDefaultTheme(ExternalContext ec) {
        // get the negotiation strategy
        TypeRegistry typeRegistry = Manager.getTypeRegistry();
        ApplicationType app = (ApplicationType) typeRegistry.lookup(
                TypeFamily.APPLICATION, ec.getRequestContextPath());
        NegotiationDef negotiation = app.getNegotiation();
        return negotiation.getDefaultTheme();
    }

    public Logo getLogo() {
        FacesContext faces = FacesContext.getCurrentInstance();
        final ExternalContext ec = faces.getExternalContext();
        String flavor = (String) ec.getRequestMap().get(
                Negotiator.NEGOTIATION_RESULT_PREFIX
                        + Negotiator.NEGOTIATION_OBJECT.collection.name());
        return getLogo(flavor);
    }

    public Logo getLogo(String flavorName) {
        if (flavorName == null) {
            return null;
        }
        ThemeStylingService service = getStylingService();
        if (service == null) {
            return null;
        }
        return service.getLogo(flavorName);
    }

    public Flavor getFlavor(String flavorName) {
        if (flavorName == null) {
            return null;
        }
        ThemeStylingService service = getStylingService();
        if (service == null) {
            return null;
        }
        return service.getFlavor(flavorName);
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

}