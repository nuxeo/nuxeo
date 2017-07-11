/*
 * (C) Copyright 2011-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     matic
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.runtime.tomcat.dev;

import java.io.File;
import java.lang.reflect.Method;

/**
 * Invokes the ReloadService by reflection as this module does not have access to the runtime context.
 *
 * @author matic
 * @since 5.5
 */
public class ReloadServiceInvoker {

    protected Object reloadService;

    protected Method deployBundle;

    protected Method undeployBundle;

    /**
     * Method to run the deployment preprocessor, previously handled by the deployBundle method
     *
     * @since 5.6
     */
    protected Method runDeploymentPreprocessor;

    /**
     * Method to install local web resources, as the deployment preprocessor won't see dev bundles as defined by Nuxeo
     * IDE
     *
     * @since 5.6
     */
    protected Method installWebResources;

    protected Method flush;

    protected Method reload;

    protected Method flushSeam;

    protected Method reloadSeam;

    public ReloadServiceInvoker(ClassLoader cl) throws ReflectiveOperationException {
        Class<?> frameworkClass = cl.loadClass("org.nuxeo.runtime.api.Framework");
        Class<?> reloadServiceClass = cl.loadClass("org.nuxeo.runtime.reload.ReloadService");
        Method getLocalService = frameworkClass.getDeclaredMethod("getLocalService", new Class<?>[] { Class.class });
        reloadService = getLocalService.invoke(null, new Object[] { reloadServiceClass });
        deployBundle = reloadServiceClass.getDeclaredMethod("deployBundle", new Class<?>[] { File.class });
        undeployBundle = reloadServiceClass.getDeclaredMethod("undeployBundle", new Class<?>[] { String.class });
        runDeploymentPreprocessor = reloadServiceClass.getDeclaredMethod("runDeploymentPreprocessor", new Class<?>[0]);
        installWebResources = reloadServiceClass.getDeclaredMethod("installWebResources", new Class<?>[] { File.class });
        flush = reloadServiceClass.getDeclaredMethod("flush", new Class<?>[0]);
        reload = reloadServiceClass.getDeclaredMethod("reload", new Class<?>[0]);
        flushSeam = reloadServiceClass.getDeclaredMethod("flushSeamComponents", new Class<?>[0]);
        reloadSeam = reloadServiceClass.getDeclaredMethod("reloadSeamComponents", new Class<?>[0]);
    }

    public void hotDeployBundles(DevBundle[] bundles) throws ReflectiveOperationException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(reloadService.getClass().getClassLoader());
            flush();
            boolean hasSeam = false;
            // rebuild existing war, this will remove previously copied web
            // resources
            // commented out for now, see NXP-9642
            // runDeploymentPreprocessor();
            for (DevBundle bundle : bundles) {
                if (bundle.devBundleType == DevBundleType.Bundle) {
                    bundle.name = (String) deployBundle.invoke(reloadService, new Object[] { bundle.file() });
                    // install its web resources
                    installWebResources.invoke(reloadService, new Object[] { bundle.file() });
                } else if (bundle.devBundleType.equals(DevBundleType.Seam)) {
                    hasSeam = true;
                }
            }
            if (hasSeam) {
                reloadSeam();
            }
            reload();
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    public void hotUndeployBundles(DevBundle[] bundles) throws ReflectiveOperationException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(reloadService.getClass().getClassLoader());
            boolean hasSeam = false;
            for (DevBundle bundle : bundles) {
                if (bundle.devBundleType.equals(DevBundleType.Bundle) && bundle.name != null) {
                    undeployBundle.invoke(reloadService, new Object[] { bundle.name });
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

    protected void flush() throws ReflectiveOperationException {
        flush.invoke(reloadService);
    }

    protected void reload() throws ReflectiveOperationException {
        reload.invoke(reloadService);
    }

    protected void reloadSeam() throws ReflectiveOperationException {
        reloadSeam.invoke(reloadService);
    }

    protected void runDeploymentPreprocessor() throws ReflectiveOperationException {
        runDeploymentPreprocessor.invoke(reloadService);
    }

}
