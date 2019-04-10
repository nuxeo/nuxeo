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

import org.apache.chemistry.opencmis.client.api.Session;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer.ActionHandler;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.google.inject.Binder;
import com.google.inject.Provider;

/**
 * Base feature that starts a CMIS client session.
 */
public abstract class CmisFeatureSession extends CmisFeatureConfiguration {

    public static final String USERNAME = "Administrator";

    public static final String PASSWORD = "test";

    protected boolean isHttp;

    protected boolean isAtomPub;

    protected boolean isBrowser;

    public Session session;

    public class SessionProvider implements Provider<Session> {
        @Override
        public Session get() {
            return session;
        }
    }

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        binder.bind(CmisFeatureSession.class).toInstance(this);
        binder.bind(Session.class).toProvider(new SessionProvider());
        runner.getFeature(RuntimeFeature.class).registerHandler(new ActionHandler() {
            @Override
            public void exec(String action, String... args) throws Exception {
                afterTeardown(runner);
                Thread.sleep(1000); // otherwise sometimes fails to set up again
                next.exec(action, args);
                beforeSetup(runner);
            }

        });
    }

    public abstract Session setUpCmisSession(String repositoryName);

    public abstract void tearDownCmisSession();

    public void setLocal() {
        isHttp = false;
        isAtomPub = false;
        isBrowser = false;
    }

    public void setAtomPub() {
        isHttp = true;
        isAtomPub = true;
        isBrowser = false;
    }

    public void setBrowser() {
        isHttp = true;
        isAtomPub = false;
        isBrowser = true;
    }

}
