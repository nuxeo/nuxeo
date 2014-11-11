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

import com.google.gwt.http.client.RequestBuilder;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Server {

    public static HttpRequest get(String uri) {
        return new HttpRequest(RequestBuilder.GET, uri);
    }

    public static HttpRequest post(String uri) {
        return new HttpRequest(RequestBuilder.POST, uri);
    }
    
    
    
//  public static Request put(String uri) {
//  return new Request(RequestBuilder.GET, uri);
//}
//
//public static Request delete(String uri) {
//  return new Request(RequestBuilder.GET, uri);
//}
//
//public static Request head(String uri) {
//  return new Request(RequestBuilder.GET, uri);
//}

    
}
