/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Damien Metzler (Leroy Merlin, http://www.leroymerlin.fr/)
 */
package org.nuxeo.ecm.webengine.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.test.JettyTransactionalFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;
import org.nuxeo.runtime.test.WorkingDirectoryConfigurator;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.test.runner.SimpleFeature;
import org.nuxeo.runtime.test.runner.web.WebDriverFeature;

@Deploy({ "org.nuxeo.ecm.platform.login", "org.nuxeo.ecm.platform.login.default", "org.nuxeo.ecm.webengine.admin",
        "org.nuxeo.ecm.webengine.jaxrs", "org.nuxeo.ecm.webengine.base", "org.nuxeo.ecm.webengine.ui",
        "org.nuxeo.ecm.webengine.gwt", "org.nuxeo.ecm.platform.test:test-usermanagerimpl/userservice-config.xml",
        "org.nuxeo.ecm.webengine.test:login-anonymous-config.xml", "org.nuxeo.ecm.webengine.test:login-config.xml",
        "org.nuxeo.ecm.webengine.test:runtimeserver-contrib.xml", "org.nuxeo.ecm.core.io" })
@Features({ PlatformFeature.class, WebDriverFeature.class, JettyTransactionalFeature.class, WebEngineFeatureCore.class })
public class WebEngineFeature extends SimpleFeature implements WorkingDirectoryConfigurator {

    protected URL config;

    @Override
    public void initialize(FeaturesRunner runner) throws Exception {
        WebXml anno = FeaturesRunner.getScanner().getFirstAnnotation(runner.getTargetTestClass(), WebXml.class);
        if (anno == null) {
            config = getResource("webengine/web/WEB-INF/web.xml");
        } else {
            config = runner.getTargetTestClass().getClassLoader().getResource(anno.value());
        }
        runner.getFeature(RuntimeFeature.class).getHarness().addWorkingDirectoryConfigurator(this);
    }

    @Override
    public void configure(RuntimeHarness harness, File workingDir) throws IOException {
        SessionFactory.setDefaultRepository("test");
        File dest = new File(workingDir, "web/root.war/WEB-INF/");
        dest.mkdirs();

        if (config == null) {
            throw new java.lang.IllegalStateException("No custom web.xml was found. "
                    + "Check your @WebXml annotation on the test class");
        }
        InputStream in = config.openStream();
        dest = new File(workingDir + "/web/root.war/WEB-INF/", "web.xml");
        try {
            FileUtils.copyToFile(in, dest);
        } finally {
            in.close();
        }
    }

    private static URL getResource(String resource) {
        return Thread.currentThread().getContextClassLoader().getResource(resource);
    }

    // public void deployTestModule() {
    // URL currentDir =
    // Thread.currentThread().getContextClassLoader().getResource(
    // ".");
    // ModuleManager moduleManager =
    // Framework.getLocalService(WebEngine.class).getModuleManager();
    // moduleManager.loadModuleFromDir(new File(currentDir.getFile()));
    // }
}
