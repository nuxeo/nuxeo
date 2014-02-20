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
package org.nuxeo.theme.jsf.facelets.vendor;

import java.io.IOException;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.view.facelets.Facelet;
import javax.faces.view.facelets.FaceletException;

/**
 * Facelet used when the underlying URL is not found on the server.
 * <p>
 * Used to fail gracefully, displaying the rest of the page, even if some
 * facelet fails to render at some point instead of stopping the rendering and
 * redirecting with a 404 error code.
 *
 * @since 5.4.2
 */
public class NotFoundFacelet extends Facelet {

    public final String error;

    public NotFoundFacelet(String error) {
        this.error = error;
    }

    @Override
    public void apply(FacesContext facesContext, UIComponent parent)
            throws IOException, FacesException, FaceletException, ELException {
        UIOutput c = new UIOutput();
        c.setId(facesContext.getViewRoot().createUniqueId());
        String errorMessage = "<span style=\"color:red;font-weight:bold;\">ERROR: some "
                + "facelet is not found: " + error + "</span><br />";
        c.setValue(errorMessage);
        parent.getChildren().add(c);
    }

}
