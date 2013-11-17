package org.nuxeo.ecm.platform.importer.executor.jaxrs;

import java.io.InputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.commons.io.IOUtils;
import org.nuxeo.ecm.platform.importer.executor.AbstractImporterExecutor;
import org.nuxeo.ecm.platform.importer.log.BufferredLogger;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;

@Produces("text/plain; charset=UTF-8")
public abstract class AbstractJaxRSImporterExecutor extends
        AbstractImporterExecutor {

    @Override
    public ImporterLogger getLogger() {
        if (log == null) {
            log = new BufferredLogger(getJavaLogger());
        }
        return log;
    }

    @GET
    @Produces("text/html; charset=UTF-8")
    public String index() throws Exception {
        try (InputStream stream = this.getClass().getResource("/static/importForm.html").openStream()) {
            return IOUtils.toString(stream, "UTF-8");
        }
    }

    @GET
    @Path("log")
    public String getLogAsString() {
        return getLogger().getLoggerBuffer();
    }

    @GET
    @Path("logActivate")
    public String enableLogging() {
        getLogger().setBufferActive(true);
        return "Logging activated";
    }

    @GET
    @Path("logDesactivate")
    public String disableLogging() {
        getLogger().setBufferActive(false);
        return "Logging desactivated";
    }

    @GET
    @Path("status")
    public String getStatus() {
        return super.getStatus();
    }

    @GET
    @Path("running")
    public String running() {
        return Boolean.toString(super.isRunning());
    }

    @GET
    @Path("kill")
    public String kill() {
        return super.kill();
    }

}
