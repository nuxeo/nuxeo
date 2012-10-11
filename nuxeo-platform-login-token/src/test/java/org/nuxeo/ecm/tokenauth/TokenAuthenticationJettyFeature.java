/*
 * (C) Copyright 2006-20012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.tokenauth;

import java.io.File;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.nuxeo.ecm.core.test.JettyTransactionalFeature;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.runtime.test.WorkingDirectoryConfigurator;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.test.runner.SimpleFeature;

/**
 * Feature to run tests needing the {@link TokenAuthenticationService} and a
 * Jetty server configured with a webapp deployment descriptor.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.7
 */
@Features({ TokenAuthenticationServiceFeature.class,
        JettyTransactionalFeature.class })
@Jetty(port = 18080)
@TokenAuthenticationJettyConfig(webappDescriptorPath = "web.xml")
@Deploy("org.nuxeo.ecm.platform.login.token.test:OSGI-INF/test-token-authentication-runtime-server-contrib.xml")
public class TokenAuthenticationJettyFeature extends SimpleFeature implements
        WorkingDirectoryConfigurator {

    protected URL webappDescriptorPath;

    @Override
    public void initialize(FeaturesRunner runner) throws Exception {

        TokenAuthenticationJettyConfig tokenAuthenticationConfig = runner.getConfig(TokenAuthenticationJettyConfig.class);
        webappDescriptorPath = runner.getTargetTestClass().getClassLoader().getResource(
                tokenAuthenticationConfig.webappDescriptorPath());

        runner.getFeature(RuntimeFeature.class).getHarness().addWorkingDirectoryConfigurator(
                this);
    }

    @Override
    public void configure(RuntimeHarness harness, File workingDir)
            throws Exception {

        File webappDir = new File(workingDir, "web/root.war/WEB-INF");
        webappDir.mkdirs();
        FileUtils.copyURLToFile(webappDescriptorPath, new File(webappDir,
                "web.xml"));
    }

}
