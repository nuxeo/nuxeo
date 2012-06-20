/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.runtime.tomcat.dev;

import java.io.File;
import java.lang.reflect.Method;

/**
 * Invokes the ReloadService by reflection as this module does not have access
 * to the runtime context.
 *
 * @author matic
 * @since 5.5
 */
public class ReloadServiceInvoker {

    protected Object reloadService;

    protected Method deployBundle;

    protected Method undeployBundle;

    protected Method flush;

    protected Method reload;

    protected Method flushSeam;

    protected Method reloadSeam;

    public ReloadServiceInvoker(ClassLoader cl) throws Exception {
        Class<?> frameworkClass = cl.loadClass("org.nuxeo.runtime.api.Framework");
        Class<?> reloadServiceClass = cl.loadClass("org.nuxeo.runtime.reload.ReloadService");
        Method getLocalService = frameworkClass.getDeclaredMethod(
                "getLocalService", new Class<?>[] { Class.class });
        reloadService = getLocalService.invoke(null,
                new Object[] { reloadServiceClass });
        deployBundle = reloadServiceClass.getDeclaredMethod("deployBundle",
                new Class<?>[] { File.class });
        undeployBundle = reloadServiceClass.getDeclaredMethod("undeployBundle",
                new Class<?>[] { String.class });
        flush = reloadServiceClass.getDeclaredMethod("flush", new Class<?>[0]);
        reload = reloadServiceClass.getDeclaredMethod("reload", new Class<?>[0]);
        flushSeam = reloadServiceClass.getDeclaredMethod("flushSeamComponents",
                new Class<?>[0]);
        reloadSeam = reloadServiceClass.getDeclaredMethod(
                "reloadSeamComponents", new Class<?>[0]);
    }

    protected void hotDeployBundles(DevBundle[] bundles) throws Exception {
        boolean hasSeam = false;
        for (DevBundle bundle : bundles) {
            if (bundle.devBundleType == DevBundleType.Bundle) {
                // TODO: use other existing method to be able to reload the
                // resources classpath?
                bundle.name = (String) deployBundle.invoke(reloadService,
                        new Object[] { bundle.file() });
            } else if (bundle.devBundleType.equals(DevBundleType.Seam)) {
                hasSeam = true;
            }
        }
        if (hasSeam) {
            reloadSeam.invoke(reloadService);
        }
        reload();
    }

    protected void hotUndeployBundles(DevBundle[] bundles) throws Exception {
        boolean hasSeam = false;
        for (DevBundle bundle : bundles) {
            if (bundle.devBundleType.equals(DevBundleType.Bundle)
                    && bundle.name != null) {
                // TODO: use other API to reload file instead, to be able to
                // reload resources (?)
                undeployBundle.invoke(reloadService,
                        new Object[] { bundle.name });
            } else if (bundle.devBundleType.equals(DevBundleType.Seam)) {
                hasSeam = true;
            }
        }
        if (hasSeam) {
            flushSeam.invoke(reloadService);
        }
        flush();
    }

    protected void flush() throws Exception {
        flush.invoke(reloadService);
    }

    protected void reload() throws Exception {
        reload.invoke(reloadService);
    }

    protected void reloadSeam() throws Exception {
        reloadSeam.invoke(reloadService);
    }
}
