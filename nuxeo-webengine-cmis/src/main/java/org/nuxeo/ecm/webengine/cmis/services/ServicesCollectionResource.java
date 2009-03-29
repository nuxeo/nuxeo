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
package org.nuxeo.ecm.webengine.cmis.services;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.chemistry.repository.Repository;
import org.nuxeo.ecm.webengine.abdera.AbderaService;
import org.nuxeo.ecm.webengine.atom.CollectionResource;
import org.nuxeo.ecm.webengine.cmis.CMISWorkspaceInfo;
import org.nuxeo.ecm.webengine.cmis.adapters.StaticCollection;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@WebObject(type="CmisServices")
public class ServicesCollectionResource extends CollectionResource {
    
    protected static StaticCollection adapter;
    
    protected CMISWorkspaceInfo ws;
    protected String id;

    @Override
    protected void initialize(Object... args) {
        this.ws = (CMISWorkspaceInfo)args[0];
    }
       
    public Repository getRepository() {
        return ws.getRepository();
    }
    
    
    @GET
    public Response doGet() {
        return AbderaService.getFeed(ctx, adapter);
    }

    @Path("{id}")    
    public Resource dispatch(@PathParam("id") String id) {
        String webType = null;//registry.getService(id);
        return newObject(webType, ws);
    }

}
