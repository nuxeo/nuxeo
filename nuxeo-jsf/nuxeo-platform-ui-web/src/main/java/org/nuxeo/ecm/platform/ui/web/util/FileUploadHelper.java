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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.jboss.seam.web.MultipartRequest;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.restlet.data.Request;

import com.noelios.restlet.ext.servlet.ServletCall;
import com.noelios.restlet.http.HttpCall;
import com.noelios.restlet.http.HttpRequest;

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
            HttpCall httpCall = httpRequest.getHttpCall();
            if (httpCall instanceof ServletCall) {
                HttpServletRequest httpServletRequest = ((ServletCall) httpCall).getRequest();
                return parseRequest(httpServletRequest);
            }
        }
        return null;
    }

    /**
     * Parses a Multipart Servlet Request to extract blobs
     */
    public static List<Blob> parseRequest(HttpServletRequest request) throws FileUploadException, IOException {
        List<Blob> blobs = new ArrayList<Blob>();

        if (request instanceof MultipartRequest) {
            MultipartRequest seamMPRequest = (MultipartRequest) request;

            Enumeration<String> names = seamMPRequest.getParameterNames();
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                try (InputStream in = seamMPRequest.getFileInputStream(name)) {
                    if (in != null) {
                        Blob blob = Blobs.createBlob(in);
                        blob.setFilename(seamMPRequest.getFileName(name));
                        blobs.add(blob);
                    }
                }
            }
        } else {
            // fallback method for non-seam servlet request
            FileUpload fu = new FileUpload(new DiskFileItemFactory());
            String fileNameCharset = request.getHeader("FileNameCharset");
            if (fileNameCharset != null) {
                fu.setHeaderEncoding(fileNameCharset);
            }
            ServletRequestContext requestContext = new ServletRequestContext(request);
            List<FileItem> fileItems = fu.parseRequest(requestContext);
            for (FileItem item : fileItems) {
                try (InputStream is = item.getInputStream()) {
                    Blob blob = Blobs.createBlob(is);
                    blob.setFilename(item.getName());
                    blobs.add(blob);
                }
            }
        }
        return blobs;
    }

}
