/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.webdav;

import java.io.IOException;

import javax.ws.rs.core.Application;

import org.nuxeo.ecm.core.test.CoreFeature;
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
import com.sun.jersey.api.core.ApplicationAdapter;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;

/**
 * @since 5.8
 */
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.types.api", //
        "org.nuxeo.ecm.platform.types.core", //
        "org.nuxeo.ecm.platform.dublincore", //
        "org.nuxeo.ecm.platform.filemanager.api", //
        "org.nuxeo.ecm.platform.filemanager.core", //
        "org.nuxeo.ecm.platform.filemanager.core.listener" })
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
        Application app = new org.nuxeo.ecm.webdav.Application();
        ApplicationAdapter conf = new ApplicationAdapter(app);
        conf.getFeatures().put(ResourceConfig.FEATURE_MATCH_MATRIX_PARAMS, Boolean.TRUE);
        jerseyAdapter.setServletInstance(new ServletContainer(conf));
        jerseyAdapter.addRootFolder(path);
        jerseyAdapter.setHandleStaticResources(true);
        jerseyAdapter.setContextPath("");
        // session cleanup
        jerseyAdapter.addFilter(new RequestContextFilter(), "RequestContextFilter", null);
        jerseyAdapter.addFilter(new SessionCleanupFilter(), "SessionCleanupFilter", null);
        jerseyAdapter.addFilter(new NuxeoAuthenticationFilter(), "NuxeoAuthenticationFilter", null);
        jerseyAdapter.addFilter(new WebEngineFilter(), "WebEngineFilter", null);

        if (DEBUG) {
            jerseyAdapter.addInitParameter("com.sun.jersey.spi.container.ContainerRequestFilters",
                    "com.sun.jersey.api.container.filter.LoggingFilter");
            jerseyAdapter.addInitParameter("com.sun.jersey.spi.container.ContainerResponseFilters",
                    "com.sun.jersey.api.container.filter.LoggingFilter");
        }
        jerseyAdapter.addInitParameter("com.sun.jersey.config.feature.logging.DisableEntitylogging", "true");

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
