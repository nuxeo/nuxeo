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
     * Returns true in built-in Daemon is Enabled by configuration
     *
     * @return
     */
    boolean isEnabled();

    /**
     * Returns true if OpenOffice is configured and found
     *
     * @return
     */
    boolean isConfigured();

    /**
     * Returns true if Daemon is running
     * @return
     */
    boolean isRunning();

    /**
     * Returns number of OpenOffice workers
     * @return
     */
    int getNbWorkers();


    /**
     * Starts the daemon and resturn immediatly
     * @return
     */
    int startDaemon();


    /**
     * Starts the Daemon and wait until Daemon is ready to accept calls
     * @return
     */
    boolean startDaemonAndWaitUntilReady();

    /**
     * Stops the Daemon and returns immediatly
     * @return
     */
    void stopDaemon();


    /**
     * Stops the Daemon and wait until it exists
     * @return
     */
    boolean stopDaemonAndWaitForCompletion();




}
