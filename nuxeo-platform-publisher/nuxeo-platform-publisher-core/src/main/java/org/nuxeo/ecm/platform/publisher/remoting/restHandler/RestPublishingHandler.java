/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.RemotePublicationTreeManager;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

//@Consumes("nuxeo/remotepub")
@Path("remotepublisher")
@Produces("nuxeo/remotepub;charset=UTF-8")
public class RestPublishingHandler extends DefaultObject {

    protected RemotePublicationTreeManager getPublisher() {
        return Framework.getLocalService(RemotePublicationTreeManager.class);
    }

    // List<PublishedDocument> getChildrenDocuments(PublicationNode node) throws
    // ClientException;
    @POST
    @Path("getChildrenDocuments")
    public RemotePubResult getChildrenDocuments(RemotePubParam param)
            throws ClientException {
        return new RemotePubResult(getPublisher().getChildrenDocuments(
                param.getAsNode()));
    }

    // List<PublicationNode> getChildrenNodes(PublicationNode node) throws
    // ClientException;
    @POST
    @Path("getChildrenNodes")
    public RemotePubResult getChildrenNodes(RemotePubParam param)
            throws ClientException {
        return new RemotePubResult(getPublisher().getChildrenNodes(
                param.getAsNode()));
    }

    // PublicationNode getParent(PublicationNode node);
    @POST
    @Path("getParent")
    public RemotePubResult getParent(RemotePubParam param)
            throws ClientException {
        return new RemotePubResult(getPublisher().getParent(param.getAsNode()));
    }

    // PublicationNode getNodeByPath(String sid, String path) throws
    // ClientException;
    @POST
    @Path("getNodeByPath")
    public RemotePubResult getNodeByPath(RemotePubParam param)
            throws ClientException {
        return new RemotePubResult(getPublisher().getNodeByPath(
                (String) param.getParams().get(0),
                (String) param.getParams().get(1)));
    }

    // List<PublishedDocument> getExistingPublishedDocument(String sid,
    // DocumentLocation docLoc) throws ClientException;
    @POST
    @Path("getExistingPublishedDocument")
    public RemotePubResult getExistingPublishedDocument(RemotePubParam param)
            throws ClientException {
        return new RemotePubResult(getPublisher().getExistingPublishedDocument(
                (String) param.getParams().get(0),
                (DocumentLocation) param.getParams().get(1)));
    }

    // List<PublishedDocument> getPublishedDocumentInNode(PublicationNode node)
    // throws ClientException;
    @POST
    @Path("getPublishedDocumentInNode")
    public RemotePubResult getPublishedDocumentInNode(RemotePubParam param)
            throws ClientException {
        return new RemotePubResult(getPublisher().getPublishedDocumentInNode(
                param.getAsNode()));
    }

    // PublishedDocument publish(DocumentModel doc, PublicationNode targetNode,
    // Map<String,String> params) throws ClientException;
    @POST
    @Path("publish")
    public RemotePubResult publish(RemotePubParam param) throws ClientException {
        RemotePubResult result;
        if (param.getParams().size() == 2) {
            result = new RemotePubResult(getPublisher().publish(
                    (DocumentModel) param.getParams().get(0),
                    (PublicationNode) param.getParams().get(1)));
        } else {
            result = new RemotePubResult(getPublisher().publish(
                    (DocumentModel) param.getParams().get(0),
                    (PublicationNode) param.getParams().get(1),
                    (Map<String, String>) param.getParams().get(2)));
        }
        return result;
    }

    // void unpublish(DocumentModel doc, PublicationNode targetNode) throws
    // ClientException;
    @POST
    @Path("unpublish")
    public void unpublish(RemotePubParam param) throws ClientException {
        Object object = param.getParams().get(1);
        if (object instanceof PublicationNode) {
            getPublisher().unpublish((DocumentModel) param.getParams().get(0),
                    (PublicationNode) object);
        } else if (object instanceof PublishedDocument) {
            getPublisher().unpublish((String) param.getParams().get(0),
                    (PublishedDocument) object);
        }
    }

    // Map<String,String> initRemoteSession(String treeConfigName, Map<String,
    // String> params) throws Exception;
    @POST
    @Path("initRemoteSession")
    public RemotePubResult initRemoteSession(RemotePubParam param)
            throws Exception {
        return new RemotePubResult(getPublisher().initRemoteSession(
                (String) param.getParams().get(0),
                (Map<String, String>) param.getParams().get(1)));
    }

    // void release(String sid);
    @POST
    @Path("release")
    public Response release(RemotePubParam param) {
        getPublisher().release((String) param.getParams().get(0));
        return Response.ok().build();
    }

    // void release(String sid);
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
