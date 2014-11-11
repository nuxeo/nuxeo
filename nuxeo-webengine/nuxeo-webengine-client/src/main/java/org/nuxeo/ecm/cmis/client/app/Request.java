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
 */
package org.nuxeo.ecm.cmis.client.app;

import java.util.ArrayList;
import java.util.List;

/**
 * An HTTP operation.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Request {

    protected String url;
    protected Object content;
    protected List<String> headers;
    protected List<String> params;

    public Request(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setHeader(String key, String value) {
        if (headers == null) {
            headers = new ArrayList<String>();
        }
        headers.add(key);
        headers.add(value);
    }

    public void setParameter(String key, String value) {
        if (params == null) {
            params = new ArrayList<String>();
        }
        params.add(key);
        params.add(value);
    }


    public List<String> getHeaders() {
        return headers;
    }

    public List<String> getParameters() {
        return params;
    }

    public void setContent(Object obj) {
        this.content = obj;
    }

    public Object getContent() {
        return content;
    }

}
