/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.apache.http.entity.ContentType;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.restlet.Request;
import org.restlet.engine.adapter.HttpRequest;
import org.restlet.engine.adapter.ServerCall;
import org.restlet.ext.servlet.internal.ServletCall;

/**
 * Helper to encapsulate Multipart requests parsing to extract blobs.
 * <p>
 * This helper is needed to provide the indirection between - the Apache file upload based solution (5.1) and - the Seam
 * MultiPartFilter bases solution (5.1 / Seam 2.x).
 *
 * @author tiry
 */
public class FileUploadHelper {

    private FileUploadHelper() {
    }

    /**
     * Parses a Multipart Restlet Request to extract blobs.
     */
    public static List<Blob> parseRequest(Request request) throws FileUploadException, IOException {
        if (request instanceof HttpRequest) {
            HttpRequest httpRequest = (HttpRequest) request;
            ServerCall httpCall = httpRequest.getHttpCall();
            if (httpCall instanceof ServletCall) {
                HttpServletRequest httpServletRequest = ((ServletCall) httpCall).getRequest();
                return parseRequest(httpServletRequest);
            }
        }
        return null;
    }

    public static List<Blob> parseRequest(HttpServletRequest request) throws FileUploadException, IOException {
        if (!isMultipartRequest(request)) {
            try (InputStream in = request.getInputStream()) {
                Blob blob = createBlob(in, request.getContentType());
                return Collections.singletonList(blob);
            }
        } else {
            FileUpload fileUpload = new FileUpload(new DiskFileItemFactory());
            String fileNameCharset = request.getHeader("FileNameCharset"); // compat with old code
            if (fileNameCharset != null) {
                fileUpload.setHeaderEncoding(fileNameCharset);
            }
            List<Blob> blobs = new ArrayList<>();
            for (FileItem item : fileUpload.parseRequest(new ServletRequestContext(request))) {
                try (InputStream is = item.getInputStream()) {
                    Blob blob = createBlob(is, item.getContentType());
                    blob.setFilename(item.getName());
                    blobs.add(blob);
                }
            }
            return blobs;
        }
    }

    public static boolean isMultipartRequest(Request request) {
        HttpServletRequest httpServletRequest = ((ServletCall) ((HttpRequest) request).getHttpCall()).getRequest();
        return isMultipartRequest(httpServletRequest);
    }

    public static boolean isMultipartRequest(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String contentType = request.getContentType();
        return contentType == null ? false : contentType.toLowerCase().startsWith("multipart/");
    }

    public static Blob createBlob(InputStream in, String contentType) throws IOException {
        ContentType ct = ContentType.parse(contentType);
        String mimeType = ct.getMimeType();
        Charset charset = ct.getCharset();
        String encoding = charset == null ? null : charset.name();
        return Blobs.createBlob(in, mimeType, encoding);
    }

}
