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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.webengine.exceptions.WebDeployException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.RuntimeContext;
import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("install")
public class Installer {

    private final static Log log = LogFactory.getLog(Installer.class);

    @XNode("@guard")
    public String guard;

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

    public void install(RuntimeContext ctx, File installDir) throws WebDeployException {
        this.ctx = ctx;
        if (guard != null) {
            if (new File(installDir, guard).exists()) return;
        }
        File bundleDir = null;
        try {
            bundleDir = getOrCreateBundleDir(ctx.getBundle());
            if (bundleDir == null) {
                return;
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
        } catch (Exception e) {
            throw new WebDeployException("Installation failed for bundle: "+ctx.getBundle().getSymbolicName(), e);
        } finally {
            if (bundleDir != null) {
                FileUtils.deleteTree(bundleDir);
            }
        }
    }

    public void uninstall(RuntimeContext ctx, File installDir) throws WebDeployException {
        //TODO
    }


    public static void copyResources(Bundle bundle, String path, File root) throws URISyntaxException, IOException {
        root.mkdirs(); // create root dir if not already exists
        // copy web dir located in the bundle jar into this dir
        //TODO: getLocation() may not work with some OSGi impl.
        String location = bundle.getLocation();
        if (location.startsWith("file:")) {
            if (location.endsWith(".jar")) {
                ZipUtils.unzip(path, new URL(location), root);
            } else {
                File file = new File(new URL(location).toURI());
                file = new File(file, path);
                FileUtils.copy(file.listFiles(), root);
            }
        } else {
            if (location.endsWith(".jar")) {
                ZipUtils.unzip(path, new File(location), root);
            } else {
                File file = new File(location);
                file = new File(file, path);
                FileUtils.copy(file.listFiles(), root);
            }
        }
    }

    protected File getTempBundleDir(Bundle bundle) {
        return new File(Framework.getRuntime().getHome(), "tmp/bundles/"+bundle.getSymbolicName());
    }

    protected File getOrCreateBundleDir(Bundle bundle) throws Exception {
        File bundleDir = null;
        String location = ctx.getBundle().getLocation();
        if (location.contains(":/")) { // an URL
            if (location.startsWith("file:")) {
                File file = new File(new URI(location));
                if (file.isFile()) { // a jar?
                    bundleDir = getTempBundleDir(bundle);
                    ZipUtils.unzip(file, bundleDir);
                } else if (file.isDirectory()) {
                    bundleDir = file;
                } else {
                    logError("Don't now how to handle the bundle location string: "+location);
                    return null;
                }
            } else { // assume a jar
                bundleDir = getTempBundleDir(ctx.getBundle());
                ZipUtils.unzip(new URL(location), bundleDir);
            }
        } else { // a file?
            File file = new File(location);
            if (!file.exists()) {
                logError("Don't now how to handle the bundle location string: "+location);
                return null;
            }
            if (file.isFile()) { // should be a jar
                bundleDir = getTempBundleDir(ctx.getBundle());
                ZipUtils.unzip(file, bundleDir);
            } else {
                bundleDir = file;
            }
        }
        return bundleDir;
    }


}
