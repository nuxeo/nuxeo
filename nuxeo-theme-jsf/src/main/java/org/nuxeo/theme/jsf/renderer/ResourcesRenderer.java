/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.theme.jsf.renderer;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.faces.application.ResourceDependencies;
import javax.faces.application.ResourceDependency;
import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.servlet.http.HttpServletRequest;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.theme.html.Utils;
import org.nuxeo.theme.html.ui.ThemeStyles;
import org.nuxeo.theme.jsf.component.UIResources;
import org.nuxeo.theme.themes.ThemeManager;

import com.sun.faces.renderkit.html_basic.ScriptStyleBaseRenderer;

/**
 * Renderer for {@link UIResources} component.
 *
 * @since 5.9.4-JSF2
 */
@ResourceDependencies({
        @ResourceDependency(library = "javax.faces", name = "jsf.js"),
        @ResourceDependency(name = "jquery.js") })
public class ResourcesRenderer extends ScriptStyleBaseRenderer {

    @Override
    protected void startElement(ResponseWriter writer, UIComponent component)
            throws IOException {
    }

    @Override
    protected void endElement(ResponseWriter writer) throws IOException {
    }

    @Override
    public void encodeEnd(FacesContext context, UIComponent component)
            throws IOException {

        Map<String, Object> attributes = component.getAttributes();
        String cache = (String) attributes.get("cache");
        String inline = (String) attributes.get("inline");
        String theme = (String) attributes.get("theme");

        final ResponseWriter writer = context.getResponseWriter();
        final ExternalContext externalContext = context.getExternalContext();

        Map<String, Object> requestMap = externalContext.getRequestMap();
        final URL themeUrl = (URL) requestMap.get("org.nuxeo.theme.url");
        if (theme == null) {
            theme = ThemeManager.getThemeNameByUrl(themeUrl);
        }

        Map<String, String> params = new HashMap<String, String>();

        params.put("themeName", theme);
        params.put("path", externalContext.getRequestContextPath());
        // FIXME: use configuration
        String basePath = Framework.getProperty("org.nuxeo.ecm.contextPath",
                "/nuxeo");
        params.put("basepath", basePath);
        String collectionName = ThemeManager.getCollectionNameByUrl(themeUrl);
        params.put("collection", collectionName);

        Boolean virtualHosting = Utils.isVirtualHosting((HttpServletRequest) externalContext.getRequest());
        writer.write(ThemeStyles.render(params, Boolean.parseBoolean(cache),
                Boolean.parseBoolean(inline), virtualHosting));
    }

}
