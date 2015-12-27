/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
