/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 */

package org.nuxeo.ecm.restapi.server.jaxrs.comment;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.comment.api.Annotation;
import org.nuxeo.ecm.platform.comment.api.AnnotationService;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @since 10.1
 */
@WebAdapter(name = AnnotationAdapter.NAME, type = "annotationAdapter")
@Produces(MediaType.APPLICATION_JSON)
public class AnnotationAdapter extends DefaultAdapter {

    public static final String NAME = "annotation";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final JavaType LIST_STRING_TYPE = MAPPER.getTypeFactory()
                                                           .constructCollectionType(List.class, String.class);

    @POST
    public Response createAnnotation(Annotation annotation) {
        DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
        annotation.setParentId(doc.getId());
        AnnotationService annotationService = Framework.getService(AnnotationService.class);
        // Set logged user as author
        annotation.setAuthor(getContext().getCoreSession().getPrincipal().getName());
        Annotation result = annotationService.createAnnotation(getContext().getCoreSession(), annotation);
        return Response.status(Response.Status.CREATED).entity(result).build();
    }

    @GET
    public List<Annotation> getAnnotations(@QueryParam("xpath") String xpath) {
        DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
        AnnotationService annotationService = Framework.getService(AnnotationService.class);
        return annotationService.getAnnotations(getContext().getCoreSession(), doc.getId(), xpath);
    }

    /**
     * @deprecated since 11.3, use {@link #getCommentsFromBody(String)} instead
     */
    @GET
    @Path("comments")
    @Deprecated(since = "11.3")
    public List<Comment> getComments(@QueryParam("annotationIds") List<String> annotationIds) {
        return getAllComments(annotationIds);
    }

    @POST
    @Path("comments")
    public List<Comment> getCommentsFromBody(String payload) {
        try {
            return getAllComments(MAPPER.readValue(payload, LIST_STRING_TYPE));
        } catch (JsonProcessingException e) {
            throw new NuxeoException("Unable to read payload", SC_BAD_REQUEST);
        }
    }

    protected List<Comment> getAllComments(List<String> annotationIds) {
        CommentManager commentManager = Framework.getService(CommentManager.class);
        CoreSession session = getContext().getCoreSession();
        List<Comment> comments = new ArrayList<>();
        for (String annotationId : annotationIds) {
            comments.addAll(getAllComments(annotationId, commentManager, session));
        }
        return comments;
    }

    protected List<Comment> getAllComments(String annotationId, CommentManager commentManager, CoreSession session) {
        List<Comment> allComments = new ArrayList<>();
        List<Comment> comments = commentManager.getComments(session, annotationId);
        for (Comment comment : comments) {
            allComments.addAll(getAllComments(comment.getId(), commentManager, session));
            allComments.add(comment);
        }
        return allComments;
    }

    @GET
    @Path("{annotationId}")
    public Annotation getAnnotation(@PathParam("annotationId") String annotationId) {
        AnnotationService annotationService = Framework.getService(AnnotationService.class);
        return annotationService.getAnnotation(getContext().getCoreSession(), annotationId);
    }

    @GET
    @Path("external/{entityId}")
    public Annotation getExternalAnnotation(@PathParam("entityId") String entityId) {
        DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
        AnnotationService annotationService = Framework.getService(AnnotationService.class);
        return annotationService.getExternalAnnotation(getContext().getCoreSession(), doc.getId(), entityId);
    }

    @PUT
    @Path("{annotationId}")
    public Annotation updateAnnotation(@PathParam("annotationId") String annotationId, Annotation annotation) {
        DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
        annotation.setParentId(doc.getId());
        AnnotationService annotationService = Framework.getService(AnnotationService.class);
        // Fetch original annotation author
        CoreSession session = getContext().getCoreSession();
        String author = annotationService.getAnnotation(session, annotationId).getAuthor();
        annotation.setAuthor(author);
        annotationService.updateAnnotation(session, annotationId, annotation);
        return annotation;
    }

    @PUT
    @Path("external/{entityId}")
    public Annotation updateExternalAnnotation(@PathParam("entityId") String entityId, Annotation annotation) {
        DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
        AnnotationService annotationService = Framework.getService(AnnotationService.class);
        // Fetch original annotation author
        CoreSession session = getContext().getCoreSession();
        String author = annotationService.getExternalAnnotation(session, doc.getId(), entityId).getAuthor();
        annotation.setAuthor(author);
        annotationService.updateExternalAnnotation(getContext().getCoreSession(), doc.getId(), entityId, annotation);
        return annotation;
    }

    @DELETE
    @Path("{annotationId}")
    public Response deleteAnnotation(@PathParam("annotationId") String annotationId) {
        AnnotationService annotationService = Framework.getService(AnnotationService.class);
        annotationService.deleteAnnotation(getContext().getCoreSession(), annotationId);
        return Response.noContent().build();
    }

    @DELETE
    @Path("external/{entityId}")
    public Response deleteExternalAnnotation(@PathParam("entityId") String entityId) {
        DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
        AnnotationService annotationService = Framework.getService(AnnotationService.class);
        annotationService.deleteExternalAnnotation(getContext().getCoreSession(), doc.getId(), entityId);
        return Response.noContent().build();
    }

}
