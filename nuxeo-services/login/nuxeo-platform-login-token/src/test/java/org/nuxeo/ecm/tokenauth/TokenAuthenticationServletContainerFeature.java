/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.tokenauth;

import java.io.File;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.nuxeo.ecm.core.test.ServletContainerTransactionalFeature;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.runtime.test.WorkingDirectoryConfigurator;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.test.runner.SimpleFeature;

/**
 * Feature to run tests needing the {@link TokenAuthenticationService} and a Jetty server configured with a webapp
 * deployment descriptor.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.7
 */
@Features({ TokenAuthenticationServiceFeature.class, ServletContainerTransactionalFeature.class })
@ServletContainer(port = 18080)
@TokenAuthenticationServletContainerConfig(webappDescriptorPath = "web.xml")
@Deploy("org.nuxeo.ecm.platform.login")
@Deploy("org.nuxeo.ecm.platform.web.common:OSGI-INF/authentication-framework.xml")
@Deploy("org.nuxeo.ecm.platform.login.token.test:OSGI-INF/test-token-authentication-runtime-server-contrib.xml")
public class TokenAuthenticationServletContainerFeature extends SimpleFeature implements WorkingDirectoryConfigurator {

    protected URL webappDescriptorPath;

    @Override
    public void initialize(FeaturesRunner runner) throws Exception {

        TokenAuthenticationServletContainerConfig tokenAuthenticationConfig = runner.getConfig(TokenAuthenticationServletContainerConfig.class);
        webappDescriptorPath = runner.getTargetTestClass().getClassLoader().getResource(
                tokenAuthenticationConfig.webappDescriptorPath());

        runner.getFeature(RuntimeFeature.class).getHarness().addWorkingDirectoryConfigurator(this);
    }

    @Override
    public void configure(RuntimeHarness harness, File workingDir) throws Exception {

        File webappDir = new File(workingDir, "web/root.war/WEB-INF");
        webappDir.mkdirs();
        FileUtils.copyURLToFile(webappDescriptorPath, new File(webappDir, "web.xml"));
    }

}
