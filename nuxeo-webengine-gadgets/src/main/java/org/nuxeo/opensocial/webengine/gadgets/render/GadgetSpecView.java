/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.opensocial.webengine.gadgets.render;

import static org.nuxeo.launcher.config.Environment.NUXEO_LOOPBACK_URL;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.opensocial.gadgets.service.InternalGadgetDescriptor;
import org.nuxeo.opensocial.service.api.OpenSocialService;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper to render the GadgetSpec via FreeMarker
 * <ul>
 * Using FreeMarker for that allows to have some dynamic computations:
 * <li>OAUth urls (that may be different depending on the IP the client uses)</li>
 * <li>resources urls</li>
 * <li>requests urls</li>
 * <li>...</li>
 * </ul>
 *
 * @author tiry
 */
public class GadgetSpecView {

    protected static GadgetSpecRenderingEngine engine;

    protected static List<String> trustedHosts;

    public static Gadgeti18n i18n = new Gadgeti18n();

    protected static List<String> getTrustedHosts() {
        if (trustedHosts == null) {
            OpenSocialService os = Framework.getLocalService(OpenSocialService.class);
            trustedHosts = new ArrayList<String>(
                    Arrays.asList(os.getTrustedHosts()));
        }
        return trustedHosts;
    }

    protected static boolean isTrustedHostAccess(String url) {
        for (String host : getTrustedHosts()) {
            if (url.startsWith("http://" + host)) {
                return true;
            }
        }
        return false;
    }

    protected static GadgetSpecRenderingEngine getEngine() {
        if (engine == null) {
            engine = new GadgetSpecRenderingEngine(new GadgetTemplateLoader());
        }

        return engine;
    }

    protected static boolean isInsideNuxeo(HttpServletRequest httpRequest) {

        String baseAccessUrl = VirtualHostHelper.getBaseURL(httpRequest);

        // let user override
        if ("true".equals(httpRequest.getParameter("external"))) {
            return false;
        }

        // test if we are called by local Nuxeo-Shindig
        if (!isTrustedHostAccess(baseAccessUrl)) {
            return false;
        }

        String remoteIP = httpRequest.getRemoteAddr();
        OpenSocialService os = Framework.getLocalService(OpenSocialService.class);
        return os.isTrustedHost(remoteIP);
    }

    protected static String getJSContext(Map<String, Object> input) {

        StringBuffer sb = new StringBuffer();

        sb.append("<script>\n");
        sb.append("var NXGadgetContext= {");

        for (String name : input.keySet()) {
            Object value = input.get(name);
            if (value instanceof String) {
                sb.append(name);
                sb.append(" : '");
                sb.append(value);
                sb.append("',\n");
            } else if (value instanceof Boolean) {
                sb.append(name);
                sb.append(" : ");
                sb.append(value.toString());
                sb.append(",\n");
            }
        }
        sb.append("ts");
        sb.append(" : '");
        sb.append(System.currentTimeMillis());
        sb.append("'\n");

        sb.append("};\n");
        sb.append("</script>\n");

        return sb.toString();
    }

    public static InputStream render(InternalGadgetDescriptor gadget,
            Map<String, Object> params) throws Exception {

        String key = "gadget://" + gadget.getName();

        WebContext ctx = WebEngine.getActiveContext();

        Map<String, Object> input = new HashMap<String, Object>();
        input.put("spec", gadget);
        HttpServletRequest httpRequest = ctx.getRequest();
        String specAccessUrl = VirtualHostHelper.getBaseURL(httpRequest);
        input.put("contextPath", VirtualHostHelper.getContextPathProperty());
        if (isInsideNuxeo(httpRequest)) {
            // we are called by local Nuxeo-Shindig
            // so we don't know the client URL, but a relative URL is ok
            input.put("serverSideBaseUrl",
                    Framework.getProperty(NUXEO_LOOPBACK_URL) + "/");
            input.put("clientSideBaseUrl",
                    VirtualHostHelper.getContextPathProperty() + "/");
            input.put("specDirectoryUrl",
                    VirtualHostHelper.getContextPathProperty()
                            + "/site/gadgets/" + gadget.getDirectory() + "/");
            input.put("insideNuxeo", true);
        } else {
            // we are called by an external gadget container
            // => we use the same url as the one used to fetch the gadget spec
            input.put("serverSideBaseUrl", specAccessUrl);
            input.put("clientSideBaseUrl", specAccessUrl);
            input.put("specDirectoryUrl", specAccessUrl + "site/gadgets/"
                    + gadget.getDirectory() + "/");
            input.put("insideNuxeo", false);
        }

        input.put("jsContext", getJSContext(input));
        input.put("i18n", i18n);

        if (params != null) {
            input.putAll(params);
        }

        input.put("contextHelper", NuxeoContextHelper.getInstance());

        // allow override / addition via request parameters
        Enumeration<String> pNames = httpRequest.getParameterNames();
        while (pNames.hasMoreElements()) {
            String name = pNames.nextElement();
            input.put(name, httpRequest.getParameter(name));
        }

        StringWriter writer = new StringWriter();
        getEngine().render(key, input, writer);

        return new ByteArrayInputStream(
                writer.getBuffer().toString().getBytes());

    }

}
