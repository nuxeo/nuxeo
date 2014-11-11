/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Julien Carsique
 *
 */

package org.nuxeo.launcher.gui.logs;

/**
 * Thread wrapping {@link LogsSource}
 *
 * @author jcarsique
 *
 */
public class LogsSourceThread extends Thread {
    private LogsSource source;

    public LogsSource getSource() {
        return source;
    }

    /**
     * @param logsSource {@link LogsSource} wrapped by this class
     */
    public LogsSourceThread(LogsSource logsSource) {
        super(logsSource);
        this.source = logsSource;
    }

}
