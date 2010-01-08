/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.chemistry.jaxrs;

import javax.servlet.http.HttpServletRequest;

import org.apache.chemistry.atompub.server.jaxrs.AbderaResource;
import org.apache.chemistry.atompub.server.jaxrs.AbderaResponseProvider;
import org.apache.chemistry.atompub.server.jaxrs.AbderaResource.PathMunger;
import org.nuxeo.ecm.core.chemistry.impl.NuxeoRepository;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Nuxeo Runtime Component whose activation is used to register things not
 * available through extension points: additional JAX-RS Providers, repository
 * for Chemistry.
 *
 * @author Florent Guillaume
 */
public class JaxrsNuxeoComponent extends DefaultComponent {

    @Override
    public void activate(ComponentContext context) throws Exception {
        // TODO should be done using an extension point
        WebEngine engine = Framework.getLocalService(WebEngine.class);
        engine.getRegistry().addMessageBodyWriter(new AbderaResponseProvider());

        // TODO should be done differently, not using a static field
        AbderaResource.repository = new NuxeoRepository("default");
        AbderaResource.pathMunger = new WebEnginePathMunger();

        // We have to set this so that a %2F is allowed in a URL. This is needed
        // to interpret the CMIS URI template "objectbypath".
        System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");
    }

    /**
     * Munges the request path according to WebEngine rules.
     * <p>
     * Uses the Nuxeo-Webengine-Base-Path header to provide a fake context &
     * servlet path.
     */
    public static class WebEnginePathMunger implements PathMunger {
        public ContextAndServletPath munge(HttpServletRequest request,
                String contextPath, String servletPath) {
            ContextAndServletPath cs = new ContextAndServletPath();
            String basePath = request.getHeader(WebContext.NUXEO_WEBENGINE_BASE_PATH);
            if (",".equals(basePath)) {
                basePath = ""; // copied from AbstractWebContext#getBasePath
            }
            if (basePath == null) {
                cs.contextPath = contextPath;
                cs.servletPath = servletPath;
            } else {
                // replace context + servlet with our own base path
                if (!basePath.startsWith("/")) {
                    basePath = '/' + basePath;
                }
                if (basePath.equals("/")) {
                    basePath = "";
                } else if (basePath.endsWith("/")) {
                    basePath = basePath.substring(0, basePath.length() - 1);
                }
                cs.contextPath = "";
                cs.servletPath = basePath;
            }
            return cs;
        }
    }

}
