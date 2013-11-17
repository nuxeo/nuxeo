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

    protected DefaultImporterService importerService;

    @Override
    protected Log getJavaLogger() {
        return log;
    }




    @GET
    @Path("run")
    @Produces("text/plain; charset=UTF-8")
    public String run(
            @QueryParam("leafType") String leafType,
            @QueryParam("folderishType") String folderishType,
            @QueryParam("inputPath") String inputPath,
            @QueryParam("targetPath") String targetPath,
            @QueryParam("skipRootContainerCreation") Boolean skipRootContainerCreation,
            @QueryParam("batchSize") Integer batchSize,
            @QueryParam("nbThreads") Integer nbThreads,
            @QueryParam("interactive") Boolean interactive) throws Exception {

        if (inputPath == null || targetPath == null) {
            return "Can not import, missing "
                    + (inputPath == null ? "inputPath" : "targetPath");
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

        if (leafType != null || folderishType != null) {
            log.info("Importing with the specified doc types");
            return getImporterService().importDocuments(this, leafType,
                    folderishType, targetPath, inputPath,
                    skipRootContainerCreation, batchSize, nbThreads,
                    interactive);
        } else {
            log.info("Importing with the deafult doc types");
            return getImporterService().importDocuments(this, targetPath,
                    inputPath, skipRootContainerCreation, batchSize, nbThreads,
                    interactive);
        }

    }

    @Override
    public String run(ImporterRunner runner, Boolean interactive)
            throws Exception {
        return doRun(runner, interactive);
    }

    protected DefaultImporterService getImporterService() throws Exception {
        if (importerService == null) {
            importerService = Framework.getService(DefaultImporterService.class);
        }
        return importerService;
    }
}
