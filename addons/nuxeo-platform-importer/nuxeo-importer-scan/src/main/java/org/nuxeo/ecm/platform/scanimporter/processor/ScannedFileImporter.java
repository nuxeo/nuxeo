/*
 * (C) Copyright 2006-2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.scanimporter.processor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.importer.base.GenericMultiThreadedImporter;
import org.nuxeo.ecm.platform.importer.log.BasicLogger;
import org.nuxeo.ecm.platform.scanimporter.service.ImporterConfig;
import org.nuxeo.ecm.platform.scanimporter.service.ScannedFileMapperService;
import org.nuxeo.runtime.api.Framework;

/**
 *
 * Setup the importer with the rights factories
 *
 * @author Thierry Delprat
 *
 */
public class ScannedFileImporter {

    private static final Log log = LogFactory.getLog(ScannedFileImporter.class);

    protected static List<String> processedDescriptors;

    protected static ReentrantReadWriteLock processedLock = new ReentrantReadWriteLock();

    public ScannedFileImporter() {
        processedDescriptors = new ArrayList<String>();
    }

    public static void addProcessedDescriptor(String fileDesc) {
        processedLock.writeLock().lock();
        try {
            processedDescriptors.add(fileDesc);
            if (processedDescriptors.size() % 100 == 0) {
                doCleanUp();
            }
        } finally {
            processedLock.writeLock().unlock();
        }
    }

    protected static void doCleanUp() {

        ScannedFileMapperService sfms = Framework.getLocalService(ScannedFileMapperService.class);
        ImporterConfig config = sfms.getImporterConfig();
        File outDir = null;

        if (config != null) {
            String outPath = config.getProcessedPath();
            if (outPath != null) {
                outDir = new File(outPath);
                if (!outDir.exists()) {
                    outDir = null;
                }
            }
        }
        for (String fileDesc : processedDescriptors) {
            File file = new File(fileDesc);
            if (file.exists()) {
                if (outDir == null) {
                    file.delete();
                } else {
                    file.renameTo(new File(outDir, file.getName()));
                }
            }
        }
        processedDescriptors = new ArrayList<String>();
    }

    public void doImport() throws Exception {

        ScannedFileMapperService sfms = Framework.getLocalService(ScannedFileMapperService.class);

        ImporterConfig config = sfms.getImporterConfig();
        if (config == null) {
            log.error("No configuration can be found, exit importer");
            return;
        }
        File folder = new File(config.getSourcePath());

        doImport(folder, config);
    }

    public void doImport(File folder, ImporterConfig config) throws Exception {

        if (folder == null || !folder.exists()) {
            throw new ClientException("Unable to access source folder "
                    + folder);
        }
        if (config.getTargetPath() == null) {
            throw new ClientException("target path must be set");
        }

        if (folder.listFiles().length == 0) {
            log.info("Nothing to import exiting");
            return;
        }

        log.info("Starting import process on path " + config.getTargetPath()
                + " from source " + folder.getAbsolutePath());
        ScanedFileSourceNode src = new ScanedFileSourceNode(folder);
        src.useXMLMapping = config.useXMLMapping();
        GenericMultiThreadedImporter importer = new GenericMultiThreadedImporter(
                src, config.getTargetPath(), config.getBatchSize(),
                config.getNbThreads(), new BasicLogger(log));
        importer.setFactory(new ScanedFileFactory(config));
        importer.run();

        log.info("Fininish moving files");
        doCleanUp();

        log.info("Ending import process");
    }

}
