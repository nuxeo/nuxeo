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

package org.nuxeo.ecm.annotation;

import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_ENTITY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_ENTITY_ID;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_ID;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_ORIGIN;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_XPATH;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_AUTHOR;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_CREATION_DATE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_DOCUMENT_ID;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_MODIFICATION_DATE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_TEXT;

import java.io.IOException;

import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 10.1
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class AnnotationJsonWriter extends ExtensibleEntityJsonWriter<Annotation> {

    public static final String ENTITY_TYPE = "annotation";

    public AnnotationJsonWriter() {
        super(ENTITY_TYPE, Annotation.class);
    }

    @Override
    protected void writeEntityBody(Annotation entity, JsonGenerator jg) throws IOException {

        jg.writeStringField(COMMENT_AUTHOR, entity.getAuthor());
        jg.writeStringField(COMMENT_TEXT, entity.getText());
        jg.writeStringField(COMMENT_DOCUMENT_ID, entity.getDocumentId());
        jg.writeStringField(COMMENT_CREATION_DATE, entity.getCreationDate().toString());
        jg.writeStringField(COMMENT_MODIFICATION_DATE, entity.getModificationDate().toString());

        jg.writeStringField(ANNOTATION_ID, entity.getId());
        jg.writeStringField(ANNOTATION_XPATH, entity.getXpath());

        jg.writeStringField(ANNOTATION_ENTITY_ID, ((ExternalAnnotation) entity).getEntityId());
        jg.writeStringField(ANNOTATION_ORIGIN, ((ExternalAnnotation) entity).getOrigin());
        jg.writeStringField(ANNOTATION_ENTITY, ((ExternalAnnotation) entity).getEntity());
    }
}
