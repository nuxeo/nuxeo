/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.automation.server.jaxrs.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.core.doc.bonita.BonitaExporter;
import org.nuxeo.ecm.automation.server.jaxrs.AutomationInfo;

/**
 * Produces a zip to be contributed to the Nuxeo Bonita connector.
 *
 * @since 5.4.1
 */
@Provider
@Produces( { "application/bonita+nxautomation", "application/zip" })
public class BonitaAutomationInfoWriter implements
        MessageBodyWriter<AutomationInfo> {

    @Override
    public long getSize(AutomationInfo arg0, Class<?> arg1, Type arg2,
            Annotation[] arg3, MediaType arg4) {
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> arg0, Type arg1, Annotation[] arg2,
            MediaType arg3) {
        return AutomationInfo.class.isAssignableFrom(arg0);
    }

    @Override
    public void writeTo(AutomationInfo arg0, Class<?> arg1, Type arg2,
            Annotation[] arg3, MediaType arg4,
            MultivaluedMap<String, Object> arg5, OutputStream arg6)
            throws IOException, WebApplicationException {
        try {
            InputStream res = BonitaExporter.toZip();
            FileUtils.copy(res, arg6);
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

}
