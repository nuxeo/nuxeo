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

package org.nuxeo.template.importer;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
public class TemplateBundleActivator implements BundleActivator {

    private BundleContext context;

    protected static final Log log = LogFactory.getLog(TemplateBundleActivator.class);

    protected static String getTemplateResourcesRootPath() {
        return ModelImporter.RESOURCES_ROOT;
    }

    public URL getResource(String path) {
        return this.context.getBundle().getResource(path);
    }

    public Enumeration findEntries(String path) {
        return this.context.getBundle().findEntries(path, null, true);
    }

    public BundleContext getContext() {
        return context;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        this.context = context;
        expandResources();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
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

    public void expandResources() throws Exception {
        log.info("Deploying templates for bundle "
                + context.getBundle().getSymbolicName());

        URL sampleRootURL = getResource(getTemplateResourcesRootPath());
        if (sampleRootURL == null) {
            return;
        }

        Path path = getDataDirPath();
        path = path.append(getTemplateResourcesRootPath());
        File dataDir = new File(path.toString());
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        Enumeration urls = findEntries(getTemplateResourcesRootPath());
        while (urls.hasMoreElements()) {
            URL resourceURL = (URL) urls.nextElement();
            InputStream is = resourceURL.openStream();
            String filePath = resourceURL.getFile();
            filePath = filePath.split("/" + getTemplateResourcesRootPath()
                    + "/")[1];
            filePath = "/" + filePath;
            File f = new File(dataDir, filePath);
            File parent = f.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }
            FileUtils.copyToFile(is, f);
            is.close();
        }
    }
}
