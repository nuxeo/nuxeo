/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntryList;

/**
 * @since 5.7.3 - LogEntries Writer for Audit
 */
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
    protected void writeItem(JsonGenerator jg, LogEntry item) throws ClientException, IOException {
        LogEntryWriter ngw = new LogEntryWriter();
        ngw.writeEntity(jg, item);
    }

}
