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
package org.nuxeo.runtime.reload;

import java.io.File;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface ReloadService {

    public static final String RELOAD_TOPIC = "org.nuxeo.runtime.reload";

    public static final String FLUSH_EVENT_ID = "flush";

    public static final String RELOAD_EVENT_ID = "reload";

    public static final String FLUSH_SEAM_EVENT_ID = "flushSeamComponents";

    public static final String RELOAD_SEAM_EVENT_ID = "reloadSeamComponents";

    /**
     * Sends a runtime event with id {@link #RELOAD_EVENT_ID} so that listeners
     * can be notified that a reload has been done.
     * <p>
     * Also calls {@link #reloadProperties()} by default, but not other reload
     * methods as they could alter the running application behaviour.
     *
     * @since 5.5
     * @see #reloadProperties()
     */
    void reload() throws Exception;

    /**
     * Reloads the Nuxeo repository configuration
     */
    void reloadRepository() throws Exception;

    /**
     * Reloads runtime framework properties
     */
    void reloadProperties() throws Exception;

    /**
     * Sends a runtime event with id {@link #RELOAD_SEAM_EVENT_ID}
     *
     * @since 5.5
     */
    void reloadSeamComponents() throws Exception;

    /**
     * Sends a runtime event with id {@link #FLUSH_EVENT_ID} so that listeners
     * can be notified that a flush is needed (after a reload for instance).
     * <p>
     * Also calls {@link #flushJaasCache()} by default, but not other flush
     * methods as they could alter the running application behaviour.
     *
     * @throws Exception
     * @see {@link #flushJaasCache()}
     * @since 5.5
     */
    void flush() throws Exception;

    /**
     * Sends an event that will trigger reset of JaasCache
     */
    void flushJaasCache() throws Exception;

    /**
     * Sends a runtime event with id {@link #FLUSH_SEAM_EVENT_ID}
     *
     * @since 5.6
     */
    void flushSeamComponents() throws Exception;

    /**
     * @since 5.5
     * @deprecated since 5.6: deploy should be handled by tasks directly
     */
    @Deprecated
    String deployBundle(File file, boolean reloadResources) throws Exception;

    /**
     * @see #deployBundle(File, boolean)
     * @deprecated since 5.6: deploy should be handled by tasks directly
     */
    @Deprecated
    String deployBundle(File file) throws Exception;

    /**
     * @since 5.5
     * @deprecated since 5.6: deploy should be handled by tasks directly
     */
    @Deprecated
    void undeployBundle(String name) throws Exception;

    /**
     * Copy web resources in nuxeo WAR.
     * <p>
     * Called by {@link #deployBundle(File, boolean)}
     *
     * @since 5.5
     * @deprecated since 5.6: install should be handled by tasks directly
     */
    @Deprecated
    void installWebResources(File file) throws Exception;

}
