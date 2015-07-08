/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *     Anahide Tchertchian
 */

package org.nuxeo.ecm.web.resources.jsf.negotiators;

import java.util.Map;

import javax.faces.context.FacesContext;

import org.nuxeo.theme.styling.negotiation.AbstractNegotiator;

public final class RequestParameter extends AbstractNegotiator {

    @Override
    public String getResult(String target, Object context) {
        FacesContext faces = null;
        if (context instanceof FacesContext) {
            faces = (FacesContext) context;
        } else {
            return null;
        }
        final Map<String, Object> parameters = faces.getExternalContext().getRequestMap();
        return (String) parameters.get(getProperty("param"));
    }

}
