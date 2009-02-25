/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.convert.oooserver;

public interface OOoDaemonService {

    /**
     * @return true in built-in Daemon is Enabled by configuration.
     */
    boolean isEnabled();

    /**
     * @return true if OpenOffice is configured and found.
     */
    boolean isConfigured();

    /**
     * @return true if Daemon is running.
     */
    boolean isRunning();

    /**
     * @return the number of OpenOffice workers.
     */
    int getNbWorkers();

    /**
     * Starts the daemon and returns immediatly.
     */
    int startDaemon();

    /**
     * Starts the Daemon and wait until Daemon is ready to accept calls.
     */
    boolean startDaemonAndWaitUntilReady();

    /**
     * Stops the Daemon and returns immediately.
     */
    void stopDaemon();

    /**
     * Stops the Daemon and wait until it exists.
     */
    boolean stopDaemonAndWaitForCompletion();

}
