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
package org.nuxeo.ecm.webapp.webcontainer;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.theme.negotiation.Negotiator;
import org.nuxeo.theme.negotiation.Scheme;
import org.nuxeo.theme.styling.service.ThemeStylingService;
import org.nuxeo.theme.styling.service.descriptors.Flavor;

/**
 * Negotiator that returns the default flavor configured for negotiated theme
 * page.
 *
 * @see ThemeStylingService
 * @see Flavor
 * @since 5.5
 */
public class DefaultThemeFlavor implements Scheme {

    private static final Log log = LogFactory.getLog(DefaultThemeFlavor.class);

    public String getOutcome(Object context) {
        FacesContext faces = (FacesContext) context;
        String theme = (String) faces.getExternalContext().getRequestMap().get(
                Negotiator.NEGOTIATION_RESULT_PREFIX
                        + Negotiator.NEGOTIATION_OBJECT.theme.name());
        if (theme != null) {
            try {
                ThemeStylingService service = Framework.getService(ThemeStylingService.class);
                return service.getDefaultFlavorName(theme);
            } catch (Exception e) {
                log.error(e);
            }
        }
        return null;
    }
}
