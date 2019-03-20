/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.platform.importer.executor.jaxrs;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.importer.base.ImporterRunner;
import org.nuxeo.ecm.platform.importer.service.DefaultImporterService;
import org.nuxeo.runtime.api.Framework;

@Path("fileImporter")
public class HttpFileImporterExecutor extends AbstractJaxRSImporterExecutor {

    private static final Log log = LogFactory.getLog(HttpFileImporterExecutor.class);

    @Override
    protected Log getJavaLogger() {
        return log;
    }

    @GET
    @Path("run")
    @Produces("text/plain; charset=UTF-8")
    public String run(@QueryParam("leafType") String leafType, @QueryParam("folderishType") String folderishType,
            @QueryParam("inputPath") String inputPath, @QueryParam("targetPath") String targetPath,
            @QueryParam("skipRootContainerCreation") Boolean skipRootContainerCreation,
            @QueryParam("batchSize") Integer batchSize, @QueryParam("nbThreads") Integer nbThreads,
            @QueryParam("interactive") Boolean interactive,
            @QueryParam("transactionTimeout") Integer transactionTimeout) {

        if (inputPath == null || targetPath == null) {
            return "Can not import, missing " + (inputPath == null ? "inputPath" : "targetPath");
        }
        if (skipRootContainerCreation == null) {
            skipRootContainerCreation = false;
        }
        if (batchSize == null) {
            batchSize = 5;
        }
        if (nbThreads == null) {
            nbThreads = 5;
        }
        if (interactive == null) {
            interactive = false;
        }
        if (transactionTimeout == null) {
            transactionTimeout = 0;
        }

        DefaultImporterService defaultImporterService = Framework.getService(DefaultImporterService.class);

        defaultImporterService.setTransactionTimeout(transactionTimeout);

        if (leafType != null || folderishType != null) {
            log.info("Importing with the specified doc types");
            return defaultImporterService.importDocuments(this, leafType, folderishType, targetPath, inputPath,
                    skipRootContainerCreation, batchSize, nbThreads, interactive);
        } else {
            log.info("Importing with the deafult doc types");
            return defaultImporterService.importDocuments(this, targetPath, inputPath, skipRootContainerCreation,
                    batchSize, nbThreads, interactive);
        }

    }

    @Override
    public String run(ImporterRunner runner, Boolean interactive) {
        return doRun(runner, interactive);
    }

}
