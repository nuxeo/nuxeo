/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.model.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.io.download.DownloadHelper;
import org.nuxeo.ecm.webengine.WebException;

/**
 * text/plain is needed otherwise resteasy will use its default text plain (@see DefaultTextPlain) writer to write
 * text/plain objects and the file is not written correctly.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Provider
@Produces({ "*/*", "text/plain" })
public class FileWriter implements MessageBodyWriter<File> {

    private static final Log log = LogFactory.getLog(FileWriter.class);

    public void writeTo(File t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException {
        try (FileInputStream in = new FileInputStream(t)) {
            IOUtils.copy(in, entityStream);
            entityStream.flush();
        } catch (RuntimeException | IOException e) {
            if (DownloadHelper.isClientAbortError(e)) {
                DownloadHelper.logClientAbort(e);
            } else {
                throw WebException.wrap("Failed to render resource", e);
            }
        }
    }

    public long getSize(File arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
        long n = arg0.length();
        return n <= 0 ? -1 : n;
    }

    public boolean isWriteable(Class<?> arg0, Type type, Annotation[] arg2, MediaType arg3) {
        return File.class.isAssignableFrom(arg0);
    }

}
