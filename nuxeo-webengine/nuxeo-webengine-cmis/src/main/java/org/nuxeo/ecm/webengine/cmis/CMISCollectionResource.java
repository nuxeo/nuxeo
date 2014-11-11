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
package org.nuxeo.ecm.webengine.cmis;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.chemistry.repository.Repository;
import org.nuxeo.ecm.webengine.abdera.AbderaRequest;
import org.nuxeo.ecm.webengine.atom.CollectionResource;
import org.nuxeo.ecm.webengine.model.WebObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@WebObject(type="ChildrenCollection")
public class CMISCollectionResource extends CollectionResource {


    public Repository getRepository() {
        return ((CMISWorkspaceInfo)info.getWorkspaceInfo()).getRepository();
    }

    @GET
    @Override
    public Response getFeed() {
        return getFeed(getRepository().getInfo().getRootFolderId());
    }

    @POST
    @Override
    public Response postEntry() {
        return postEntry(getRepository().getInfo().getRootFolderId());
    }

    @GET
    @Path("{uid}")
    public Response getFeed(@PathParam("uid") String uid) {
        AbderaRequest.setParameter(ctx, "objectid", uid);
        return super.getFeed();
    }

    @POST
    @Path("{uid}")
    public Response postEntry(@PathParam("uid") String uid) {
        AbderaRequest.setParameter(ctx, "objectid", uid);
        return super.postEntry();
    }

}
