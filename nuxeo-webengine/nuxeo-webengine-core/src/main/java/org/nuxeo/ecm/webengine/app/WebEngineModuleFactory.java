/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.app;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.runtime.api.Framework;
import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebEngineModuleFactory {

    public static Log log = LogFactory.getLog(WebEngineModuleFactory.class);

    public static WebEngineModule getApplication(WebEngineModule app, Bundle bundle, Map<String,String> attrs) throws Exception {
        WebEngine engine = Framework.getLocalService(WebEngine.class);

        boolean explode = true;
        if (attrs != null) {
            if ("false".equals(attrs.get("explode"))) {
                explode = false;
            }
        }
        // register the web engine

        File moduleDir = null;
        File bundleFile = Framework.getRuntime().getBundleFile(bundle);
        if (explode) { // this will also add the exploded directory to WebEngine
                       // class loader
            moduleDir = explodeBundle(engine, bundle, bundleFile);
        } else if (bundleFile.isDirectory()) {
            moduleDir = bundleFile;
        }

        if (engine.isDevMode() && moduleDir != null) {
            engine.getWebLoader().addClassPathElement(moduleDir);
            app = (WebEngineModule)engine.loadClass(app.getClass().getName()).newInstance();
        }
        app.init(engine, bundle, moduleDir, attrs);
        engine.addApplication(app);

        log.info("Deployed web module found in bundle: " + bundle.getSymbolicName());

        return app;
    }


    private static File explodeBundle(WebEngine engine, Bundle bundle, File bundleFile) throws IOException {
        if (bundleFile.isDirectory()) { // exploded jar - deploy it as is.
            return bundleFile;
        } else { // should be a JAR - we copy the bundle module content
            File moduleRoot = new File(engine.getRootDirectory(), "modules/"
                    + bundle.getSymbolicName());
            if (moduleRoot.exists()) {
                if (bundleFile.lastModified() < moduleRoot.lastModified()) {
                    // already deployed and JAR was not modified since.
                    return moduleRoot;
                }
                // remove existing files
                FileUtils.deleteTree(moduleRoot);
            }
            // create the module root
            moduleRoot.mkdirs();
            ZipUtils.unzip(bundleFile, moduleRoot);
            return moduleRoot;
        }
    }

}
