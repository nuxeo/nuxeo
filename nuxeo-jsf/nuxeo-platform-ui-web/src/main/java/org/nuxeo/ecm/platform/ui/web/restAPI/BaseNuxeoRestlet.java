/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.restAPI;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMDocumentFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.core.io.download.DownloadService.ByteRange;
import org.nuxeo.runtime.api.Framework;
import org.restlet.Restlet;
import org.restlet.data.CharacterSet;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.OutputRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.StringRepresentation;
import org.w3c.dom.Element;

import com.noelios.restlet.ext.servlet.ServletCall;
import com.noelios.restlet.http.HttpCall;
import com.noelios.restlet.http.HttpRequest;
import com.noelios.restlet.http.HttpResponse;

/**
 * Base class for Nuxeo Restlet.
 * <p>
 * Provides utility methods:
 * <ul>
 * <li>error handling
 * <li>authentication
 * <li>http request/response retrieval
 * </ul>
 *
 * @author tiry
 */
public class BaseNuxeoRestlet extends Restlet {

    // error handling

    protected static void handleError(Response res, String message) {
        DOMDocumentFactory domFactory = new DOMDocumentFactory();
        DOMDocument result = (DOMDocument) domFactory.createDocument();
        handleError(result, res, message);
    }

    protected static void handleError(Response res, Exception e) {
        DOMDocumentFactory domFactory = new DOMDocumentFactory();
        DOMDocument result = (DOMDocument) domFactory.createDocument();
        handleError(result, res, e.getMessage(), e.getClass().getCanonicalName());
    }

    protected static void handleError(DOMDocument result, Response res, Exception e) {
        handleError(result, res, e.getMessage(), e.getClass().getCanonicalName());
    }

    protected static void handleError(DOMDocument result, Response res, String message) {
        handleError(result, res, message, null);
    }

    private static void handleError(DOMDocument result, Response res, String message, String classMessage) {
        Element error = result.createElement("error");
        result.setRootElement((org.dom4j.Element) error);
        error.setAttribute("message", message);
        if (classMessage != null) {
            error.setAttribute("class", classMessage);
        }
        result.setRootElement((org.dom4j.Element) error);

        Representation rep = new StringRepresentation(result.asXML(), MediaType.APPLICATION_XML);
        rep.setCharacterSet(CharacterSet.UTF_8);
        res.setEntity(rep);
    }

    protected static HttpServletRequest getHttpRequest(Request req) {
        if (req instanceof HttpRequest) {
            HttpRequest httpRequest = (HttpRequest) req;
            HttpCall httpCall = httpRequest.getHttpCall();
            if (httpCall instanceof ServletCall) {
                return ((ServletCall) httpCall).getRequest();
            }
        }
        return null;
    }

    protected static HttpServletResponse getHttpResponse(Response res) {
        if (res instanceof HttpResponse) {
            HttpResponse httpResponse = (HttpResponse) res;
            HttpCall httpCall = httpResponse.getHttpCall();
            if (httpCall instanceof ServletCall) {
                return ((ServletCall) httpCall).getResponse();
            }
        }
        return null;
    }

    protected static String getRestletFullUrl(Request request) {
        String url = getHttpRequest(request).getRequestURL().toString();
        String qs = getHttpRequest(request).getQueryString();
        if (qs != null) {
            return url + '?' + qs;
        } else {
            return url;
        }
    }

    protected static String getQueryParamValue(Request req, String paramName, String defaultValue) {
        return req.getResourceRef().getQueryAsForm().getFirstValue(paramName, defaultValue);
    }

    /**
     * Sets the response entity to a representation that will write the blob.
     *
     * @param blob the blob
     * @param byteRange the byte range
     * @param res the response
     * @since 7.10
     */
    public void setEntityToBlobOutput(Blob blob, ByteRange byteRange, Response res) {
        res.setEntity(new OutputRepresentation(null) {
            @Override
            public void write(OutputStream out) throws IOException {
                DownloadService downloadService = Framework.getService(DownloadService.class);
                downloadService.transferBlobWithByteRange(blob, byteRange, () -> out);
            }
        });
    }

}
