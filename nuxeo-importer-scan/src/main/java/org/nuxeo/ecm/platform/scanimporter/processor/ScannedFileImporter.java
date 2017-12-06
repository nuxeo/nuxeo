/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.ecm.platform.scanimporter.processor;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.importer.base.GenericMultiThreadedImporter;
import org.nuxeo.ecm.platform.importer.factories.ImporterDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.log.BasicLogger;
import org.nuxeo.ecm.platform.importer.service.DefaultImporterService;
import org.nuxeo.ecm.platform.importer.source.FileSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.ecm.platform.scanimporter.service.ImporterConfig;
import org.nuxeo.ecm.platform.scanimporter.service.ScannedFileMapperService;
import org.nuxeo.runtime.api.Framework;

/**
 * Setup the importer with the rights factories
 *
 * @author Thierry Delprat
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

        ScannedFileMapperService sfms = Framework.getService(ScannedFileMapperService.class);
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
                    Path source = file.toPath();
                    Path target = outDir.toPath().resolve(file.getName());
                    try {
                        Files.move(source, target);
                    } catch (IOException e) {
                        log.error("An exception occured while moving " + source.getFileName(), e);
                    }
                }
            }
        }
        processedDescriptors = new ArrayList<String>();
    }

    public void doImport() {

        ScannedFileMapperService sfms = Framework.getService(ScannedFileMapperService.class);

        ImporterConfig config = sfms.getImporterConfig();
        if (config == null) {
            log.error("No configuration can be found, exit importer");
            return;
        }
        File folder = new File(config.getSourcePath());

        doImport(folder, config);
    }

    public void doImport(File folder, ImporterConfig config) {

        if (folder == null || !folder.exists()) {
            throw new NuxeoException("Unable to access source folder " + folder);
        }
        if (config.getTargetPath() == null) {
            throw new NuxeoException("target path must be set");
        }

        if (folder.listFiles().length == 0) {
            log.info("Nothing to import exiting");
            return;
        }

        log.info("Starting import process on path " + config.getTargetPath() + " from source "
                + folder.getAbsolutePath());
        SourceNode src = initSourceNode(folder);

        ScanedFileSourceNode.useXMLMapping = config.useXMLMapping();
        GenericMultiThreadedImporter importer = new GenericMultiThreadedImporter(src, config.getTargetPath(),
                !config.isCreateInitialFolder(), config.getBatchSize(), config.getNbThreads(), new BasicLogger(log));

        ImporterDocumentModelFactory factory = initDocumentModelFactory(config);
        importer.setEnablePerfLogging(Framework.getService(
                DefaultImporterService.class).getEnablePerfLogging());
        importer.setFactory(factory);
        importer.setTransactionTimeout(config.getTransactionTimeout());
        importer.run();

        log.info("Fininish moving files");
        doCleanUp();

        log.info("Ending import process");
    }

    /**
     * @since 5.7.3
     */
    private ImporterDocumentModelFactory initDocumentModelFactory(ImporterConfig config) {
        Class<? extends ImporterDocumentModelFactory> factoryClass = Framework.getService(
                DefaultImporterService.class).getDocModelFactoryClass();
        // Class<? extends DefaultDocumentModelFactory> factoryClass = ScanedFileFactory.class;
        Constructor<? extends ImporterDocumentModelFactory> cst = null;

        try {
            try {
                cst = factoryClass.getConstructor(ImporterConfig.class);
                return cst.newInstance(config);
            } catch (NoSuchMethodException e) {
                return factoryClass.newInstance();
            }
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException(e);
        }
    }

    /**
     * @throws Exception
     * @since 5.7.3
     */
    private SourceNode initSourceNode(File file) {
        Class<? extends SourceNode> srcClass = Framework.getService(DefaultImporterService.class).getSourceNodeClass();
        // Class<? extends SourceNode> srcClass = ScanedFileSourceNode.class;
        if (!FileSourceNode.class.isAssignableFrom(srcClass)) {
            throw new NuxeoException("Waiting source node extending FileSourceNode for Scan Importer");
        }
        try {
            return srcClass.getConstructor(File.class).newInstance(file);
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException(e);
        }
    }

}
