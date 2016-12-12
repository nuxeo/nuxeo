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
package org.nuxeo.template.deckjs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * The activator expand the sample documents in the data directory.
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @author <a href="mailto:ldoguin@nuxeo.com">Laurent Doguin</a>
 */
public class TemplateBundleActivator implements BundleActivator {

    protected static TemplateBundleActivator instance;

    private BundleContext context;

    protected static final Log log = LogFactory.getLog(TemplateBundleActivator.class);

    private static File tmpDir;

    private static String dataDirPath;

    protected static String getTemplateResourcesRootPath() {
        return "templatesamples";
    }

    public URL getResource(String path) {
        return this.context.getBundle().getResource(path);
    }

    public Enumeration<?> findEntries(String path) {
        return this.context.getBundle().findEntries(path, null, true);
    }

    public BundleContext getContext() {
        return context;
    }

    @Override
    public void start(BundleContext context) {
        instance = this;
        this.context = context;
        initDataDirPath();
        expandResources();
    }

    @Override
    public void stop(BundleContext context) {
        this.context = null;
        cleanupDataDirPath();
    }

    /* Note that this may be called twice, because several activators inherit from this class. */
    protected static void initDataDirPath() {
        if (dataDirPath != null) {
            return;
        }
        String dataDir = Environment.getDefault().getData().getPath();
        Path path = new Path(dataDir);
        path = path.append("resources");
        dataDirPath = path.toString();
    }

    @SuppressWarnings("deprecation")
    protected static void cleanupDataDirPath() {
        if (tmpDir != null) {
            FileUtils.deleteTree(tmpDir);
            tmpDir = null;
        }
        dataDirPath = null;
    }

    protected static Path getDataDirPath() {
        return new Path(dataDirPath);
    }

    public void expandResources() {
        log.info("Deploying templates for bundle " + context.getBundle().getSymbolicName());

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

        Enumeration<?> urls = findEntries(getTemplateResourcesRootPath());
        while (urls.hasMoreElements()) {
            URL resourceURL = (URL) urls.nextElement();
            try (InputStream is = resourceURL.openStream()) {
                String filePath = resourceURL.getFile();
                filePath = filePath.split("/" + getTemplateResourcesRootPath() + "/")[1];
                filePath = "/" + filePath;
                File f = new File(dataDir, filePath);
                File parent = f.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }
                FileUtils.copyToFile(is, f);
            } catch (IOException e) {
                throw new NuxeoException("Failed for template: " + resourceURL, e);
            }
        }
    }

    public static InputStream getResourceAsStream(String path) throws IOException {
        URL url = instance.getResource(path);
        return url != null ? url.openStream() : null;
    }

}
