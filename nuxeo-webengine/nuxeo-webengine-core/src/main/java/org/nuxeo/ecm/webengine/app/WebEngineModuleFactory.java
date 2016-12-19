/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.webengine.app;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.common.utils.PathFilter;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.osgi.BundleManifestReader;
import org.nuxeo.runtime.api.Framework;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class WebEngineModuleFactory {

    public static Log log = LogFactory.getLog(WebEngineModuleFactory.class);

    public static Bundle[] getFragments(Bundle bundle) {
        BundleContext context = bundle.getBundleContext();
        ServiceReference ref = context.getServiceReference(PackageAdmin.class.getName());
        PackageAdmin admin = (PackageAdmin) context.getService(ref);
        return admin.getFragments(bundle);
    }

    public static WebEngineModule getApplication(WebEngineModule app, Bundle bundle, Map<String, String> attrs)
            throws ReflectiveOperationException, IOException {
        WebEngine engine = Framework.getLocalService(WebEngine.class);

        boolean explode = true;
        if (attrs != null) {
            if ("false".equals(attrs.get("explode"))) {
                explode = false;
            }
        }
        // register the web engine

        File moduleDir = locateModuleDir(bundle, engine, explode);

        app.init(engine, bundle, new File(moduleDir, "module.xml"), attrs);

        app.cfg.directory = moduleDir;

        Bundle[] fragments = getFragments(bundle);
        for (Bundle fragment : fragments) {
            File fragmentDir = locateModuleDir(fragment, engine, explode);
            app.cfg.fragmentDirectories.add(fragmentDir);
        }
        app.cfg.allowHostOverride = Boolean.parseBoolean((String) bundle.getHeaders().get(
                BundleManifestReader.ALLOW_HOST_OVERRIDE));
        engine.addApplication(app);

        log.info("Deployed web module found in bundle: " + bundle.getSymbolicName());

        return app;
    }

    private static File locateModuleDir(Bundle bundle, WebEngine engine, boolean explode) throws IOException {
        File moduleDir = null;
        File bundleFile = Framework.getRuntime().getBundleFile(bundle);
        if (explode) {
            // this will also add the exploded directory to WebEngine class
            // loader
            moduleDir = explodeBundle(engine, bundle, bundleFile);
        } else if (bundleFile.isDirectory()) {
            moduleDir = bundleFile;
        }
        return moduleDir;
    }

    private static File explodeBundle(WebEngine engine, Bundle bundle, File bundleFile) throws IOException {
        if (bundleFile.isDirectory()) { // exploded jar - deploy it as is.
            return bundleFile;
        } else { // should be a JAR - we copy the bundle module content
            File moduleRoot = new File(engine.getRootDirectory(), "modules/" + bundle.getSymbolicName());
            if (moduleRoot.exists()) {
                if (bundleFile.lastModified() < moduleRoot.lastModified()) {
                    // already deployed and JAR was not modified since.
                    return moduleRoot;
                }
                // remove existing files
                FileUtils.deleteQuietly(moduleRoot);
            }
            // create the module root
            moduleRoot.mkdirs();
            // avoid unziping classes
            ZipUtils.unzip(bundleFile, moduleRoot, new PathFilter() {
                @Override
                public boolean isExclusive() {
                    return false;
                }

                @Override
                public boolean accept(Path arg0) {
                    return !arg0.lastSegment().endsWith(".class");
                }
            });
            return moduleRoot;
        }
    }

}
