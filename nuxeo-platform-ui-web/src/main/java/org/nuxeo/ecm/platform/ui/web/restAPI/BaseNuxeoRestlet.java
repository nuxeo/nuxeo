/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.restAPI;

import java.io.Serializable;
import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMDocumentFactory;
import org.restlet.Restlet;
import org.restlet.data.CharacterSet;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
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

    private static void handleError(DOMDocument result, Response res,
            String message, String classMessage) {
        Element error = result.createElement("error");
        result.setRootElement((org.dom4j.Element) error);
        error.setAttribute("message", message);
        if (classMessage != null) {
            error.setAttribute("class", classMessage);
        }
        result.setRootElement((org.dom4j.Element) error);

        Representation rep = new StringRepresentation(result.asXML(),
                MediaType.APPLICATION_XML);
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

    protected static Principal getUserPrincipal(Request req) {
        HttpServletRequest httpServletRequest = getHttpRequest(req);
        if (httpServletRequest == null) {
            return null;
        }
        return httpServletRequest.getUserPrincipal();
    }

    protected static Serializable getSerializablePrincipal(Request req) {
        Principal principal = getUserPrincipal(req);
        if (principal instanceof Serializable) {
            return (Serializable) principal;
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

    protected static String getQueryParamValue(Request req,
            String paramName, String defaultValue) {
        return req.getResourceRef().getQueryAsForm().getFirstValue(paramName, defaultValue);
    }

}
