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

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@WebObject(type="atomws")
public class WorkspaceResource extends DefaultObject {

    protected WorkspaceInfo ws;

    protected void initialize(Object ... args) {
        this.ws = (WorkspaceInfo)args[0];
    }

    @Path("{segment}")
    public Object dispatch(@PathParam("segment") String segment) {
        CollectionInfo col = ws.getCollection(segment);
        if (col == null) {
            throw new WebException(404);
        }
        return col.getResource(ctx);
    }

    public WorkspaceInfo getWorkspaceInfo() {
        return ws;
    }
}
