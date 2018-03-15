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

import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_DOCUMENT_ID;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_ENTITY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_ID;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_XPATH;
import static org.nuxeo.ecm.annotation.AnnotationJsonWriter.ENTITY_TYPE;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

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
        annotation.setDocumentId(jn.get(ANNOTATION_DOCUMENT_ID).textValue());
        annotation.setXpath(jn.get(ANNOTATION_XPATH).textValue());
        annotation.setEntity(jn.get(ANNOTATION_ENTITY).textValue());

        return annotation;
    }

}
