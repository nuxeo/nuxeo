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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.impl.ModuleManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.NuxeoRunner;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.test.runner.SimpleFeature;

@Deploy({
    "org.nuxeo.ecm.platform.login",
    "org.nuxeo.ecm.platform.web.common",
    "org.nuxeo.ecm.webengine.admin",
    "org.nuxeo.ecm.webengine.base",
    "org.nuxeo.ecm.webengine.core",
    "org.nuxeo.ecm.webengine.resteasy.adapter",
    "org.nuxeo.runtime.jetty",
    "org.nuxeo.ecm.webengine.ui",
    "org.nuxeo.theme.core",
    "org.nuxeo.theme.html",
    "org.nuxeo.theme.fragments",
    "org.nuxeo.theme.webengine",
    "org.nuxeo.ecm.platform.test:test-usermanagerimpl/userservice-config.xml",
    "org.nuxeo.ecm.webengine.test:authentication-config.xml",
    "org.nuxeo.ecm.webengine.test:login-anonymous-config.xml",
    "org.nuxeo.ecm.webengine.test:login-config.xml",    
    "org.nuxeo.ecm.webengine.test:runtimeserver-contrib.xml"
})
@Features(PlatformFeature.class)
public class WebEngineFeature extends SimpleFeature {

    @Override
    public void initialize(NuxeoRunner runner, Class<?> testClass) throws Exception {
        setupWorkingDir(runner.getHarness());
    }
    
    private void setupWorkingDir(RuntimeHarness harness) throws IOException {
        File dest = new File(harness.getWorkingDir(), "config");
        dest.mkdir();

        InputStream in = getResource("webengine/config/default-web.xml").openStream();
        dest = new File(harness.getWorkingDir() + "/config", "default-web.xml");
        FileOutputStream out = new FileOutputStream(dest);
        FileUtils.copy(in, out);

        in = getResource("webengine/config/jetty.xml").openStream();
        dest = new File(harness.getWorkingDir() + "/config", "jetty.xml");
        out = new FileOutputStream(dest);
        FileUtils.copy(in, out);

        dest = new File(harness.getWorkingDir(), "web/root.war/WEB-INF/");
        dest.mkdirs();

        in = getResource("webengine/web/WEB-INF/web.xml").openStream();
        dest = new File(harness.getWorkingDir() + "/web/root.war/WEB-INF/",
                "web.xml");
        out = new FileOutputStream(dest);
        FileUtils.copy(in, out);
    }

    private static URL getResource(String resource) {
        return Thread.currentThread().getContextClassLoader().getResource(
                resource);
    }

    public void deployTestModule() {
        URL currentDir = Thread.currentThread().getContextClassLoader().getResource(
                ".");
        ModuleManager moduleManager = Framework.getLocalService(WebEngine.class).getModuleManager();
        moduleManager.loadModuleFromDir(new File(currentDir.getFile()));
    }
}
