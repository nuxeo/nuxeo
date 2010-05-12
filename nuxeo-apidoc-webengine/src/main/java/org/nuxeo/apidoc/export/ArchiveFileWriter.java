/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.apidoc.export;

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

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.webengine.WebException;

@Provider
@Produces({"*/*", "text/plain"})
public class ArchiveFileWriter implements MessageBodyWriter<ArchiveFile> {

     public void writeTo(ArchiveFile t, Class<?> type, Type genericType,
                Annotation[] annotations, MediaType mediaType,
                MultivaluedMap<String, Object> httpHeaders,
                OutputStream entityStream) throws IOException {
            FileInputStream in = null;
            try {
                in = new FileInputStream(t);
                FileUtils.copy(in, entityStream);
                entityStream.flush();
            } catch (Throwable e) {
                throw WebException.wrap("Failed to render resource", e);
            } finally {
                if (in != null) in.close();
                if (t!=null) {
                    t.delete();
                }
            }
        }

        public long getSize(ArchiveFile arg0, Class<?> arg1, Type arg2,
                Annotation[] arg3, MediaType arg4) {
            long n = arg0.length();
            return n <= 0 ? -1 : n;
        }

        public boolean isWriteable(Class<?> arg0, Type type, Annotation[] arg2,
                MediaType arg3) {
            return ArchiveFile.class.isAssignableFrom(arg0);
        }


}
