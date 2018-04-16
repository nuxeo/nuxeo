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
 *     bstefanescu
 */
package org.nuxeo.runtime.test.runner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.nuxeo.runtime.test.WorkingDirectoryConfigurator;

import sun.net.www.http.HttpClient;

/**
 * Runs an embedded Jetty server with the nuxeo webapp deployed.
 * <p>
 * Note that at initialization the feature disables the {@code retryPostProp} property of
 * {@link sun.net.www.http.HttpClient}, the underlying HTTP client used by {@link com.sun.jersey.api.client.Client}.
 * <p>
 * This is to prevent the JDK's default behavior kept for backward compatibility: an unsuccessful HTTP POST request is
 * automatically resent to the server, unsuccessful in this case meaning the server did not send a valid HTTP response
 * or an {@code IOException} occurred. Yet in the tests using the Jersey client to make calls to Nuxeo we don't want
 * this as it can hide errors occurring in the HTTP communication that should prevent an appropriate response from being
 * sent by the server.
 */
@Deploy("org.nuxeo.runtime.jetty")
@Features(RuntimeFeature.class)
public class ServletContainerFeature extends SimpleFeature implements WorkingDirectoryConfigurator {

    @Override
    public void initialize(FeaturesRunner runner) throws Exception {
        disableSunHttpClientRetryPostProp();

        ServletContainer jetty = runner.getConfig(ServletContainer.class);
        if (jetty == null) {
            jetty = Defaults.of(ServletContainer.class);
        }
        configureJetty(jetty);

        runner.getFeature(RuntimeFeature.class).getHarness().addWorkingDirectoryConfigurator(this);
    }

    protected void configureJetty(ServletContainer jetty) {
        int p = jetty.port();
        try {
            String s = System.getenv("JETTY_PORT");
            if (s != null) {
                p = Integer.parseInt(s);
            }
        } catch (Exception e) {
            // do nothing ; the jetty.port
        }
        if (p > 0) {
            System.setProperty("jetty.port", Integer.toString(p));
        }

        String host = System.getenv("JETTY_HOST");
        if (host == null) {
            host = jetty.host();
        }
        if (host.length() > 0) {
            System.setProperty("jetty.host", host);
        }

        String config = System.getenv("JETTY_CONFIG");
        if (config == null) {
            config = jetty.config();
        }
        if (config.length() > 0) {
            System.setProperty("org.nuxeo.jetty.config", config);
        }

        System.setProperty("org.nuxeo.jetty.propagateNaming", Boolean.toString(jetty.propagateNaming()));
    }

    @Override
    public void configure(RuntimeHarness harness, File workingDir) throws IOException {
        File dest = new File(workingDir, "config");
        dest.mkdirs();

        dest = new File(workingDir + "/config", "default-web.xml");
        try (InputStream in = getResource("jetty/default-web.xml").openStream()) {
            FileUtils.copyInputStreamToFile(in, dest);
        }

        dest = new File(workingDir + "/config", "jetty.xml");
        try (InputStream in = getResource("jetty/jetty.xml").openStream()) {
            FileUtils.copyInputStreamToFile(in, dest);
        }
    }

    private static URL getResource(String resource) {
        // return
        // Thread.currentThread().getContextClassLoader().getResource(resource);
        return ServletContainer.class.getClassLoader().getResource(resource);
    }

    /**
     * Prevents the JDK's default behavior of resending an unsuccessful HTTP POST request automatically to the server by
     * disabling the the {@code retryPostProp} property of {@link sun.net.www.http.HttpClient}.
     * <p>
     * This can also be achieved by setting the {@code sun.net.http.retryPost} system property to {@code false}.
     *
     * @since 9.3
     */
    public static void disableSunHttpClientRetryPostProp()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field field = HttpClient.class.getDeclaredField("retryPostProp");
        field.setAccessible(true);
        field.setBoolean(null, false);
    }

}
