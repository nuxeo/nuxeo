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
package org.nuxeo.ecm.directory.multi;

import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.core.api.local.LoginStack;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.login.test.ClientLoginFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;

/**
 * Feature for multi directory unit tests
 *
 * @since 6.0
 */
@Features({ ClientLoginFeature.class })
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.directory.api")
@Deploy("org.nuxeo.ecm.directory")
@Deploy("org.nuxeo.ecm.core.schema")
@Deploy("org.nuxeo.ecm.directory.types.contrib")
@Deploy("org.nuxeo.ecm.directory.multi")
@Deploy("org.nuxeo.ecm.directory.multi.tests:schemas-config.xml")
public class MultiDirectoryFeature implements RunnerFeature {

    protected LoginStack loginStack;

    @Override
    public void beforeSetup(FeaturesRunner runner) {
        loginStack = ClientLoginModule.getThreadLocalLogin();
        loginStack.push(new SystemPrincipal(null), null, null);
    }

    @Override
    public void afterTeardown(FeaturesRunner runner) {
        loginStack.pop();
    }
}
