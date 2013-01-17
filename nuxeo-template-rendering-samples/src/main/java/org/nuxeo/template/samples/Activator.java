/*
 * (C) Copyright 2006-2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.template.samples;

import java.io.File;
import java.net.URL;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.runtime.api.Framework;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * The activator expand the sample documents in the data directory.
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @author <a href="mailto:ldoguin@nuxeo.com">Laurent Doguin</a>
 * 
 */
public class Activator implements BundleActivator {

    protected static final String SAMPLES_ROOT_PATH = "templateSamples";

    private static volatile Activator instance;

    private BundleContext context;

    public static URL getResource(String path) {
        Activator a = instance;
        if (a != null) {
            return a.context.getBundle().getResource(path);
        }
        return Thread.currentThread().getContextClassLoader().getResource(path);
    }

    public static Activator getInstance() {
        return instance;
    }

    public BundleContext getContext() {
        return context;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        this.context = context;
        instance = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        instance = null;
        this.context = null;
    }

    protected static Path getDataDirPath() {
        String dataDir = null;

        if (Framework.isTestModeSet()) {
            dataDir = "/tmp";
        } else {
            dataDir = Framework.getProperty("nuxeo.data.dir");
        }
        Path path = new Path(dataDir);
        path = path.append("resources");
        return path;
    }

    public static void expandResources() throws Exception {
        URL resourceURL = getResource(SAMPLES_ROOT_PATH);
        File resourceRoot = new File(resourceURL.getFile());
        Path path = getDataDirPath();
        File dataDir = new File(path.toString());
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        FileUtils.copyTree(resourceRoot, dataDir);
    }

}
