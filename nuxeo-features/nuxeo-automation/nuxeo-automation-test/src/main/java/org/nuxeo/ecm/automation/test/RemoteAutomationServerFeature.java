/*
 * (C) Copyright 2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Vladimir Pasquier
 */
package org.nuxeo.ecm.automation.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.SimpleFeature;

import com.google.inject.Binder;
import com.google.inject.Provider;
import com.google.inject.Scopes;

/**
 * Feature to test automation client against an Nuxeo Automation Server running
 * in a remote JVM.
 *
 * @since 5.7
 *
 */
public class RemoteAutomationServerFeature extends SimpleFeature {

    protected static final Log log = LogFactory.getLog(RemoteAutomationServerFeature.class);

    protected static final String TEST_AUTOMATION_URL = "TEST_AUTOMATION_URL";

    protected HttpAutomationClient client;

    protected Session session;

    protected String automationUrl;

    public RemoteAutomationServerFeature() {
        automationUrl = System.getenv(TEST_AUTOMATION_URL);
        if (automationUrl == null) {
            automationUrl = "http://localhost:8080/nuxeo/site/automation";
            log.info("Could not find " + TEST_AUTOMATION_URL
                    + " environment variable: fallback to: " + automationUrl);
        } else {
            log.info("Testing against: " + automationUrl);
        }
    }

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
                        if (client == null) {
                            client = new HttpAutomationClient(automationUrl);
                        }
                        return client;
                    }
                }).in(Scopes.SINGLETON);
        binder.bind(Session.class).toProvider(new Provider<Session>() {
            @Override
            public Session get() {
                if (client == null) {
                    client = new HttpAutomationClient(automationUrl);
                }
                if (session == null) {
                    session = client.getSession("Administrator",
                            "Administrator");
                }
                return session;
            }
        }).in(Scopes.SINGLETON);
    }

}
