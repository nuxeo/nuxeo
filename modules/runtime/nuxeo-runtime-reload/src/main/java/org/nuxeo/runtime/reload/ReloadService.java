/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.runtime.reload;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.nuxeo.runtime.deployment.preprocessor.DeploymentPreprocessor;
import org.nuxeo.runtime.service.TimestampedService;
import org.osgi.framework.BundleException;

/**
 * Service tracking reload related events or commands when installing a package.
 * <p />
 * WARNING: This interface is used by reflection in org.nuxeo.runtime.tomcat.dev.ReloadServiceInvoker.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface ReloadService extends TimestampedService {

    String RELOAD_TOPIC = "org.nuxeo.runtime.reload";

    String FLUSH_EVENT_ID = "flush";

    String BEFORE_RELOAD_EVENT_ID = "before-reload";

    String RELOAD_EVENT_ID = "reload";

    String AFTER_RELOAD_EVENT_ID = "after-reload";

    String FLUSH_SEAM_EVENT_ID = FLUSH_EVENT_ID + "SeamComponents";

    String RELOAD_SEAM_EVENT_ID = "reloadSeamComponents";

    /**
     * This property allows to use the former way to hot reload Nuxeo server.
     * <p/>
     * The property and the former mechanism will be removed in the next LTS version.
     *
     * @since 9.3
     */
    String USE_COMPAT_HOT_RELOAD = "nuxeo.hotreload.compat.mechanism";

    /**
     * Sends a runtime event with id {@link #RELOAD_EVENT_ID} so that listeners can be notified that a reload has been
     * done.
     * <p>
     * Also calls {@link #reloadProperties()} by default, but not other reload methods as they could alter the running
     * application behaviour.
     *
     * @since 5.5
     * @see #reloadProperties()
     */
    void reload() throws InterruptedException;

    /**
     * Reloads runtime framework properties
     */
    void reloadProperties() throws IOException;

    /**
     * Sends a runtime event with id {@link #RELOAD_SEAM_EVENT_ID}
     *
     * @since 5.5
     */
    void reloadSeamComponents();

    /**
     * Sends a runtime event with id {@link #FLUSH_EVENT_ID} so that listeners can be notified that a flush is needed
     * (after a reload for instance).
     * <p>
     * Also calls {@link #flushJaasCache()} by default, but not other flush methods as they could alter the running
     * application behaviour.
     *
     * @see #flushJaasCache()
     * @since 5.5
     */
    void flush();

    /**
     * Returns the last time one of the flush commands where called on this service instance ({@link #flush()} or
     * {@link #flushJaasCache()} or {@link #flushSeamComponents()}, or null if never called.
     *
     * @since 5.6
     */
    Long lastFlushed();

    /**
     * Sends an event that can trigger reset of JaasCache.
     */
    void flushJaasCache();

    /**
     * Sends a runtime event with id {@link #FLUSH_SEAM_EVENT_ID}.
     *
     * @since 5.6
     */
    void flushSeamComponents();

    /**
     * Deploys bundle to the runtime, without reloading resources.
     *
     * @since 5.5
     * @see #deployBundles(List, boolean)
     * @deprecated since 9.3 use {@link #reloadBundles(ReloadContext)} instead. Kept for backward compatibility.
     */
    @Deprecated(since = "9.3")
    default void deployBundle(File file) throws BundleException {
        deployBundle(file, false);
    }

    /**
     * Deploys bundle to the runtime, gives possibility to control resources reloading.
     *
     * @since 5.5
     * @see #deployBundles(List, boolean)
     * @deprecated since 9.3 use {@link #reloadBundles(ReloadContext)} instead. Kept for backward compatibility.
     */
    @Deprecated(since = "9.3")
    default void deployBundle(File file, boolean reloadResources) throws BundleException {
        deployBundles(Collections.singletonList(file), reloadResources);
    }

    /**
     * Deploys bundles to the runtime, without reloading resources.
     *
     * @since 9.3
     * @see #deployBundles(List, boolean)
     * @deprecated since 9.3 use {@link #reloadBundles(ReloadContext)} instead. Kept for backward compatibility.
     */
    @Deprecated(since = "9.3")
    default void deployBundles(List<File> files) throws BundleException {
        deployBundles(files, false);
    }

    /**
     * Deploys bundles to the runtime, gives possibility to control resources reloading.
     *
     * @since 9.3
     * @deprecated since 9.3 use {@link #reloadBundles(ReloadContext)} instead. Kept for backward compatibility.
     */
    @Deprecated(since = "9.3")
    void deployBundles(List<File> files, boolean reloadResources) throws BundleException;

    /**
     * Undeploys bundle from the runtime, without reloading resources.
     *
     * @since 5.6
     * @see #undeployBundles(List, boolean)
     * @deprecated since 9.3 use {@link #reloadBundles(ReloadContext)} instead. Kept for backward compatibility.
     */
    @Deprecated(since = "9.3")
    default void undeployBundle(String bundleName) throws BundleException {
        undeployBundle(bundleName, false);
    }

    /**
     * Undeploys bundle from the runtime, gives possibility to control resources reloading.
     *
     * @since 9.3
     * @see #undeployBundles(List, boolean)
     * @deprecated since 9.3 use {@link #reloadBundles(ReloadContext)} instead. Kept for backward compatibility.
     */
    @Deprecated(since = "9.3")
    default void undeployBundle(String bundleName, boolean reloadResources) throws BundleException {
        undeployBundles(Collections.singletonList(bundleName), reloadResources);
    }

    /**
     * Undeploys bundles from the runtime, without reloading resources.
     *
     * @since 9.3
     * @see #undeployBundles(List, boolean)
     * @deprecated since 9.3 use {@link #reloadBundles(ReloadContext)} instead. Kept for backward compatibility.
     */
    @Deprecated(since = "9.3")
    default void undeployBundles(List<String> bundleNames) throws BundleException {
        undeployBundles(bundleNames, false);
    }

    /**
     * Undeploys bundles from the runtime, gives possibility to control resources reloading.
     *
     * @since 9.3
     * @deprecated since 9.3 use {@link #reloadBundles(ReloadContext)} instead. Kept for backward compatibility.
     */
    @Deprecated(since = "9.3")
    void undeployBundles(List<String> bundleNames, boolean reloadResources) throws BundleException;

    /**
     * Called by ReloadServiceInvoker#hotReloadBundles.
     *
     * @return the result of hot reload operation
     * @since 9.3
     */
    ReloadResult reloadBundles(ReloadContext context) throws BundleException;

    /**
     * Runs the deployment preprocessor.
     *
     * @since 5.6
     * @see DeploymentPreprocessor
     */
    void runDeploymentPreprocessor() throws IOException;

    /**
     * Copies the bundle web resources into the nuxeo WAR directory.
     *
     * @since 5.5
     * @deprecated since 5.6: use {@link #runDeploymentPreprocessor()} method instead, now re-deploys all jars so that
     *             the nuxeo.war holds the same content than it would at startup. This method is called by reflection by
     *             ReloadServiceInvoker#hotDeployBundles. Keep it as compatibility code until NXP-9642 is done.
     */
    @Deprecated
    void installWebResources(File file) throws IOException;

    /***
     * Returns the OSGI bundle name if given file can be identified as an OSGI bundle, or null. The OSGI bundle can be a
     * jar or an exploded jar on file system.
     *
     * @since 5.6
     */
    String getOSGIBundleName(File file);

}
