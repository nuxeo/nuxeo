/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 */
package org.nuxeo.importer.stream.automation;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.nuxeo.importer.stream.StreamImporters.DEFAULT_LOG_BLOB_NAME;
import static org.nuxeo.importer.stream.StreamImporters.DEFAULT_LOG_CONFIG;

import java.io.File;
import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.importer.stream.StreamImporters;
import org.nuxeo.importer.stream.message.BlobMessage;
import org.nuxeo.importer.stream.producer.FileBlobMessageProducerFactory;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.lib.stream.pattern.producer.ProducerPool;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamService;

/**
 * Create blob messages using a list of files.
 *
 * @since 10.2
 */
@Operation(id = FileBlobProducers.ID, category = Constants.CAT_SERVICES, label = "Produces blobs from a list of files", since = "10.2", description = "Produces blobs from a list of files.")
public class FileBlobProducers {
    private static final Log log = LogFactory.getLog(FileBlobProducers.class);

    public static final String ID = "StreamImporter.runFileBlobProducers";

    @Context
    protected OperationContext ctx;

    @Param(name = "listFile")
    protected String listFile;

    @Param(name = "nbBlobs", required = false)
    protected Integer nbBlobs;

    @Param(name = "nbThreads", required = false)
    protected Integer nbThreads = 1;

    @Param(name = "logName", required = false)
    protected String logName = DEFAULT_LOG_BLOB_NAME;

    @Param(name = "logSize", required = false)
    protected Integer logSize;

    @Param(name = "basePath", required = false)
    protected String basePath;

    @OperationMethod
    public void run() throws OperationException {
        checkAccess(ctx);
        LogManager manager = Framework.getService(StreamService.class).getLogManager();
        manager.createIfNotExists(Name.ofUrn(logName), getLogSize());
        Codec<BlobMessage> codec = StreamImporters.getBlobCodec();
        try (ProducerPool<BlobMessage> producers = new ProducerPool<>(logName, manager, codec,
                new FileBlobMessageProducerFactory(getListFile(), getBasePath(), getNbBlobs()),
                nbThreads.shortValue())) {
            producers.start().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Operation interrupted");
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            log.error("Operation fails", e);
            throw new OperationException(e);
        }
    }

    protected long getNbBlobs() {
        if (nbBlobs == null) {
            // this means all the files listed in the files
            return 0;
        }
        return nbBlobs.longValue();
    }

    protected int getLogSize() {
        if (logSize != null && logSize > 0) {
            return logSize;
        }
        return nbThreads;
    }

    protected String getBasePath() {
        if (isEmpty(basePath)) {
            return null;
        }
        if (!new File(basePath).exists()) {
            throw new IllegalArgumentException("Can not access basePath: " + basePath);
        }
        return basePath;
    }

    protected File getListFile() {
        File ret = new File(listFile);
        if (!ret.exists() || !ret.canRead()) {
            throw new IllegalArgumentException("Can not access or read listFile: " + listFile);
        }
        return ret;
    }

    protected static void checkAccess(OperationContext context) {
        NuxeoPrincipal principal = context.getPrincipal();
        if (principal == null || !principal.isAdministrator()) {
            throw new RuntimeException("Unauthorized access: " + principal);
        }
    }
}
