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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Invokes the ReloadService by reflection as this module does not have access to the runtime context.
 *
 * @author matic
 * @since 5.5
 */
public class ReloadServiceInvoker {

    protected Object reloadService;

    protected Method deployBundles;

    protected Method undeployBundles;

    /**
     * Method to run the deployment preprocessor, previously handled by the deployBundle method
     *
     * @since 5.6
     */
    protected Method runDeploymentPreprocessor;

    /**
     * Method to install local web resources, as the deployment preprocessor won't see dev bundles as defined by Nuxeo
     * IDE.
     *
     * @since 5.6
     * @deprecated since 5.6, use {@link #runDeploymentPreprocessor} instead, also see
     *             org.nuxeo.runtime.reload.ReloadService
     */
    protected Method installWebResources;

    protected Method flush;

    protected Method reload;

    protected Method flushSeam;

    protected Method reloadSeam;

    /**
     * @since 9.3
     */
    protected Method getOSGIBundleName;

    public ReloadServiceInvoker(ClassLoader cl) throws ReflectiveOperationException {
        Class<?> frameworkClass = cl.loadClass("org.nuxeo.runtime.api.Framework");
        Class<?> reloadServiceClass = cl.loadClass("org.nuxeo.runtime.reload.ReloadService");
        Method getLocalService = frameworkClass.getDeclaredMethod("getLocalService", Class.class);
        reloadService = getLocalService.invoke(null, reloadServiceClass);
        // TODO REVIEW - should we reload resources when deploying and undeploying ?
        deployBundles = reloadServiceClass.getDeclaredMethod("deployBundles", List.class);
        undeployBundles = reloadServiceClass.getDeclaredMethod("undeployBundles", List.class);
        runDeploymentPreprocessor = reloadServiceClass.getDeclaredMethod("runDeploymentPreprocessor");
        installWebResources = reloadServiceClass.getDeclaredMethod("installWebResources", File.class);
        flush = reloadServiceClass.getDeclaredMethod("flush");
        reload = reloadServiceClass.getDeclaredMethod("reload");
        flushSeam = reloadServiceClass.getDeclaredMethod("flushSeamComponents");
        reloadSeam = reloadServiceClass.getDeclaredMethod("reloadSeamComponents");
        getOSGIBundleName = reloadServiceClass.getDeclaredMethod("getOSGIBundleName", File.class);
    }

    public void hotDeployBundles(DevBundle[] bundles) throws ReflectiveOperationException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(reloadService.getClass().getClassLoader());
            flush();
            // rebuild existing war, this will remove previously copied web
            // resources
            // commented out for now, see NXP-9642
            // runDeploymentPreprocessor();

            // don't use stream here, cause of ReflectiveOperationException
            boolean hasSeam = false;
            List<File> files = new ArrayList<>(bundles.length);
            for (DevBundle bundle : bundles) {
                if (bundle.getDevBundleType() == DevBundleType.Bundle) {
                    File file = bundle.file();
                    // fill dev bundle with its OSGI bundle name in order to allow SDK to undeploy them
                    // this is how admin center get bundle name
                    bundle.name = getOSGIBundleName(file);
                    files.add(file);
                } else {
                    hasSeam = hasSeam || bundle.getDevBundleType() == DevBundleType.Seam;
                }
            }
            // deploy bundles
            deployBundles(files);
            // install their web resources
            for (File file : files) {
                installWebResources(file);
            }
            // check if we need to reload seam
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
            List<String> bundleNames = Stream.of(bundles)
                                             .filter(bundle -> bundle.devBundleType == DevBundleType.Bundle)
                                             .map(DevBundle::getName)
                                             .filter(Objects::nonNull)
                                             .collect(Collectors.toList());
            // undeploy bundles
            undeployBundles(bundleNames);
            // run deployment preprocessor again: this will remove potential
            // resources that were copied in the war at deploy
            // commented out for now, see NXP-9642
            // runDeploymentPreprocessor();

            // check if we need to flush seam
            if (Stream.of(bundles).map(DevBundle::getDevBundleType).anyMatch(DevBundleType.Seam::equals)) {
                flushSeam();
            }
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    protected void deployBundles(List<File> files) throws ReflectiveOperationException {
        // instantiate an object array in order to prevent array to var-args
        deployBundles.invoke(reloadService, files);
    }

    protected void undeployBundles(List<String> bundleNames) throws ReflectiveOperationException {
        // instantiate an object array in order to prevent array to var-args
        undeployBundles.invoke(reloadService, bundleNames);
    }

    protected void flush() throws ReflectiveOperationException {
        flush.invoke(reloadService);
    }

    protected void flushSeam() throws ReflectiveOperationException {
        flushSeam.invoke(reloadService);
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

    /**
     * @since 9.3
     */
    protected String getOSGIBundleName(File file) throws ReflectiveOperationException {
        return (String) getOSGIBundleName.invoke(reloadService, file);
    }

    /**
     * @deprecated since 5.6, use {@link #runDeploymentPreprocessor()} instead, also see
     *             org.nuxeo.runtime.reload.ReloadService
     */
    @Deprecated
    protected void installWebResources(File file) throws ReflectiveOperationException {
        installWebResources.invoke(reloadService, file);
    }

}
