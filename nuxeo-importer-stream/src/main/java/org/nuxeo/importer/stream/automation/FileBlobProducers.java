/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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

import static org.nuxeo.importer.stream.automation.BlobConsumers.DEFAULT_LOG_CONFIG;

import java.io.File;
import java.nio.file.Paths;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.importer.stream.message.BlobMessage;
import org.nuxeo.importer.stream.producer.FileBlobMessageProducerFactory;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.pattern.producer.ProducerPool;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamService;

/**
 * @since 9.1
 */
@Operation(id = FileBlobProducers.ID, category = Constants.CAT_SERVICES, label = "Produces random blobs", since = "9.1", description = "Produces random blobs in a Log.")
public class FileBlobProducers {
    private static final Log log = LogFactory.getLog(FileBlobProducers.class);

    public static final String ID = "StreamImporter.runFileBlobProducers";

    public static final String DEFAULT_BLOB_LOG_NAME = "import-blob";

    @Context
    protected OperationContext ctx;

    @Param(name = "nbBlobs")
    protected Integer nbBlobs;

    @Param(name = "nbThreads", required = false)
    protected Integer nbThreads = 8;

    @Param(name = "logName", required = false)
    protected String logName;

    @Param(name = "logSize", required = false)
    protected Integer logSize;

    @Param(name = "logConfig", required = false)
    protected String logConfig;

    @Param(name = "listFile", required = true)
    protected String listFile;

    @Param(name = "basePath", required = false)
    protected String basePath;

    @OperationMethod
    public void run() {
        checkAccess(ctx);
        StreamService service = Framework.getService(StreamService.class);
        LogManager manager = service.getLogManager(getLogConfig());
        try {
            manager.createIfNotExists(getLogName(), getLogSize());
            try (ProducerPool<BlobMessage> producers = new ProducerPool<>(getLogName(), manager,
                    new FileBlobMessageProducerFactory(getListFile(), getBasePath(), nbBlobs), nbThreads.shortValue())) {
                producers.start().get();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    protected int getLogSize() {
        if (logSize != null && logSize > 0) {
            return logSize;
        }
        return nbThreads;
    }

    protected String getBasePath() {
        if (basePath == null || basePath.isEmpty()) {
            return null;
        }
        if (! new File(basePath).exists()) {
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

    protected String getLogConfig() {
        if (logConfig != null) {
            return logConfig;
        }
        return DEFAULT_LOG_CONFIG;
    }

    protected String getLogName() {
        if (logName != null) {
            return logName;
        }
        return DEFAULT_BLOB_LOG_NAME;
    }

    protected static void checkAccess(OperationContext context) {
        NuxeoPrincipal principal = (NuxeoPrincipal) context.getPrincipal();
        if (principal == null || !principal.isAdministrator()) {
            throw new RuntimeException("Unauthorized access: " + principal);
        }
    }
}
