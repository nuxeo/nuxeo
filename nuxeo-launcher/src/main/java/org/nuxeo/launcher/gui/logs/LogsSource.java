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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Observable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author jcarsique
 * @since 5.4.2
 */
public class LogsSource extends Observable implements Runnable {
    static final Log log = LogFactory.getLog(LogsSource.class);

    private static final long WAIT_FOR_FILE_EXISTS = 2000;

    private static final long WAIT_FOR_READING_CONTENT = 1000;

    private File logFile;

    private boolean pause = true;

    private long charsToSkip = 0;

    /**
     * @param logFile Log file to manage
     */
    public LogsSource(File logFile) {
        this.logFile = logFile;
    }

    @Override
    public void run() {
        BufferedReader in = null;
        try {
            while (!logFile.exists()) {
                Thread.sleep(WAIT_FOR_FILE_EXISTS);
            }
            in = new BufferedReader(new FileReader(logFile));
            // Avoid reading and formating chars which won't be displayed
            if (charsToSkip > 0) {
                in.skip(charsToSkip);
            }
            // marker for detecting log rotate
            long lastModified = logFile.lastModified();
            while (true) {
                if (pause) {
                    synchronized (this) {
                        wait();
                    }
                }
                String line = in.readLine();
                if (line != null) {
                    lastModified = logFile.lastModified();
                    setChanged();
                    notifyObservers(line);
                } else {
                    if (logFile.lastModified() > lastModified) {
                        log.debug("File rotation detected");
                        IOUtils.closeQuietly(in);
                        in = new BufferedReader(new FileReader(logFile));
                    } else {
                        synchronized (this) {
                            wait(WAIT_FOR_READING_CONTENT);
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            log.error(e);
        } catch (IOException e) {
            log.error(e);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * Ask thread to pause until {@link #resume()}
     */
    public synchronized void pause() {
        this.pause = true;
    }

    /**
     * Resume thread after call to {@link #pause()}
     */
    public void resume() {
        this.pause = false;
        synchronized (this) {
            notify();
        }
    }

    /**
     * @param i
     */
    public void skip(long length) {
        charsToSkip = length;
    }

}
