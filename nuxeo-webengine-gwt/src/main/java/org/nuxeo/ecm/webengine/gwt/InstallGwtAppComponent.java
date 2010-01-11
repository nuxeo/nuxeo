/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.webengine.gwt;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class InstallGwtAppComponent extends DefaultComponent {

    private final static Log log = LogFactory.getLog(InstallGwtAppComponent.class);

    public final static String XP_INSTALL = "install";

    protected static File root;

    public static File getRoot() {
        return root;
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        root = new File(Framework.getLocalService(WebEngine.class).getRootDirectory(), "gwt");
        root.mkdirs();
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if ("install".equals(extensionPoint)) {
            InstallGwtAppDescriptor descriptor = (InstallGwtAppDescriptor)contribution;
            String moduleId = Framework.expandVars(descriptor.id);
            File dir = new File(root, moduleId);
            Bundle bundle = contributor.getContext().getBundle();
            File bf = contributor.getContext().getRuntime().getBundleFile(bundle);
            if (bf == null) {
                log.warn("Bundle type not supported - cannot be resolved to a file. Bundle: "
                        + bundle.getSymbolicName());
                return;
            }
            if (dir.isDirectory() && dir.lastModified() > bf.lastModified()) {
                return;
            }
            log.info("Installing GWT Application '"+moduleId+"' from bundle "+bundle.getSymbolicName());
            try {
                if (bf.isDirectory()) {
                    // copy bundle content to target directory
                    File[] files = bf.listFiles();
                    if (files == null) {
                        return;
                    }
                    for (File f : files) {
                        FileUtils.copyTree(f, new File(bf, f.getName()));
                    }
                } else {
                    // unzip bundle file to target directory
                    ZipUtils.unzip(bf, root);
                }
            } catch (Exception e) {
                log.error("Installing GWT Application failed!", e);
                FileUtils.deleteTree(root);
            }
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        // do nothing
    }
}
