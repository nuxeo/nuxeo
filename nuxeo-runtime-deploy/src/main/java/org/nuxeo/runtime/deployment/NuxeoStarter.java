/*
 * (C) Copyright 2011-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 *     Julien Carsique
 */
package org.nuxeo.runtime.deployment;

import static org.nuxeo.common.Environment.JBOSS_HOST;
import static org.nuxeo.common.Environment.NUXEO_CONFIG_DIR;
import static org.nuxeo.common.Environment.NUXEO_DATA_DIR;
import static org.nuxeo.common.Environment.NUXEO_LOG_DIR;
import static org.nuxeo.common.Environment.NUXEO_RUNTIME_HOME;
import static org.nuxeo.common.Environment.NUXEO_TMP_DIR;
import static org.nuxeo.common.Environment.NUXEO_WEB_DIR;
import static org.nuxeo.common.Environment.TOMCAT_HOST;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.osgi.application.loader.FrameworkLoader;

/**
 * This is called at WAR startup and starts the Nuxeo OSGi runtime and registers
 * the Nuxeo bundles with it.
 * <p>
 * This class must be configured as a {@code <listener>/<listener-class>} in
 * {@code META-INF/web.xml}.
 * <p>
 * It uses servlet init parameters defined through
 * {@code <context-param>/<param-name>/<param-value>} in web.xml. Allowable
 * parameter names come from {@link org.nuxeo.common.Environment}, mainly
 * {@link org.nuxeo.common.Environment#NUXEO_RUNTIME_HOME NUXEO_RUNTIME_HOME}
 * and {@link org.nuxeo.common.Environment#NUXEO_CONFIG_DIR NUXEO_CONFIG_DIR},
 * but also {@link org.nuxeo.common.Environment#NUXEO_DATA_DIR NUXEO_DATA_DIR},
 * {@link org.nuxeo.common.Environment#NUXEO_LOG_DIR NUXEO_LOG_DIR},
 * {@link org.nuxeo.common.Environment#NUXEO_TMP_DIR NUXEO_TMP_DIR} and
 * {@link org.nuxeo.common.Environment#NUXEO_WEB_DIR NUXEO_WEB_DIR}.
 */
public class NuxeoStarter implements ServletContextListener {

    private static final Log log = LogFactory.getLog(NuxeoStarter.class);

    /** Default location of the home in the server current directory. */
    private static final String DEFAULT_HOME = "nuxeo";

    /**
     * Name of the file listing Nuxeo bundles. If existing, this file will be
     * used at start, else {@code "/WEB-INF/lib/"} will be scanned.
     *
     * @since 5.9.3
     * @see #findBundles(ServletContext)
     */
    public static final String NUXEO_BUNDLES_LIST = ".nuxeo-bundles";

    protected final Map<String, Object> env = new HashMap<>();

    protected List<File> bundleFiles = new ArrayList<>();

    @Override
    public void contextInitialized(ServletContextEvent event) {
        try {
            long startTime = System.currentTimeMillis();
            start(event);
            long finishedTime = System.currentTimeMillis();
            @SuppressWarnings("boxing")
            Double duration = (finishedTime - startTime) / 1000.0;
            log.info(String.format("Nuxeo framework started in %.1f sec.",
                    duration));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        try {
            stop();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void start(ServletContextEvent event) throws Exception {
        ServletContext servletContext = event.getServletContext();
        findBundles(servletContext);
        findEnv(servletContext);

        ClassLoader cl = getClass().getClassLoader();
        File home = new File((String) env.get(NUXEO_RUNTIME_HOME));
        FrameworkLoader.initialize(cl, home, bundleFiles, env);
        FrameworkLoader.start();
    }

    protected void stop() throws Exception {
        FrameworkLoader.stop();
    }

    protected void findBundles(ServletContext servletContext) throws Exception {
        InputStream bundlesListStream = servletContext.getResourceAsStream("/WEB-INF/"
                + NUXEO_BUNDLES_LIST);
        if (bundlesListStream != null) {
            File lib = new File(servletContext.getRealPath("/WEB-INF/lib/"));
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(bundlesListStream))) {
                String bundleName;
                while ((bundleName = reader.readLine()) != null) {
                    bundleFiles.add(new File(lib, bundleName));
                }
            }
        }
        if (bundleFiles.isEmpty()) { // Fallback on directory scan
            File root = new File(servletContext.getRealPath("/"));
            Set<String> ctxpaths = servletContext.getResourcePaths("/WEB-INF/lib/");
            for (String ctxpath : ctxpaths) {
                if (!ctxpath.endsWith(".jar")) {
                    continue;
                }
                bundleFiles.add(new File(root, ctxpath));
            }
        }
    }

    protected void findEnv(ServletContext servletContext) {
        for (String param : Arrays.asList( //
                NUXEO_RUNTIME_HOME, //
                NUXEO_CONFIG_DIR, //
                NUXEO_DATA_DIR, //
                NUXEO_LOG_DIR, //
                NUXEO_TMP_DIR, //
                NUXEO_WEB_DIR)) {
            String value = servletContext.getInitParameter(param);
            if (value != null && !"".equals(value.trim())) {
                env.put(param, value);
            }
        }
        // default env values
        if (!env.containsKey(NUXEO_CONFIG_DIR)) {
            String webinf = servletContext.getRealPath("/WEB-INF");
            env.put(NUXEO_CONFIG_DIR, webinf);
        }
        if (!env.containsKey(NUXEO_RUNTIME_HOME)) {
            File home = new File(DEFAULT_HOME);
            env.put(NUXEO_RUNTIME_HOME, home.getAbsolutePath());
        }
        // host
        if (getClass().getClassLoader().getClass().getName().startsWith(
                "org.jboss.classloader")) {
            env.put(FrameworkLoader.HOST_NAME, JBOSS_HOST);
        } else if (servletContext.getClass().getName().startsWith(
                "org.apache.catalina")) {
            env.put(FrameworkLoader.HOST_NAME, TOMCAT_HOST);
        }
    }

}
