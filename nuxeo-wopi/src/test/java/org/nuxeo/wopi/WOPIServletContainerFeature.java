/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 *
 */

package org.nuxeo.wopi;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.nuxeo.ecm.core.test.ServletContainerTransactionalFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.WorkingDirectoryConfigurator;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.test.runner.ServletContainerFeature;

/**
 * @since 10.3
 */
@Features(ServletContainerTransactionalFeature.class)
public class WOPIServletContainerFeature implements RunnerFeature, WorkingDirectoryConfigurator {

    @Override
    public void initialize(FeaturesRunner runner) {
        runner.getFeature(RuntimeFeature.class).getHarness().addWorkingDirectoryConfigurator(this);
    }

    @Override
    public void start(FeaturesRunner runner) {
        int port = runner.getFeature(ServletContainerFeature.class).getPort();
        String baseURL = "http://localhost:" + port + "/";
        Framework.getProperties().setProperty(Constants.WOPI_DISCOVERY_URL_PROPERTY, baseURL + "discovery.xml");
    }

    @Override
    public void configure(RuntimeHarness harness, File workingDir) throws IOException, URISyntaxException {
        Path war = workingDir.toPath().resolve("web/root.war/");
        Files.createDirectories(war);
        URI testPageURI = getResource("test-discovery.xml").toURI();
        Files.copy(Paths.get(testPageURI), war.resolve("discovery.xml"));
        URI wopiJSPURI = getResource("test-wopi.jsp").toURI();
        Files.copy(Paths.get(wopiJSPURI), war.resolve("wopi.jsp"));
    }

    private static URL getResource(String resource) {
        return Thread.currentThread().getContextClassLoader().getResource(resource);
    }

}
