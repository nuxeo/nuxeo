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

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.comment.api.Annotation;
import org.nuxeo.ecm.platform.comment.api.AnnotationService;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 10.1
 */
@WebAdapter(name = AnnotationAdapter.NAME, type = "annotationAdapter")
@Produces(MediaType.APPLICATION_JSON)
public class AnnotationAdapter extends DefaultAdapter {

    public static final String NAME = "annotation";

    @POST
    public Response createAnnotation(Annotation annotation) {
        AnnotationService annotationService = Framework.getService(AnnotationService.class);
        Annotation result = annotationService.createAnnotation(getContext().getCoreSession(), annotation);
        return Response.status(Response.Status.CREATED).entity(result).build();
    }

    @GET
    public List<Annotation> getAnnotations(@QueryParam("xpath") String xpath) {
        DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
        AnnotationService annotationService = Framework.getService(AnnotationService.class);
        return annotationService.getAnnotations(getContext().getCoreSession(), doc.getId(), xpath);
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
        AnnotationService annotationService = Framework.getService(AnnotationService.class);
        return annotationService.getExternalAnnotation(getContext().getCoreSession(), entityId);
    }

    @PUT
    @Path("{annotationId}")
    public Annotation updateAnnotation(@PathParam("annotationId") String annotationId, Annotation annotation) {
        AnnotationService annotationService = Framework.getService(AnnotationService.class);
        annotationService.updateAnnotation(getContext().getCoreSession(), annotationId, annotation);
        return annotation;
    }

    @PUT
    @Path("external/{entityId}")
    public Annotation updateExternalAnnotation(@PathParam("entityId") String entityId, Annotation annotation) {
        AnnotationService annotationService = Framework.getService(AnnotationService.class);
        annotationService.updateExternalAnnotation(getContext().getCoreSession(), entityId, annotation);
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
        AnnotationService annotationService = Framework.getService(AnnotationService.class);
        annotationService.deleteExternalAnnotation(getContext().getCoreSession(), entityId);
        return Response.noContent().build();
    }

}
