/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.test;

import static org.nuxeo.ecm.core.api.security.SecurityConstants.ADMINISTRATOR;

import java.io.IOException;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.nuxeo.ecm.core.test.DetectThreadDeadlocksFeature;
import org.nuxeo.ecm.webengine.test.WebEngineFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.ServletContainerFeature;

import com.google.inject.Binder;
import com.google.inject.Scopes;

/**
 * Shortcut to deploy bundles required by automation in your test
 *
 * @since 5.7
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Features({ DetectThreadDeadlocksFeature.class, WebEngineFeature.class, AutomationServerFeature.class })
public class EmbeddedAutomationServerFeature implements RunnerFeature {

    @Inject
    protected ServletContainerFeature servletContainerFeature;

    protected HttpAutomationClient client;

    protected HttpAutomationSession session;

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
                client = getHttpAutomationClient();
            }
            return client;
        }).in(Scopes.SINGLETON);
        binder.bind(HttpAutomationSession.class).toProvider(() -> {
            if (client == null) {
                client = getHttpAutomationClient();
            }
            if (session == null) {
                try {
                    session = client.getSession(ADMINISTRATOR, ADMINISTRATOR);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return session;
        }).in(Scopes.SINGLETON);
    }

    protected HttpAutomationClient getHttpAutomationClient() {
        // port must be supplied dynamically because it may change after hot-reload of services
        Supplier<String> urlSupplier = () -> "http://localhost:" + servletContainerFeature.getPort() + "/automation/";
        HttpAutomationClient client = new HttpAutomationClient(urlSupplier);
        return client;
    }

}
