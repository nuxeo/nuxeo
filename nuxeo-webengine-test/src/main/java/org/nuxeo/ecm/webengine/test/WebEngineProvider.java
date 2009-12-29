package org.nuxeo.ecm.webengine.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebEngineComponent;
import org.nuxeo.ecm.webengine.model.impl.ModuleManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class WebEngineProvider implements Provider<WebEngine> {

    private final RuntimeHarness harness;

    @Inject
    public WebEngineProvider(RuntimeHarness harness, UserManager um) {

        assert harness != null;
        assert um != null;

        this.harness = harness;
    }

    public WebEngine get() {

        try {
            harness.deployBundle("org.nuxeo.ecm.platform.login");
            harness.deployBundle("org.nuxeo.ecm.platform.web.common");


            setupWorkingDir();


            harness.deployBundle("org.nuxeo.ecm.webengine.admin");
            harness.deployBundle("org.nuxeo.ecm.webengine.base");
            harness.deployBundle("org.nuxeo.ecm.webengine.core");

            harness.deployBundle("org.nuxeo.ecm.webengine.resteasy.adapter");


            harness.deployBundle("org.nuxeo.ecm.webengine.ui");

            harness.deployBundle("org.nuxeo.theme.core");
            harness.deployBundle("org.nuxeo.theme.html");
            harness.deployBundle("org.nuxeo.theme.fragments");
            harness.deployBundle("org.nuxeo.theme.webengine");

            harness.deployContrib("org.nuxeo.ecm.platform.test",
                    "test-usermanagerimpl/userservice-config.xml");

            harness.deployContrib("org.nuxeo.ecm.webengine.test",
                    "authentication-config.xml");

            harness.deployContrib("org.nuxeo.ecm.webengine.test",
                    "login-anonymous-config.xml");

            harness.deployContrib("org.nuxeo.ecm.webengine.test", "login-config.xml");

            harness.deployBundle("org.nuxeo.runtime.jetty");

            harness.deployContrib("org.nuxeo.ecm.webengine.test","runtimeserver-contrib.xml");

            harness.fireFrameworkStarted();

            WebEngineComponent we = (WebEngineComponent) Framework.getRuntime()
                    .getComponent(WebEngineComponent.NAME);
            return we.getEngine();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private void setupWorkingDir() throws IOException {
        File dest = new File(harness.getWorkingDir(), "config");
        dest.mkdir();

        InputStream in = getResource("webengine/config/default-web.xml")
                .openStream();
        dest = new File(harness.getWorkingDir() + "/config",
                "default-web.xml");
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
        URL currentDir = Thread.currentThread().getContextClassLoader()
                .getResource(".");

        ModuleManager moduleManager = Framework
                .getLocalService(WebEngine.class).getModuleManager();
        moduleManager.loadModuleFromDir(new File(currentDir.getFile()));

    }

}
