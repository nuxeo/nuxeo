package com.leroymerlin.corp.fr.nuxeo.portal.testing;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.impl.ModuleManager;
import org.nuxeo.runtime.api.Framework;

/**
 *
 * @author dmetzler
 * @deprecated Use the new annotation mecanism
 *
 */
public class WebEngineHarness {

    private TestRuntimeHarness harness;

    public WebEngineHarness(TestRuntimeHarness rtHarness) {
        this.harness = rtHarness;
    }

    public void start() throws Exception {
        harness.deployBundle("org.nuxeo.ecm.platform.api");

        harness.deployContrib("org.nuxeo.ecm.directory",
                "OSGI-INF/DirectoryService.xml");
        harness.deployContrib("org.nuxeo.ecm.directory.sql",
                "OSGI-INF/SQLDirectoryFactory.xml");

        harness.deployBundle("org.nuxeo.ecm.platform.login");

        harness.deployContrib("org.nuxeo.ecm.platform.usermanager",
                "OSGI-INF/UserService.xml");

        harness.deployBundle("org.nuxeo.ecm.platform.web.common");

        harness.deployContrib("org.nuxeo.test.util",
                "test-usermanagerimpl/schemas-config.xml");
        harness.deployContrib("org.nuxeo.test.util",
                "test-usermanagerimpl/directory-config.xml");
        harness.deployContrib("org.nuxeo.test.util",
                "test-usermanagerimpl/userservice-config.xml");

        assertNotNull(Framework.getService(UserManager.class));

        // TODO :Put it in the TestRuntimeHarness ?
        Environment.setDefault(new Environment(harness.getWorkingDir()));

        File dest = new File(harness.getWorkingDir(), "config");
        dest.mkdir();

        InputStream in = getResource("webengine/config/default-web.xml")
                .openStream();
        dest = new File(harness.getWorkingDir() + "/config", "default-web.xml");
        FileOutputStream out = new FileOutputStream(dest);
        FileUtils.copy(in, out);

        in = getResource("webengine/config/jetty.xml").openStream();
        dest = new File(harness.getWorkingDir() + "/config", "jetty.xml");
        out = new FileOutputStream(dest);
        FileUtils.copy(in, out);

        dest = new File(harness.getWorkingDir(), "web/WEB-INF/");
        dest.mkdirs();

        in = getResource("webengine/web/WEB-INF/web.xml").openStream();
        dest = new File(harness.getWorkingDir() + "/web/WEB-INF/", "web.xml");
        out = new FileOutputStream(dest);
        FileUtils.copy(in, out);

        harness.deployBundle("org.nuxeo.ecm.webengine.resteasy.adapter");

        harness.deployBundle("org.nuxeo.ecm.webengine.admin");
        harness.deployBundle("org.nuxeo.ecm.webengine.base");
        harness.deployBundle("org.nuxeo.ecm.webengine.core");
        harness.deployBundle("org.nuxeo.ecm.webengine.ui");

        harness.deployBundle("org.nuxeo.theme.core");
        harness.deployBundle("org.nuxeo.theme.html");
        harness.deployBundle("org.nuxeo.theme.fragments");
        harness.deployBundle("org.nuxeo.theme.webengine");

        harness.deployContrib("org.nuxeo.test.util",
                "test-usermanagerimpl/userservice-config.xml");

        harness.deployContrib("org.nuxeo.test.util",
                "authentication-config.xml");

        harness.deployContrib("org.nuxeo.test.util",
                "login-anonymous-config.xml");

        harness.deployContrib("org.nuxeo.test.util", "login-config.xml");

        harness.deployBundle("org.nuxeo.runtime.jetty");
    }

    private static URL getResource(String resource) {
        return Thread.currentThread().getContextClassLoader().getResource(
                resource);
    }

    public void deployTestModule() {
        URL currentDir = Thread.currentThread().getContextClassLoader()
                .getResource(".");

        ModuleManager moduleManager = Framework
                .getLocalService(WebEngine.class).getModuleManager();
        moduleManager.loadModuleFromDir(new File(currentDir.getFile()));

    }

}
