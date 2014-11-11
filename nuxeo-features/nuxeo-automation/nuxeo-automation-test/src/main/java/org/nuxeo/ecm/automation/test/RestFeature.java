/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.test;

import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.webengine.test.WebEngineFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.SimpleFeature;

import com.google.inject.Binder;
import com.google.inject.Provider;
import com.google.inject.Scopes;

/**
 * Shortcut to deploy bundles required by automation in your test
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Deploy({ "org.nuxeo.ecm.automation.core",
        "org.nuxeo.ecm.automation.server", "org.nuxeo.ecm.automation.features",
        "org.nuxeo.ecm.platform.query.api" })
@Features({WebEngineFeature.class})
public class RestFeature extends SimpleFeature  {

    protected HttpAutomationClient client ;

    protected Session session;

     @Override
    public void afterRun(FeaturesRunner runner) throws Exception {
        if (client != null) {
            client.shutdown();
            client = null;
            session = null;
        }
        super.afterRun(runner);
    }

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        super.configure(runner, binder);
        binder.bind(HttpAutomationClient.class).toProvider(
                new Provider<HttpAutomationClient>() {
                    @Override
                    public HttpAutomationClient get() {
                        if (client ==null) {
                            client = new HttpAutomationClient("http://localhost:18080/automation");
                        }
                        return client;
                    }
                }).in(Scopes.SINGLETON);
        binder.bind(Session.class).toProvider(new Provider<Session>() {
            @Override
            public Session get() {
                if (client == null) {
                     client = new HttpAutomationClient("http://localhost:18080/automation");
                }
                if (session == null) {
                    session = client.getSession("Administrator", "Administrator");
                }
                return session;
            }
        }).in(Scopes.SINGLETON);
    }

}
