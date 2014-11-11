/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.localconfiguration.theme.negociation;

import java.util.List;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Component;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.theme.localconfiguration.LocalThemeConfig;
import org.nuxeo.theme.localconfiguration.LocalThemeHelper;
import org.nuxeo.theme.negotiation.Negotiator;
import org.nuxeo.theme.negotiation.Scheme;
import org.nuxeo.theme.styling.service.ThemeStylingService;

/**
 * Returns the flavor associated to the current space (workspace, section, ...)
 * as a String. Return null otherwise.
 *
 * @since 5.5
 */
public class LocalThemeFlavor implements Scheme {

    private static final Log log = LogFactory.getLog(LocalThemeFlavor.class);

    public String getOutcome(Object context) {
        Boolean useOldThemeConf = Boolean.valueOf(Framework.getProperty(LocalThemeConfig.OLD_THEME_CONFIGURATION_PROPERTY));
        if (Boolean.TRUE.equals(useOldThemeConf)) {
            return null;
        }

        DocumentModel currentSuperSpace = (DocumentModel) Component.getInstance("currentSuperSpace");
        if (currentSuperSpace == null) {
            return null;
        }

        // Get the placeful local theme configuration for the current
        // workspace.
        LocalThemeConfig localThemeConfig = LocalThemeHelper.getLocalThemeConfig(currentSuperSpace);
        if (localThemeConfig == null) {
            return null;
        }

        // extract the flavor
        String flavor = localThemeConfig.getFlavor();
        if (flavor == null) {
            return null;
        }

        // Check that the theme page accepts this flavor
        FacesContext faces = (FacesContext) context;
        String theme = (String) faces.getExternalContext().getRequestMap().get(
                Negotiator.NEGOTIATION_RESULT_PREFIX
                        + Negotiator.NEGOTIATION_OBJECT.theme.name());
        if (theme != null) {
            try {
                ThemeStylingService service = Framework.getService(ThemeStylingService.class);
                if (service == null) {
                    log.error("Could not find the ThemeStylingService");
                    return null;
                }
                List<String> flavors = service.getFlavorNames(theme);
                if (flavors != null && flavors.contains(flavor)) {
                    return flavor;
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format(
                                "Local configuration: current theme page "
                                        + "'%s' does not accept the flavor '%s'",
                                theme, flavor));
                    }
                }
            } catch (Exception e) {
                log.error(e);
            }
        }
        return null;
    }
}
