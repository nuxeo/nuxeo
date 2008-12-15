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

import com.google.gwt.http.client.Response;

/**
 * When a remote call returns an error HTTP code 
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ServerException extends Exception {

    private static final long serialVersionUID = 1L;

    protected Response response;
    
    public ServerException(Response response) {
        super(response.getStatusCode()+" "+response.getStatusText());
        this.response = response;
    }
    
    /**
     * Get the status code of the server response 
     */
    public int getStatusCode() {
        return response.getStatusCode(); 
    }

    /**
     * Get the server response
     * @return the response.
     */
    public Response getResponse() {
        return response;
    }
}
