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

import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_COLOR;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_CONTENT;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_CREATION_DATE;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_DATE;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_DOCUMENT_ID;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_FLAGS;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_ID;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_INTERIOR_COLOR;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_LAST_MODIFIER;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_OPACITY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_PAGE;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_PARENT_ID;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_POSITION;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_SECURITY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_SUBJECT;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_TYPE;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_XPATH;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

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
        jg.writeStringField(ANNOTATION_ID, entity.getId());
        jg.writeStringField(ANNOTATION_TYPE, entity.getType());
        jg.writeStringField(ANNOTATION_COLOR, entity.getColor());
        jg.writeStringField(ANNOTATION_INTERIOR_COLOR, entity.getInteriorColor());

        String date = null;
        if (entity.getDate() != null) {
            date = entity.getDate().toInstant().toString();
        }
        jg.writeStringField(ANNOTATION_DATE, date);

        jg.writeStringField(ANNOTATION_FLAGS, entity.getFlags());
        jg.writeStringField(ANNOTATION_DOCUMENT_ID, entity.getDocumentId());
        jg.writeStringField(ANNOTATION_XPATH, entity.getXpath());
        jg.writeStringField(ANNOTATION_LAST_MODIFIER, entity.getLastModifier());
        jg.writeNumberField(ANNOTATION_PAGE, entity.getPage());
        jg.writeStringField(ANNOTATION_POSITION, entity.getPosition());

        String creationDate = null;
        if (entity.getCreationDate() != null) {
            creationDate = entity.getCreationDate().toInstant().toString();
        }
        jg.writeStringField(ANNOTATION_CREATION_DATE, creationDate);

        jg.writeNumberField(ANNOTATION_OPACITY, entity.getOpacity());
        jg.writeStringField(ANNOTATION_SUBJECT, entity.getSubject());
        jg.writeStringField(ANNOTATION_SECURITY, entity.getSecurity());
        jg.writeStringField(ANNOTATION_CONTENT, entity.getContent());
        jg.writeStringField(ANNOTATION_PARENT_ID, entity.getParentId());
    }
}
