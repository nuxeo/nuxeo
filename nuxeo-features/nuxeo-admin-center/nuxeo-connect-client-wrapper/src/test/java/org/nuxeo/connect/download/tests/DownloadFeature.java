/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.connect.download.tests;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.runtime.test.WorkingDirectoryConfigurator;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.JettyFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.test.runner.SimpleFeature;

@Deploy({ "org.nuxeo.connect.client.wrapper:OSGI-INF/runtimeserver-contrib.xml",
        "org.nuxeo.connect.client.wrapper:OSGI-INF/connect-client-framework.xml" })
@Features({ JettyFeature.class })
public class DownloadFeature extends SimpleFeature implements
        WorkingDirectoryConfigurator {

    @Override
    public void initialize(FeaturesRunner runner) throws Exception {
        runner.getFeature(RuntimeFeature.class).getHarness().addWorkingDirectoryConfigurator(
                this);
    }

    public void configure(RuntimeHarness harness, File workingDir)
            throws IOException {
        File dest = new File(workingDir, "web/root.war/WEB-INF/");
        dest.mkdirs();

        InputStream in = getResource("webtest/WEB-INF/web.xml").openStream();
        dest = new File(workingDir + "/web/root.war/WEB-INF/", "web.xml");
        try {
            FileUtils.copyToFile(in, dest);
        } finally {
            in.close();
        }

        File data = new File(workingDir, "web/root.war/test.data");
        FileUtils.writeFile(data, "TestMe");
    }

    private static URL getResource(String resource) {
        return Thread.currentThread().getContextClassLoader().getResource(
                resource);
    }
}
