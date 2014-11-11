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

import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebContext;

/**
 * An object describing an APP workspace
 * The id uniquely identify this workspace in the service. It will also be used to construct workspace path.
 * <service_url>/ws_id/col_id
 *
 *
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WorkspaceInfo {

    protected String id;
    protected String title;
    protected Map<String, CollectionInfo> collections;
    protected String resourceType;

    protected ServiceInfo serviceInfo;

    public WorkspaceInfo(String id, String title) {
        this (id, null, title);
    }

    public WorkspaceInfo(String id, String resourceType, String title) {
        this.id = id;
        this.title = title;
        this.collections = new HashMap<String, CollectionInfo>();
        this.resourceType = resourceType == null ? "atomws" : resourceType;
    }

    public ServiceInfo getServiceInfo() {
        return serviceInfo;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void addCollection(CollectionInfo col) {
        collections.put(col.getId(), col);
        col.workspaceInfo = this;
    }

    public CollectionInfo getCollection(String id) {
        return collections.get(id);
    }

    public CollectionInfo[] getCollections() {
        return collections.values().toArray(new CollectionInfo[collections.size()]);
    }

    public String getResourceType() {
        return resourceType;
    }

    public Resource getResource(WebContext ctx) {
        return ctx.newObject(resourceType, this);
    }

    protected String createUrl(String baseUrl) {
        return baseUrl + "/" + id;
    }

    public void writeTo(String baseUrl, XMLWriter xw) throws IOException {
        baseUrl = createUrl(baseUrl);
        xw.element("workspace")
            .start()
                .element("title").content(title);
        writeWorkspaceElements(baseUrl, xw);
        writeWorkspaceCollections(baseUrl, xw);
        xw.end();
    }

    protected void writeWorkspaceElements(String wsUrl, XMLWriter xw) throws IOException {
        // do nothing
    }

    protected void writeWorkspaceCollections(String wsUrl, XMLWriter xw) throws IOException {
        for (CollectionInfo col : collections.values()) {
            col.writeTo(wsUrl, xw);
        }
    }

}
