/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.ExceptionHelper;
import org.nuxeo.ecm.webengine.WebException;

/**
 * text/plain is needed otherwise resteasy will use its default text plain
 * (@see DefaultTextPlain) writer to write text/plain objects and the file is
 * not written correctly.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Provider
@Produces( { "*/*", "text/plain" })
public class FileWriter implements MessageBodyWriter<File> {

    private static final Log log = LogFactory.getLog(FileWriter.class);

    public void writeTo(File t, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream) throws IOException {
        FileInputStream in = null;
        try {
            in = new FileInputStream(t);
            FileUtils.copy(in, entityStream);
            entityStream.flush();
        } catch (Throwable e) {
            Throwable unwrappedError = ExceptionHelper.unwrapException(e);
            if (ExceptionHelper.isClientAbortError(unwrappedError)) {
                // ignore but log as warn
                log.warn(unwrappedError.getMessage());
            } else if (unwrappedError instanceof IOException) {
                // can be a broken pipe => do not display the whole stack trace
                log.error(unwrappedError.getMessage());
            } else {
                throw WebException.wrap("Failed to render resource", e);
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    public long getSize(File arg0, Class<?> arg1, Type arg2, Annotation[] arg3,
            MediaType arg4) {
        long n = arg0.length();
        return n <= 0 ? -1 : n;
    }

    public boolean isWriteable(Class<?> arg0, Type type, Annotation[] arg2,
            MediaType arg3) {
        return File.class.isAssignableFrom(arg0);
    }

}
