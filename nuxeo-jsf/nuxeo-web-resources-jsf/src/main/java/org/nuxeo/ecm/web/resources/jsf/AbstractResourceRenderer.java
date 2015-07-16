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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.application.ProjectStage;
import javax.faces.application.Resource;
import javax.faces.component.UIComponent;
import javax.faces.component.UIParameter;
import javax.faces.context.FacesContext;

import org.nuxeo.ecm.web.resources.api.ResourceType;
import org.nuxeo.ecm.web.resources.api.service.WebResourceManager;
import org.nuxeo.runtime.api.Framework;

import com.sun.faces.config.WebConfiguration;
import com.sun.faces.renderkit.html_basic.HtmlBasicRenderer.Param;

/**
 * Base class for web resources resolution, factoring out helper methods for resources retrieval.
 *
 * @since 7.3
 */
public abstract class AbstractResourceRenderer extends ScriptStyleBaseRenderer {

    public static final String ENDPOINT_PATH = "/wapi/v1/resource/bundle/";

    public static final String COMPONENTS_PATH = "/bower_components/";

    protected static final Param[] EMPTY_PARAMS = new Param[0];

    /**
     * Resolve url either from src, looking up resource in the war, either from JSF resources, given a name (and
     * optional library).
     */
    protected String resolveUrl(FacesContext context, UIComponent component) throws IOException {
        Map<String, Object> attributes = component.getAttributes();
        String src = (String) attributes.get("src");
        String url;
        if (src != null) {
            url = resolveResourceFromSource(context, component, src);
        } else {
            String name = (String) attributes.get("name");
            String library = (String) attributes.get("library");
            url = resolveResourceUrl(context, component, library, name);
        }
        return url;
    }

    protected String resolveResourceFromSource(FacesContext context, UIComponent component, String src)
            throws UnsupportedEncodingException {
        String value = context.getApplication().getViewHandler().getResourceURL(context, src);
        return getUrlWithParams(context, component, value);
    }

    protected org.nuxeo.ecm.web.resources.api.Resource resolveNuxeoResource(FacesContext context,
            UIComponent component, String resource) throws UnsupportedEncodingException {
        WebResourceManager wrm = Framework.getService(WebResourceManager.class);
        return wrm.getResource(resource);
    }

    protected String resolveNuxeoResourcePath(org.nuxeo.ecm.web.resources.api.Resource resource) {
        if (resource == null) {
            return null;
        }
        String name = resource.getName();
        if (ResourceType.css.matches(resource)) {
            String suffixed = name;
            if (!suffixed.endsWith(ResourceType.css.getSuffix())) {
                suffixed += ResourceType.css.getSuffix();
            }
            return ENDPOINT_PATH + suffixed;
        } else if (ResourceType.js.matches(resource)) {
            String suffixed = name;
            if (!suffixed.endsWith(ResourceType.js.getSuffix())) {
                suffixed += ResourceType.js.getSuffix();
            }
            return ENDPOINT_PATH + suffixed;
        } else if (ResourceType.html.matches(resource)) {
            // assume html resources are copied to the war "components" sub-directory for now
            return COMPONENTS_PATH + resource.getPath();
        }
        // fallback on URI
        return resource.getURI();
    }

    protected String resolveNuxeoResourceUrl(FacesContext context, UIComponent component, String uri)
            throws UnsupportedEncodingException {
        String value = context.getApplication().getViewHandler().getResourceURL(context, uri);
        return getUrlWithParams(context, component, value);
    }

    protected String resolveResourceUrl(FacesContext context, UIComponent component, String library, String name) {
        Map<Object, Object> contextMap = context.getAttributes();

        String key = name + library;

        if (null == name) {
            return null;
        }

        // Ensure this import is not rendered more than once per request
        if (contextMap.containsKey(key)) {
            return null;
        }
        contextMap.put(key, Boolean.TRUE);

        // Special case of scripts that have query strings
        // These scripts actually use their query strings internally, not externally
        // so we don't need the resource to know about them
        int queryPos = name.indexOf("?");
        String query = null;
        if (queryPos > -1 && name.length() > queryPos) {
            query = name.substring(queryPos + 1);
            name = name.substring(0, queryPos);
        }

        Resource resource = context.getApplication().getResourceHandler().createResource(name, library);

        String resourceSrc = "RES_NOT_FOUND";

        WebConfiguration webConfig = WebConfiguration.getInstance();

        if (library == null
                && name != null
                && name.startsWith(webConfig.getOptionValue(WebConfiguration.WebContextInitParameter.WebAppContractsDirectory))) {

            if (context.isProjectStage(ProjectStage.Development)) {

                String msg = "Illegal path, direct contract references are not allowed: " + name;
                context.addMessage(component.getClientId(context), new FacesMessage(FacesMessage.SEVERITY_ERROR, msg,
                        msg));
            }
            resource = null;
        }

        if (resource == null) {

            if (context.isProjectStage(ProjectStage.Development)) {
                String msg = "Unable to find resource " + (library == null ? "" : library + ", ") + name;
                context.addMessage(component.getClientId(context), new FacesMessage(FacesMessage.SEVERITY_ERROR, msg,
                        msg));
            }

        } else {
            resourceSrc = resource.getRequestPath();
            if (query != null) {
                resourceSrc = resourceSrc + ((resourceSrc.indexOf("?") > -1) ? "&amp;" : "?") + query;
            }
            resourceSrc = context.getExternalContext().encodeResourceURL(resourceSrc);
        }

        return resourceSrc;
    }

    protected String getUrlWithParams(FacesContext context, UIComponent component, String src)
            throws UnsupportedEncodingException {
        // Write Anchor attributes

        Param paramList[] = getParamList(component);
        StringBuffer sb = new StringBuffer();
        sb.append(src);
        boolean paramWritten = false;
        for (int i = 0, len = paramList.length; i < len; i++) {
            String pn = paramList[i].name;
            if (pn != null && pn.length() != 0) {
                String pv = paramList[i].value;
                sb.append((paramWritten) ? '&' : '?');
                sb.append(URLEncoder.encode(pn, "UTF-8"));
                sb.append('=');
                if (pv != null && pv.length() != 0) {
                    sb.append(URLEncoder.encode(pv, "UTF-8"));
                }
                paramWritten = true;
            }
        }

        return context.getExternalContext().encodeResourceURL(sb.toString());
    }

    protected Param[] getParamList(UIComponent command) {
        if (command.getChildCount() > 0) {
            ArrayList<Param> parameterList = new ArrayList<Param>();

            for (UIComponent kid : command.getChildren()) {
                if (kid instanceof UIParameter) {
                    UIParameter uiParam = (UIParameter) kid;
                    if (!uiParam.isDisable()) {
                        Object value = uiParam.getValue();
                        Param param = new Param(uiParam.getName(), (value == null ? null : value.toString()));
                        parameterList.add(param);
                    }
                }
            }
            return parameterList.toArray(new Param[parameterList.size()]);
        } else {
            return EMPTY_PARAMS;
        }
    }

}
