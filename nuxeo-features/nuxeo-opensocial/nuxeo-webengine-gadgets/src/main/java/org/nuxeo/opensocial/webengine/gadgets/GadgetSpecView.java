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

package org.nuxeo.opensocial.webengine.gadgets;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.opensocial.gadgets.service.InternalGadgetDescriptor;
import org.nuxeo.opensocial.service.api.OpenSocialService;
import org.nuxeo.runtime.api.Framework;

import freemarker.cache.StringTemplateLoader;

/**
 * Helper to render the GadgetSpec via FreeMarker
 *
 * Using FreeMarker for that allow to have some dynamic computations :
 *  - OAUth urls (that may be different depending on the IP the client uses)
 *  - resources urls
 *  - requests urls
 *  - ...
 *
 * @author tiry
 *
 */
public class GadgetSpecView{

    protected static StringTemplateLoader specLoader = new StringTemplateLoader();

    protected static GadgetSpecRenderingEngine engine;

    protected static List<String> trustedHosts;

    protected static List<String> getTrustedHosts() {
        if (trustedHosts==null) {
            trustedHosts = new ArrayList<String>();
            OpenSocialService os = Framework.getLocalService(OpenSocialService.class);
            for (String host : os.getTrustedHosts()) {
                trustedHosts.add(host);
            }
        }
        return trustedHosts;
    }

    protected static boolean isTrustedHostAccess(String url) {
        for (String host : getTrustedHosts()) {
            if (url.startsWith("http://"+ host)) {
                return true;
            }
        }
        return false;
    }

    protected static GadgetSpecRenderingEngine getEngine() {
        if (engine==null) {
            engine = new GadgetSpecRenderingEngine(specLoader);
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
        for (String host : getTrustedHosts()) {
            if (host.contains(remoteIP)) {
                return true;
            }
        }
        return false;
    }

    public static InputStream render(InternalGadgetDescriptor gadget, Map<String, Object> params) throws Exception {

        String key = "fs://" + gadget.getMountPoint() + "/" + gadget.getEntryPoint();
        synchronized (specLoader) {
            if (specLoader.findTemplateSource(key)==null) {
                String specData = FileUtils.read(gadget.getResourceAsStream(gadget.getEntryPoint()));
                specLoader.putTemplate(key, specData);
            }
        }

        WebContext ctx = WebEngine.getActiveContext();

        Map<String, Object> input = new HashMap<String, Object>();
        input.put("spec", gadget);
        HttpServletRequest httpRequest = ctx.getRequest();
        String specAccessUrl = VirtualHostHelper.getBaseURL(httpRequest);
        input.put("serverSideBaseUrl", specAccessUrl);
        input.put("contextPath", VirtualHostHelper.getContextPathProperty());
        if (isInsideNuxeo(httpRequest)) {
            // we are called by local Nuxeo-Shindig
            // so we don't know the client URL, but a relative URL is ok
            input.put("clientSideBaseUrl", VirtualHostHelper.getContextPathProperty() + "/");
            input.put("insideNuxeo", true);
        } else {
            // we are called by an external gadget container
            // => we use the same url as the one used to fetch the gadget spec
            input.put("clientSideBaseUrl", specAccessUrl);
            input.put("insideNuxeo", false);
        }

        if (params!=null) {
            input.putAll(params);
        }

        // allow override / addition via request parameters
        Enumeration<String> pNames = httpRequest.getParameterNames();
        while (pNames.hasMoreElements()) {
            String name = pNames.nextElement();
            input.put(name,httpRequest.getParameter(name));
        }

        StringWriter writer = new StringWriter();
        getEngine().render(key, input, writer);

        return new ByteArrayInputStream(writer.getBuffer().toString().getBytes());

    }



}
