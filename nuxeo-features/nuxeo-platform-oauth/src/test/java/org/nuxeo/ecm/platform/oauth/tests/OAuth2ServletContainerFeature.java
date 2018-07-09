/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.oauth.tests;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.nuxeo.ecm.core.test.ServletContainerTransactionalFeature;
import org.nuxeo.runtime.test.WorkingDirectoryConfigurator;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

/**
 * @since 9.2
 */
@Features(ServletContainerTransactionalFeature.class)
@Deploy("org.nuxeo.ecm.platform.oauth:OSGI-INF/servletcontainer-config.xml")
@Deploy("org.nuxeo.ecm.platform.oauth:OSGI-INF/test-oauth2-authentication-contrib.xml")
public class OAuth2ServletContainerFeature implements RunnerFeature, WorkingDirectoryConfigurator {

    @Override
    public void initialize(FeaturesRunner runner) {
        runner.getFeature(RuntimeFeature.class).getHarness().addWorkingDirectoryConfigurator(this);
    }

    @Override
    public void configure(RuntimeHarness harness, File workingDir) throws IOException, URISyntaxException {
        Path war = workingDir.toPath().resolve("web/root.war");
        Files.createDirectories(war);
        URI testPageURI = getResource("test-oauth2page.jsp").toURI();
        Files.copy(Paths.get(testPageURI), war.resolve("oauth2Grant.jsp"));
        Files.copy(Paths.get(testPageURI), war.resolve("oauth2error.jsp"));
    }

    private static URL getResource(String resource) {
        return Thread.currentThread().getContextClassLoader().getResource(resource);
    }
}
