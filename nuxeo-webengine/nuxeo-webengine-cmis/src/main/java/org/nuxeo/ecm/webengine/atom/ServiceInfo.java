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
package org.nuxeo.ecm.webengine.atom;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.webengine.model.WebContext;

/**
 * An APP service descriptor that will expose the APP resources according to the
 * layout: <code>&lt;service_url&gt;/workspace_id/collection_id</code>
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class ServiceInfo implements UrlResolver {

    protected Map<String, WorkspaceInfo> workspaces = new HashMap<String, WorkspaceInfo>();

    
    public ServiceInfo() {
        
    }
    
    public WorkspaceInfo addWorkspace(WorkspaceInfo ws) {
        workspaces.put(ws.getId(), ws);
        ws.serviceInfo = this;
        return ws;
    }

    public WorkspaceInfo getWorkspace(String id) {
        return workspaces.get(id);
    }

    public WorkspaceInfo[] getWorkspaces() {
        return workspaces.values().toArray(new WorkspaceInfo[workspaces.size()]);
    }
    
    public void writeTo(String baseUrl, XMLWriter xw) throws IOException {
        xw.element("service").xmlns("http://www.w3.org/2007/app");
        writeServiceAttributes(baseUrl, xw);
        xw.start();
        for (WorkspaceInfo wi : workspaces.values()) {
            wi.writeTo(baseUrl, xw);
        }
        xw.end();
    }
    
    protected void writeServiceAttributes(String baseUrl, XMLWriter xw) throws IOException {
        // do nothing
    }

    /**
     * Resolve an object defined by the given key and parameters to an URL.
     * Must be overrided by subclasses if needed
     */
    public String urlFor(WebContext ctx, Object key, Object param) {
        return ctx.getURL()+"/"+key.toString();
    }
    
}
