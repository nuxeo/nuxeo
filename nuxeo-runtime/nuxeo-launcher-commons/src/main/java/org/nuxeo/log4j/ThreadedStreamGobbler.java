/*
 * (C) Copyright 2011-2012 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.log4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.SimpleLog;

/**
 * Helper class for logging process output
 *
 * @since 5.4.1
 */
public class ThreadedStreamGobbler extends Thread {
    static final Log log = LogFactory.getLog(ThreadedStreamGobbler.class);

    private static final String DEFAULT_PREFIX = "Nuxeo-stream-gobbler-";

    private static AtomicInteger threadNumber = new AtomicInteger();

    private InputStream is;

    private int logLevel;

    private List<String> output;

    private OutputStream outputStream;

    public ThreadedStreamGobbler(String prefix, InputStream is, int logLevel) {
        this.is = is;
        this.logLevel = logLevel;
        this.setDaemon(true);
        setName(prefix + threadNumber.incrementAndGet());
    }

    public ThreadedStreamGobbler(InputStream is, int logLevel) {
        this(DEFAULT_PREFIX, is, logLevel);
    }

    /**
     * @param inputStream InputStream to read
     * @param output List to store output instead of logging it.
     * @since 5.5
     */
    public ThreadedStreamGobbler(InputStream inputStream, List<String> output) {
        this(inputStream, SimpleLog.LOG_LEVEL_OFF);
        this.output = output;
    }

    /**
     * @param inputStream InputStream to read
     * @param output OutputStream where to write.
     * @since 5.5
     */
    public ThreadedStreamGobbler(InputStream inputStream, OutputStream output) {
        this(inputStream, SimpleLog.LOG_LEVEL_OFF);
        this.outputStream = output;
    }

    @Override
    public void run() {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        final byte[] newLine = "\n".getBytes();

        try {
            while ((line = br.readLine()) != null) {
                switch (logLevel) {
                case SimpleLog.LOG_LEVEL_DEBUG:
                    log.debug(line);
                    break;
                case SimpleLog.LOG_LEVEL_INFO:
                    log.info(line);
                    break;
                case SimpleLog.LOG_LEVEL_ERROR:
                    log.error(line);
                    break;
                case SimpleLog.LOG_LEVEL_OFF:
                    if (output != null) {
                        output.add(line);
                    }
                    if (outputStream != null) {
                        outputStream.write(line.getBytes());
                        outputStream.write(newLine);
                    }
                    break;
                default:
                }
            }
        } catch (IOException e) {
            log.error(e);
        } finally {
            IOUtils.closeQuietly(br);
        }
    }
}
