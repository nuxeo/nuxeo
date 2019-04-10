package org.nuxeo.ecm.platform.importer.executor.jaxrs;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.platform.importer.executor.AbstractImporterExecutor;
import org.nuxeo.ecm.platform.importer.log.BufferredLogger;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;

@Produces("text/plain; charset=UTF-8")
public abstract class AbstractJaxRSImporterExecutor extends
        AbstractImporterExecutor {

    @Override
    protected ImporterLogger getLogger() {
        if (log == null) {
            log = new BufferredLogger(getJavaLogger());
        }
        return log;
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
    @Path("isRunning")
    public Boolean isRunning() {
        return super.isRunning();
    }

    @GET
    @Path("kill")
    public void kill() {
        super.kill();
    }

}
