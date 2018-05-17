/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.bindings.CmisBindingFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.nuxeo.ecm.core.opencmis.tests.StatusLoggingDefaultHttpInvoker;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;
import org.nuxeo.runtime.test.runner.ServletContainerFeature;

/**
 * Feature that starts an embedded server configured for CMIS.
 * <p>
 * This is abstract, so subclasses can specify if AtomPub, Browser Bindings or Web Services are used
 */
@Features(ServletContainerFeature.class)
@ServletContainer(port = CmisFeatureSessionHttp.PORT)
@Deploy("org.nuxeo.ecm.core.opencmis.tests.tests:OSGI-INF/servletcontainer-base-config.xml")
@Deploy("org.nuxeo.ecm.platform.web.common")
public abstract class CmisFeatureSessionHttp extends CmisFeatureSession {

    public static final String BASE_RESOURCE = "web";

    public static final String HOST = "localhost";

    public static final int PORT = 17488;

    // org.apache.chemistry.opencmis.server.shared.HttpUtils.splitPath returns [""] instead of []
    // if there's not at least a non-empty servlet or context, so provide one.
    // this context value is also mentioned in the XML contributions for servlets, filters and listeners
    public static final String CONTEXT = "context";

    public URI serverURI;

    @Override
    public void beforeRun(FeaturesRunner runner) throws Exception {
        String repositoryName = runner.getFeature(CoreFeature.class).getRepositoryName();
        setUpServer();
        setUpCmisSession(repositoryName);
    }

    @Override
    public void afterRun(FeaturesRunner runner) throws Exception {
        tearDownCmisSession();
    }

    @Override
    public Session setUpCmisSession(String repositoryName) {

        SessionFactory sf = SessionFactoryImpl.newInstance();
        Map<String, String> params = new HashMap<String, String>();

        params.put(SessionParameter.AUTHENTICATION_PROVIDER_CLASS, CmisBindingFactory.STANDARD_AUTHENTICATION_PROVIDER);

        params.put(SessionParameter.CACHE_SIZE_REPOSITORIES, "10");
        params.put(SessionParameter.CACHE_SIZE_TYPES, "100");
        params.put(SessionParameter.CACHE_SIZE_OBJECTS, "100");

        params.put(SessionParameter.REPOSITORY_ID, repositoryName);
        params.put(SessionParameter.USER, USERNAME);
        params.put(SessionParameter.PASSWORD, PASSWORD);

        params.put(SessionParameter.HTTP_INVOKER_CLASS, StatusLoggingDefaultHttpInvoker.class.getName());

        addParams(params);

        session = sf.createSession(params);
        return session;
    }

    @Override
    public void tearDownCmisSession() {
        if (session != null) {
            session.clear();
            session = null;
        }
    }

    /** Adds protocol-specific parameters. */
    protected abstract void addParams(Map<String, String> params);

    protected void setUpServer() throws Exception {
        serverURI = new URI("http://localhost:" + PORT + '/' + CONTEXT);

    }

}
