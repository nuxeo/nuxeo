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

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.abdera.protocol.server.impl.AbstractCollectionAdapter;
import org.apache.chemistry.repository.Repository;
import org.nuxeo.ecm.webengine.abdera.AbderaRequest;
import org.nuxeo.ecm.webengine.abdera.AbderaService;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@WebObject(type="CmisObject")
public class ObjectResource extends DefaultObject {

    protected CMISWorkspaceInfo ws;
    protected String id;

    @Override
    protected void initialize(Object... args) {
        this.id = (String)args[0];
        this.ws = (CMISWorkspaceInfo)args[1];
        AbderaRequest.setParameter(ctx, "objectid", id);
    }

    public Repository getRepository() {
        return ws.getRepository();
    }

    public AbstractCollectionAdapter getCollectionAdapter() {
        return ws.getCollection("children").getCollectionAdapter();
    }

    @GET
    public Response doGet() {
        return AbderaService.getEntry(ctx, getCollectionAdapter());
    }

    @PUT
    @Consumes("application/atom+xml;type=entry")
    public Response doPut() {
        return AbderaService.putEntry(ctx, getCollectionAdapter());
    }

    @POST
    @Consumes("application/atom+xml;type=entry")
    public Response doPost() {     
        return AbderaService.postEntry(ctx, getCollectionAdapter());
    }
    
    @DELETE    
    public Response doDelete() {
        return AbderaService.deleteEntry(ctx, getCollectionAdapter());
    }

    @HEAD
    public Response doHead() {
        return AbderaService.headEntry(ctx, getCollectionAdapter());
    }

    //TODO implement OPTIONS annotation
//    @OPTIONS
//    public Response doOptions() {
//        return AbderaService.optionsEntry(ctx, getCollectionAdapter());
//    }


    //TODO: delegate media requests to a media web object instead of handling media here?

    @GET
    @Path("files/{fileid}")
    public Response doGetFile(@PathParam("fileid") String fileid) {
        AbderaRequest.setParameter(ctx, "fileid", fileid);
        return AbderaService.getMedia(ctx, getCollectionAdapter());
    }

    @POST
    @Path("files/{fileid}")
    public Response doPostFile(@PathParam("fileid") String fileid) {
        AbderaRequest.setParameter(ctx, "fileid", fileid);
        return AbderaService.postMedia(ctx, getCollectionAdapter());
    }

    @PUT
    @Path("files/{fileid}")
    public Response doPutFile(@PathParam("fileid") String fileid) {
        AbderaRequest.setParameter(ctx, "fileid", fileid);
        return AbderaService.putMedia(ctx, getCollectionAdapter());
    }

    @DELETE
    @Path("files/{fileid}")
    public Response doDeleteFile(@PathParam("fileid") String fileid) {
        AbderaRequest.setParameter(ctx, "fileid", fileid);
        return AbderaService.deleteMedia(ctx, getCollectionAdapter());
    }

    @HEAD
    @Path("files/{fileid}")
    public Response doHeadFile(@PathParam("fileid") String fileid) {
        AbderaRequest.setParameter(ctx, "fileid", fileid);
        return AbderaService.headMedia(ctx, getCollectionAdapter());
    }

//    @OPTIONS
//    @Path("files/{fileid}")
//    public Response doOptionsFile(@PathParam("fileid") String fileid) {
//        AbderaRequest.setParameter(ctx, "fileid", fileid);
//        return AbderaService.optionsMedia(ctx, getCollectionAdapter());
//    }

}
