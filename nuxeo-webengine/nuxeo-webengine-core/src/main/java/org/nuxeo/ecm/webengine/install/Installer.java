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
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.install;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebDeployException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.RuntimeContext;
import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("install")
public class Installer {

    private static final Log log = LogFactory.getLog(Installer.class);

    @XNode("@module")
    public String module;

    @XNodeList(value="copy", type=ArrayList.class, componentType=CopyOperation.class)
    private List<CopyOperation> copyOperations;

    @XNodeList(value="append", type=ArrayList.class, componentType=AppendOperation.class)
    private List<AppendOperation> appendOperations;

    protected RuntimeContext ctx;

    public Bundle getBundle() {
        return ctx.getBundle();
    }

    public RuntimeContext getContext() {
        return ctx;
    }

    public void logError(String message, Throwable t) {
        log.error(message, t);
    }

    public void logError(String message) {
        log.error(message);
    }

    public void logInfo(String message) {
        log.error(message);
    }

    public void logWarning(String message) {
        log.error(message);
    }

    public void logTrace(String message) {
        log.error(message);
    }

    public void install(RuntimeContext ctx, File installDir) {
        this.ctx = ctx;
        if (module != null) {
            if (new File(installDir, module).exists()) {
                return;
            }
        }
        boolean deleteDir = false;
        File bundleDir = null;
        try {
            Bundle bundle = ctx.getBundle();
            File file = getBundleFile(bundle);
            if (file == null) {
                throw new UnsupportedOperationException("Couldn't transform the bundle location into a file");
            }
            if (file.isDirectory()) {
                bundleDir = file;
            } else {
                deleteDir = true;
                bundleDir = getTempBundleDir(bundle);
                ZipUtils.unzip(file, bundleDir);
            }
            if (copyOperations != null) {
                for (CopyOperation copy : copyOperations) {
                    copy.run(this, bundleDir, installDir);
                }
            }
            if (appendOperations != null) {
                for (AppendOperation append : appendOperations) {
                    append.run(this, bundleDir, installDir);
                }
            }
// TODO remove this modules are lazy loaded now
//            if (module != null) {
//                WebEngine engine = Framework.getService(WebEngine.class);
//                engine.getModuleManager().loadModule(new File(engine.getRootDirectory(), module), module);
//            }
        } catch (Exception e) {
            throw new WebDeployException("Installation failed for bundle: "+ctx.getBundle().getSymbolicName(), e);
        } finally {
            if (deleteDir && bundleDir != null) {
                FileUtils.deleteTree(bundleDir);
            }
        }
    }

    public void uninstall(RuntimeContext ctx, File installDir) {
        //TODO
    }

    public static void copyResources(Bundle bundle, String path, File root) throws IOException {
        File file = Framework.getRuntime().getBundleFile(bundle);
        if (file == null) {
            throw new UnsupportedOperationException("Couldn't transform the bundle location into a file");
        }
        root.mkdirs();
        if (file.isDirectory()) {
            file = new File(file, path);
            FileUtils.copy(file.listFiles(), root);
        } else {
            ZipUtils.unzip(path, file, root);
        }
    }

    protected File getTempBundleDir(Bundle bundle) {
        return new File(Framework.getRuntime().getHome(), "tmp/bundles/"+bundle.getSymbolicName());
    }

    protected File getBundleFile(Bundle bundle) {
        return Framework.getRuntime().getBundleFile(bundle);
    }

}
