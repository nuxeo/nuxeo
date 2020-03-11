/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.targetplatforms.jaxrs;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.nuxeo.targetplatforms.api.TargetPackage;
import org.nuxeo.targetplatforms.api.TargetPackageInfo;
import org.nuxeo.targetplatforms.api.TargetPlatform;
import org.nuxeo.targetplatforms.api.TargetPlatformInfo;
import org.nuxeo.targetplatforms.api.TargetPlatformInstance;
import org.nuxeo.targetplatforms.io.JSONExporter;

/**
 * @since 5.9.3
 */
@Provider
@Produces({ MediaType.APPLICATION_JSON, "text/plain" })
public class JsonWriter implements MessageBodyWriter<Object> {

    @Context
    protected HttpServletRequest request;

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return TargetPackageInfo.class.isAssignableFrom(type) || TargetPackage.class.isAssignableFrom(type)
                || TargetPlatformInfo.class.isAssignableFrom(type)
                || TargetPlatformInstance.class.isAssignableFrom(type) || TargetPlatform.class.isAssignableFrom(type)
                || TargetPlatformsInfo.class.isAssignableFrom(type) || TargetPlatforms.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(Object t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(Object t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException,
            WebApplicationException {
        boolean pretty = Boolean.parseBoolean(request.getParameter("pretty"));
        if (TargetPackage.class.isAssignableFrom(type)) {
            JSONExporter.exportToJson((TargetPackage) t, entityStream, pretty);
        } else if (TargetPackageInfo.class.isAssignableFrom(type)) {
            JSONExporter.exportToJson((TargetPackageInfo) t, entityStream, pretty);
        } else if (TargetPlatform.class.isAssignableFrom(type)) {
            JSONExporter.exportToJson((TargetPlatform) t, entityStream, pretty);
        } else if (TargetPlatformInstance.class.isAssignableFrom(type)) {
            JSONExporter.exportToJson((TargetPlatformInstance) t, entityStream, pretty);
        } else if (TargetPlatformInfo.class.isAssignableFrom(type)) {
            JSONExporter.exportToJson((TargetPlatformInfo) t, entityStream, pretty);
        } else if (TargetPlatforms.class.isAssignableFrom(type)) {
            JSONExporter.exportToJson((TargetPlatforms) t, entityStream, pretty);
        } else if (TargetPlatformsInfo.class.isAssignableFrom(type)) {
            JSONExporter.exportInfosToJson((TargetPlatformsInfo) t, entityStream, pretty);
        } else {
            throw new IllegalArgumentException(String.format("Unsupported type '%s'", type));
        }
    }
}
