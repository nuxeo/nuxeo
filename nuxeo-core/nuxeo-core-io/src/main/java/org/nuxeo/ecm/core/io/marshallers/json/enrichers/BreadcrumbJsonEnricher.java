/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.marshallers.json.enrichers;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelListJsonWriter;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.SessionWrapper;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Enrich {@link DocumentModel} Json.
 * <p>
 * Add document breadcrumb (list of all parents document) as json attachment.
 * </p>
 * <p>
 * Enable if parameter enrichers-document=breadcrumb is present.
 * </p>
 * <p>
 * Format is:
 *
 * <pre>
 * {@code
 * {
 *   "entity-type":"document",
 *   ...
 *   "contextParameters": {
 *     "breadcrumb": { see {@link DocumentModelListJsonWriter} for format }
 *   }
 * }
 * </pre>
 *
 * </p>
 *
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class BreadcrumbJsonEnricher extends AbstractJsonEnricher<DocumentModel> {

    public static final String NAME = "breadcrumb";

    public BreadcrumbJsonEnricher() {
        super(NAME);
    }

    @Override
    public void write(JsonGenerator jg, DocumentModel document) throws IOException {
        List<DocumentModel> parentDocuments = null;
        try (SessionWrapper wrapper = ctx.getSession(document)) {
            if (!wrapper.getSession().exists(document.getRef())) {
                return;
            }
            parentDocuments = wrapper.getSession().getParentDocuments(document.getRef());
        }
        DocumentModelListImpl documentList = new DocumentModelListImpl(parentDocuments);
        jg.writeFieldName(NAME);
        writeEntity(documentList, jg);
    }

}
