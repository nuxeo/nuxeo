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

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.theme.negotiation.Negotiator;
import org.nuxeo.theme.negotiation.Scheme;
import org.nuxeo.theme.styling.service.ThemeStylingService;

/**
 * @since 5.4.3
 */
public class DefaultThemeFlavor implements Scheme {

    private static final Log log = LogFactory.getLog(DefaultThemeFlavor.class);

    public String getOutcome(Object context) {
        FacesContext faces = (FacesContext) context;
        // FIXME: retrieve current theme returned by negotiation instead of
        // using default theme
        String defaultTheme = (String) faces.getExternalContext().getRequestMap().get(
                Negotiator.NEGOTIATION_RESULT_PREFIX
                        + Negotiator.NEGOTIATION_OBJECT.theme.name());
        if (defaultTheme != null) {
            try {
                ThemeStylingService service = Framework.getService(ThemeStylingService.class);
                return service.getDefaultFlavor(defaultTheme);
            } catch (Exception e) {
                log.error(e);
            }
        }
        return null;
    }

}
