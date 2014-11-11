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

package org.nuxeo.ecm.platform.gwt.client.http;

import com.google.gwt.http.client.Header;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class HttpResponse extends Response {

    protected Response response;

    public HttpResponse(Response response) {
        this.response = response;
    }

    /**
     * @param header
     * @return
     * @see com.google.gwt.http.client.Response#getHeader(java.lang.String)
     */
    public String getHeader(String header) {
        return response.getHeader(header);
    }

    /**
     * @return
     * @see com.google.gwt.http.client.Response#getHeaders()
     */
    public Header[] getHeaders() {
        return response.getHeaders();
    }

    /**
     * @return
     * @see com.google.gwt.http.client.Response#getHeadersAsString()
     */
    public String getHeadersAsString() {
        return response.getHeadersAsString();
    }

    /**
     * @return
     * @see com.google.gwt.http.client.Response#getStatusCode()
     */
    public int getStatusCode() {
        return response.getStatusCode();
    }

    /**
     * @return
     * @see com.google.gwt.http.client.Response#getStatusText()
     */
    public String getStatusText() {
        return response.getStatusText();
    }

    /**
     * @return
     * @see com.google.gwt.http.client.Response#getText()
     */
    public String getText() {
        return response.getText();
    }
    
    public JSONValue asJSON() {
        return JSONParser.parse(getText());
    }

}
