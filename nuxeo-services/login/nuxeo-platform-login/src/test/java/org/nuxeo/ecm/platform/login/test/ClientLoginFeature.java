/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     mhilaire
 */
package org.nuxeo.ecm.platform.login.test;

import java.security.Principal;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.junit.runners.model.FrameworkMethod;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;

/**
 * That feature should not be installed in conjunction with the
 * org.nuxeo.ecm.platform.web.common bundle which provide the real client login
 * infrastucture.
 *
 *
 * @since 8.3
 */
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.core.schema")
@Deploy("org.nuxeo.ecm.directory.types.contrib")
@Deploy("org.nuxeo.ecm.platform.login")
@Deploy("org.nuxeo.ecm.platform.login:dummy-client-login-config.xml")
public class ClientLoginFeature implements RunnerFeature {

    protected LoginContext logContext = null;

    public Principal login(String username) throws LoginException {
        logContext = Framework.login(username, username);
        return logContext.getSubject().getPrincipals().iterator().next();
    }

    public void logout() throws LoginException {
        if (logContext != null) {
            logContext.logout();
            logContext = null;
        }
    }

    @Override
    public void afterMethodRun(FeaturesRunner runner, FrameworkMethod method, Object test) throws Exception {
        logout();
    }

}
