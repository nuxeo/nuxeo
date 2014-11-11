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
import java.util.ArrayList;
import java.util.List;

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

    /**
     * Method to install local web resources, as the deployment preprocessor
     * won't see dev bundles as defined by Nuxeo IDE
     *
     * @since 5.6
     */
    protected Method installWebResources;

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
        installWebResources = reloadServiceClass.getDeclaredMethod(
                "installWebResources", new Class<?>[] { File.class });
        flush = reloadServiceClass.getDeclaredMethod("flush");
        reload = reloadServiceClass.getDeclaredMethod("reload", File[].class);
        flushSeam = reloadServiceClass.getDeclaredMethod("flushSeamComponents");
        reloadSeam = reloadServiceClass.getDeclaredMethod("reloadSeamComponents");
    }

    public void hotDeployBundles(DevBundle[] bundles) throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(
                    reloadService.getClass().getClassLoader());
            flush();
            boolean hasSeam = false;
            for (DevBundle bundle : bundles) {
                if (bundle.devBundleType == DevBundleType.Bundle) {
                    bundle.name = (String) deployBundle.invoke(reloadService,
                            new Object[] { bundle.file() });
                    // install its web resources
                    installWebResources.invoke(reloadService,
                            new Object[] { bundle.file() });
                } else if (bundle.devBundleType.equals(DevBundleType.Seam)) {
                    hasSeam = true;
                }
            }
            if (hasSeam) {
                reloadSeam();
            }
//            reload();
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    public void hotUndeployBundles(DevBundle[] bundles) throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(
                    reloadService.getClass().getClassLoader());
            boolean hasSeam = false;
            for (DevBundle bundle : bundles) {
                if (bundle.devBundleType.equals(DevBundleType.Bundle)
                        && bundle.name != null) {
                    undeployBundle.invoke(reloadService,
                            new Object[] { bundle.name });
                } else if (bundle.devBundleType.equals(DevBundleType.Seam)) {
                    hasSeam = true;
                }
            }
            // run deployment preprocessor again: this will remove potential
            // resources that were copied in the war at deploy
            // commented out for now, see NXP-9642
            // runDeploymentPreprocessor();
            if (hasSeam) {
                flushSeam.invoke(reloadService);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    protected void flush() throws Exception {
        flush.invoke(reloadService);
    }

    protected void reload(File[] additionalFragments) throws Exception {
        reload.invoke(reloadService, (Object)additionalFragments);
    }

    protected void reloadSeam() throws Exception {
        reloadSeam.invoke(reloadService);
    }

}
