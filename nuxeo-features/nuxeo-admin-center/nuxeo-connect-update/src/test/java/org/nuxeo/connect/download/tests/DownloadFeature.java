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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.connect.download.tests;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.nuxeo.runtime.test.WorkingDirectoryConfigurator;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainerFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.test.runner.SimpleFeature;

@Deploy("org.nuxeo.connect.client.wrapper:OSGI-INF/runtimeserver-contrib.xml")
@Deploy("org.nuxeo.connect.client.wrapper:OSGI-INF/connect-client-framework.xml")
@Deploy("org.nuxeo.connect.update")
@Features({ ServletContainerFeature.class })
public class DownloadFeature extends SimpleFeature implements WorkingDirectoryConfigurator {

    @Override
    public void initialize(FeaturesRunner runner) throws Exception {
        runner.getFeature(RuntimeFeature.class).getHarness().addWorkingDirectoryConfigurator(this);
    }

    @Override
    public void configure(RuntimeHarness harness, File workingDir) throws IOException {
        File dest = new File(workingDir, "web/root.war/WEB-INF/");
        dest.mkdirs();

        dest = new File(workingDir + "/web/root.war/WEB-INF/", "web.xml");
        try (InputStream in = getResource("webtest/WEB-INF/web.xml").openStream()) {
            FileUtils.copyInputStreamToFile(in, dest);
        }

        File data = new File(workingDir, "web/root.war/test.data");
        FileUtils.writeStringToFile(data, "TestMe", UTF_8);
    }

    private static URL getResource(String resource) {
        return Thread.currentThread().getContextClassLoader().getResource(resource);
    }
}
