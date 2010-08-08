/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.jboss.seam.web.MultipartRequest;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.restlet.data.Request;

import com.noelios.restlet.ext.servlet.ServletCall;
import com.noelios.restlet.http.HttpCall;
import com.noelios.restlet.http.HttpRequest;

/**
 * Helper to encapsulate Multipart requests parsing to extract blobs.
 * <p>
 * This helper is needed to provide the indirection between - the Apache file
 * upload based solution (5.1) and - the Seam MultiPartFilter bases solution
 * (5.1 / Seam 2.x).
 *
 * @author tiry
 */
public class FileUploadHelper {

    private FileUploadHelper() {
    }

    /**
     * Parses a Multipart Restlet Request to extract blobs.
     */
    public static List<Blob> parseRequest(Request request) throws Exception {
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
    @SuppressWarnings("unchecked")
    public static List<Blob> parseRequest(HttpServletRequest request)
            throws Exception {
        List<Blob> blobs = new ArrayList<Blob>();

        if (request instanceof MultipartRequest) {
            MultipartRequest seamMPRequest = (MultipartRequest) request;

            Enumeration<String> names = seamMPRequest.getParameterNames();
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                InputStream in = seamMPRequest.getFileInputStream(name);
                if (in != null) {
                    Blob blob = StreamingBlob.createFromStream(in);
                    blob.setFilename(seamMPRequest.getFileName(name));
                    blobs.add(blob);
                }
            }
        } else {
            // fallback method for non-seam servlet request
            FileUpload fu = new FileUpload(new DiskFileItemFactory());
            String fileNameCharset = request.getHeader("FileNameCharset");
            if (fileNameCharset != null) {
                fu.setHeaderEncoding(fileNameCharset);
            }
            ServletRequestContext requestContext = new ServletRequestContext(
                    request);
            List<FileItem> fileItems = fu.parseRequest(requestContext);
            for (FileItem item : fileItems) {
                Blob blob = StreamingBlob.createFromStream(
                        item.getInputStream()).persist();
                blob.setFilename(item.getName());
                blobs.add(blob);
            }
        }
        return blobs;
    }

}
