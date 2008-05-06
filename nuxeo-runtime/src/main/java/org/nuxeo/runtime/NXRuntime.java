/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime;

import org.nuxeo.common.collections.ListenerList;
import org.nuxeo.runtime.api.Framework;

/**
 * Facade to access the installed nuxeo runtime service.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 * @deprecated Use {@link Framework} instead.
 */
@Deprecated
public final class NXRuntime {

    private static RuntimeService runtime;

    private static final ListenerList listeners = new ListenerList();


    private NXRuntime() {
    }

    /**
     * Tests whether or not the runtime was initialized.
     *
     * @return true if the runtime was intialized, false otherwise
     */
    public static synchronized boolean isInitialized() {
        return runtime != null;
    }

    /**
     * Tests whether or not the runtime was started and it's running.
     *
     * @return true if the runtime was started, false otherwise
     */
    public static synchronized boolean isRunning() {
        return runtime != null && runtime.isStarted();
    }

    /**
     * Gets the runtime service instance.
     * <p>
     * This method doesn't check if a runtime service was installed. If no
     * runtime is found null is returned.
     *
     * @return the runtime service instance or null if no runtime is found
     * @see NXRuntime#getInstance()
     */
    public static RuntimeService getRuntime() {
        return runtime;
    }

    /**
     * Gets the runtime service instance.
     * <p>
     * This method doesn't check if a runtime service was installed. If no
     * runtime is found an exception is thrown.
     *
     * @return the runtime service instance or null if no runtime is found
     * @throws IllegalStateException if no runtime was initialized
     * @see NXRuntime#getInstance()
     */
    public static synchronized RuntimeService getInstance() {
        if (runtime != null) {
            return runtime;
        } else {
            throw new IllegalStateException("Runtime was not initialized");
        }
    }

    /**
     * Sets the runtime instance.
     * <p>
     * This method should not be used by clients. It should be used only by
     * runtime implementations in the bootstrap process to initialize the
     * instance used by the facade
     * <p>
     * If a runtime instance was already set, an exception is thrown.
     *
     * @param instance
     *            the runtime instance
     * @throws IllegalStateException
     *             if a runtime service was already intialized
     */
    public static synchronized void setInstance(RuntimeService instance) {
        if (runtime == null) {
            setRuntime(instance);
        } else {
            throw new IllegalStateException("Runtime was already initialized");
        }
    }

    /**
     * Sets the runtime instance.
     * <p>
     * This method should not be used by clients. It should be used only by
     * runtime implementations in the bootstrap process to initialize the
     * instance used by the facade.
     * <p>
     * This doesn't check if a runtime was already set. If a runtime is
     * already set it will be replaced with the given instance.
     *
     * @param runtime
     *            the runtime instance
     */
    public static synchronized void setRuntime(RuntimeService runtime) {
        NXRuntime.runtime = runtime;
    }

    public static void sendEvent(RuntimeServiceEvent event) {
        Object[] listenersArray = listeners.getListeners();
        for (Object listener : listenersArray) {
            ((RuntimeServiceListener) listener).handleEvent(event);
        }
    }

    /**
     * Registers a listener to be notified about runtime events.
     * <p>
     * If the listener is already registered, do nothing.
     *
     * @param listener the listener to register
     */
    public static void addListener(RuntimeServiceListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove the given listener.
     * <p>
     * If the listener is not registered do nothing
     * @param listener
     */
    public static void removeListener(RuntimeServiceListener listener) {
        listeners.remove(listener);
    }

}
