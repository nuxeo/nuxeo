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
 *     Vladimir Pasquier
 */
package org.nuxeo.ecm.automation.test;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;

import com.google.inject.Binder;
import com.google.inject.Scopes;

/**
 * Feature to test automation client against an Nuxeo Automation Server running in a remote JVM.
 *
 * @since 5.7
 */
public class RemoteAutomationServerFeature implements RunnerFeature {

    protected static final Log log = LogFactory.getLog(RemoteAutomationServerFeature.class);

    protected static final String TEST_AUTOMATION_URL = "TEST_AUTOMATION_URL";

    protected HttpAutomationClient client;

    protected Session session;

    protected String automationUrl;

    public RemoteAutomationServerFeature() {
        automationUrl = System.getenv(TEST_AUTOMATION_URL);
        if (automationUrl == null) {
            automationUrl = "http://localhost:8080/nuxeo/site/automation";
            log.info("Could not find " + TEST_AUTOMATION_URL + " environment variable: fallback to: " + automationUrl);
        } else {
            log.info("Testing against: " + automationUrl);
        }
    }

    @Override
    public void afterRun(FeaturesRunner runner) {
        if (client != null) {
            client.shutdown();
            client = null;
            session = null;
        }
    }

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        binder.bind(HttpAutomationClient.class).toProvider(() -> {
            if (client == null) {
                client = new HttpAutomationClient(automationUrl);
            }
            return client;
        }).in(Scopes.SINGLETON);
        binder.bind(Session.class).toProvider(() -> {
            if (client == null) {
                client = new HttpAutomationClient(automationUrl);
            }
            if (session == null) {
                try {
                    session = client.getSession("Administrator", "Administrator");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return session;
        }).in(Scopes.SINGLETON);
    }

}
