/*
 * (C) Copyright 2011-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
            Thread.currentThread().interrupt();
            return;
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
        pause = true;
    }

    /**
     * Resume thread after call to {@link #pause()}
     */
    public void resume() {
        pause = false;
        synchronized (this) {
            notifyAll();
        }
    }

    /**
     * @param length
     */
    public void skip(long length) {
        charsToSkip = length;
    }

}
