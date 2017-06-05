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
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.nuxeo.runtime.test.WorkingDirectoryConfigurator;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.JettyFeature;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.test.runner.SimpleFeature;

@Features(JettyFeature.class)
@LocalDeploy("org.nuxeo.ecm.platform.oauth:OSGI-INF/jetty-test-config.xml")
public class OAuth2JettyFeature extends SimpleFeature implements WorkingDirectoryConfigurator {

    @Override
    public void initialize(FeaturesRunner runner) throws Exception {
        runner.getFeature(RuntimeFeature.class).getHarness().addWorkingDirectoryConfigurator(this);
    }

    @Override
    public void configure(RuntimeHarness harness, File workingDir) throws IOException, URISyntaxException {
        File dest = new File(workingDir, "web/root.war/WEB-INF/");
        dest.mkdirs();

        dest = new File(workingDir + "/web/root.war/WEB-INF/", "web.xml");
        try (InputStream in = getResource("test-web.xml").openStream()) {
            FileUtils.copyInputStreamToFile(in, dest);
        }

        URL resource = getResource("web/nuxeo.war");
        File nuxeoWar = new File(resource.toURI());
        FileUtils.copyDirectory(nuxeoWar, new File(workingDir, "web/root.war/"));

        // deploy the jar containing the taglibs
        dest = new File(workingDir + "/web/root.war/WEB-INF/", "lib");
        dest.mkdirs();
        resource = getResource("META-INF/c.tld");
        String jarPath = new URI(resource.getFile()).getPath();
        jarPath = jarPath.substring(0, jarPath.indexOf("!"));
        FileUtils.copyFileToDirectory(new File(jarPath), dest);
    }

    private static URL getResource(String resource) {
        return Thread.currentThread().getContextClassLoader().getResource(resource);
    }
}
