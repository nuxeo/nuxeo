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

import org.apache.abdera.protocol.server.impl.AbstractCollectionAdapter;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebContext;

/**
 * An object describing a CMIS repository (e.g. APP workspace)
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CollectionInfo {

    protected String id;

    protected String title;

    protected String resourceType;

    protected AbstractCollectionAdapter adapter;

    protected WorkspaceInfo workspaceInfo;

    public CollectionInfo(String id, String title, AbstractCollectionAdapter adapter) {
        this (id, title, null, adapter);
    }

    public CollectionInfo(String id, String title, String resourceType, AbstractCollectionAdapter adapter) {
        this.id = id;
        this.title = title;
        this.adapter = adapter;
        this.resourceType = resourceType == null ? "atomcollection" : resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public WorkspaceInfo getWorkspaceInfo() {
        return workspaceInfo;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public AbstractCollectionAdapter getCollectionAdapter() {
        return adapter;
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
        xw.element("collection").attr("href", createUrl(baseUrl));
        writeCollectionAttributes(baseUrl, xw);
        xw.start().element("title").attr("type", "text").content(title);
        writeCollectionElements(baseUrl, xw);
        xw.end();
    }

    protected void writeCollectionAttributes(String baseUrl, XMLWriter xw)
            throws IOException {
        // do nothing
    }

    protected void writeCollectionElements(String baseUrl, XMLWriter xw)
            throws IOException {
        // do nothing
    }
}
