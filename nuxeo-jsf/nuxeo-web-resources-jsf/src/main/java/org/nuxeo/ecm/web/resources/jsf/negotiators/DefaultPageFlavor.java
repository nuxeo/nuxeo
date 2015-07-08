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
package org.nuxeo.ecm.web.resources.jsf.negotiators;

import javax.faces.context.FacesContext;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.theme.styling.negotiation.AbstractNegotiator;
import org.nuxeo.theme.styling.service.ThemeStylingService;
import org.nuxeo.theme.styling.service.descriptors.Flavor;

/**
 * Negotiator that returns the default flavor configured for negotiated theme page.
 *
 * @see ThemeStylingService
 * @see Flavor
 * @since 5.5
 */
public class DefaultPageFlavor extends AbstractNegotiator {

    @Override
    public String getResult(String target, Object context) {
        FacesContext faces = null;
        if (context instanceof FacesContext) {
            faces = (FacesContext) context;
        } else {
            return null;
        }
        String theme = (String) faces.getExternalContext().getRequestMap().get(getProperty("negotiatedPageVariable"));
        if (theme != null) {
            ThemeStylingService service = Framework.getService(ThemeStylingService.class);
            return service.getDefaultFlavorName(theme);
        }
        return null;
    }
}
