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
package org.nuxeo.ecm.webengine.cmis.util;

import java.io.IOException;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.chemistry.Connection;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class Request {

    protected Connection conn;
    protected HttpServletRequest req;
    protected HttpServletResponse resp;
    protected Entry entry;
    protected String objectId;
    protected String objectPath;
    protected String type; // document, type, relation, policy
    
    protected void init(HttpServletRequest req, HttpServletResponse resp) {
        this.req = req;
        this.resp = resp;
    }
    
    public Connection getConnection() {
        if (conn == null) {
            conn = createConnection();
        }
        return conn;
    }
    
    protected abstract Connection createConnection();    
    
    public String getObjectId() {
        return objectId;
    }

    public String getObjectPath() {
        return objectPath;
    }

    public String getBaseUrl() {
        return null;
    }
    
    public Entry getRequestEntry() throws IOException {
        if (entry == null) {
            String baseUrl = getBaseUrl(); 
            Document<Entry> doc = null;
            if (baseUrl != null) {
                doc = Abdera.getInstance().getParser().parse(req.getInputStream(), baseUrl);
            } else {
                doc = Abdera.getInstance().getParser().parse(req.getInputStream());    
            }
            entry = doc.getRoot(); // in abdera they clone the doc...
        }
        return entry;
    }

    public void setResponseHeader(String key, String value) {
        
    }

    public void setResponseHeader(String key, Date value) {
        
    }

}
