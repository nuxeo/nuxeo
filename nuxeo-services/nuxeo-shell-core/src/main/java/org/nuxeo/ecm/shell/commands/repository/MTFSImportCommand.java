/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.shell.commands.repository;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.shell.CommandLine;

public class MTFSImportCommand extends AbstractCommand {

    static ThreadPoolExecutor importTP;

    static Map<String, Long> nbCreatedDocsByThreads = new ConcurrentHashMap<String, Long>();

    private static final Log log = LogFactory.getLog(MTFSImportCommand.class);

    public static ThreadPoolExecutor getExecutor() {
        return importTP;
    }

    public static synchronized void addCreatedDoc(String taskId, long nbDocs) {
        String tid = Thread.currentThread().getName();
        nbCreatedDocsByThreads.put(tid + "-" + taskId, nbDocs);
    }

    public static synchronized long getCreatedDocsCounter() {
        long counter = 0;
        for (String tid : nbCreatedDocsByThreads.keySet()) {
            Long tCounter = nbCreatedDocsByThreads.get(tid);
            if (tCounter != null) {
                counter += tCounter;
            }
        }
        return counter;
    }

    private void printHelp() {
        System.out.println("");
        System.out.println("Syntax: mtfsimport local_file_path [remote_path] [batch_size] [nbThreads]");
    }

    @Override
    public void run(CommandLine cmdLine) throws Exception {
        nbCreatedDocsByThreads = new ConcurrentHashMap<String, Long>();
        String[] elements = cmdLine.getParameters();
        DocumentModel parent;
        if (elements.length == 0) {
            log.error("SYNTAX ERROR: the mtfsimport command must take at least one argument: "
                    + "fsimport local_file_path [remote_path] [batch_size] [nbThreads] ");
            printHelp();
            return;
        }

        File localFile = new File(elements[0]);
        if ("help".equals(elements[0])) {
            printHelp();
            return;
        }

        if (elements.length >= 2) {
            Path path = new Path(elements[1]);
            try {
                parent = context.fetchDocument(path);
            } catch (Exception e) {
                log.error("Failed to retrieve the given folder", e);
                return;
            }
        } else {
            parent = context.fetchDocument();
        }

        Integer batchSize = 50;
        if (elements.length >= 3) {
            try {
                batchSize = Integer.parseInt(elements[2]);
            } catch (Throwable t) {
                batchSize = 10;
                log.error(
                        "Failed to parse batch size, using default batchSize="
                                + batchSize, t);
            }
        }

        Integer nbThreads = 5;
        if (elements.length >= 4) {
            try {
                nbThreads = Integer.parseInt(elements[3]);
            } catch (Throwable t) {
                log.error("Failed to parse nbThreads, using default", t);
            }
        }
        importTP = new ThreadPoolExecutor(nbThreads, nbThreads, 500L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(100));

        ThreadedImportTask rootImportTask = new ThreadedImportTask(localFile,
                parent);
        rootImportTask.setRootTask();
        long t0 = System.currentTimeMillis();
        importTP.execute(rootImportTask);
        Thread.sleep(200);
        int activeTasks = importTP.getActiveCount();
        int oldActiveTasks = 0;
        while (activeTasks > 0) {
            Thread.sleep(200);
            activeTasks = importTP.getActiveCount();
            if (oldActiveTasks != activeTasks) {
                oldActiveTasks = activeTasks;
                log.info("currently " + activeTasks + " active import Threads");
                long inbCreatedDocs = getCreatedDocsCounter();
                log.info(inbCreatedDocs + " docs created");
                long ti = System.currentTimeMillis();
                log.info(1000 * ((float) (inbCreatedDocs) / (ti - t0))
                        + " docs/s");
            }
        }
        log.info("All Threads terminated");
        long t1 = System.currentTimeMillis();
        long nbCreatedDocs = getCreatedDocsCounter();
        log.info(nbCreatedDocs + " docs created");
        log.info(1000 * ((float) (nbCreatedDocs) / (t1 - t0)) + " docs/s");
        for (String k : nbCreatedDocsByThreads.keySet()) {
            log.debug(k + " --> " + nbCreatedDocsByThreads.get(k));
        }
    }

}
