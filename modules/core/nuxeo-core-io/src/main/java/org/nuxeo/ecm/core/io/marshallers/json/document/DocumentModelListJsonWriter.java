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

package org.nuxeo.ecm.core.io.marshallers.json.document;

import static org.nuxeo.ecm.automation.core.util.PaginableDocumentModelList.CODEC_PARAMETER_NAME;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import org.nuxeo.ecm.automation.core.util.PaginableDocumentModelList;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.DefaultListJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * see {@link DefaultListJsonWriter}
 *
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class DocumentModelListJsonWriter extends DefaultListJsonWriter<DocumentModel> {

    public static final String ENTITY_DOCUMENT_LIST = "documents";

    public DocumentModelListJsonWriter() {
        super(ENTITY_DOCUMENT_LIST, DocumentModel.class);
    }

    @Override
    public void write(List<DocumentModel> docs, JsonGenerator jg) throws IOException {
        if (docs instanceof PaginableDocumentModelList) {
            PaginableDocumentModelList paginable = (PaginableDocumentModelList) docs;
            String codecName = paginable.getDocumentLinkBuilder();
            try (Closeable resource = ctx.wrap().with(CODEC_PARAMETER_NAME, codecName).open()) {
                super.write(docs, jg);
            }
        } else {
            super.write(docs, jg);
        }
    }

}
