/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.runtime.tomcat;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.catalina.Container;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.core.ContainerBase;
import org.nuxeo.osgi.application.FrameworkBootstrap;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class NuxeoDeployer implements LifecycleListener {

    protected String home = "nxserver";

    protected FrameworkBootstrap bootstrap;

    public void setHome(String home) {
        this.home = home;
    }

    public String getHome() {
        return home;
    }

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        Lifecycle lifecycle = event.getLifecycle();
        String type = event.getType();

        if (lifecycle instanceof Container && Lifecycle.BEFORE_START_EVENT.equals(type)) {
            Container container = (Container) lifecycle;
            preprocess(container);
        }
    }

    protected void preprocess(Container container) {
        try {
            ClassLoader parentCl = container.getParentClassLoader();
            File homeDir = resolveHomeDirectory();
            File bundles = new File(homeDir, "bundles");
            File lib = new File(homeDir, "lib");
            File deployerJar = FrameworkBootstrap.findFileStartingWidth(bundles, "nuxeo-runtime-deploy");
            File commonJar = FrameworkBootstrap.findFileStartingWidth(bundles, "nuxeo-common");
            if (deployerJar == null || commonJar == null) {
                System.out.println("Deployer and/or common JAR (nuxeo-runtime-deploy* | nuxeo-common*) not found in "
                        + bundles);
                return;
            }
            List<URL> urls = new ArrayList<>();
            PathMatcher jar = FileSystems.getDefault().getPathMatcher("glob:**.jar");
            try (Stream<Path> stream = Files.list(lib.toPath())) {
                stream.filter(jar::matches).map(this::toURL).forEach(urls::add);
            }
            try (Stream<Path> stream = Files.list(bundles.toPath())) {
                stream.filter(jar::matches).map(this::toURL).forEach(urls::add);
            }
            urls.add(homeDir.toURI().toURL());
            urls.add(new File(homeDir, "config").toURI().toURL());
            try (URLClassLoader cl = new URLClassLoader(urls.toArray(new URL[0]), parentCl)) {
                System.out.println("# Running Nuxeo Preprocessor ...");
                Class<?> klass = cl.loadClass("org.nuxeo.runtime.deployment.preprocessor.DeploymentPreprocessor");
                Method main = klass.getMethod("main", String[].class);
                main.invoke(null, new Object[] { new String[] { homeDir.getAbsolutePath() } });
                System.out.println("# Preprocessing done.");
            }
        } catch (IOException | ReflectiveOperationException | IllegalStateException e) {
            throw new RuntimeException("Failed to handle event", e);
        }
    }

    protected URL toURL(Path p) {
        try {
            return p.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    protected File resolveHomeDirectory() {
        String path;
        if (home.startsWith("/")) {
            path = home;
        } else {
            path = getTomcatHome() + '/' + home;
        }
        return new File(path);
    }

    public String getTomcatHome() {
        String tomcatHome = System.getProperty("catalina.base");
        if (tomcatHome == null) {
            tomcatHome = System.getProperty("catalina.home");
        }
        return tomcatHome;
    }

    /**
     * @deprecated Since 10.1, use {@link #preprocess(Container)} instead.
     */
    @Deprecated
    protected void handleEvent(ContainerBase container, LifecycleEvent event) {
        if (Lifecycle.BEFORE_START_EVENT.equals(event.getType())) {
            preprocess(container);
        }
    }

}
