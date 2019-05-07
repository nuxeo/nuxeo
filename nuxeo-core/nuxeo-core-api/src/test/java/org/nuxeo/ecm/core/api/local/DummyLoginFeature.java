/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.api.local;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.junit.runners.model.FrameworkMethod;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.LoginComponent;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

/**
 * Feature allowing to mock the {@link LoginComponent} in order to test services which need a security context.
 * <p>
 * This feature contributes {@link DummyLoginAs} to the component and registers 2 domains:
 * <ul>
 * <li>nuxeo-system-login</li>
 * <li>nuxeo-client-login</li>
 * </ul>
 * The feature will look for {@link WithUser} annotation on the test method, then on the class method, and use its value
 * to log in the given user.
 * <p>
 * By default the feature will log in as Administrator, like the {@link org.nuxeo.ecm.core.test.CoreFeature} does.
 *
 * @since 11.1
 */
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.core.api.tests:OSGI-INF/dummy-login-config.xml")
@SuppressWarnings("JavadocReference")
public class DummyLoginFeature implements RunnerFeature {

    protected LoginContext loginContext;

    public NuxeoPrincipal getPrincipal() {
        return loginContext.getSubject()
                           .getPrincipals(NuxeoPrincipal.class)
                           .stream()
                           .findFirst()
                           .orElseThrow(() -> new NuxeoException("Unable to find the NuxeoPrincipal"));
    }

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        RuntimeHarness harness = runner.getFeature(RuntimeFeature.class).getHarness();
        if (harness.getContext().getRuntime().getBundle("org.nuxeo.ecm.platform.web.common") == null) {
            harness.deployContrib("org.nuxeo.ecm.core.api.tests", "OSGI-INF/dummy-login-as-config.xml");
        }
    }

    @Override
    public void beforeRun(FeaturesRunner runner) throws LoginException {
        login(runner.getConfig(WithUser.class));
    }

    @Override
    public void beforeSetup(FeaturesRunner runner, FrameworkMethod method, Object test) throws LoginException {
        // check if we need to log in with a different user
        WithUser classWithUser = runner.getConfig(WithUser.class);
        WithUser methodWithUser = runner.getConfig(method, WithUser.class);
        if (!methodWithUser.value().equals(classWithUser.value())) {
            logout();
            login(methodWithUser);
        }
    }

    @Override
    public void afterTeardown(FeaturesRunner runner, FrameworkMethod method, Object test) throws LoginException {
        // check if we have logged in with a different user
        WithUser classWithUser = runner.getConfig(WithUser.class);
        WithUser methodWithUser = runner.getConfig(method, WithUser.class);
        if (!methodWithUser.value().equals(classWithUser.value())) {
            logout();
            login(classWithUser);
        }
    }

    @Override
    public void afterRun(FeaturesRunner runner) throws LoginException {
        logout();
    }

    protected void login(WithUser withUser) throws LoginException {
        loginContext = Framework.loginAsUser(withUser.value());
    }

    protected void logout() throws LoginException {
        if (loginContext != null) {
            loginContext.logout();
            loginContext = null;
        }
    }
}
