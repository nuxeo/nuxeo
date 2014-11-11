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

package org.nuxeo.ecm.webengine.atom;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.abdera.protocol.server.CollectionAdapter;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.WorkspaceInfo;
import org.apache.abdera.protocol.server.WorkspaceManager;
import org.apache.abdera.protocol.server.RequestContext.Scope;
import org.apache.abdera.protocol.server.impl.SimpleWorkspaceInfo;
import org.apache.abdera.protocol.server.servlet.ServletRequestContext;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DocumentWorkspaceManager implements WorkspaceManager {

    protected Path wsRoot = new Path("/default-domain/workspaces");

    public CollectionAdapter getCollectionAdapter(RequestContext request) {
        Path path = (Path)request.getAttribute(Scope.REQUEST, "PATH_INFO");
        if (path == null) {
            String pathInfo = ((ServletRequestContext)request).getRequest().getPathInfo();
            path = new Path(pathInfo == null ? "/" : pathInfo);
        }
        if (path.segmentCount() == 0) {
            throw new IllegalStateException("path must not be empty - this should be redirected to SERVICE");
        }
        String ws = path.segment(0); // the entry
        try {
            String basePath = request.getTargetBasePath()+'/'+ws;
            DocumentCollectionAdapter ca = new DocumentCollectionAdapter(wsRoot.append(ws).toString());
            ca.setHref(ws);
            return ca;
        } catch (ClientException e) {
            return null;
        }
    }

    public Collection<WorkspaceInfo> getWorkspaces(RequestContext request) {
        Collection<WorkspaceInfo> workspaces = new ArrayList<WorkspaceInfo>();
        try {
            CoreSession session = AbderaHelper.getCoreSession(request);
            DocumentModelList list = session.getChildren(new PathRef(wsRoot.toString()));
            SimpleWorkspaceInfo wi = new SimpleWorkspaceInfo();
            wi.setTitle("Nuxeo Workspace");
            for (DocumentModel ws : list) {
                String basePath = request.getTargetBasePath()+'/'+ws.getName();
                DocumentCollectionAdapter ca = new DocumentCollectionAdapter(ws.getRef());
                ca.setHref(ws.getName());
                // add possibility to have multiple collections
                wi.addCollection(ca);
            }
            workspaces.add(wi);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return workspaces;
    }

}
