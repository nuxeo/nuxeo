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
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_CREATION_DATE;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_DATE;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_DOCUMENT_ID;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_FLAGS;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_ID;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_LAST_MODIFIER;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_OPACITY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_PAGE;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_POSITION;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_SECURITY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_SUBJECT;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_XPATH;
import static org.nuxeo.ecm.annotation.AnnotationJsonWriter.ENTITY_TYPE;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.util.Calendar;

import org.joda.time.Instant;
import org.nuxeo.ecm.core.io.marshallers.json.EntityJsonReader;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since 10.1
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class AnnotationJsonReader extends EntityJsonReader<Annotation> {

    public AnnotationJsonReader() {
        super(ENTITY_TYPE);
    }

    @Override
    protected Annotation readEntity(JsonNode jn) throws IOException {
        Annotation annotation = new AnnotationImpl();
        annotation.setId(jn.get(ANNOTATION_ID).textValue());
        annotation.setColor(jn.get(ANNOTATION_COLOR).textValue());

        String dateValue = jn.get(ANNOTATION_DATE).textValue();
        if (dateValue != null) {
            Calendar date = Calendar.getInstance();
            date.setTime(Instant.parse(dateValue).toDate());
            annotation.setDate(date);
        }

        annotation.setFlags(jn.get(ANNOTATION_FLAGS).textValue());
        annotation.setDocumentId(jn.get(ANNOTATION_DOCUMENT_ID).textValue());
        annotation.setXpath(jn.get(ANNOTATION_XPATH).textValue());
        annotation.setLastModifier(jn.get(ANNOTATION_LAST_MODIFIER).textValue());
        annotation.setPage(jn.get(ANNOTATION_PAGE).longValue());
        annotation.setPosition(jn.get(ANNOTATION_POSITION).textValue());

        String creationDateValue = jn.get(ANNOTATION_CREATION_DATE).textValue();
        if (creationDateValue != null) {
            Calendar creationDate = Calendar.getInstance();
            creationDate.setTime(Instant.parse(creationDateValue).toDate());
            annotation.setCreationDate(creationDate);
        }

        annotation.setOpacity(jn.get(ANNOTATION_OPACITY).doubleValue());
        annotation.setSubject(jn.get(ANNOTATION_SUBJECT).textValue());
        annotation.setSecurity(jn.get(ANNOTATION_SECURITY).textValue());

        return annotation;
    }

}
