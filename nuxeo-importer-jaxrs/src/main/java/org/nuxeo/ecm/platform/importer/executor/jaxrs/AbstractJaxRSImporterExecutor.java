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

import org.apache.commons.io.IOUtils;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.importer.executor.AbstractImporterExecutor;
import org.nuxeo.ecm.platform.importer.log.BufferredLogger;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Produces("text/plain; charset=UTF-8")
public abstract class AbstractJaxRSImporterExecutor extends AbstractImporterExecutor {

    @Override
    public ImporterLogger getLogger() {
        if (log == null) {
            log = new BufferredLogger(getJavaLogger());
        }
        return log;
    }

    @GET
    @Produces("text/html; charset=UTF-8")
    public String index() throws IOException {
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

    @GET
    @Path("waitForAsyncJobs")
    public Response waitForAsyncJobs(@QueryParam("timeoutInSeconds") Integer timeoutInSeconds) {
        // do not maintain a tx for this
        TransactionHelper.commitOrRollbackTransaction();
        WorkManager workManager = Framework.getService(WorkManager.class);
        if (timeoutInSeconds == null) {
            timeoutInSeconds = 120;
        }
        try {
            if (workManager.awaitCompletion(timeoutInSeconds, TimeUnit.SECONDS)) {
                return Response.ok().build();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NuxeoException(e);
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Timeout").build();
    }

}
