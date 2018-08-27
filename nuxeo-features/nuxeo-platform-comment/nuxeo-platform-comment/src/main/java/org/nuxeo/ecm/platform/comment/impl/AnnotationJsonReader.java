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

package org.nuxeo.ecm.platform.comment.impl;

import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_ID;
import static org.nuxeo.ecm.platform.comment.api.AnnotationConstants.ANNOTATION_ID;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_ORIGIN;
import static org.nuxeo.ecm.platform.comment.api.AnnotationConstants.ANNOTATION_XPATH;
import static org.nuxeo.ecm.platform.comment.impl.AnnotationJsonWriter.ENTITY_TYPE;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_AUTHOR_FIELD;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_CREATION_DATE_FIELD;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_DOCUMENT_ID_FIELD;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_MODIFICATION_DATE_FIELD;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_TEXT_FIELD;

import java.io.IOException;
import java.time.Instant;

import org.nuxeo.ecm.core.io.marshallers.json.EntityJsonReader;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.comment.api.Annotation;
import org.nuxeo.ecm.platform.comment.api.AnnotationImpl;
import org.nuxeo.ecm.platform.comment.api.ExternalEntity;

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

        annotation.setAuthor(jn.get(COMMENT_AUTHOR_FIELD).textValue());
        annotation.setText(jn.get(COMMENT_TEXT_FIELD).textValue());
        annotation.setDocumentId(jn.get(COMMENT_DOCUMENT_ID_FIELD).textValue());
        Instant creationDate = jn.get(COMMENT_CREATION_DATE_FIELD).textValue() != null
                ? Instant.parse(jn.get(COMMENT_CREATION_DATE_FIELD).textValue()) : null;
        annotation.setCreationDate(creationDate);
        Instant modificationDate = jn.get(COMMENT_MODIFICATION_DATE_FIELD).textValue() != null
                ? Instant.parse(jn.get(COMMENT_MODIFICATION_DATE_FIELD).textValue()) : null;
        annotation.setModificationDate(modificationDate);

        annotation.setId(jn.get(ANNOTATION_ID).textValue());
        annotation.setXpath(jn.get(ANNOTATION_XPATH).textValue());

        if (jn.has(EXTERNAL_ENTITY_ID)) {
            ((ExternalEntity) annotation).setEntityId(jn.get(EXTERNAL_ENTITY_ID).textValue());
            ((ExternalEntity) annotation).setOrigin(jn.get(EXTERNAL_ENTITY_ORIGIN).textValue());
            ((ExternalEntity) annotation).setEntity(jn.get(EXTERNAL_ENTITY).textValue());
        }

        return annotation;
    }

}
