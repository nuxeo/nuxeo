/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.tag.handler;

import java.io.IOException;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletHandler;

/**
 * Leaf Facelet Handler (facelet handler that does nothing).
 * <p>
 * Used when there is no next handler to apply, as next handler can never be null.
 *
 * @since 7.4
 */
public class LeafFaceletHandler implements FaceletHandler {

    public LeafFaceletHandler() {
    }

    public void apply(FaceletContext ctx, UIComponent parent) throws IOException, FacesException, ELException {
    }

    @Override
    public String toString() {
        return "FaceletHandler Tail";
    }

}
