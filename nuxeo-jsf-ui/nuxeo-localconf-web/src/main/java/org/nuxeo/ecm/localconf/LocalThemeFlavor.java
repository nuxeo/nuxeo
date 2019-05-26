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
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.localconf;

import java.util.List;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Component;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.theme.localconfiguration.LocalThemeConfig;
import org.nuxeo.theme.localconfiguration.LocalThemeHelper;
import org.nuxeo.theme.styling.negotiation.AbstractNegotiator;
import org.nuxeo.theme.styling.service.ThemeStylingService;

/**
 * Returns the flavor associated to the current space (workspace, section, ...) as a String. Return null otherwise.
 *
 * @since 5.5
 */
public class LocalThemeFlavor extends AbstractNegotiator {

    private static final Log log = LogFactory.getLog(LocalThemeFlavor.class);

    @Override
    public String getResult(String target, Object context) {
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
        FacesContext faces = null;
        if (context instanceof FacesContext) {
            faces = (FacesContext) context;
        } else {
            return null;
        }
        String theme = (String) faces.getExternalContext().getRequestMap().get(getProperty("negotiatedPageVariable"));
        if (theme != null) {
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
                    log.debug(String.format("Local configuration: current theme page "
                            + "'%s' does not accept the flavor '%s'", theme, flavor));
                }
            }
        }
        return null;
    }
}
