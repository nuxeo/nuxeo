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
package org.nuxeo.ecm.web.resources.jsf;

import java.io.IOException;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.render.Renderer;

import com.sun.faces.renderkit.html_basic.HeadRenderer;

/**
 * Renderer for resources added as component resources to the {@link UIViewRoot} for given target.
 * <p>
 * Plays a role similar role to the {@link HeadRenderer} when JSF resources are declared with target "head", useful to
 * place js resources at the end of the page, for instance.
 *
 * @since 7.4
 */
public class ResourceInsertRenderer extends Renderer {

    public static final String RENDERER_TYPE = "org.nuxeo.ecm.web.resources.jsf.ResourceInsert";

    @Override
    public boolean getRendersChildren() {
        return true;
    }

    @Override
    public void encodeChildren(FacesContext context, UIComponent component) throws IOException {
        if (context == null || component == null) {
            throw new NullPointerException();
        }
        Map<String, Object> attributes = component.getAttributes();
        String target = (String) attributes.get("target");
        UIViewRoot viewRoot = context.getViewRoot();
        for (UIComponent resource : viewRoot.getComponentResources(context, target)) {
            resource.encodeAll(context);
        }
    }

    @Override
    public void decode(FacesContext context, UIComponent component) {
        // NOOP
    }

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        // NOOP
    }

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        // NOOP
    }

}
