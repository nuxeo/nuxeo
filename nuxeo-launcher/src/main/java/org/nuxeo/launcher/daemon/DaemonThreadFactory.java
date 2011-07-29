/*
 * (C) Copyright 2010-2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Benoit Delbosc
 *     Julien Carsique
 *
 * $Id$
 */

package org.nuxeo.launcher.daemon;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A factory to create daemon thread, this prevents the JVM to hang on exit
 * waiting for non-daemon threads to finish.
 *
 * @author ben
 * @since 5.4.2
 *
 */
public class DaemonThreadFactory implements ThreadFactory {

    private static final AtomicInteger count = new AtomicInteger(0);

    private String basename;

    private boolean isDaemon;

    /**
     * @param basemane String to use in thread name
     */
    public DaemonThreadFactory(String basename) {
        this(basename, true);
    }

    /**
     * @param basemane String to use in thread name
     * @param isDaemon Will created threads be set as daemon ?
     */
    public DaemonThreadFactory(String basename, boolean isDaemon) {
        this.basename = basename;
        this.isDaemon = isDaemon;
    }

    /**
     * New daemon thread.
     */
    public Thread newThread(final Runnable runnable) {
        final Thread thread = new Thread(runnable, basename + "-"
                + count.getAndIncrement());
        thread.setDaemon(isDaemon);
        return thread;
    }

}
