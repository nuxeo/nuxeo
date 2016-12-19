/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.jaxrs.io.audit;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.automation.jaxrs.io.EntityListWriter;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntryList;
import org.nuxeo.ecm.webengine.jaxrs.coreiodelegate.JsonCoreIODelegate;

/**
 * @since 5.7.3 - LogEntries Writer for Audit
 * @deprecated since 7.2 this marshaller was migrated to org.nuxeo.ecm.platform.audit.io.LogEntryListJsonWriter. To use
 *             it in JAX-RS, register the {@link JsonCoreIODelegate} to forward the JAX-RS marshalling to nuxeo-core-io.
 */
@Deprecated
@Provider
@Produces({ "application/json+nxentity", "application/json" })
public class LogEntryListWriter extends EntityListWriter<LogEntry> {

    @Override
    protected String getEntityType() {
        return "logEntries";
    }

    @Override
    public boolean isWriteable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
        if (LogEntryList.class.isAssignableFrom(arg0)) {
            return true;
        } else {
            return super.isWriteable(arg0, arg1, arg2, arg3);
        }
    }

    @Override
    protected void writeItem(JsonGenerator jg, LogEntry item) throws IOException {
        LogEntryWriter ngw = new LogEntryWriter();
        ngw.writeEntity(jg, item);
    }

}
