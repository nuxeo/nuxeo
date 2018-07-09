/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Damien Metzler (Leroy Merlin, http://www.leroymerlin.fr/)
 */
package org.nuxeo.ecm.webengine.test;

import java.net.URL;

import org.nuxeo.ecm.core.test.ServletContainerTransactionalFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.web.WebDriverFeature;

@Deploy("org.nuxeo.ecm.platform.login")
@Deploy("org.nuxeo.ecm.platform.login.default")
@Deploy("org.nuxeo.ecm.webengine.jaxrs")
@Deploy("org.nuxeo.ecm.webengine.base")
@Deploy("org.nuxeo.ecm.webengine.ui")
@Deploy("org.nuxeo.ecm.platform.test:test-usermanagerimpl/userservice-config.xml")
@Deploy("org.nuxeo.ecm.webengine.test:login-anonymous-config.xml")
@Deploy("org.nuxeo.ecm.webengine.test:login-config.xml")
@Deploy("org.nuxeo.ecm.webengine.test:runtimeserver-contrib.xml")
@Features({ PlatformFeature.class, WebDriverFeature.class, ServletContainerTransactionalFeature.class,
        WebEngineFeatureCore.class })
public class WebEngineFeature implements RunnerFeature {

    protected URL config;

    @Override
    public void initialize(FeaturesRunner runner) {
        SessionFactory.setDefaultRepository("test");
    }

}
