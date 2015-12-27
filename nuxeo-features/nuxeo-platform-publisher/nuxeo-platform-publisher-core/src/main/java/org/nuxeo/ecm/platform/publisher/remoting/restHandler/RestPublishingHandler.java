/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.remoting.restHandler;

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.RemotePublicationTreeManager;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

@Path("remotepublisher")
@Produces("nuxeo/remotepub;charset=UTF-8")
public class RestPublishingHandler extends DefaultObject {

    protected RemotePublicationTreeManager getPublisher() {
        return Framework.getLocalService(RemotePublicationTreeManager.class);
    }

    @POST
    @Path("getChildrenDocuments")
    public RemotePubResult getChildrenDocuments(RemotePubParam param) {
        return new RemotePubResult(getPublisher().getChildrenDocuments(param.getAsNode()));
    }

    @POST
    @Path("getChildrenNodes")
    public RemotePubResult getChildrenNodes(RemotePubParam param) {
        return new RemotePubResult(getPublisher().getChildrenNodes(param.getAsNode()));
    }

    @POST
    @Path("getParent")
    public RemotePubResult getParent(RemotePubParam param) {
        return new RemotePubResult(getPublisher().getParent(param.getAsNode()));
    }

    @POST
    @Path("getNodeByPath")
    public RemotePubResult getNodeByPath(RemotePubParam param) {
        return new RemotePubResult(getPublisher().getNodeByPath((String) param.getParams().get(0),
                (String) param.getParams().get(1)));
    }

    @POST
    @Path("getExistingPublishedDocument")
    public RemotePubResult getExistingPublishedDocument(RemotePubParam param) {
        return new RemotePubResult(getPublisher().getExistingPublishedDocument((String) param.getParams().get(0),
                (DocumentLocation) param.getParams().get(1)));
    }

    @POST
    @Path("getPublishedDocumentInNode")
    public RemotePubResult getPublishedDocumentInNode(RemotePubParam param) {
        return new RemotePubResult(getPublisher().getPublishedDocumentInNode(param.getAsNode()));
    }

    @POST
    @Path("publish")
    public RemotePubResult publish(RemotePubParam param) {
        RemotePubResult result;
        if (param.getParams().size() == 2) {
            result = new RemotePubResult(getPublisher().publish((DocumentModel) param.getParams().get(0),
                    (PublicationNode) param.getParams().get(1)));
        } else {
            result = new RemotePubResult(getPublisher().publish((DocumentModel) param.getParams().get(0),
                    (PublicationNode) param.getParams().get(1), (Map<String, String>) param.getParams().get(2)));
        }
        return result;
    }

    @POST
    @Path("unpublish")
    public void unpublish(RemotePubParam param) {
        Object object = param.getParams().get(1);
        if (object instanceof PublicationNode) {
            getPublisher().unpublish((DocumentModel) param.getParams().get(0), (PublicationNode) object);
        } else if (object instanceof PublishedDocument) {
            getPublisher().unpublish((String) param.getParams().get(0), (PublishedDocument) object);
        }
    }

    @POST
    @Path("initRemoteSession")
    public RemotePubResult initRemoteSession(RemotePubParam param) {
        return new RemotePubResult(getPublisher().initRemoteSession((String) param.getParams().get(0),
                (Map<String, String>) param.getParams().get(1)));
    }

    @POST
    @Path("release")
    public Response release(RemotePubParam param) {
        getPublisher().release((String) param.getParams().get(0));
        return Response.ok().build();
    }

    @GET
    @Path("release/{sid}")
    public Response release(@PathParam("sid") String sid) {
        getPublisher().release(sid);
        return Response.ok().build();
    }

    @GET
    @Path("ping")
    public String ping() {
        return "pong";
    }

    @GET
    @Path("superPing")
    public RemotePubResult superPing() {
        return new RemotePubResult("pong");
    }

}
