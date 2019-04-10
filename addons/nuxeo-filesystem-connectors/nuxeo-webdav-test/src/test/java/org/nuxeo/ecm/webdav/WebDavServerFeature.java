/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     dmetzler
 */
package org.nuxeo.ecm.webdav;

import java.io.IOException;

import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;
import org.nuxeo.ecm.webengine.app.WebEngineFilter;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestContextFilter;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionCleanupFilter;
import org.nuxeo.ecm.webengine.test.WebEngineFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.sun.grizzly.http.embed.GrizzlyWebServer;
import com.sun.grizzly.http.servlet.ServletAdapter;
import com.sun.jersey.spi.container.servlet.ServletContainer;

/**
 *
 *
 * @since 5.8
 */
@Features({ TransactionalFeature.class, CoreFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.types.api",
        "org.nuxeo.ecm.platform.types.core",
        "org.nuxeo.ecm.platform.dublincore",
        "org.nuxeo.ecm.platform.mimetype.api",
        "org.nuxeo.ecm.platform.mimetype.core",
        "org.nuxeo.ecm.platform.filemanager.api",
        "org.nuxeo.ecm.platform.filemanager.core" })
public class WebDavServerFeature extends WebEngineFeature {

    static boolean DEBUG = false;

    static String BASE_URI = "http://localhost/";

    static int PORT = 9998;

    private GrizzlyWebServer gws;

    @Override
    public void beforeRun(FeaturesRunner runner) {
        startGrizzly();
    }

    private void startGrizzly() {
        // static content is linked from here
        String path = "src/main/resources/www";

        gws = new GrizzlyWebServer(PORT, path);

        ServletAdapter jerseyAdapter = new ServletAdapter();
        // Using the portable way of registering JAX-RS resources.
        jerseyAdapter.addInitParameter("javax.ws.rs.Application",
                Application.class.getCanonicalName());
        jerseyAdapter.addRootFolder(path);
        jerseyAdapter.setHandleStaticResources(true);
        jerseyAdapter.setServletInstance(new ServletContainer());
        jerseyAdapter.setContextPath("");
        // session cleanup
        jerseyAdapter.addFilter(new RequestContextFilter(),
                "RequestContextFilter", null);
        jerseyAdapter.addFilter(new SessionCleanupFilter(),
                "SessionCleanupFilter", null);
        jerseyAdapter.addFilter(new NuxeoAuthenticationFilter(),
                "NuxeoAuthenticationFilter", null);
        jerseyAdapter.addFilter(new WebEngineFilter(),
                "WebEngineFilter", null);

        if (DEBUG) {
            jerseyAdapter.addInitParameter(
                    "com.sun.jersey.spi.container.ContainerRequestFilters",
                    "com.sun.jersey.api.container.filter.LoggingFilter");
            jerseyAdapter.addInitParameter(
                    "com.sun.jersey.spi.container.ContainerResponseFilters",
                    "com.sun.jersey.api.container.filter.LoggingFilter");
        }
        jerseyAdapter.addInitParameter(
                "com.sun.jersey.config.feature.logging.DisableEntitylogging",
                "true");

        gws.addGrizzlyAdapter(jerseyAdapter, new String[] { "" });

        // let Grizzly run
        try {
            gws.start();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void afterRun(FeaturesRunner runner) throws Exception {
        gws.stop();
    }
}
